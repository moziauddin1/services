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
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * NameTreePaths are a view of a tree linking directly to Names. The contain the tree path from root to the current name
 * as a string of name IDs and nameTreePath ids.
 *
 * This inverts the structure of the tree as compared to the tree nodes and links and allows you to do rapid queries to
 * get the set of names under a particular name on the tree by finding all the nameTreePaths that start with this path.
 *
 * The job of this service is to keep this view of the tree up to date. NameTreePaths only represent the current tree for
 * now to optimise the most common operations.
 *
 */
@Transactional
class NameTreePathService {

    GrailsApplication grailsApplication
    def classificationService

    /**
     * If a name is moved on the tree you need to update the NameTreePath for that name. This involves making a new
     * NametreePath and updating all the children of the old NameTreePath
     *
     * @param oldTreePath
     * @param currentNode
     * @return
     */
    NameTreePath updateNameTreePath(NameTreePath oldTreePath, Node currentNode) {

        log.debug "Updating name tree path ${oldTreePath} to node ${currentNode}"
        //do some sanity checks
        if (oldTreePath.name != currentNode.name) {
            throw new IllegalArgumentException("Node doesn't match tree path name.")
        }

        if (currentNode.next) {
            throw new IllegalArgumentException("Node is not current.")
        }

        if (oldTreePath.next) {
            throw new IllegalArgumentException("TreePath is not current.")
        }

        NameTreePath newTreePath = addNameTreePath(oldTreePath.name, currentNode)

        oldTreePath.next = newTreePath
        oldTreePath.save(flush: true)

        if (oldTreePath.nameIdPath != newTreePath.nameIdPath) {
            updateChildren(oldTreePath, newTreePath)
        }
        return newTreePath
    }

    //this could take a long long time
    private void updateChildren(NameTreePath oldParent, NameTreePath newParent) {
        List<NameTreePath> children = currentChildren(oldParent) //in order from top of the tree (!important)
        NameTreePath.withSession { session ->
            children.each { NameTreePath child ->
                log.debug "updating $child.name child name tree path."
                Node currentChildNode = classificationService.isNameInClassification(child.name, newParent.tree)
                String nodeIdPath = newParent.nodeIdPath + child.nodeIdPath.substring(oldParent.nodeIdPath.size())
                String nameIdPath = newParent.nameIdPath + child.nameIdPath.substring(oldParent.nameIdPath.size())
                String namePath = newParent.namePath + child.namePath.substring(oldParent.namePath.size())
                String rankPath = newParent.rankPath + child.rankPath.substring(oldParent.rankPath.size())

                if (child.id == currentChildNode.id) {
                    child.nameIdPath = nameIdPath
                    child.nodeIdPath = nodeIdPath
                    child.namePath = namePath
                    child.rankPath = rankPath
                } else {
                    NameTreePath newChild = new NameTreePath()
                    newChild.id = currentChildNode.id
                    newChild.tree = newParent.tree
                    newChild.parent = child.parent.next
                    newChild.nodeIdPath = nodeIdPath
                    newChild.nameIdPath = nameIdPath
                    newChild.namePath = namePath
                    newChild.rankPath = rankPath
                    newChild.inserted = System.currentTimeMillis()
                    newChild.name = child.name
                    newChild.save()
                    child.next = newChild
                    child.save()
                }
            }
            session.flush()
        }
    }

    public Name updateNameTreePath(Name name) {
        log.debug "Update Name Tree Path for $name"
        if (name.nameType.scientific || name.nameType.cultivar) {
            if (name.parent || RankUtils.rankHigherThan(name.nameRank, 'Classis')) {
                //we don't need domains to have a parent
                Node currentNode = classificationService.isNameInAPNI(name)
                if (currentNode) {
                    NameTreePath ntp = findCurrentNameTreePath(name, currentNode.root)
                    if (ntp) {
                        if (ntp.id != currentNode.id) {
                            log.info "updating name tree path for $name"
                            updateNameTreePath(ntp, currentNode)
                        }
                    } else {
                        //this really shouldn't happen but self healing should be OK
                        log.warn "Name $name didn't have a NameTreePath, and it probably should have, so I'll make one."
                        addNameTreePath(name, currentNode)
                    }
                } else {
                    log.error "No current tree node for updated name $name, which should have one."
                }
            } else {
                log.error "No parent for name $name, which should have one."
            }
        }
        return name
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
        NameTreePath.findAllByPathLikeAndTreeAndNextIsNull("${parentTreePath.nameIdPath}.%", parentTreePath.tree).sort { a, b -> a.namePathIds().size() <=> b.namePathIds().size() }
    }

