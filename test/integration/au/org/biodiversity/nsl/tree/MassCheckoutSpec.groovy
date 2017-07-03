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
import au.org.biodiversity.nsl.*;

@Mixin(BuildSampleTreeMixin)

public class MassCheckoutSpec extends Specification {
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl
    static final Log log = LogFactory.getLog(DomainSpec.class)

    // fields
    BasicOperationsService basicOperationsService

    def 'attempt a mass checkout'() {
        Link[] sub;


        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new java.sql.Timestamp(System.currentTimeMillis()), 'TEST', 'attempt a mass checkout')

        SomeStuffEmptyTree s1 = makeSampleEmptyTree() // the working tree
        SomeStuff s2 = makeSampleTree() // some sample data

        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        // we have to adopt a and b separately because the somestuff root node
        // never gets persisted

        basicOperationsService.adoptNode(s1.rootNode, s2.nodeA, VersioningMethod.F)
        basicOperationsService.adoptNode(s1.rootNode, s2.nodeB, VersioningMethod.F)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        basicOperationsService.massCheckout(s1.tree.node, [s2.nodeAA, s2.nodeB])
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        // check that the new nodes have been inserted into the draft tree.

        then:
        // FIRST: I want to check that my sample tree s2 is unaffected
        // this means checking all seven nodes of the tree. Sorry

        // check root node
        s2.tree.node == s2.rootNode
        !DomainUtils.isCheckedIn(s2.rootNode)

        when:
        sub = DomainUtils.getSublinksAsArray(s2.rootNode)
        then:
        sub.length == 3
        sub[1].subnode == s2.nodeA
        sub[2].subnode == s2.nodeB

        // check node a
        DomainUtils.isCheckedIn(s2.nodeA)
        when:
        sub = DomainUtils.getSublinksAsArray(s2.nodeA)
        then:
        sub.length == 3
        sub[1].subnode == s2.nodeAA
        sub[2].subnode == s2.nodeAB

        // check node aa
        DomainUtils.isCheckedIn(s2.nodeAA)
        when:
        sub = DomainUtils.getSublinksAsArray(s2.nodeAA)
        then:
        sub.length == 1

        // check node ab
        DomainUtils.isCheckedIn(s2.nodeAB)
        when:
        sub = DomainUtils.getSublinksAsArray(s2.nodeAB)
        then:
        sub.length == 1

        // check node b
        DomainUtils.isCheckedIn(s2.nodeB)
        when:
        sub = DomainUtils.getSublinksAsArray(s2.nodeB)
        then:
        sub.length == 3
        sub[1].subnode == s2.nodeBA
        sub[2].subnode == s2.nodeBB

        // check node aa
        DomainUtils.isCheckedIn(s2.nodeBA)
        when:
        sub = DomainUtils.getSublinksAsArray(s2.nodeBA)
        then:
        sub.length == 1

        // check node ab
        DomainUtils.isCheckedIn(s2.nodeBB)
        when:
        sub = DomainUtils.getSublinksAsArray(s2.nodeBB)
        then:
        sub.length == 1

        // OKAY!!!  Now to test the work tree, which should have various checked-out subnodes

        // root should be a draft node with one subnode - a checked out version of s2.root

        s1.tree.node == s1.rootNode
        !DomainUtils.isCheckedIn(s2.rootNode)

        when:
        sub = DomainUtils.getSublinksAsArray(s1.tree.node)
        then:
        sub.length == 3

        !DomainUtils.isCheckedIn(sub[1].subnode)
        sub[1].subnode != s2.nodeA
        sub[1].subnode.prev == s2.nodeA

        !DomainUtils.isCheckedIn(sub[2].subnode)
        sub[2].subnode != s2.nodeB
        sub[2].subnode.prev == s2.nodeB

        when:
        sub = DomainUtils.getSublinksAsArray(s1.tree.node)
        sub = DomainUtils.getSublinksAsArray(sub[1].subnode)

        then:
        sub.length == 3

        !DomainUtils.isCheckedIn(sub[1].subnode)
        sub[1].subnode != s2.nodeAA
        sub[1].subnode.prev == s2.nodeAA
        DomainUtils.getSublinksAsArray(sub[1].subnode).length == 1

        DomainUtils.isCheckedIn(sub[2].subnode)
        sub[2].subnode == s2.nodeAB
        DomainUtils.getSublinksAsArray(sub[2].subnode).length == 1

        when:
        sub = DomainUtils.getSublinksAsArray(s1.tree.node)
        sub = DomainUtils.getSublinksAsArray(sub[2].subnode)

        then:
        sub.length == 3

        DomainUtils.isCheckedIn(sub[2].subnode)
        sub[1].subnode == s2.nodeBA
        DomainUtils.getSublinksAsArray(sub[1].subnode).length == 1

        DomainUtils.isCheckedIn(sub[2].subnode)
        sub[2].subnode == s2.nodeBB
        DomainUtils.getSublinksAsArray(sub[2].subnode).length == 1


    }
}
