/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.*
import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.jdbc.Work

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

/**
 * This class contains code that is specific to the APC tree in NSL.
 * Code in this class operates at the level of TreeOperationsService. However, it is not in
 * TreeOperationsService because it is specific to the data we have here at ANBG.
 * Created by ibis on 3/06/15.
 */
@Transactional
class ApcTreeService {
    GrailsApplication grailsApplication
    SessionFactory sessionFactory_nsl
    BasicOperationsService basicOperationsService;
    QueryService queryService
    VersioningService versioningService;

    static class ApcData {
        Long node_id;
        Long instance_id;
        Long name_id;
        String inst_apc_comment;
        String inst_apc_dist;
        String tree_apc_comment;
        String tree_apc_dist;
        boolean comment_diff;
        boolean dist_diff;
    }

    def transferApcProfileData(Namespace namespace, String classificationTreeLabel) {
        log.info "applying instance APC comments and distribution text to the APC tree"

        sessionFactory_nsl.getCurrentSession().doWork(new Work() {
            void execute(Connection connection) throws SQLException {
                Closure getN = { sql ->
                    ResultSet rs = connection.createStatement().executeQuery(sql);
                    rs.next();
                    int n = rs.getInt(1);
                    rs.close();
                    return n;
                }

                int eventId = getN('''select id from tree_event where note = 'APC import - create empty classification' ''')
                int apcId = getN('''select id from tree_arrangement where label = 'APC' ''')
                int apcNsId = getN('''select id from tree_uri_ns where label = 'apc-voc' ''')
                int xsNsId = getN('''select id from tree_uri_ns where label = 'xs' ''')

                log.debug("APC tree creation event is ${eventId}")
                log.debug("APC tree is ${apcId}")
                log.debug("APC vocabulary is ${apcNsId}")
                log.debug("XML vocabulary is ${xsNsId}")


                log.debug "deleting links"
                connection.createStatement().execute('''
DELETE FROM tree_link l
USING
    tree_node subnode JOIN tree_arrangement sub_a ON subnode.tree_arrangement_id = sub_a.id
WHERE
    l.subnode_id = subnode.id
    AND l.type_uri_id_part IN ('comment', 'distribution')
    AND subnode.internal_type = 'V'
'''
                )

                log.debug "deleting nodes"
                connection.createStatement().execute('''
DELETE FROM tree_node n
WHERE n.internal_type = 'V'
AND NOT exists (
  SELECT l.id FROM tree_link l WHERE l.subnode_id = n.id
)
'''
                )

                /*
                    Ok, time to extract the notes. I will make one value node for each relevant instance_note.
                    They could be combined, but I think it would confuse people.
                 */

                log.debug "creating temp table"
                connection.createStatement().execute('''
DROP TABLE IF  EXISTS tmp_instance_note_nodes
'''
                );

                connection.createStatement().execute('''
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_instance_note_nodes (
instance_note_id BIGINT PRIMARY KEY,
instance_id BIGINT,
note_key CHARACTER VARYING(255) NOT NULL,
node_id BIGINT NOT NULL
)
ON COMMIT DELETE ROWS'''
                );

                log.debug "populating temp table"
                // this is not frequently executed, so I'll just put the ids in the string with groovy
                connection.createStatement().execute("""
insert into tmp_instance_note_nodes
select instance_note.id,
instance_note.instance_id,
instance_note_key.name,
nextval('nsl_global_seq')
from instance_note join instance_note_key on instance_note.instance_note_key_id = instance_note_key.id
where instance_note_key.name in ('APC Comment', 'APC Dist.')
and exists (
  select tree_node.id
  from tree_node
    join tree_arrangement on tree_node.tree_arrangement_id = tree_arrangement.id
  where tree_node.instance_id = instance_note.instance_id
  and (tree_arrangement.id = ${apcId} or tree_arrangement.base_arrangement_id = ${apcId})
)
"""
                );

                ResultSet rs = connection.createStatement().executeQuery("SELECT count(*) n FROM tmp_instance_note_nodes ")
                rs.next()
                log.debug "${rs.getInt('n')} values found"
                rs.close()

                log.debug "creating value nodes"
                connection.createStatement().execute("""
insert into tree_node(
  id,
  lock_version,
  checked_in_at_id,
  internal_type,
  is_synthetic,
  literal,
  tree_arrangement_id,
  type_uri_ns_part_id,
  type_uri_id_part
)
select
  nn.node_id,--id,
  1,--lock_version,
  ${eventId},--checked_in_at_id,
  'V',--internal_type,
  'N',
  instance_note.value,--literal,
  ${apcId},--tree_arrangement_id,
  case nn.note_key when 'APC Comment' then ${xsNsId} when 'APC Dist.' then ${apcNsId} end,--type_uri_ns_part_id,
  case nn.note_key when 'APC Comment' then 'string' when 'APC Dist.' then 'distributionstring' end--type_uri_id_part
from tmp_instance_note_nodes nn join instance_note on nn.instance_note_id = instance_note.id
"""
                );

                log.debug "creating links to value nodes"
                connection.createStatement().execute("""
insert into tree_link(
  id,
  lock_version,
  link_seq,
  supernode_id,
  subnode_id,
  is_synthetic,
  type_uri_ns_part_id,
  type_uri_id_part,
  versioning_method
)
select
  nextval('nsl_global_seq'),--id,
  1,--lock_version,
  currval('nsl_global_seq'),--link_seq,
  n.id,--supernode_id,
  nn.node_id,--subnode_id,
  'N',--is_synthetic,
  ${apcNsId},--type_uri_ns_part_id,
  case nn.note_key when 'APC Comment' then 'comment' when 'APC Dist.' then 'distribution' end,--type_uri_id_part,
  'F'--versioning_method
from tree_arrangement a
join tree_node n on a.id = n.tree_arrangement_id
join tmp_instance_note_nodes nn on n.instance_id = nn.instance_id
where (a.id = ${apcId} or a.base_arrangement_id = ${apcId})
"""
                );


                log.debug "dropping temp table"
                connection.createStatement().execute('''
DROP TABLE IF  EXISTS instance_note_nodes
'''
                );

            }
        })

        return "All comments and distributions reset."
    }