    /**
     * Get the Node on the Tree that this NameTreePath represents
     * @param treePath
     * @return
     */
    Node getTreeNode(NameTreePath treePath) {
        Node.get(treePath.id)
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

        if (node) {
            NameTreePath parentNameTreePath
            if (name.parent) {
                parentNameTreePath = findCurrentNameTreePath(name.parent, node.root)
                if (!parentNameTreePath) {
                    log.debug "${name}'s parent doesn't have a name tree path, attempting to add it."
                    Node parentNode = classificationService.isNameInAPNI(name.parent)
                    parentNameTreePath = addNameTreePath(name.parent, parentNode)
                }
            }

            NameTreePath nameTreePath = new NameTreePath()
            nameTreePath.id = node.id
            nameTreePath.tree = node.root
            nameTreePath.inserted = System.currentTimeMillis()
            nameTreePath.name = name
            if (parentNameTreePath) {
                nameTreePath.parent = parentNameTreePath
                nameTreePath.nodeIdPath = "${parentNameTreePath.nodeIdPath}.${parentNameTreePath.id}" as String
                nameTreePath.nameIdPath = "${parentNameTreePath.nameIdPath}.${node.nameUriIdPart}" as String
                nameTreePath.namePath = "${parentNameTreePath.namePath}.${name.simpleName}"
                nameTreePath.rankPath = "${parentNameTreePath.rankPath}.${name.nameRank.name}"
            } else {
                nameTreePath.nodeIdPath = "${node.id}" as String
                nameTreePath.nameIdPath = "${node.nameUriIdPart}" as String
                nameTreePath.namePath = name.simpleName
                nameTreePath.rankPath = name.nameRank.name
            }

            nameTreePath.save(flush: true)
            log.debug "Added NameTreePath ${nameTreePath.dump()}"
            return nameTreePath
        }
        log.info "adding NameTreePath for ${name} but node was null."
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
                Namespace.findByName(grailsApplication.config.shard.classification.namespace),
                treeLabel)
        if (arrangement) {
            List results = Node.executeQuery('''
        select count(nd) from Node nd
        where nd.root = :tree
            and nd.internalType = 'T'
            and nd.checkedInAt IS NOT NULL
            and nd.next IS NULL
            and nd.nameUriIdPart IS NOT NULL
            and not exists (select 1 from NameTreePath ntp where ntp.id = nd.id)''', [tree: Arrangement.findByNamespaceAndLabel(
                    Namespace.findByName(grailsApplication.config.shard.classification.namespace),
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
                Sql sql = SearchService.getNSL()
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
        Sql sql = SearchService.getNSL()
        sql.connection.autoCommit = false

        try {
            sql.execute('''
WITH RECURSIVE level(node_id, tree_id, parent_id, node_path, name_id_path, name_path, rank_path, name_id)
AS (
  SELECT
    l2.subnode_id                                                  AS node_id,
    a.id                                                           AS tree_id,
    NULL :: BIGINT                                                 AS parent_id,
    n.id :: TEXT                                                   AS node_path,
    n.name_uri_id_part :: TEXT                                     AS name_id_path,
    nm.simple_name :: TEXT                                         AS name_path,
    r.name :: TEXT || ':' || coalesce(nm.name_element, '') :: TEXT AS rank_path,
    n.name_id                                                      AS name_id
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
    (parent.node_path || '.' || subnode.id :: TEXT)                                             AS node_path,
    (parent.name_id_path || '.' || subnode.name_uri_id_part :: TEXT)                            AS name_id_path,
    (parent.name_path || '>' || nm.simple_name :: TEXT)                                         AS name_path,
    (parent.rank_path || '>' || r.name :: TEXT || ':' || coalesce(nm.name_element, '') :: TEXT) AS rank_path,
    subnode.name_id                                                                             AS name_id
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
INSERT INTO name_tree_path (id,
                            tree_id,
                            parent_id,
                            node_id_path,
                            name_id_path,
                            name_path,
                            rank_path,
                            name_id,
                            version,
                            inserted,
                            next_id)
  (SELECT
     l.node_id,
     l.tree_id,
     l.parent_id,
     l.node_path,
     l.name_id_path,
     l.name_path,
     l.rank_path,
     l.name_id,
     0    AS verison,
     0    AS inserted,
     NULL AS next_id
   FROM level l
   WHERE name_id IS NOT NULL
         AND name_path IS NOT NULL)''') //not null tests for DeclaredBT that don't exists see NSL-1017
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
