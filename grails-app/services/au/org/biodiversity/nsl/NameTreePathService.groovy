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

import grails.transaction.Transactional
import groovy.sql.Sql

/**
 * NameTreePaths are a view of a tree linking directly to Names. They contain the tree path from root to the current name
 * as a string of name IDs and nameTreePath ids.
 *
 * This inverts the structure of the tree as compared to the tree nodes and links and allows you to do rapid queries to
 * get the set of names under a particular name on the tree by finding all the nameTreePaths that start with this path.
 *
 * The name tree path namePath has been set up as a sort key for sorting the output
 *
 * The job of this service is to keep this view of the tree up to date. NameTreePaths only represent the current tree for
 * now to optimise the most common operations.
 *
 */
@Transactional
class NameTreePathService {

    def configService
    def classificationService

    /**
     * update or create a NameTreePath for a Node.
     *
     * @param currentNode
     * @return
     */
    NameTreePath updateNameTreePathFromNode(Node currentNode) {
        log.debug "update name tree path for $currentNode"
        NameTreePath currentNtp = findCurrentNameTreePath(currentNode.name, currentNode.root)
        updateNameTreePathFromNode(currentNode, currentNtp)
    }

    /**
     * update or create a NameTreePath for a Node.
     *
     * @param currentNode
     * @param currentNtp
     * @return the new current NameTreePath for the node.
     */
    NameTreePath updateNameTreePathFromNode(Node currentNode, NameTreePath currentNtp) {
        //there may be a current name tree path we need to get it and all it's children to update
        log.debug "update name tree path for $currentNode, $currentNtp"
        Name name = currentNode.name
        if (!currentNtp) {
            return addNameTreePath(name, currentNode)
        }
        Name currentParentName = getCurrentParentName(currentNode)
        NameTreePath parentNtp = null
        if (currentParentName) {
            parentNtp = findCurrentNameTreePath(currentParentName, currentNode.root)
        }
        if (currentNtp.parent?.id != parentNtp?.id) {
            String oldNameIdPath = currentNtp.nameIdPath
            String oldRankPath = currentNtp.rankPath
            String oldNamePath = currentNtp.namePath
            currentNtp.nameIdPath = (parentNtp ? "${parentNtp.nameIdPath}." : '') + currentNode.name.id as String
            currentNtp.rankPath = (parentNtp ? "${parentNtp.rankPath}>" : '') + "${name.nameRank.name}:${name.nameElement}"
            currentNtp.parent = parentNtp
            currentNtp.inserted = System.currentTimeMillis()
            currentNtp.save(flush: true)
            updateChildren(oldNameIdPath, oldRankPath, oldNamePath, currentNtp)
        }
        return currentNtp
    }

    private Name getCurrentParentName(Node node) {
        List<Name> nameList = classificationService.getPath(node.name, node.root)
        if (nameList.size() > 1) {
            return nameList[-2]
        }
        return null
    }

    private void updateChildren(String oldNameIdPath, String oldRankPath, String oldNamePath, NameTreePath newParent) {
        List<NameTreePath> children = currentChildren(newParent) //in order from top of the tree (!important)
        NameTreePath.withSession { session ->
            children.each { NameTreePath child ->
                log.debug "updating $child.name child name tree path."
                child.nameIdPath = newParent.nameIdPath + (child.nameIdPath - oldNameIdPath)
                child.rankPath = newParent.rankPath + (child.rankPath - oldRankPath)
                child.namePath = newParent.namePath + (child.namePath - oldNamePath)
                if(newParent.family) {
                    child.family = newParent.family
                }
            }
            session.flush()
        }
    }

    /**
     * Get the current nodes in the NameTreePath branch by name. This is a synthetic method of getting the  nodes in
     * the branch to replace storing the nodeIdPath because the tree code copies nodes above a change to new nodes to
     * track changes this means we would have to update the nodeIdPath of every NameTreePath for nodes attached back to
     * say Plantae, which defeats the purpose.
     */
    List<Node> getCurrentNodesInBranch(NameTreePath ntp) {
        ntp.namesInBranch().collect { Name name ->
            classificationService.isNameInClassification(name, ntp.tree)
        }
    }

    /**
     * get the current children of a NameTreePath in order of path elements size, i.e. paths closest to the root of the
     * tree (the treePath passed in).
     *
     * This will not return the parent tree path in the results
     *
     * @param parentTreePath
     * @return
     */
    List<NameTreePath> currentChildren(NameTreePath parentTreePath) {
        NameTreePath.findAllByNameIdPathLikeAndTreeAndNextIsNull("%${parentTreePath.name.id}.%", parentTreePath.tree)
                    .sort { a, b ->
            a.namePathIds().size() <=> b.namePathIds().size()
        }
    }


