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
    def configService

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

    Node hasThisBeenOnTheTree(Instance instance, Arrangement tree){
        Node.findByInstanceAndRoot(instance, tree)
    }

    String distribution(Instance instance) {
        valueNodeFromTree(instance, 'distribution', configService.classificationTree)
    }

    String comment(Instance instance) {
        valueNodeFromTree(instance, 'comment', configService.classificationTree)
    }

    String valueNodeFromTree(Instance instance, String type, Arrangement tree) {
        List<Node> valueNodes = Node.executeQuery("""
select vnode 
from Node n, Link vlink, Node vnode 
where n.checkedInAt is not null 
and n.next is null 
and n.internalType = 'T' 
and n.root = :root
and n.instance = :instance
and n = vlink.supernode and vlink.typeUriIdPart = :valueType
and vlink.subnode = vnode
""", [root: tree, valueType: type, instance: instance])

        if (valueNodes && !valueNodes.empty) {
            return valueNodes.first().literal
        }
        return null
    }

}
