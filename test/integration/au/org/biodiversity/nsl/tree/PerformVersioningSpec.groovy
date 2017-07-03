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

import javax.sql.DataSource

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.SessionFactory

import spock.lang.*
import au.org.biodiversity.nsl.Event
import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.NodeInternalType
import au.org.biodiversity.nsl.VersioningMethod

import java.sql.Timestamp

@Mixin(BuildSampleTreeMixin)
class PerformVersioningSpec extends Specification {
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl
    static final Log log = LogFactory.getLog(PerformVersioningSpec.class)

    // fields
    BasicOperationsService basicOperationsService
    VersioningService versioningService

    // feature methods

    void "test versioning"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new Timestamp(System.currentTimeMillis()), 'TEST', 'test versioning')
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()
        basicOperationsService.persistNode(e, s1.tree.node)
        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        s1.nodeA
        s2.nodeA

        when:

        Map<Node, Node> replace = new HashMap<Node, Node>()
        replace.put(s1.nodeAA, s2.nodeAA)

        versioningService.performVersioning e, replace, s1.tree
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        s1.nodeA.next
        s1.nodeAA.next == s2.nodeAA
        !s1.nodeAB.next

        !s1.nodeB.next
        !s1.nodeBA.next
        !s1.nodeBB.next

        !DomainUtils.isCurrent(s1.nodeA)
        !DomainUtils.isCurrent(s1.nodeAA)
        DomainUtils.isCurrent(s1.nodeAB)

        DomainUtils.isCurrent(s1.nodeB)
        DomainUtils.isCurrent(s1.nodeBA)
        DomainUtils.isCurrent(s1.nodeBB)
    }

    void "test versioning replacing 2"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new Timestamp(System.currentTimeMillis()), 'TEST', 'test versioning replacing 2')
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()
        basicOperationsService.persistNode(e, s1.tree.node)
        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        s1.nodeA
        s2.nodeA

        when:

        Map<Node, Node> replace = new HashMap<Node, Node>()
        replace.put(s1.nodeAA, s2.nodeAA)
        replace.put(s1.nodeAB, s2.nodeAB)

        versioningService.performVersioning e, replace, s1.tree
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        dumpStuff([s1, s2])

        then:
        s1.nodeA.next
        s1.nodeAA.next == s2.nodeAA
        s1.nodeAB.next == s2.nodeAB

        !s1.nodeB.next
        !s1.nodeBA.next
        !s1.nodeBB.next

        !DomainUtils.isCurrent(s1.nodeA)
        !DomainUtils.isCurrent(s1.nodeAA)
        !DomainUtils.isCurrent(s1.nodeAB)

        DomainUtils.isCurrent(s1.nodeB)
        DomainUtils.isCurrent(s1.nodeBA)
        DomainUtils.isCurrent(s1.nodeBB)
    }

    void "test versioning replacing parent and child"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new Timestamp(System.currentTimeMillis()), 'TEST', 'test versioning replacing parent and child')
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()
        basicOperationsService.persistNode(e, s1.tree.node)
        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        s1.nodeA
        s2.nodeA

        when:

        Map<Node, Node> replace = new HashMap<Node, Node>()
        replace.put(s1.nodeA, s2.nodeA)
        replace.put(s1.nodeAA, s2.nodeAA)

        versioningService.performVersioning e, replace, s1.tree
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        dumpStuff([s1, s2])

        then:
        thrown ServiceException
    }

    void "test versioning replacing parent and child without orphans"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new Timestamp(System.currentTimeMillis()), 'TEST', 'test versioning replacing parent and child without orphans')
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()
        basicOperationsService.persistNode(e, s1.tree.node)
        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        s1.nodeA
        s2.nodeA

        when:

        Map<Node, Node> replace = new HashMap<Node, Node>()
        replace.put(s1.nodeA, s2.nodeA)
        replace.put(s1.nodeAA, s2.nodeAA)
        replace.put(s1.nodeAB, DomainUtils.getEndNode())

        versioningService.performVersioning e, replace, s1.tree
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        dumpStuff([s1, s2])

        then:
        s1.nodeA.next == s2.nodeA
        s1.nodeAA.next == s2.nodeAA
        s1.nodeAB.next == DomainUtils.getEndNode()

        !s1.nodeB.next
        !s1.nodeBA.next
        !s1.nodeBB.next

        !DomainUtils.isCurrent(s1.nodeA)
        !DomainUtils.isCurrent(s1.nodeAA)
        !DomainUtils.isCurrent(s1.nodeAB)

        DomainUtils.isCurrent(s1.nodeB)
        DomainUtils.isCurrent(s1.nodeBA)
        DomainUtils.isCurrent(s1.nodeBB)
    }

    void "test a more-or-less believable versioning"() {
        /**
         * Ok! I am going to checkout node aa and modify it, checkout node b, delete bb, add bc,
         * then persist and version the changes
         */
        when:
        Event event = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(),
                new Timestamp(System.currentTimeMillis()),
                'TEST',
                'test a more-or-less believable versioning')

        SomeStuffWithHistory afd = makeSampleTreeWithHistory()
        SomeStuffEmptyTree workSpace = makeSampleEmptyTree()

        Closure clearSessionAndReload = {
            sessionFactory_nsl.currentSession.clear()
            afd.reloadWithoutClear()
            workSpace.reloadWithoutClear()
        }

        basicOperationsService.persistNode(event, afd.tree.node)
        clearSessionAndReload()

        basicOperationsService.adoptNode(workSpace.tree.node, afd.nodeAA, VersioningMethod.V)
        clearSessionAndReload()

        long newAAid = basicOperationsService.checkoutNode(workSpace.tree.node, afd.nodeAA).id
        clearSessionAndReload()

        basicOperationsService.updateDraftNode Node.get(newAAid), name: DomainUtils.uri('afd-name', 'newAA')
        clearSessionAndReload()

        basicOperationsService.adoptNode(workSpace.tree.node, afd.nodeB, VersioningMethod.V)
        clearSessionAndReload()

        long newBid = basicOperationsService.checkoutNode(workSpace.tree.node, afd.nodeB).id
        clearSessionAndReload()

        basicOperationsService.deleteLink Node.get(newBid), 2
        clearSessionAndReload()

        Node newBC = basicOperationsService.createDraftNode(Node.get(newBid), VersioningMethod.V, NodeInternalType.T,
                seq: 3,
                name: DomainUtils.uri('afd-name', 'newBC'))
        clearSessionAndReload()

        basicOperationsService.persistNode event, workSpace.tree.node
        clearSessionAndReload()

        BuildSampleTreeUtil.dumpStuff sessionFactory_nsl, log, [afd, workSpace]

        Map<Node, Node> version = new HashMap<Node, Node>()

        version.put(afd.nodeAA, Node.get(newAAid))
        version.put(afd.nodeB, Node.get(newBid))
        version.put(afd.nodeBB, DomainUtils.getEndNode())

        log.debug "replacing ${afd.nodeBB} with ${version.get(afd.nodeBB)}"

        versioningService.performVersioning(event, version, afd.tree)
        clearSessionAndReload()

        BuildSampleTreeUtil.dumpStuff sessionFactory_nsl, log, [afd, workSpace]

        basicOperationsService.moveFinalNodesFromTreeToTree workSpace.tree, afd.tree
        clearSessionAndReload()

        basicOperationsService.deleteArrangement(workSpace.tree)
        // dont use reset, because ws.t will be cleared and I want to see that it's properly gone
        sessionFactory_nsl.currentSession.clear()
        afd.reloadWithoutClear()

        BuildSampleTreeUtil.dumpStuff sessionFactory_nsl, log, [afd, workSpace]

        then:
        1 == 1


    }

}