    void removeNameTreePath(Name name, Arrangement arrangement) {
        NameTreePath ntp = findCurrentNameTreePath(name, arrangement)
        log.debug "remove name tree path for $name, $ntp for tree $arrangement.label"

        if (ntp) {
            if (!currentChildren(ntp).isEmpty()) {
                throw new Exception("Can't delete Name Tree Path with children. $ntp")
            }
            ntp.delete()
        }
    }

    /**
     * Make a new NameTreePath for a name using this tree node as it's reference. This method uses the current NameTreePath
     * of the name.parent property as the parent.
     *
     * If the node doesn't exist no NameTreePath is created and null is returned.
     * @param name
     * @param node
     * @return
     */
    NameTreePath addNameTreePath(Name name, Node node) {
        NameTreePath nameTreePath = makeNameTreePath(name, node)
        if (nameTreePath) {
            nameTreePath.save(flush: true)
            log.debug "Added NameTreePath ${nameTreePath.dump()}"
            return nameTreePath
        }
        return null
    }

    NameTreePath makeNameTreePath(Name name, Node node) {

        if (node) {
            Name currentParentName = getCurrentParentName(node)
            NameTreePath parentNameTreePath = null
            if (currentParentName) {
                parentNameTreePath = findCurrentNameTreePath(currentParentName, node.root)
                if (!parentNameTreePath) {
                    log.debug "${name}'s parent doesn't have a name tree path, attempting to add it."
                    Node parentNode = classificationService.isNameInClassification(currentParentName, node.root)
                    if (parentNode) {
                        parentNameTreePath = addNameTreePath(name.parent, parentNode)
                    }
                }
            }

            NameTreePath nameTreePath = new NameTreePath()
            nameTreePath.tree = node.root
            nameTreePath.inserted = System.currentTimeMillis()
            nameTreePath.name = name
            if (parentNameTreePath) {
                nameTreePath.parent = parentNameTreePath
                nameTreePath.nameIdPath = "${parentNameTreePath.nameIdPath}.${name.id}" as String
                nameTreePath.rankPath = "${parentNameTreePath.rankPath}>${name.nameRank.name}:${name.nameElement}"
                nameTreePath.namePath = "${parentNameTreePath.namePath}.${name.nameElement}"
                nameTreePath.family = parentNameTreePath.family
            } else {
                nameTreePath.nameIdPath = "${name.id}" as String
                nameTreePath.rankPath = "${name.nameRank.name}:${name.nameElement}"
                nameTreePath.namePath = name.nameElement
                if(name.nameRank.name == 'Familia') {
                    nameTreePath.family = name
                }
            }
            return nameTreePath
        }
        log.error "making NameTreePath for ${name} but node was null."
        return null
    }

    static NameTreePath findCurrentNameTreePath(Name name, String treeLabel) {
        Arrangement arrangement = Arrangement.findByLabel(treeLabel)
        arrangement ? findCurrentNameTreePath(name, arrangement) : null
    }

    static NameTreePath findCurrentNameTreePath(Name name, Arrangement tree) {
        NameTreePath.findByNameAndTreeAndNextIsNull(name, tree)
    }

    static List<NameTreePath> findAllCurrentNameTreePathsForNames(List<Name> names, Arrangement tree) {
        NameTreePath.findAllByTreeAndNameInListAndNextIsNull(tree, names)
    }

    static List<String> findAllTreesForName(Name name) {
        (NameTreePath.executeQuery("select distinct ntp.tree.label from NameTreePath ntp where ntp.name = :name", [name: name]) as List<String>)
    }

    Integer treePathReport(String treeLabel) {
        Arrangement arrangement = Arrangement.findByNamespaceAndLabel(
                configService.nameSpace,
                treeLabel)
        if (arrangement) {
            List results = Node.executeQuery('''
        select count(nd) from Node nd
        where nd.root = :tree
            and nd.internalType = 'T'
            and nd.checkedInAt IS NOT NULL
            and nd.next IS NULL
            and nd.nameUriIdPart IS NOT NULL
            and not exists (select 1 from NameTreePath ntp where ntp.name = nd.name and ntp.tree = nd.root)''',
                    [tree: Arrangement.findByNamespaceAndLabel(configService.nameSpace,
                    treeLabel)])
            results?.first() as Integer
        } else {
            return 0
        }
    }

