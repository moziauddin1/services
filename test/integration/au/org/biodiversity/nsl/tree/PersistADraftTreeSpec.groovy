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

import au.org.biodiversity.nsl.Event
import org.hibernate.SessionFactory
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection

@Mixin(BuildSampleTreeMixin)
class PersistADraftTreeSpec extends Specification {
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl

    def queryService

    // fields
    BasicOperationsService basicOperationsService

    // fixture methods

    def setup() {
    }

    def cleanup() {
    }

    def setupSpec() {
    }

    def cleanupSpec() {
    }

    // feature methods

    void "make a sample tree and persist it"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new java.sql.Timestamp(System.currentTimeMillis()), 'TEST', 'make a sample tree and persist it')
        SomeStuff s = makeSampleTree()

        println queryService.dumpNodes([s.tree.node])

//		s.reload()

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
        basicOperationsService.persistNode(e, s.tree.node)
        s.reload()

        then:
        !DomainUtils.isCheckedIn(s.tree.node)
        DomainUtils.isCheckedIn(s.nodeA)
        DomainUtils.isCheckedIn(s.nodeAA)
        DomainUtils.isCheckedIn(s.nodeAB)
        DomainUtils.isCheckedIn(s.nodeB)
        DomainUtils.isCheckedIn(s.nodeBA)
        DomainUtils.isCheckedIn(s.nodeBB)
    }

    void "put tree in an illegal state and attempt to persist it"() {
        when:
        Event e = basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(), new java.sql.Timestamp(System.currentTimeMillis()), 'TEST', 'put tree in an illegal state and attempt to persist it')
        SomeStuff s1 = makeSampleTree()
        SomeStuff s2 = makeSampleTree()

        HibernateSessionUtils.doWork sessionFactory_nsl, { Connection cnct ->
            cnct.createStatement().executeUpdate("""
					insert into tree_link (
					 ID,					   --NOT NULL NUMBER(38)
					 LOCK_VERSION,				   --NOT NULL NUMBER(38)
					 SUPERNODE_ID,			   --NOT NULL NUMBER(38)
					 SUBNODE_ID,			   --NOT NULL NUMBER(38)
					 TYPE_URI_NS_PART_ID,	   -- NUMBER(38)
					 TYPE_URI_ID_PART,		   -- VARCHAR2(255)
					 LINK_SEQ,				   --NOT NULL NUMBER(38)
					 VERSIONING_METHOD,		   --NOT NULL CHAR(1)
					 IS_SYNTHETIC			   --NOT NULL CHAR(1)
					)
					values (
						nextval('nsl_global_seq'),
						1,
						${s1.nodeBBId},
						${s2.nodeAAId},
						0,
						null,
						1,
						'V',
						'N'
					)		
			""")
        }

        basicOperationsService.persistNode(e, s1.tree.node)

        then:
        thrown IllegalStateException

    }

}