    private List<ApcData> getFixups() {
        List<ApcData> fixups = new ArrayList<ApcData>();

        Session s = sessionFactory_nsl.getCurrentSession()
        s.doWork(new Work() {
            @Override
            void execute(Connection connection) throws SQLException {
                Statement stmt = connection.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery('''
WITH zz AS (
SELECT tax.id node_id, instance.id instance_id, instance.name_id name_id,
  (SELECT max(value)
    FROM instance_note n
    JOIN instance_note_key nk ON n.instance_note_key_id=nk.id
    WHERE n.instance_id = instance.id
    AND nk.name='APC Comment\'
  ) AS inst_apc_comment,
  (SELECT max(value)
    FROM instance_note n
    JOIN instance_note_key nk ON n.instance_note_key_id=nk.id
    WHERE n.instance_id = instance.id
    AND nk.name='APC Dist.\'
  ) AS inst_apc_dist,
  (SELECT max(lit.literal)
    FROM tree_link
    JOIN tree_uri_ns ns ON  tree_link.type_uri_ns_part_id = ns.id
    JOIN tree_node lit ON tree_link.subnode_id = lit.id
    WHERE
        tax.id = tree_link.supernode_id
    AND ns.label = 'apc-voc\'
    AND tree_link.type_uri_id_part = 'comment\'
    AND lit.internal_type = 'V\'
    ) AS tree_apc_comment,
  (SELECT max(lit.literal)
    FROM tree_link
    JOIN tree_uri_ns ns ON  tree_link.type_uri_ns_part_id = ns.id
    JOIN tree_node lit ON tree_link.subnode_id = lit.id
    WHERE
        tax.id = tree_link.supernode_id
    AND ns.label = 'apc-voc\'
    AND tree_link.type_uri_id_part = 'distribution\'
    AND lit.internal_type = 'V\'
    ) AS tree_apc_dist
  FROM tree_arrangement apc_tree
  JOIN tree_node tax ON apc_tree.id = tax.tree_arrangement_id
  JOIN instance ON tax.instance_id = instance.id
  WHERE apc_tree.label = 'APC\'
    AND tax.checked_in_at_id IS NOT NULL
    AND tax.replaced_at_id IS NULL
)
SELECT
    CASE WHEN zz.inst_apc_comment IS NULL THEN '***NULLL***' ELSE zz.inst_apc_comment END
    <>
    CASE WHEN zz.tree_apc_comment IS NULL THEN '***NULLL***' ELSE zz.tree_apc_comment END
      COMMENT_DIFF,
    CASE WHEN zz.inst_apc_dist IS NULL THEN '***NULLL***' ELSE zz.inst_apc_dist END
    <>
    CASE WHEN zz.tree_apc_dist IS NULL THEN '***NULLL***' ELSE zz.tree_apc_dist END
      DIST_DIFF,
zz.* FROM zz
WHERE
    CASE WHEN zz.inst_apc_comment IS NULL THEN '***NULLL***' ELSE zz.inst_apc_comment END
    <>
    CASE WHEN zz.tree_apc_comment IS NULL THEN '***NULLL***' ELSE zz.tree_apc_comment END
  OR
    CASE WHEN zz.inst_apc_dist IS NULL THEN '***NULLL***' ELSE zz.inst_apc_dist END
    <>
    CASE WHEN zz.tree_apc_dist IS NULL THEN '***NULLL***' ELSE zz.tree_apc_dist END
''');
                    try {
                        while (rs.next()) {
                            ApcData d = new ApcData();
                            d.node_id = rs.getLong('node_id');
                            if (rs.wasNull()) d.node_id = null;
                            d.instance_id = rs.getLong('node_id');
                            if (rs.wasNull()) d.instance_id = null;
                            d.name_id = rs.getLong('node_id');
                            if (rs.wasNull()) d.name_id = null;

                            d.inst_apc_comment = rs.getString('inst_apc_comment')
                            d.inst_apc_dist = rs.getString('inst_apc_dist')
                            d.tree_apc_comment = rs.getString('tree_apc_comment')
                            d.tree_apc_dist = rs.getString('tree_apc_dist')

                            d.comment_diff = rs.getBoolean('comment_diff');
                            d.dist_diff = rs.getBoolean('dist_diff');

                            fixups.add(d);
                        }

                    } finally {
                        rs.close();
                    }
                } finally {
                    stmt.close()
                }

            }
        });

