package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import grails.validation.ValidationException
import spock.lang.Specification

import javax.sql.DataSource

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(TreeService)
class TreeServiceSpec extends Specification {

    def grailsApplication
    DataSource dataSource_nsl


    def setup() {
        service.dataSource_nsl = dataSource_nsl
        service.configService = new ConfigService(grailsApplication: grailsApplication)
        service.linkService = Mock(LinkService)
    }

    def cleanup() {
    }

    void "test create new tree"() {
        when: 'I create a new unique tree'
        Tree tree = service.createNewTree('aTree', 'aGroup', null)

        then: 'It should work'
        tree
        tree.name == 'aTree'
        tree.groupName == 'aGroup'
        tree.referenceId == null
        tree.currentTreeVersion == null
        tree.defaultDraftTreeVersion == null
        tree.id != null

        when: 'I try and create another tree with the same name'
        service.createNewTree('aTree', 'aGroup', null)

        then: 'It will fail with an exception'
        thrown ObjectExistsException

        when: 'I try and create another tree with null name'
        service.createNewTree(null, 'aGroup', null)

        then: 'It will fail with an exception'
        thrown ValidationException

        when: 'I try and create another tree with null group name'
        service.createNewTree('aNotherTree', null, null)

        then: 'It will fail with an exception'
        thrown ValidationException

        when: 'I try and create another tree with reference ID'
        Tree tree2 = service.createNewTree('aNotherTree', 'aGroup', 12345l)

        then: 'It will work'
        tree2
        tree2.name == 'aNotherTree'
        tree2.groupName == 'aGroup'
        tree2.referenceId == 12345l
    }

    void "Test editing tree"() {
        given:
        Tree atree = new Tree(name: 'aTree', groupName: 'aGroup').save()
        Tree btree = new Tree(name: 'b tree', groupName: 'aGroup').save()
        Long treeId = atree.id

        expect:
        atree
        treeId
        atree.name == 'aTree'
        btree
        btree.name == 'b tree'

        when: 'I change the name of a tree'
        Tree tree2 = service.editTree(atree, 'A new name', atree.groupName, 123456)

        then: 'The name and referenceID are changed'
        atree == tree2
        atree.name == 'A new name'
        atree.groupName == 'aGroup'
        atree.referenceId == 123456

        when: 'I change nothing'

        Tree tree3 = service.editTree(atree, 'A new name', atree.groupName, 123456)

        then: 'everything remains the same'
        atree == tree3
        atree.name == 'A new name'
        atree.groupName == 'aGroup'
        atree.referenceId == 123456

        when: 'I change the group and referenceId'

        Tree tree4 = service.editTree(atree, atree.name, 'A different group', null)

        then: 'changes as expected'
        atree == tree4
        atree.name == 'A new name'
        atree.groupName == 'A different group'
        atree.referenceId == null

        when: 'I give a null name'

        service.editTree(atree, null, atree.groupName, null)

        then: 'I get a bad argument exception'
        thrown BadArgumentsException

        when: 'I give a null group name'

        service.editTree(atree, atree.name, null, null)

        then: 'I get a bad argument exception'
        thrown BadArgumentsException

        when: 'I give a name that is the same as another tree'

        service.editTree(atree, btree.name, atree.groupName, null)

        then: 'I get a object exists exception'
        thrown ObjectExistsException
    }

