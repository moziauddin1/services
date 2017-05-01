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
import au.org.biodiversity.nsl.Event;
import au.org.biodiversity.nsl.Link;
import au.org.biodiversity.nsl.VersioningMethod;
import spock.lang.*

import java.sql.Timestamp

@Mixin(BuildSampleTreeMixin)
class AdoptNodeSpec extends Specification {
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl
    static final Log log = LogFactory.getLog(AdoptNodeSpec.class)

    // fields
    BasicOperationsService basicOperationsService

    void "test simple node adoption"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new Timestamp(System.currentTimeMillis()), 'TEST', 'test simple node adoption')

        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()

        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        !DomainUtils.isCheckedIn(s1.nodeA)
        DomainUtils.isCheckedIn(s2.nodeA)

        when:
        basicOperationsService.adoptNode(s1.nodeA, s2.nodeA, VersioningMethod.V, seq: 3)
        basicOperationsService.adoptNode(s1.nodeA, s2.nodeB, VersioningMethod.V, seq: 4)
        Link[] links = DomainUtils.getSublinksAsArray(s1.nodeA)
        println links
        then:
        s1.nodeA.subLink.size() == 4
        s2.nodeA.supLink.size() == 2
        s2.nodeB.supLink.size() == 2
        links[1].subnode == s1.nodeAA
        links[2].subnode == s1.nodeAB
        links[3].subnode == s2.nodeA
        links[4].subnode == s2.nodeB

        when: "we adopt a node with using an existing seqence number"
        Link insertedLink = basicOperationsService.adoptNode(s1.nodeA, s2.nodeAB, VersioningMethod.V, seq: 2)
        Link[] links2 = DomainUtils.getSublinksAsArray(s1.nodeA)
        println Link.findAllBySupernode(s1.nodeA)

        then: "the other nodes are moved up in sequence"
        insertedLink
        insertedLink.linkSeq == 2
        s1.nodeA.subLink.size() == 5
        s2.nodeA.supLink.size() == 2
        s2.nodeB.supLink.size() == 2
        s2.nodeAB.supLink.size() == 2
        links2[1].subnode == s1.nodeAA
        links2[2].subnode == s2.nodeAB
        links2[3].subnode == s1.nodeAB
        links2[4].subnode == s2.nodeA
        links2[5].subnode == s2.nodeB

    }

    void "test adopting into persistent node"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new Timestamp(System.currentTimeMillis()), 'TEST', 'test adopting into persistent node')

        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()

        basicOperationsService.persistNode(e, s1.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        basicOperationsService.persistNode(e, s2.tree.node)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        DomainUtils.isCheckedIn(s1.nodeA)
        DomainUtils.isCheckedIn(s2.nodeA)

        when:

        basicOperationsService.adoptNode s1.nodeA, s2.nodeA, VersioningMethod.V
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then:
        thrown ServiceException
    }

    void "test adopting draft node"() {
        when:
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()

        then:
        !DomainUtils.isCheckedIn(s1.nodeA)
        !DomainUtils.isCheckedIn(s2.nodeA)

        when: "You try to adopt a draft node"
        basicOperationsService.adoptNode(s1.nodeA, s2.nodeA, VersioningMethod.V)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()

        then: "A service exception is thrown"
        thrown ServiceException
    }


}