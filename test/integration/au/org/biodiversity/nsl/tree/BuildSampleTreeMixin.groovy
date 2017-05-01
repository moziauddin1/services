/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL tree services plugin project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl.tree

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.Namespace
import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.NodeInternalType
import au.org.biodiversity.nsl.VersioningMethod
import org.apache.commons.logging.Log
import org.hibernate.SessionFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

class TreeTestUtil {
    static Namespace getTestNamespace() {
        Namespace n = Namespace.findByName('TREETEST')
        if(!n) {
            n = new Namespace(name: 'TREETEST', descriptionHtml: 'Tree Services Plugin integration tests')
            n.save()
        }
        return n
    }
}

class BuildSampleTreeMixin {

    SomeStuff makeSampleTree() {
        SomeStuff s = new SomeStuff((SessionFactory)sessionFactory_nsl, (BasicOperationsService)basicOperationsService)
        s.makeTree()
        return s
    }

    SomeStuffWithHistory makeSampleTreeWithHistory() {
        SomeStuffWithHistory s = new SomeStuffWithHistory((SessionFactory)sessionFactory_nsl, (BasicOperationsService)basicOperationsService)
        s.makeTree()
        return s
    }

    SomeStuffEmptyTree makeSampleEmptyTree() {
        SomeStuffEmptyTree s = new SomeStuffEmptyTree((SessionFactory)sessionFactory_nsl, (BasicOperationsService)basicOperationsService)
        s.makeTree()
        return s
    }

    void dumpStuff(List stuff) {
        BuildSampleTreeUtil.dumpStuff((SessionFactory)sessionFactory_nsl, (Log)log, stuff)
    }
}

class BuildSampleTreeUtil {


    static void dumpStuff(final SessionFactory sessionFactory_nsl, final Log log, final List stuff) {
        HibernateSessionUtils.doWork sessionFactory_nsl, { Connection cnct ->
            log.debug("----")
            log.debug("digraph {")
            stuff.each { SomeStuffEmptyTree s ->
                Long arrId = s.treeId
                log.debug("subgraph cluster${arrId} {")
                log.debug("label=\"${arrId}\";")

                HibernateSessionUtils.withQ cnct, """select
									n.id, 
									n.tree_arrangement_id, 
									n.name_uri_id_part, 
									n.PREV_NODE_ID, 
									n.checked_in_at_id, 
									n.IS_SYNTHETIC, 
									n.replaced_at_id
							from tree_node n where n.tree_arrangement_id = ${arrId}
							order by n.tree_arrangement_id""", { PreparedStatement qry ->
                    ResultSet rs = qry.executeQuery()
                    //noinspection ChangeToOperator
                    while (rs.next()) {
                        log.debug("\t${rs.getLong(1)} [" //
                                + (rs.getObject(7) == null ? "" : " style=\"dotted\"")
                                + " label=\"${rs.getLong(1)} ${rs.getString(3)}${rs.getString(6) == 'Y' ? '*' : ''}\"" //
                                + " shape=\"${rs.getString(5) == 'Y' ? 'rectangle' : 'oval'}\"];")
                    }
                }
                log.debug("}")
            }

            log.debug('0 [label="[END]", shape="hexagon"];')

            stuff.each { ss ->

                SomeStuffEmptyTree s = (SomeStuffEmptyTree) ss

                Long arrId = s.treeId

                HibernateSessionUtils.withQ cnct, "select l.supernode_id, l.subnode_id, l.link_seq, l.id from tree_link l, tree_node n where n.id = l.supernode_id and n.tree_arrangement_id = ${arrId}", { PreparedStatement qry ->
                    ResultSet rs = qry.executeQuery()
                    //noinspection ChangeToOperator
                    while (rs.next()) {
                        log.debug("${rs.getLong(1)} -> ${rs.getLong(2)} [taillabel=\"${rs.getInt(3)}\", label=\"${rs.getLong(4)}\", ];")
                    }
                }

                HibernateSessionUtils.withQ cnct, """select n.id, n.PREV_NODE_ID from tree_node n where n.tree_arrangement_id = ${
                    arrId
                } and n.PREV_NODE_ID is not null""", { PreparedStatement qry ->
                    ResultSet rs = qry.executeQuery()
                    //noinspection ChangeToOperator
                    while (rs.next()) {
                        log.debug("${rs.getLong(2)} -> ${rs.getLong(1)} [style=\"dotted\", constraint=\"false\"];")
                    }
                }
                HibernateSessionUtils.withQ cnct, """
						select n.id, n.NEXT_NODE_ID
						from tree_node n
						where n.tree_arrangement_id = ${arrId}
						and n.NEXT_NODE_ID is not null
						""", { PreparedStatement qry ->
                    ResultSet rs = qry.executeQuery()
                    //noinspection ChangeToOperator
                    while (rs.next()) {
                        log.debug("${rs.getLong(1)} -> ${rs.getLong(2)} [style=\"dashed\" ${rs.getLong(2) == 0 ? '' : ', constraint=\"false\"'}];")
                    }
                }
            }
            log.debug("}")
            log.debug("----")
        }
    }
}

class SomeStuffEmptyTree {
    long treeId
    Arrangement tree
    long rootid
    Node rootNode

    final SessionFactory sessionFactory_nsl
    final BasicOperationsService basicOperationsService

    SomeStuffEmptyTree(SessionFactory sessionFactory_nsl, BasicOperationsService basicOperationsService) {
        this.sessionFactory_nsl = sessionFactory_nsl
        this.basicOperationsService = basicOperationsService
    }

