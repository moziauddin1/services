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
            return Node.findByNameAndRoot(name, tree)
        }

        classificationServiceMock.demand.getPath(0..10) { name, tree ->
            Name parent = name.parent
            List<Name> path = [name]
            while(parent){
                path.add(parent)
                parent = parent.parent
            }
            return path.reverse()
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
                nameIdPath: "0.${a.id}",
                namePath: a.nameElement,
                rankPath: 'Familia:a',
                inserted: System.currentTimeMillis()
        ).save()

        NameTreePath ntpb = service.addNameTreePath(b, nb)
        NameTreePath ntpc = service.addNameTreePath(c, nc)
        NameTreePath ntpd = service.addNameTreePath(d, nd)
        NameTreePath ntpe = service.addNameTreePath(e, ne)
        NameTreePath ntpf = service.addNameTreePath(f, nf)
        NameTreePath ntpg = service.addNameTreePath(g, ng)

        List<NameTreePath> childSearch = NameTreePath.findAll()
        println childSearch
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
            println "${ntp.id} ${ntp.name}: path ${ntp.nameIdPath}, rank path: $ntp.rankPath, name path: $ntp.namePath"
        }

        then:
        ntpc
        ntpc.name == c
        ntpc.nameIdPath == '0.1.3'
        ntpc.rankPath == 'Familia:a>Genus:c'
        ntpc.namePath == 'ac'
        ntpc.parent
        ntpc.parent.name == a
        ntpd.name == d
        ntpd.nameIdPath == '0.1.2.4'
        ntpd.rankPath == 'Familia:a>Genus:b>Species:d'
        ntpd.namePath == 'abd'
        ntpe.name == e
        ntpe.nameIdPath == '0.1.2.5'
        ntpe.rankPath == 'Familia:a>Genus:b>Species:e'
        ntpe.namePath =='abe'

        when: "we move name b under c"   //this wouldn't happen since both are genus, but...
        b.parent = c
        Node nb2 = makeNode(b).save()
        nb.next = nb2 //don't really need this
        service.updateNameTreePathFromNode(nb2, ntpb)

        println '--'
        NameTreePath.list().each { ntp ->
            println "${ntp.id} ${ntp.name}: path ${ntp.nameIdPath}, rank path: $ntp.rankPath, name path: $ntp.namePath"
        }
        println '--'

        then:
        ntpc.next == null
        ntpc.parent == rootTreePath

        //Name Tree Paths are now just updated
        ntpb.next == null  //Note next is not currently updated or used
        ntpb.name == b
        ntpb.nameIdPath == '0.1.3.2'     // name ids
        ntpb.parent == ntpc
        ntpb.rankPath == 'Familia:a>Genus:c>Genus:b'
        ntpb.namePath == 'ab' //this is correct because its for this name

        ntpd.next == null  //Note next is not currently updated or used
        ntpd.name == d
        ntpd.parent == ntpb
        ntpd.nameIdPath == '0.1.3.2.4'   //name ids
        ntpd.rankPath == 'Familia:a>Genus:c>Genus:b>Species:d'
        ntpd.namePath == 'acd' //this uses the first genus as expected, though this is not a good case.

        ntpe.next == null  //Note next is not currently updated or used
        ntpe.name == e
        ntpe.parent == ntpb
        ntpe.nameIdPath == '0.1.3.2.5'   //name ids
        ntpe.rankPath == 'Familia:a>Genus:c>Genus:b>Species:e'
        ntpe.namePath == 'ace'

        ntpf.next == null
        ntpf.nameIdPath == '0.1.3.6'
        ntpf.rankPath == 'Familia:a>Genus:c>Species:f'
        ntpf.namePath == 'acf'

        ntpg.next == null
        ntpg.nameIdPath == '0.1.3.7'
        ntpg.rankPath == 'Familia:a>Genus:c>Species:g'
        ntpg.namePath == 'acg'


        service.findCurrentNameTreePath(d, tree) == ntpd
    }

    private Node makeNode(Name name) {
        new Node(
                internalType: NodeInternalType.T,
                typeUriNsPart: ns,
                checkedInAt: new Event(authUser: 'tester', timeStamp: new Timestamp(System.currentTimeMillis())),
                nameUriIdPart: name.id.toString(),
                nameUriNsPart: ns,
                root: tree,
                name: name
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