    def makeAllTreePathsSql() {
        log.debug "in makeAllTreePathsSql"
        runAsync {
            try {
                Sql sql = configService.getSqlForNSLDB()
                log.info "Truncating name tree path."
                sql.execute('TRUNCATE TABLE ONLY name_tree_path RESTART IDENTITY')
                sql.close()
                makeTreePathsSql()
                log.info "Completed making APNI and APC tree paths"
            } catch (e) {
                log.error e
                throw e
            }
        }
    }

    def makeTreePathsSql() {
        log.info "Making Tree Paths for all trees."
        Long start = System.currentTimeMillis()
        Sql sql = configService.getSqlForNSLDB()
        sql.connection.autoCommit = false

        try {
            sql.execute('''
alter table name_tree_path drop CONSTRAINT fk_sfj3hoevcuni3ak7no6byjp3;

WITH RECURSIVE level(node_id, tree_id, parent_id, name_id_path, name_path, rank_path, name_id, family_id)
AS (
  SELECT
    l2.subnode_id                                                  AS node_id,
    a.id                                                           AS tree_id,
    NULL :: BIGINT                                                 AS parent_id,
    n.name_uri_id_part :: TEXT                                     AS name_id_path,
    nm.simple_name :: TEXT                                         AS name_path,
    r.name :: TEXT || ':' || coalesce(nm.name_element, '') :: TEXT AS rank_path,
    n.name_id                                                      AS name_id,
    CASE
    WHEN r.name = 'Familia\'
      THEN nm.id
    ELSE NULL
    END                                                            AS family_id

  FROM tree_arrangement a, tree_link l, tree_link l2, tree_node n, name nm, name_rank r
  WHERE
    l.supernode_id = a.node_id
    AND l2.supernode_id = l.subnode_id
    AND n.id = l2.subnode_id
    AND n.name_id = nm.id
    AND nm.name_rank_id = r.id

  UNION ALL
  SELECT
    subnode.id                                                                                  AS node_id,
    parent.tree_id                                                                              AS tree_id,
    parentnode.id                                                                               AS parent_id,
    (parent.name_id_path || '.' || subnode.name_uri_id_part :: TEXT)                            AS name_id_path,

    (parent.name_path || '.' || coalesce(nm.name_element, '') :: TEXT)                          AS name_path,
    (parent.rank_path || '>' || r.name :: TEXT || ':' || coalesce(nm.name_element, '') :: TEXT) AS rank_path,
    subnode.name_id                                                                             AS name_id,
    CASE
    WHEN r.name = 'Familia\'
      THEN nm.id
    WHEN parent.family_id is not null
      then parent.family_id
    ELSE NULL
    END                                                                                         AS family_id

  FROM level parent, tree_node parentnode, tree_node subnode, tree_link l, name nm, name_rank r
  WHERE parentnode.id = parent.node_id -- this node is now parent
        AND l.supernode_id = parentnode.id
        AND l.subnode_id = subnode.id
        AND subnode.tree_arrangement_id = parent.tree_id
        AND subnode.internal_type = 'T\'
        AND subnode.checked_in_at_id IS NOT NULL
        AND subnode.next_node_id IS NULL
        AND subnode.name_id = nm.id
        AND nm.name_rank_id = r.id
)
INSERT INTO name_tree_path (tree_id,
                            parent_id,
                            name_id_path,
                            name_path,
                            rank_path,
                            name_id,
                            family_id,
                            version,
                            inserted,
                            next_id)
  (SELECT
     l.tree_id,
     l.parent_id,
     l.name_id_path,
     l.name_path,
     l.rank_path,
     l.name_id,
     l.family_id,
     0    AS version,
     0    AS inserted,
     NULL AS next_id
   FROM level l
   WHERE name_id IS NOT NULL
         AND name_path IS NOT NULL);

UPDATE name_tree_path target
SET parent_id = (SELECT ntp.id
                 FROM tree_node node
                   JOIN name n ON node.name_id = n.id
                   , name_tree_path ntp
                 WHERE node.id = target.parent_id AND ntp.name_id = n.id AND ntp.tree_id = target.tree_id)
WHERE target.parent_id IS NOT NULL;

alter table if exists name_tree_path
  add constraint FK_sfj3hoevcuni3ak7no6byjp3
foreign key (parent_id)
references name_tree_path;
''') //not null tests for DeclaredBT that don't exists see NSL-1017
            sql.commit()
        } catch (e) {
            sql.rollback()
            while (e) {
                log.error(e.message)
                if (e.message.contains('getNextException')) {
                    log.error e
                    e = e.getNextException()
                } else {
                    e = e.cause
                }
            }
        }
        sql.close()
        log.info "Made Tree Paths in ${System.currentTimeMillis() - start}ms"
    }

}