    void makeTree() {
        tree = basicOperationsService.createTemporaryArrangement(TreeTestUtil.getTestNamespace())
        treeId = tree.id
        rootNode = tree.node
        rootid = rootNode.id
    }

    void reload() {
        sessionFactory_nsl.currentSession.clear()
        reloadWithoutClear()
    }

    void reloadWithoutClear() {
        tree = Arrangement.get(treeId)
        rootNode = Node.get(rootid)
    }
}

class SomeStuff extends SomeStuffEmptyTree {
    long nodeAId
    long nodeAAId
    long nodeABId
    long nodeBId
    long nodeBAId
    long nodeBBId

    Node nodeA
    Node nodeAA
    Node nodeAB
    Node nodeB
    Node nodeBA
    Node nodeBB


    SomeStuff(SessionFactory sessionFactory_nsl, BasicOperationsService basicOperationsService) {
        super(sessionFactory_nsl, basicOperationsService)
    }

    void makeTree() {
        tree = basicOperationsService.createTemporaryArrangement(TreeTestUtil.getTestNamespace())
        treeId = tree.id
        rootNode = tree.node
        rootid = rootNode.id

        nodeA = basicOperationsService.createDraftNode(tree.node, VersioningMethod.V, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'A'), taxon: DomainUtils.uri('afd-taxon', 'A'))
        nodeAA = basicOperationsService.createDraftNode(nodeA, VersioningMethod.V, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'AA'), taxon: DomainUtils.uri('afd-taxon', 'AA'))
        nodeAB = basicOperationsService.createDraftNode(nodeA, VersioningMethod.V, NodeInternalType.T, seq: 2, name: DomainUtils.uri('afd-name', 'AB'), taxon: DomainUtils.uri('afd-taxon', 'AB'))
        nodeB = basicOperationsService.createDraftNode(tree.node, VersioningMethod.V, NodeInternalType.T, Seq: 2, name: DomainUtils.uri('afd-name', 'B'), taxon: DomainUtils.uri('afd-taxon', 'B'))
        nodeBA = basicOperationsService.createDraftNode(nodeB, VersioningMethod.V, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'BA'), taxon: DomainUtils.uri('afd-taxon', 'BA'))
        nodeBB = basicOperationsService.createDraftNode(nodeB, VersioningMethod.V, NodeInternalType.T, seq: 2, name: DomainUtils.uri('afd-name', 'BB'), taxon: DomainUtils.uri('afd-taxon', 'BB'))

        tree.refresh()     //needed because these are loaded by createTemp once createDraft is hibernate based remove
        rootNode.refresh()

        nodeAId = nodeA.id
        nodeAAId = nodeAA.id
        nodeABId = nodeAB.id
        nodeBId = nodeB.id
        nodeBAId = nodeBA.id
        nodeBBId = nodeBB.id

        reloadWithoutClear()
    }


    void reloadWithoutClear() {
        super.reloadWithoutClear()
        nodeA = Node.get(nodeAId)
        nodeAA = Node.get(nodeAAId)
        nodeAB = Node.get(nodeABId)
        nodeB = Node.get(nodeBId)
        nodeBA = Node.get(nodeBAId)
        nodeBB = Node.get(nodeBBId)

//		// this should not be necessary - the clear session should do the job
//		[a,aa,ab,b,ba,bb].each {
//			it.refresh();
//		}
    }
}

class SomeStuffWithHistory extends SomeStuff {
    Node startRoot
    long startRootId

    SomeStuffWithHistory(SessionFactory sessionFactory_nsl, BasicOperationsService basicOperationsService) {
        super(sessionFactory_nsl, basicOperationsService)
    }

    void makeTree() {
        tree = basicOperationsService.createTemporaryArrangement(TreeTestUtil.getTestNamespace(), )
        treeId = tree.id
        rootNode = tree.node
        rootid = rootNode.id

        startRoot = basicOperationsService.createDraftNode tree.node, VersioningMethod.T, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'ROOT')
        startRootId = startRoot.id

        nodeA = basicOperationsService.createDraftNode startRoot, VersioningMethod.V, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'A')
        nodeAA = basicOperationsService.createDraftNode nodeA, VersioningMethod.V, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'AA')
        nodeAB = basicOperationsService.createDraftNode nodeA, VersioningMethod.V, NodeInternalType.T, seq: 2, name: DomainUtils.uri('afd-name', 'AB')
        nodeB = basicOperationsService.createDraftNode startRoot, VersioningMethod.V, NodeInternalType.T, seq: 2, name: DomainUtils.uri('afd-name', 'B')
        nodeBA = basicOperationsService.createDraftNode nodeB, VersioningMethod.V, NodeInternalType.T, seq: 1, name: DomainUtils.uri('afd-name', 'BA')
        nodeBB = basicOperationsService.createDraftNode nodeB, VersioningMethod.V, NodeInternalType.T, seq: 2, name: DomainUtils.uri('afd-name', 'BB')

        nodeAId = nodeA.id
        nodeAAId = nodeAA.id
        nodeABId = nodeAB.id
        nodeBId = nodeB.id
        nodeBAId = nodeBA.id
        nodeBBId = nodeBB.id

        reloadWithoutClear()
    }

    void reloadWithoutClear() {
        super.reloadWithoutClear()
        startRoot = Node.get(startRootId)
        startRoot.refresh()
    }
}
