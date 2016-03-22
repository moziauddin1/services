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
        log.debug "applying instance APC comments and distribution text to the APC tree"

        /**
         * find instances where the comment or apc distribution text does not match the text of the current APC tree node
         * if there are any, then {*   make a workspace and an event.
         *   Check out the APC root into it.
         *   For each instance needing to be fixed {*     check it out into the ws if it is not already checked out
         *     fix the profile items needing to be fixed
         *}*   bulk versioning
         *}* return number of instances fixed up.
         */

        Collection<ApcData> fixups = getFixups();

        if (fixups.isEmpty()) {
            log.debug "No fixups needed."
            return "No fixups needed."
        }

        log.debug "${fixups.size()} fixups needed."

        Arrangement apc = Arrangement.findByNamespaceAndLabel(namespace, classificationTreeLabel);

        if (!apc) {
            throw new IllegalStateException('No APC tree?')
        }

        log.info "transferring profile data in ${apc}"

        log.debug "temp arrangement"
        Arrangement tempSpace = basicOperationsService.createTemporaryArrangement(namespace)
        apc = DomainUtils.refetchArrangement(apc)
        Link topLink = basicOperationsService.adoptNode(tempSpace.node, DomainUtils.getSingleSubnode(apc.node), VersioningMethod.F)
        basicOperationsService.checkoutLink(topLink)

        fixups.each { ApcData fixup -> fixupProfileItem(fixup, tempSpace) }

        // OK! Now to fix up the profile data!

        log.debug "event"
        Event transferEvent = basicOperationsService.newEvent(namespace, 'Transfer instance profile data to the APC tree')

        log.debug "persist"
        basicOperationsService.persistNode(transferEvent, tempSpace.node)

        log.debug "version"
        Map<Node, Node> v = versioningService.getStandardVersioningMap(tempSpace, apc)

        versioningService.performVersioning(transferEvent, v, apc)
        log.debug "cleanup"
        basicOperationsService.moveFinalNodesFromTreeToTree(tempSpace, apc)
        basicOperationsService.deleteArrangement(tempSpace)
        log.debug "done"

        return "${fixups.size()} fixups performed."
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
with zz as (
select tax.id node_id, instance.id instance_id, instance.name_id name_id,
  (select max(value)
    from instance_note n
    join instance_note_key nk on n.instance_note_key_id=nk.id
    where n.instance_id = instance.id
    and nk.name='APC Comment\'
  ) as inst_apc_comment,
  (select max(value)
    from instance_note n
    join instance_note_key nk on n.instance_note_key_id=nk.id
    where n.instance_id = instance.id
    and nk.name='APC Dist.\'
  ) as inst_apc_dist,
  (select max(lit.literal)
    from tree_link
    join tree_uri_ns ns on  tree_link.type_uri_ns_part_id = ns.id
    join tree_node lit on tree_link.subnode_id = lit.id
    where
        tax.id = tree_link.supernode_id
    and ns.label = 'apc-voc\'
    and tree_link.type_uri_id_part = 'comment\'
    and lit.internal_type = 'V\'
    ) as tree_apc_comment,
  (select max(lit.literal)
    from tree_link
    join tree_uri_ns ns on  tree_link.type_uri_ns_part_id = ns.id
    join tree_node lit on tree_link.subnode_id = lit.id
    where
        tax.id = tree_link.supernode_id
    and ns.label = 'apc-voc\'
    and tree_link.type_uri_id_part = 'distribution\'
    and lit.internal_type = 'V\'
    ) as tree_apc_dist
  from tree_arrangement apc_tree
  join tree_node tax on apc_tree.id = tax.tree_arrangement_id
  join instance on tax.instance_id = instance.id
  where apc_tree.label = 'APC\'
    and tax.checked_in_at_id is not null
    and tax.replaced_at_id is null
)
select
    case when zz.inst_apc_comment is null then '***NULLL***' else zz.inst_apc_comment end
    <>
    case when zz.tree_apc_comment is null then '***NULLL***' else zz.tree_apc_comment end
      COMMENT_DIFF,
    case when zz.inst_apc_dist is null then '***NULLL***' else zz.inst_apc_dist end
    <>
    case when zz.tree_apc_dist is null then '***NULLL***' else zz.tree_apc_dist end
      DIST_DIFF,
zz.* from zz
where
    case when zz.inst_apc_comment is null then '***NULLL***' else zz.inst_apc_comment end
    <>
    case when zz.tree_apc_comment is null then '***NULLL***' else zz.tree_apc_comment end
  or
    case when zz.inst_apc_dist is null then '***NULLL***' else zz.inst_apc_dist end
    <>
    case when zz.tree_apc_dist is null then '***NULLL***' else zz.tree_apc_dist end
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
