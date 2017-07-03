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

@Mixin(BuildSampleTreeMixin)
class MoveDraftLinkSpec extends Specification {
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl
    static final Log log = LogFactory.getLog(MoveDraftLinkSpec.class)

    // fields
    BasicOperationsService basicOperationsService

    // feature methods

    void "make a sample tree and check it"() {
        when:
        SomeStuff s = makeSampleTree()
        s.reload()

        then:
        s.tree
        s.nodeA
        s.nodeB
        s.nodeAA
        s.nodeAB
        s.nodeBA
        s.nodeBB

        s.nodeA != s.nodeAA
        s.nodeA != s.nodeAB
        s.nodeA != s.nodeB
        s.nodeA != s.nodeBA
        s.nodeA != s.nodeBB

        s.nodeAA != s.nodeAB
        s.nodeAA != s.nodeB
        s.nodeAA != s.nodeBA
        s.nodeAA != s.nodeBB

        s.nodeAB != s.nodeB
        s.nodeAB != s.nodeBA
        s.nodeAB != s.nodeBB

        s.nodeB != s.nodeBA
        s.nodeB != s.nodeBB

        s.nodeBA != s.nodeBB

        s.tree.node.supLink.size() == 0
        s.nodeA.supLink.size() == 1
        s.nodeAA.supLink.size() == 1
        s.nodeAB.supLink.size() == 1
        s.nodeB.supLink.size() == 1
        s.nodeBA.supLink.size() == 1
        s.nodeBB.supLink.size() == 1

        DomainUtils.getDraftNodeSupernode(s.nodeA) == s.tree.node
        DomainUtils.getDraftNodeSuperlink(s.nodeA).linkSeq == 1
        DomainUtils.getDraftNodeSupernode(s.nodeAA) == s.nodeA
        DomainUtils.getDraftNodeSuperlink(s.nodeAA).linkSeq == 1
        DomainUtils.getDraftNodeSupernode(s.nodeAB) == s.nodeA
        DomainUtils.getDraftNodeSuperlink(s.nodeAB).linkSeq == 2

        DomainUtils.getDraftNodeSupernode(s.nodeB) == s.tree.node
        DomainUtils.getDraftNodeSuperlink(s.nodeB).linkSeq == 2
        DomainUtils.getDraftNodeSupernode(s.nodeBA) == s.nodeB
        DomainUtils.getDraftNodeSuperlink(s.nodeBA).linkSeq == 1
        DomainUtils.getDraftNodeSupernode(s.nodeBB) == s.nodeB
        DomainUtils.getDraftNodeSuperlink(s.nodeBB).linkSeq == 2

        s.tree.node.subLink.size() == 2
        s.nodeA.subLink.size() == 2
        s.nodeAA.subLink.size() == 0
        s.nodeAB.subLink.size() == 0
        s.nodeB.subLink.size() == 2
        s.nodeBA.subLink.size() == 0
        s.nodeBB.subLink.size() == 0

        when:
        basicOperationsService.deleteArrangement(s.tree)
        s.reload()

        then:
        !s.tree
        !s.nodeA
        !s.nodeB
        !s.nodeAA
        !s.nodeAB
        !s.nodeBA
        !s.nodeBB
    }

    void "perform a simple, legal move"() {
        when:
        SomeStuff s = makeSampleTree()

        then:
        s.nodeA.subLink.size() == 2
        s.nodeB.subLink.size() == 2

        when:
        basicOperationsService.updateDraftNodeLink(s.nodeA, 1, supernode: s.nodeB)
        s.reload()

        then:
        s.nodeA.subLink.size() == 1
        s.nodeB.subLink.size() == 3
        DomainUtils.getDraftNodeSupernode(s.nodeAA) == s.nodeB
        DomainUtils.getDraftNodeSuperlink(s.nodeAA).linkSeq == 3

    }

    void "attempt an illegal move"() {
        when:
        SomeStuff s = makeSampleTree()

        basicOperationsService.updateDraftNodeLink(s.tree.node, 1, supernode: s.nodeAA)
        s.reload()

        then:
        thrown ServiceException
    }

    void "attempt a move to a different tree"() {
        when:
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()

        basicOperationsService.updateDraftNodeLink(s1.nodeA, 1, supernode: s2.nodeB)
        sessionFactory_nsl.currentSession.clear()
        s1.reloadWithoutClear()
        s2.reloadWithoutClear()


        then:
        thrown ServiceException
    }
}


