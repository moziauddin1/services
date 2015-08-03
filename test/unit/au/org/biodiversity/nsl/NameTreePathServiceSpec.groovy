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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

import java.sql.Timestamp

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(NameTreePathService)
@Mock([Namespace, NameTreePath, Name, Arrangement, UriNs, Node, Event, NameGroup, NameCategory, NameType, NameStatus, NameRank])
class NameTreePathServiceSpec extends Specification {

    UriNs ns
    Arrangement tree
    Namespace testNameSpace

    def setup() {
        TestUte.setUpNameInfrastructure()
        ns = new UriNs(uri: 'http://biodiversity.org.au/apni.name/',
                label: 'apni-name',
                title: 'APNI name',
                description: 'An APNI name.',
                ownerUriNsPart: null,
                ownerUriIdPart: null,
                idMapperNamespaceId: 1,
                idMapperSystem: 'PLANT_NAME')
        ns.save()
        tree = new Arrangement(id: 1, label: 'tree', arrangementType: ArrangementType.Z, synthetic: 'F')
        tree.save()
        testNameSpace = new Namespace(name: 'TEST')
        testNameSpace.save()

        def classificationServiceMock = mockFor(ClassificationService)
        classificationServiceMock.demand.isNameInClassification(0..2) { name, tree ->
            return Node.findByNameUriIdPart(name.id.toString())
        }
        service.classificationService = classificationServiceMock.createMock()

    }

    def cleanup() {
    }

    void "test add name tree path"() {
        when:

        Event event = new Event(authUser: 'tester', timeStamp: new Timestamp(System.currentTimeMillis()))
        event.save()

        Name a = makeName('a', 'Familia', null).save()
        Name b = makeName('b', 'Genus', a).save()
        Name c = makeName('c', 'Genus', a).save()
        Name d = makeName('d', 'Species', b).save()
        Name e = makeName('e', 'Species', b).save()
        Name f = makeName('f', 'Species', c).save()
        Name g = makeName('g', 'Species', c).save()

        Node na = makeNode(a).save()
        Node nb = makeNode(b).save()
        Node nc = makeNode(c).save()
        Node nd = makeNode(d).save()
        Node ne = makeNode(e).save()
        Node nf = makeNode(f).save()
        Node ng = makeNode(g).save()

        NameTreePath rootTreePath = new NameTreePath(
                id: na.id,
                tree: na.root,
                name: a,
                path: "0.${a.id}",
                treePath: '0',
                inserted: System.currentTimeMillis()
        ).save()

        NameTreePath ntpb = service.addNameTreePath(b, nb)
        NameTreePath ntpc = service.addNameTreePath(c, nc)
        NameTreePath ntpd = service.addNameTreePath(d, nd)
        NameTreePath ntpe = service.addNameTreePath(e, ne)
        NameTreePath ntpf = service.addNameTreePath(f, nf)
        NameTreePath ntpg = service.addNameTreePath(g, ng)

        List<NameTreePath> children = service.currentChildren(rootTreePath)

        then:
        Name.count() == 7
        Node.count() == 7
        NameTreePath.count() == 7
        children.size() == 6 // all except root
        //ordered by number of path elements
        (children[0] in [ntpb, ntpc])
        (children[2] in [ntpd, ntpe, ntpf, ntpg])


        when:
        ntpc = service.findCurrentNameTreePath(c, tree)

        NameTreePath.list().each { ntp ->
            println "${ntp.id} ${ntp.name}: path ${ntp.path}, tpath ${ntp.treePath}"
        }

        then:
        ntpc
        ntpc.name == c
        ntpc.path == '0.1.3'
        ntpc.treePath == '0.1'
        ntpc.parent
        ntpc.parent.name == a
        ntpd.name == d
        ntpd.path == '0.1.2.4'
        ntpd.treePath == '0.1.2'
        ntpe.name == e
        ntpe.path == '0.1.2.5'
        ntpe.treePath == '0.1.2'

        when: "we move a name tree path"
        b.parent = c
        Node nb2 = makeNode(b).save()
        nb.next = nb2 //don't really need this
        service.updateNameTreePath(ntpb, nb2)

        println '--'
        NameTreePath.list().each { ntp ->
            println "${ntp.id} ${ntp.name}: path ${ntp.path}, tpath ${ntp.treePath}"
        }
        println '--'

        then:
        ntpc.next == null
        ntpc.parent == rootTreePath

        ntpb.next != null
        ntpb.name == b
        ntpb.next.name == b
        ntpb.next.path == '0.1.3.2'     // name ids
        ntpb.next.treePath == '0.1.3'   //tree path ids
        ntpb.next.parent == ntpc

        ntpd.next != null
        ntpd.name == d
        ntpd.next.name == d
        ntpd.next.parent == ntpb.next
        ntpd.next.path == '0.1.3.2.4'   //name ids
        ntpd.next.treePath == '0.1.3.2' //tree path ids

        ntpe.next != null
        ntpe.name == e
        ntpe.next.name == e
        ntpe.next.parent == ntpb.next
        ntpe.next.path == '0.1.3.2.5'   //name ids
        ntpe.next.treePath == '0.1.3.2' //tree path ids

        ntpf.next == null
        ntpg.next == null


        service.findCurrentNameTreePath(d, tree) == ntpd.next
    }

    private Node makeNode(Name name) {
        new Node(
                internalType: NodeInternalType.T,
                typeUriNsPart: ns,
                checkedInAt: new Event(authUser: 'tester', timeStamp: new Timestamp(System.currentTimeMillis())),
                nameUriIdPart: name.id.toString(),
                nameUriNsPart: ns,
                root: tree
        )
    }

    private Name makeName(String element, String rank, Name parent) {
        new Name(
                nameType: NameType.findByName('scientific'),
                nameStatus: NameStatus.findByName('legitimate'),
                nameRank: NameRank.findByName(rank),
                createdBy: 'tester',
                updatedBy: 'tester',
                createdAt: new Timestamp(System.currentTimeMillis()),
                updatedAt: new Timestamp(System.currentTimeMillis()),
                nameElement: element,
                parent: parent,
                namespace: testNameSpace
        )
    }
}