    def "test creating a tree version"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)

        expect:
        tree

        when: 'I create a new version on a new tree without a version'
        TreeVersion version = service.createTreeVersion(tree, null, 'my first draft')

        then: 'A new version is created on that tree'
        version
        version.tree == tree
        version.draftName == 'my first draft'
        tree.treeVersions.contains(version)

        when: 'I add some test elements to the version'
        makeTestElements(version, testElementData())
        println version.treeElements

        then: 'It should have 30 tree elements'
        version.treeElements.size() == 30
        version.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(9284583, version))
        version.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(8511644, version))
        version.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(8014176, version))
        TreeElement.findByTreeElementIdAndTreeVersion(8014176, version).simpleName == 'Cycadidae'

        when: 'I make a new version from this version'
        TreeVersion version2 = service.createTreeVersion(tree, version, 'my second draft')
        println version2.treeElements

        then: 'It should copy the elements and set the previous version'
        version2
        version2.draftName == 'my second draft'
        version != version2
        version.id != version2.id
        version2.previousVersion == version
        version2.treeElements.size() == 30
        version2.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(9284583, version2))
        version2.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(8511644, version2))
        version2.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(8014176, version2))
        TreeElement.findByTreeElementIdAndTreeVersion(8014176, version2).simpleName == 'Cycadidae'

        when: 'I publish a draft version'
        TreeVersion version2published = service.publishTreeVersion(version2, 'testy mctestface', 'Publishing draft as a test')

        then: 'It should be published and set as the current version on the tree'
        version2published
        version2published.published
        version2published == version2
        version2published.logEntry == 'Publishing draft as a test'
        version2published.publishedBy == 'testy mctestface'
        tree.currentTreeVersion == version2published

        when: 'I create a default draft'
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')

        then: 'It copies the current version and sets it as the defaultDraft'
        draftVersion
        draftVersion != tree.currentTreeVersion
        tree.defaultDraftTreeVersion == draftVersion
        draftVersion.previousVersion == version2published
        draftVersion.treeElements.size() == 30
        draftVersion.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(9284583, draftVersion))
        draftVersion.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(8511644, draftVersion))
        draftVersion.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(8014176, draftVersion))
        TreeElement.findByTreeElementIdAndTreeVersion(8014176, draftVersion).simpleName == 'Cycadidae'

        when: 'I set the first draft version as the default'
        TreeVersion draftVersion2 = service.setDefaultDraftVersion(version)

        then: 'It replaces draftVersion as the defaultDraft'
        draftVersion2
        draftVersion2 == version
        tree.defaultDraftTreeVersion == draftVersion2

        when: 'I try and set a published version as the default draft'
        service.setDefaultDraftVersion(version2published)

        then: 'It fails with bad argument'
        thrown BadArgumentsException
    }

    def "test making and deleting a tree"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')

        expect:
        tree
        draftVersion
        draftVersion.treeElements.size() == 30
        publishedVersion
        publishedVersion.treeElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion

        when: 'I delete the tree'
        service.deleteTree(tree)

        then: 'the tree it\'s versions and their elements are gone'
        Tree.get(tree.id) == null
        TreeVersion.get(draftVersion.id) == null
        TreeVersion.get(publishedVersion.id) == null
        TreeElement.findByTreeVersion(draftVersion) == null
    }

    def "test making and deleting a tree version"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')

        expect:
        tree
        draftVersion
        draftVersion.treeElements.size() == 30
        publishedVersion
        publishedVersion.treeElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion

        when: 'I delete the tree'
        tree = service.deleteTreeVersion(draftVersion)
        publishedVersion.refresh() //the refresh() is required by deleteTreeVersion

        then: 'the tree it\'s versions and their elements are gone'
        tree
        tree.currentTreeVersion == publishedVersion
        //the below should no longer exist.
        tree.defaultDraftTreeVersion == null
        TreeVersion.get(draftVersion.id) == null
        TreeElement.executeQuery('select element from TreeElement element where treeVersion.id = :draftVersionId',
                [draftVersionId: draftVersion.id]).empty
    }

    def "test check synonyms"() {
        given:
        Tree tree = Tree.findByName('APC')
        TreeVersion treeVersion = tree.currentTreeVersion
        Instance ficusVirensSublanceolata = Instance.get(692695)

        expect:
        tree
        treeVersion
        ficusVirensSublanceolata

        when: 'I try to place Ficus virens var. sublanceolata sensu Jacobs & Packard (1981)'
        TaxonData taxonData = service.elementDataFromInstance(ficusVirensSublanceolata)
        List<Map> existingSynonyms = service.checkSynonyms(taxonData, treeVersion)
        println existingSynonyms

        then: 'I get two found synonyms'
        existingSynonyms != null
        !existingSynonyms.empty
        existingSynonyms.size() == 2
        existingSynonyms.first().simpleName == 'Ficus virens'
    }

    def "test check validation, relationship instance"() {
        when: "I try to get taxonData for a relationship instance"
        Instance relationshipInstance = Instance.get(889353)
        TaxonData taxonData = service.elementDataFromInstance(relationshipInstance)

        then: 'I get null'
        taxonData == null
    }

    def "test check validation, existing instance"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, doodiaElementData, asperaElementData])
        Instance asperaInstance = Instance.get(781104)
        TreeElement doodiaElement = TreeElement.findBySimpleNameAndTreeVersion('Doodia', draftVersion)
        TreeElement asperaElement = TreeElement.findBySimpleNameAndTreeVersion('Doodia aspera', draftVersion)

        //return a url that matches the name link of aspera
        service.linkService.getPreferredLinkForObject(asperaInstance.name) >> 'http://something that does not match the name'
        service.linkService.getPreferredLinkForObject(asperaInstance) >> asperaElement.instanceLink

        expect:
        tree
        draftVersion
        doodiaElement
        asperaInstance
        asperaElement

        when: 'I try to place Doodia aspera'
        TaxonData taxonData = service.elementDataFromInstance(asperaInstance)
        service.validateNewElementPlacement(doodiaElement, taxonData)

        then: 'I get bad argument, instance already on the tree'
        def e = thrown(BadArgumentsException)
        e.message == "$tree.name version $draftVersion.id already contains taxon $asperaElement.instanceLink. See $asperaElement.elementLink"

    }

    def "test check validation, ranked above parent"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, asperaElementData])
        Instance doodiaInstance = Instance.get(578615)
        TreeElement blechnaceaeElement = TreeElement.findBySimpleNameAndTreeVersion('Blechnaceae', draftVersion)
        TreeElement asperaElement = TreeElement.findBySimpleNameAndTreeVersion('Doodia aspera', draftVersion)

        //these shouldn't matter so long as they're not on the draft tree
        service.linkService.getPreferredLinkForObject(doodiaInstance.name) >> 'http://blah/name/apni/70914'
        service.linkService.getPreferredLinkForObject(doodiaInstance) >> 'http://blah/instance/apni/578615'

        expect:
        tree
        draftVersion
        draftVersion.treeElements.size() == 2
        tree.defaultDraftTreeVersion == draftVersion
        blechnaceaeElement
        asperaElement
        doodiaInstance

        when: 'I try to place Doodia under Doodia aspera '
        TaxonData taxonData = service.elementDataFromInstance(doodiaInstance)
        service.validateNewElementPlacement(asperaElement, taxonData)

        then: 'I get bad argument, doodia aspera ranked below doodia'
        def e = thrown(BadArgumentsException)
        e.message == 'Name Doodia of rank Genus is not below rank Species of Doodia aspera.'

        when: 'I try to place Doodia under Blechnaceae'
        taxonData = service.elementDataFromInstance(doodiaInstance)
        service.validateNewElementPlacement(blechnaceaeElement, taxonData)

        then: 'it should work'
        notThrown(BadArgumentsException)
    }

    def "test check validation, nomIlleg nomInval"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, asperaElementData])
        Instance doodiaInstance = Instance.get(578615)
        TreeElement blechnaceaeElement = TreeElement.findBySimpleNameAndTreeVersion('Blechnaceae', draftVersion)

        //these shouldn't matter so long as they're not on the draft tree
        service.linkService.getPreferredLinkForObject(doodiaInstance.name) >> 'http://blah/name/apni/70914'
        service.linkService.getPreferredLinkForObject(doodiaInstance) >> 'http://blah/instance/apni/578615'

        expect:
        tree
        draftVersion
        draftVersion.treeElements.size() == 2
        tree.defaultDraftTreeVersion == draftVersion
        blechnaceaeElement
        doodiaInstance

        when: 'I try to place a nomIlleg name'
        def taxonData = service.elementDataFromInstance(doodiaInstance)
        taxonData.nomIlleg = true
        List<String> warnings = service.validateNewElementPlacement(blechnaceaeElement, taxonData)

        then: 'I get a warning, nomIlleg'
        warnings
        warnings.first() == 'Doodia is nomIlleg'

        when: 'I try to place a nomInval name'
        taxonData.nomIlleg = false
        taxonData.nomInval = true
        warnings = service.validateNewElementPlacement(blechnaceaeElement, taxonData)

        then: 'I get a warning, nomInval'
        warnings
        warnings.first() == 'Doodia is nomInval'

    }

    def "test check validation, existing name"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, doodiaElementData, asperaElementData])
        Instance asperaInstance = Instance.get(781104)
        TreeElement doodiaElement = TreeElement.findBySimpleNameAndTreeVersion('Doodia', draftVersion)
        TreeElement asperaElement = TreeElement.findBySimpleNameAndTreeVersion('Doodia aspera', draftVersion)

        //return a url that matches the name link of aspera
        service.linkService.getPreferredLinkForObject(asperaInstance.name) >> 'http://localhost:7070/nsl-mapper/name/apni/70944'
        service.linkService.getPreferredLinkForObject(asperaInstance) >> 'http://something that does not match the instance'

        expect:
        tree
        draftVersion
        doodiaElement
        asperaInstance
        asperaElement

        when: 'I try to place Doodia aspera under Doodia'
        TaxonData taxonData = service.elementDataFromInstance(asperaInstance)
        service.validateNewElementPlacement(doodiaElement, taxonData)

        then: 'I get bad argument, name is already on the tree'
        def e = thrown(BadArgumentsException)
        e.message == "$tree.name version $draftVersion.id already contains name $taxonData.nameLink. See $asperaElement.elementLink"

    }

    private static makeTestElements(TreeVersion version, List<Map> elementData) {
        elementData.each { Map data ->
            TreeElement parent = null
            if (data.parent_element_id) {
                parent = TreeElement.get(new TreeElement(treeVersion: version, treeElementId: data.parent_element_id))
            }

            version.addToTreeElements([
                    treeVersion      : version,
                    treeElementId    : data.tree_element_id,
                    displayString    : data.display_string,
                    elementLink      : data.element_link,
                    excluded         : data.excluded,
                    instanceId       : data.instance_id,
                    instanceLink     : data.instance_link,
                    nameId           : data.name_id,
                    nameLink         : data.name_link,
                    namePath         : data.name_path,
                    names            : data.names,
                    parentElement    : parent,
                    profile          : data.profile,
                    rankPath         : data.rank_path,
                    simpleName       : data.simple_name,
                    sourceElementLink: data.source_element_link,
                    sourceShard      : data.source_shard,
                    synonyms         : data.synonyms,
                    treePath         : data.tree_path,
                    updatedAt        : data.updated_at,
                    updatedBy        : data.updated_by
            ])
            version.save()
        }
        version.save(flush: true)
        version.refresh()
    }

    private static Map doodiaElementData = [
            "tree_version_id"    : 146,
            "tree_element_id"    : 2910041,
            "lock_version"       : 0,
            "display_string"     : "<div class='tr Regnum Division Classis Subclassis Ordo Familia'><x><x><x><x><x><x><data><scientific><name id='70914'><element class='Doodia'>Doodia</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific> <citation>Parris, B.S. in McCarthy, P.M. (ed.) (1998), Doodia. <i>Flora of Australia</i> 48</citation></data></x></x></x></x></x></x></div>",
            "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2910041",
            "excluded"           : false,
            "instance_id"        : 578615,
            "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/578615",
            "name_id"            : 70914,
            "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/70914",
            "name_path"          : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia",
            "names"              : "|Doodia",
            "parent_version_id"  : 146,
            "parent_element_id"  : 8032171,
            "previous_version_id": 145,
            "previous_element_id": 2910041,
            "profile"            : ['APC Dist.': ['value': 'NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas', 'source_id': 14274, 'created_at': '2008-08-06T00:00:00+10:00', 'created_by': 'BRONWYNC', 'updated_at': '2008-08-06T00:00:00+10:00', 'updated_by': 'BRONWYNC', 'source_system': 'APC_CONCEPT'], 'APC Comment': ['value': '<i>Doodia</i> R.Br. is included in <i>Blechnum</i> L. in NSW.', 'source_id': null, 'created_at': '2016-06-10T15:22:41.742+10:00', 'created_by': 'blepschi', 'updated_at': '2016-06-10T15:23:01.201+10:00', 'updated_by': 'blepschi', 'source_system': null]],
            "rank_path"          : ['Ordo': ['id': 223583, 'name': 'Polypodiales'], 'Genus': ['id': 70914, 'name': 'Doodia'], 'Regnum': ['id': 54717, 'name': 'Plantae'], 'Classis': ['id': 223519, 'name': 'Equisetopsida'], 'Familia': ['id': 222592, 'name': 'Blechnaceae'], 'Division': ['id': 224706, 'name': 'Charophyta'], 'Subclassis': ['id': 224852, 'name': 'Polypodiidae']],
            "simple_name"        : "Doodia",
            "source_element_link": null,
            "source_shard"       : "APNI",
            "synonyms"           : null,
            "tree_path"          : "9284583/9284582/9284581/9134301/9032536/8032171/2910041",
            "updated_at"         : "2017-07-11 00:00:00.000000",
            "updated_by"         : "import"
    ]

    private static Map blechnaceaeElementData = [
            "tree_version_id"    : 146,
            "tree_element_id"    : 8032171,
            "lock_version"       : 0,
            "display_string"     : "<div class='tr Regnum Division Classis Subclassis Ordo'><x><x><x><x><x><data><scientific><name id='222592'><element class='Blechnaceae'>Blechnaceae</element> <authors><author id='8244' title='Newman, E.'>Newman</author></authors></name></scientific> <citation>CHAH (2009), <i>Australian Plant Census</i></citation></data></x></x></x></x></x></div>",
            "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8032171",
            "excluded"           : false,
            "instance_id"        : 651382,
            "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/651382",
            "name_id"            : 222592,
            "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/222592",
            "name_path"          : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae",
            "names"              : "|Blechnaceae",
            "parent_version_id"  : 146,
            "parent_element_id"  : 9032536,
            "previous_version_id": 145,
            "previous_element_id": 8032171,
            "profile"            : ['APC Dist.': ['value': 'WA, NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas, MI', 'source_id': 22346, 'created_at': '2009-10-27T00:00:00+11:00', 'created_by': 'KIRSTENC', 'updated_at': '2009-10-27T00:00:00+11:00', 'updated_by': 'KIRSTENC', 'source_system': 'APC_CONCEPT']],
            "rank_path"          : ['Ordo': ['id': 223583, 'name': 'Polypodiales'], 'Regnum': ['id': 54717, 'name': 'Plantae'], 'Classis': ['id': 223519, 'name': 'Equisetopsida'], 'Familia': ['id': 222592, 'name': 'Blechnaceae'], 'Division': ['id': 224706, 'name': 'Charophyta'], 'Subclassis': ['id': 224852, 'name': 'Polypodiidae']],
            "simple_name"        : "Blechnaceae",
            "source_element_link": null,
            "source_shard"       : "APNI",
            "synonyms"           : null,
            "tree_path"          : "9284583/9284582/9284581/9134301/9032536/8032171",
            "updated_at"         : "2017-07-11 00:00:00.000000",
            "updated_by"         : "import"
    ]
    private static Map asperaElementData = [
            "tree_version_id"    : 146,
            "tree_element_id"    : 2895769,
            "lock_version"       : 0,
            "display_string"     : "<div class='tr Regnum Division Classis Subclassis Ordo Familia Genus'><x><x><x><x><x><x><x><data><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific> <citation>CHAH (2014), <i>Australian Plant Census</i></citation></data></x></x></x></x></x></x></x></div>",
            "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2895769",
            "excluded"           : false,
            "instance_id"        : 781104,
            "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/781104",
            "name_id"            : 70944,
            "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/70944",
            "name_path"          : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia/aspera",
            "names"              : "|Doodia aspera|Blechnum neohollandicum|Doodia aspera var. angustifrons|Doodia aspera var. aspera|Woodwardia aspera",
            "parent_version_id"  : 146,
            "parent_element_id"  : 2910041,
            "previous_version_id": 145,
            "previous_element_id": 2895769,
            "profile"            : ['APC Dist.': ['value': 'Qld, NSW, LHI, NI, Vic, Tas', 'source_id': 42500, 'created_at': '2014-03-25T00:00:00+11:00', 'created_by': 'KIRSTENC', 'updated_at': '2014-03-25T14:04:06+11:00', 'updated_by': 'KIRSTENC', 'source_system': 'APC_CONCEPT'], 'APC Comment': ['value': 'Treated as <i>Blechnum neohollandicum</i> Christenh. in NSW.', 'source_id': null, 'created_at': '2016-06-10T15:21:38.135+10:00', 'created_by': 'blepschi', 'updated_at': '2016-06-10T15:21:38.135+10:00', 'updated_by': 'blepschi', 'source_system': null]],
            "rank_path"          : ['Ordo': ['id': 223583, 'name': 'Polypodiales'], 'Genus': ['id': 70914, 'name': 'Doodia'], 'Regnum': ['id': 54717, 'name': 'Plantae'], 'Classis': ['id': 223519, 'name': 'Equisetopsida'], 'Familia': ['id': 222592, 'name': 'Blechnaceae'], 'Species': ['id': 70944, 'name': 'aspera'], 'Division': ['id': 224706, 'name': 'Charophyta'], 'Subclassis': ['id': 224852, 'name': 'Polypodiidae']],
            "simple_name"        : "Doodia aspera",
            "source_element_link": null,
            "source_shard"       : "APNI",
            "synonyms"           : ['Woodwardia aspera': ['type': 'nomenclatural synonym', 'name_id': 106698], 'Blechnum neohollandicum': ['type': 'taxonomic synonym', 'name_id': 239547], 'Doodia aspera var. aspera': ['type': 'nomenclatural synonym', 'name_id': 70967], 'Doodia aspera var. angustifrons': ['type': 'taxonomic synonym', 'name_id': 70959]],
            "tree_path"          : "9284583/9284582/9284581/9134301/9032536/8032171/2910041/2895769",
            "updated_at"         : "2017-07-11 00:00:00.000000",
            "updated_by"         : "import"
    ]

    private static List<Map> testElementData() {
        [
                [
                        tree_version_id    : 146,
                        tree_element_id    : 9284583,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum"><data><scientific><name id="54717"><element class="Plantae">Plantae</element> <authors><author id="3882" title="Haeckel, Ernst Heinrich Philipp August">Haeckel</author></authors></name></scientific> <citation>CHAH (2012), <i>Australian Plant Census</i></citation></data></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/9284583',
                        excluded           : false,
                        instance_id        : 738442,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/738442',
                        name_id            : 54717,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/54717',
                        name_path          : 'Plantae',
                        names              : '|Plantae',
                        parent_version_id  : null,
                        parent_element_id  : null,
                        previous_version_id: null,
                        previous_element_id: null,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"]],
                        simple_name        : 'Plantae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8513225,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum"><x><data><scientific><name id="238892"><element class="Anthocerotophyta">Anthocerotophyta</element> <authors><ex id="7053" title="Rothmaler, W.H.P.">Rothm.</ex> ex <author id="5307" title="Stotler,R.E. &amp; Crandall-Stotler,B.J.">Stotler & Crand.-Stotl.</author></authors></name></scientific> <citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8513225',
                        excluded           : false,
                        instance_id        : 8509445,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/8509445',
                        name_id            : 238892,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/238892',
                        name_path          : 'Plantae/Anthocerotophyta',
                        names              : '|Anthocerotophyta',
                        parent_version_id  : 146,
                        parent_element_id  : 9284583,
                        previous_version_id: 145,
                        previous_element_id: 8513225,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Division": ["id": 238892, "name": "Anthocerotophyta"]],
                        simple_name        : 'Anthocerotophyta',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8513225',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8513223,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division"><x><x><data><scientific><name id="238893"><element class="Anthocerotopsida">Anthocerotopsida</element> <authors><ex id="2484" title="de Bary, H.A.">de Bary</ex> ex <author id="3443" title="Janczewski, E. von G.">Jancz.</author></authors></name></scientific> <citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8513223',
                        excluded           : false,
                        instance_id        : 8509444,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/8509444',
                        name_id            : 238893,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/238893',
                        name_path          : 'Plantae/Anthocerotophyta/Anthocerotopsida',
                        names              : '|Anthocerotopsida',
                        parent_version_id  : 146,
                        parent_element_id  : 8513225,
                        previous_version_id: 145,
                        previous_element_id: 8513223,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"]],
                        simple_name        : 'Anthocerotopsida',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8513225/8513223',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8513221,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="240384"><element class="Anthocerotidae">Anthocerotidae</element> <authors><author id="6453" title="Rosenvinge, J.L.A.K.">Rosenv.</author></authors></name></scientific> <citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8513221',
                        excluded           : false,
                        instance_id        : 8509443,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/8509443',
                        name_id            : 240384,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/240384',
                        name_path          : 'Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae',
                        names              : '|Anthocerotidae',
                        parent_version_id  : 146,
                        parent_element_id  : 8513223,
                        previous_version_id: 145,
                        previous_element_id: 8513221,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simple_name        : 'Anthocerotidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8513225/8513223/8513221',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8513224,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="240391"><element class="Dendrocerotidae">Dendrocerotidae</element> <authors><author id="5261" title="Duff, R.J., Villarreal, J.C., Cargill, D.C. &amp; Renzaglia, K.S.">Duff, J.C.Villarreal, Cargill & Renzaglia</author></authors></name></scientific> <citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8513224',
                        excluded           : false,
                        instance_id        : 8511897,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/8511897',
                        name_id            : 240391,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/240391',
                        name_path          : 'Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae',
                        names              : '|Dendrocerotidae',
                        parent_version_id  : 146,
                        parent_element_id  : 8513223,
                        previous_version_id: 145,
                        previous_element_id: 8513224,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        simple_name        : 'Dendrocerotidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8513225/8513223/8513224',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8511644,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="240386"><element class="Notothylatidae">Notothylatidae</element> <authors><author id="5261" title="Duff, R.J., Villarreal, J.C., Cargill, D.C. &amp; Renzaglia, K.S.">Duff, J.C.Villarreal, Cargill & Renzaglia</author></authors></name></scientific> <citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8511644',
                        excluded           : false,
                        instance_id        : 8510450,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/8510450',
                        name_id            : 240386,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/240386',
                        name_path          : 'Plantae/Anthocerotophyta/Anthocerotopsida/Notothylatidae',
                        names              : '|Notothylatidae',
                        parent_version_id  : 146,
                        parent_element_id  : 8513223,
                        previous_version_id: 145,
                        previous_element_id: 8511644,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240386, "name": "Notothylatidae"]],
                        simple_name        : 'Notothylatidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8513225/8513223/8511644',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 9284582,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum"><x><data><scientific><name id="224706"><element class="Charophyta">Charophyta</element> <authors><author id="5865" title="Sachs, J.">Sachs</author></authors></name></scientific> <citation>CHAH (2009), <i>Australian Plant Census</i></citation></data></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/9284582',
                        excluded           : false,
                        instance_id        : 654810,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/654810',
                        name_id            : 224706,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/224706',
                        name_path          : 'Plantae/Charophyta',
                        names              : '|Charophyta|Characeae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284583,
                        previous_version_id: 145,
                        previous_element_id: 9261321,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Division": ["id": 224706, "name": "Charophyta"]],
                        simple_name        : 'Charophyta',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : ["Characeae": "orthographic variant"],
                        tree_path          : '9284583/9284582',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 9284581,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division"><x><x><data><scientific><name id="223519"><element class="Equisetopsida">Equisetopsida</element> <authors><author id="1475" title="Agardh, C.A.">C.Agardh</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/9284581',
                        excluded           : false,
                        instance_id        : 653303,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/653303',
                        name_id            : 223519,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/223519',
                        name_path          : 'Plantae/Charophyta/Equisetopsida',
                        names              : '|Equisetopsida|Equisetaceae|Sphenopsida',
                        parent_version_id  : 146,
                        parent_element_id  : 9284582,
                        previous_version_id: 145,
                        previous_element_id: 9261320,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"]],
                        simple_name        : 'Equisetopsida',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : ["Sphenopsida": "taxonomic synonym", "Equisetaceae": "orthographic variant"],
                        tree_path          : '9284583/9284582/9284581',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8014176,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="225054"><element class="Cycadidae">Cycadidae</element> <authors><author id="6898" title="Pax, F.A.">Pax</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8014176',
                        excluded           : false,
                        instance_id        : 655417,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/655417',
                        name_id            : 225054,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/225054',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Cycadidae',
                        names              : '|Cycadidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 8014176,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 225054, "name": "Cycadidae"]],
                        simple_name        : 'Cycadidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/8014176',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2887470,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="224784"><element class="Equisetidae">Equisetidae</element> <authors><author id="10187" title="Warming, J.A.B.">Warm.</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2887470',
                        excluded           : false,
                        instance_id        : 654946,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/654946',
                        name_id            : 224784,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/224784',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Equisetidae',
                        names              : '|Equisetidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 2887470,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224784, "name": "Equisetidae"]],
                        simple_name        : 'Equisetidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/2887470',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8330979,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="224758"><element class="Lycopodiidae">Lycopodiidae</element> <authors><author id="5868" title="Beketov, A.N.">Bek.</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8330979',
                        excluded           : false,
                        instance_id        : 654891,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/654891',
                        name_id            : 224758,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/224758',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Lycopodiidae',
                        names              : '|Lycopodiidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 8330979,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224758, "name": "Lycopodiidae"]],
                        simple_name        : 'Lycopodiidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/8330979',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 9284580,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="214954"><element class="Magnoliidae">Magnoliidae</element> <authors><ex id="5507" title="Novak, F.A.">Novak</ex> ex <author id="10145" title="Takhtajan, A.L.">Takht.</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/9284580',
                        excluded           : false,
                        instance_id        : 655769,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/655769',
                        name_id            : 214954,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/214954',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Magnoliidae',
                        names              : '|Magnoliidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 9261319,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 214954, "name": "Magnoliidae"]],
                        simple_name        : 'Magnoliidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/9284580',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2917478,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="224786"><element class="Marattiidae">Marattiidae</element> <authors><author id="8389" title="Klinge, J.">Klinge</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2917478',
                        excluded           : false,
                        instance_id        : 654963,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/654963',
                        name_id            : 224786,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/224786',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Marattiidae',
                        names              : '|Marattiidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 2917478,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224786, "name": "Marattiidae"]],
                        simple_name        : 'Marattiidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/2917478',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2909562,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="224850"><element class="Ophioglossidae">Ophioglossidae</element> <authors><author id="8389" title="Klinge, J.">Klinge</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2909562',
                        excluded           : false,
                        instance_id        : 655050,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/655050',
                        name_id            : 224850,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/224850',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Ophioglossidae',
                        names              : '|Ophioglossidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 2909562,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224850, "name": "Ophioglossidae"]],
                        simple_name        : 'Ophioglossidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/2909562',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8905704,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="225446"><element class="Pinidae">Pinidae</element> <authors><author id="5508" title="Cronquist, A.J., Takhtajan, A.L. &amp; Zimmermann, Walter M.">Cronquist, Takht. & W.Zimm.</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8905704',
                        excluded           : false,
                        instance_id        : 655753,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/655753',
                        name_id            : 225446,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/225446',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Pinidae',
                        names              : '|Pinidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 8905704,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 225446, "name": "Pinidae"]],
                        simple_name        : 'Pinidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/8905704',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 9134301,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="224852"><element class="Polypodiidae">Polypodiidae</element> <authors><author id="5508" title="Cronquist, A.J., Takhtajan, A.L. &amp; Zimmermann, Walter M.">Cronquist, Takht. & W.Zimm.</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/9134301',
                        excluded           : false,
                        instance_id        : 655059,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/655059',
                        name_id            : 224852,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/224852',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Polypodiidae',
                        names              : '|Polypodiidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 9134301,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
                        simple_name        : 'Polypodiidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/9134301',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2918370,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="225440"><element class="Psilotidae">Psilotidae</element> <authors><author id="2549" title="Reveal, J.L.">Reveal</author></authors></name></scientific> <citation>CHAH (2010), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2918370',
                        excluded           : false,
                        instance_id        : 655744,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/655744',
                        name_id            : 225440,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/225440',
                        name_path          : 'Plantae/Charophyta/Equisetopsida/Psilotidae',
                        names              : '|Psilotidae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284581,
                        previous_version_id: 145,
                        previous_element_id: 2918370,
                        profile            : null,
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 225440, "name": "Psilotidae"]],
                        simple_name        : 'Psilotidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/9284582/9284581/2918370',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8303633,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum"><x><data><scientific><name id="237378"><element class="Marchantiophyta">Marchantiophyta</element> <authors><author id="5307" title="Stotler,R.E. &amp; Crandall-Stotler,B.J.">Stotler & Crand.-Stotl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8303633',
                        excluded           : false,
                        instance_id        : 742148,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742148',
                        name_id            : 237378,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237378',
                        name_path          : 'Plantae/Marchantiophyta',
                        names              : '|Marchantiophyta',
                        parent_version_id  : 146,
                        parent_element_id  : 9284583,
                        previous_version_id: 145,
                        previous_element_id: 8303633,
                        profile            : ["APC Dist.": ["value": "WA, NT, SA, Qld (native and naturalised), NSW (native and naturalised), LHI, ACT (native and naturalised), Vic (native and naturalised), Tas (native and naturalised), HI, MI", "source_id": 33908, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2016-11-07T10:52:02.643+11:00", "updated_by": "amonro", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Division": ["id": 237378, "name": "Marchantiophyta"]],
                        simple_name        : 'Marchantiophyta',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2915863,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division"><x><x><data><scientific><name id="237379"><element class="Haplomitriopsida">Haplomitriopsida</element> <authors><author id="5307" title="Stotler,R.E. &amp; Crandall-Stotler,B.J.">Stotler & Crand.-Stotl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2915863',
                        excluded           : false,
                        instance_id        : 742149,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742149',
                        name_id            : 237379,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237379',
                        name_path          : 'Plantae/Marchantiophyta/Haplomitriopsida',
                        names              : '|Haplomitriopsida',
                        parent_version_id  : 146,
                        parent_element_id  : 8303633,
                        previous_version_id: 145,
                        previous_element_id: 2915863,
                        profile            : ["APC Dist.": ["value": "NSW, Vic, Tas", "source_id": 33909, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237379, "name": "Haplomitriopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"]],
                        simple_name        : 'Haplomitriopsida',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/2915863',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2913324,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="237382"><element class="Haplomitriidae">Haplomitriidae</element> <authors><author id="5307" title="Stotler,R.E. &amp; Crandall-Stotler,B.J.">Stotler & Crand.-Stotl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2913324',
                        excluded           : false,
                        instance_id        : 742150,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742150',
                        name_id            : 237382,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237382',
                        name_path          : 'Plantae/Marchantiophyta/Haplomitriopsida/Haplomitriidae',
                        names              : '|Haplomitriidae',
                        parent_version_id  : 146,
                        parent_element_id  : 2915863,
                        previous_version_id: 145,
                        previous_element_id: 2913324,
                        profile            : ["APC Dist.": ["value": "NSW, Tas", "source_id": 33910, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237379, "name": "Haplomitriopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"], "Subclassis": ["id": 237382, "name": "Haplomitriidae"]],
                        simple_name        : 'Haplomitriidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/2915863/2913324',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2887183,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="237381"><element class="Treubiidae">Treubiidae</element> <authors><author id="5307" title="Stotler,R.E. &amp; Crandall-Stotler,B.J.">Stotler & Crand.-Stotl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2887183',
                        excluded           : false,
                        instance_id        : 742151,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742151',
                        name_id            : 237381,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237381',
                        name_path          : 'Plantae/Marchantiophyta/Haplomitriopsida/Treubiidae',
                        names              : '|Treubiidae',
                        parent_version_id  : 146,
                        parent_element_id  : 2915863,
                        previous_version_id: 145,
                        previous_element_id: 2887183,
                        profile            : ["APC Dist.": ["value": "Vic, Tas", "source_id": 33911, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237379, "name": "Haplomitriopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"], "Subclassis": ["id": 237381, "name": "Treubiidae"]],
                        simple_name        : 'Treubiidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/2915863/2887183',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8303632,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division"><x><x><data><scientific><name id="237380"><element class="Jungermanniopsida">Jungermanniopsida</element> <authors><author id="5307" title="Stotler,R.E. &amp; Crandall-Stotler,B.J.">Stotler & Crand.-Stotl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8303632',
                        excluded           : false,
                        instance_id        : 742169,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742169',
                        name_id            : 237380,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237380',
                        name_path          : 'Plantae/Marchantiophyta/Jungermanniopsida',
                        names              : '|Jungermanniopsida',
                        parent_version_id  : 146,
                        parent_element_id  : 8303633,
                        previous_version_id: 145,
                        previous_element_id: 8303632,
                        profile            : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, LHI, ACT, Vic, Tas, HI, MI", "source_id": 33929, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237380, "name": "Jungermanniopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"]],
                        simple_name        : 'Jungermanniopsida',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/8303632',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 8303631,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="237419"><element class="Jungermanniidae">Jungermanniidae</element> <authors><author id="6910" title="Engler, H.G.A.">Engl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/8303631',
                        excluded           : false,
                        instance_id        : 742185,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742185',
                        name_id            : 237419,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237419',
                        name_path          : 'Plantae/Marchantiophyta/Jungermanniopsida/Jungermanniidae',
                        names              : '|Jungermanniidae',
                        parent_version_id  : 146,
                        parent_element_id  : 8303632,
                        previous_version_id: 145,
                        previous_element_id: 8303631,
                        profile            : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, LHI, ACT, Vic, Tas, MI", "source_id": 33944, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237380, "name": "Jungermanniopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"], "Subclassis": ["id": 237419, "name": "Jungermanniidae"]],
                        simple_name        : 'Jungermanniidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/8303632/8303631',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2896736,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="237416"><element class="Metzgeriidae">Metzgeriidae</element> <authors><author id="6352" title="Bartholomew-Began, S.E.">Barthol.-Began</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2896736',
                        excluded           : false,
                        instance_id        : 742180,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742180',
                        name_id            : 237416,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237416',
                        name_path          : 'Plantae/Marchantiophyta/Jungermanniopsida/Metzgeriidae',
                        names              : '|Metzgeriidae',
                        parent_version_id  : 146,
                        parent_element_id  : 8303632,
                        previous_version_id: 145,
                        previous_element_id: 2896736,
                        profile            : ["APC Dist.": ["value": "WA, SA, Qld, NSW, ACT, Vic, Tas", "source_id": 33939, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237380, "name": "Jungermanniopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"], "Subclassis": ["id": 237416, "name": "Metzgeriidae"]],
                        simple_name        : 'Metzgeriidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/8303632/2896736',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2908988,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="237407"><element class="Pelliidae">Pelliidae</element> <authors><author id="6348" title="He-Nygren, X., Juslen, A., Ahonen, I., Glenny, D. &amp; Piipo, S.">He-Nygren, Juslen, Ahonen, Glenny & Piipo</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2908988',
                        excluded           : false,
                        instance_id        : 742170,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742170',
                        name_id            : 237407,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237407',
                        name_path          : 'Plantae/Marchantiophyta/Jungermanniopsida/Pelliidae',
                        names              : '|Pelliidae',
                        parent_version_id  : 146,
                        parent_element_id  : 8303632,
                        previous_version_id: 145,
                        previous_element_id: 2908988,
                        profile            : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, ACT, Vic, Tas, HI, MI", "source_id": 33930, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2012-02-24T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237380, "name": "Jungermanniopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"], "Subclassis": ["id": 237407, "name": "Pelliidae"]],
                        simple_name        : 'Pelliidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/8303632/2908988',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2917604,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division"><x><x><data><scientific><name id="237383"><element class="Marchantiopsida">Marchantiopsida</element> <authors><author id="5508" title="Cronquist, A.J., Takhtajan, A.L. &amp; Zimmermann, Walter M.">Cronquist, Takht. & W.Zimm.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2917604',
                        excluded           : false,
                        instance_id        : 742156,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742156',
                        name_id            : 237383,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237383',
                        name_path          : 'Plantae/Marchantiophyta/Marchantiopsida',
                        names              : '|Marchantiopsida',
                        parent_version_id  : 146,
                        parent_element_id  : 8303633,
                        previous_version_id: 145,
                        previous_element_id: 2917604,
                        profile            : ["APC Dist.": ["value": "WA, NT, SA, Qld (native and naturalised), NSW (native and naturalised), ACT (native and naturalised), Vic (native and naturalised), Tas (native and naturalised)", "source_id": 33916, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2016-11-07T10:51:42.227+11:00", "updated_by": "amonro", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237383, "name": "Marchantiopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"]],
                        simple_name        : 'Marchantiopsida',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/2917604',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2903297,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Division Classis"><x><x><x><data><scientific><name id="237394"><element class="Marchantiidae">Marchantiidae</element> <authors><author id="6910" title="Engler, H.G.A.">Engl.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2903297',
                        excluded           : false,
                        instance_id        : 742157,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/742157',
                        name_id            : 237394,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/237394',
                        name_path          : 'Plantae/Marchantiophyta/Marchantiopsida/Marchantiidae',
                        names              : '|Marchantiidae',
                        parent_version_id  : 146,
                        parent_element_id  : 2917604,
                        previous_version_id: 145,
                        previous_element_id: 2903297,
                        profile            : ["APC Dist.": ["value": "WA, NT, SA, Qld (native and naturalised), NSW (native and naturalised), ACT (native and naturalised), Vic (native and naturalised), Tas (native and naturalised)", "source_id": 33917, "created_at": "2012-02-24T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2016-11-07T10:51:22.407+11:00", "updated_by": "amonro", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 237383, "name": "Marchantiopsida"], "Division": ["id": 237378, "name": "Marchantiophyta"], "Subclassis": ["id": 237394, "name": "Marchantiidae"]],
                        simple_name        : 'Marchantiidae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/8303633/2917604/2903297',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2913689,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum"><x><data><scientific><name id="156250"><element class="Rhacocarpaceae">Rhacocarpaceae</element> <authors><author id="8275" title="Kindberg, N.C.">Kindb.</author></authors></name></scientific> <citation>CHAH (2011), <i>Australian Plant Census</i></citation></data></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2913689',
                        excluded           : false,
                        instance_id        : 748954,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/748954',
                        name_id            : 156250,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/156250',
                        name_path          : 'Plantae/Rhacocarpaceae',
                        names              : '|Rhacocarpaceae',
                        parent_version_id  : 146,
                        parent_element_id  : 9284583,
                        previous_version_id: 145,
                        previous_element_id: 2913689,
                        profile            : ["APC Dist.": ["value": "WA, SA", "source_id": 35166, "created_at": "2012-05-21T00:00:00+10:00", "created_by": "KIRSTENC", "updated_at": "2012-05-21T00:00:00+10:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Regnum": ["id": 54717, "name": "Plantae"], "Familia": ["id": 156250, "name": "Rhacocarpaceae"]],
                        simple_name        : 'Rhacocarpaceae',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/2913689',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2912851,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Familia"><x><x><data><scientific><name id="123661"><element class="Rhacocarpus">Rhacocarpus</element> <authors><author id="1471" title="Lindberg, S.O.">Lindb.</author></authors></name></scientific> <citation>CHAH (2006), <i>Australian Plant Census</i></citation></data></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2912851',
                        excluded           : false,
                        instance_id        : 738183,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/738183',
                        name_id            : 123661,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/123661',
                        name_path          : 'Plantae/Rhacocarpaceae/Rhacocarpus',
                        names              : '|Rhacocarpus',
                        parent_version_id  : 146,
                        parent_element_id  : 2913689,
                        previous_version_id: 145,
                        previous_element_id: 2912851,
                        profile            : ["APC Dist.": ["value": "WA, SA", "source_id": 33480, "created_at": "2012-02-08T00:00:00+11:00", "created_by": "ECLIFTON", "updated_at": "2012-02-08T00:00:00+11:00", "updated_by": "ECLIFTON", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Genus": ["id": 123661, "name": "Rhacocarpus"], "Regnum": ["id": 54717, "name": "Plantae"], "Familia": ["id": 156250, "name": "Rhacocarpaceae"]],
                        simple_name        : 'Rhacocarpus',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : null,
                        tree_path          : '9284583/2913689/2912851',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ],
                [
                        tree_version_id    : 146,
                        tree_element_id    : 2902800,
                        lock_version       : 0,
                        display_string     : '<div class="tr Regnum Familia Genus"><x><x><x><data><scientific><name id="201432"><scientific><name id="123661"><element class="Rhacocarpus">Rhacocarpus</element></name></scientific> <element class="rehmannianus">rehmannianus</element> <authors>(<base id="1445" title="M&uuml;ller, J.K.(C.)A.(F.W.)">Müll.Hal.</base>) <author id="1597" title="Wijk, R.J. van der &amp; Margadant, W.D.">Wijk & Margad.</author></authors></name></scientific> <citation>CHAH (2006), <i>Australian Plant Census</i></citation></data></x></x></x></div>',
                        element_link       : 'http://localhost:7070/nsl-mapper/tree/146/2902800',
                        excluded           : false,
                        instance_id        : 734183,
                        instance_link      : 'http://localhost:7070/nsl-mapper/instance/apni/734183',
                        name_id            : 201432,
                        name_link          : 'http://localhost:7070/nsl-mapper/name/apni/201432',
                        name_path          : 'Plantae/Rhacocarpaceae/Rhacocarpus/rehmannianus',
                        names              : '|Rhacocarpus rehmannianus|Harrisonia rehmanniana',
                        parent_version_id  : 146,
                        parent_element_id  : 2912851,
                        previous_version_id: 145,
                        previous_element_id: 2902800,
                        profile            : ["APC Dist.": ["value": "WA, SA", "source_id": 33130, "created_at": "2012-01-19T00:00:00+11:00", "created_by": "AMONRO", "updated_at": "2012-01-19T00:00:00+11:00", "updated_by": "AMONRO", "source_system": "APC_CONCEPT"]],
                        rank_path          : ["Genus": ["id": 123661, "name": "Rhacocarpus"], "Regnum": ["id": 54717, "name": "Plantae"], "Familia": ["id": 156250, "name": "Rhacocarpaceae"], "Species": ["id": 201432, "name": "rehmannianus"]],
                        simple_name        : 'Rhacocarpus rehmannianus',
                        source_element_link: null,
                        source_shard       : 'APNI',
                        synonyms           : ["Harrisonia rehmanniana": "nomenclatural synonym"],
                        tree_path          : '9284583/2913689/2912851/2902800',
                        updated_at         : '2017-07-11 00:00:00.000000',
                        updated_by         : 'import'
                ]
        ]
    }
}