        return fixups;

    }

    private fixupProfileItem(ApcData fixup, Arrangement tempSpace) {
        // ok either check out the node and grab the checked out node, or
        // find the node already checked out.

        if (fixup.comment_diff) log.debug("set comment on ${fixup.instance_id} from ${fixup.tree_apc_comment} to ${fixup.inst_apc_comment}")
        if (fixup.dist_diff) log.debug("set dist on ${fixup.instance_id} from ${fixup.tree_apc_dist} to ${fixup.inst_apc_dist}")

        Uri commentLinkUri = new Uri(DomainUtils.ns('apc-voc'), 'comment');
        Uri commentTypeUri = new Uri(DomainUtils.ns('xs'), 'string');
        Uri distLinkUri = new Uri(DomainUtils.ns('apc-voc'), 'distribution');
        Uri distTypeUri = new Uri(DomainUtils.ns('apc-voc'), 'distributionstring');

        Link fixupLink = queryService.findNodeCurrentOrCheckedout(tempSpace.node, Node.get(fixup.node_id));

        if (!fixupLink) {
            throw new IllegalStateException("Tempspace does not contain node ${fixup.node_id}");
        }

        Node checkedOut;

        if (DomainUtils.isCheckedIn(fixupLink.subnode)) {
            checkedOut = basicOperationsService.checkoutNode(tempSpace.node, fixupLink.subnode)
        } else {
            checkedOut = fixupLink.subnode;
        }

        if (fixup.comment_diff) {
            if (fixup.tree_apc_comment) {
                // we do the each *first* so that the link deletions dont to odd things to the collection
                checkedOut.subLink.findAll { Link profileLink ->
                    DomainUtils.getLinkTypeUri(profileLink) == commentLinkUri
                }.each { Link profileLink ->
                    profileLink = DomainUtils.refetchLink(profileLink);
                    basicOperationsService.deleteLink(profileLink.supernode, profileLink.linkSeq)
                }
            }

            if (fixup.inst_apc_comment) {
                checkedOut = DomainUtils.refetchNode(checkedOut)

                basicOperationsService.createDraftNode(checkedOut, VersioningMethod.F, NodeInternalType.V,
                        nodeType: commentTypeUri,
                        linkType: commentLinkUri,
                        literal: fixup.inst_apc_comment
                );
            }
        }

        checkedOut = DomainUtils.refetchNode(checkedOut)

        if (fixup.dist_diff) {
            if (fixup.tree_apc_dist) {
                checkedOut = DomainUtils.refetchNode(checkedOut)
                // we do the each *first* so that the link deletions dont to odd things to the collection
                checkedOut.subLink.findAll { Link profileLink ->
                    DomainUtils.getLinkTypeUri(profileLink) == distLinkUri
                }.each { Link profileLink ->
                    profileLink = DomainUtils.refetchLink(profileLink);
                    basicOperationsService.deleteLink(profileLink.supernode, profileLink.linkSeq)
                }
            }

            if (fixup.inst_apc_dist) {
                checkedOut = DomainUtils.refetchNode(checkedOut)

                basicOperationsService.createDraftNode(checkedOut, VersioningMethod.F, NodeInternalType.V,
                        nodeType: distTypeUri,
                        linkType: distLinkUri,
                        literal: fixup.inst_apc_dist
                );
            }
        }

    }


}
