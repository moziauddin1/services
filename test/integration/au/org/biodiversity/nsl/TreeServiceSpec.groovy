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
        version.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(6722223, version))
        version.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(2891695, version))
        TreeElement.findByTreeElementIdAndTreeVersion(2891695, version).simpleName == 'Folioceros fuciformis'

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
        version2.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(6722223, version2))
        version2.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(2891695, version2))
        TreeElement.findByTreeElementIdAndTreeVersion(2891695, version2).simpleName == 'Folioceros fuciformis'

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
        draftVersion.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(6722223, draftVersion))
        draftVersion.treeElements.contains(TreeElement.findByTreeElementIdAndTreeVersion(2891695, draftVersion))
        TreeElement.findByTreeElementIdAndTreeVersion(2891695, draftVersion).simpleName == 'Folioceros fuciformis'

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
                    displayHtml      : data.display_html,
                    synonymsHtml     : data.synonyms_html,
                    elementLink      : data.element_link,
                    excluded         : data.excluded,
                    instanceId       : data.instance_id,
                    instanceLink     : data.instance_link,
                    nameId           : data.name_id,
                    nameLink         : data.name_link,
                    namePath         : data.name_path,
                    depth            : data.depth,
                    parentElement    : parent,
                    profile          : data.profile,
                    rank             : data.rank,
                    rankPath         : data.rank_path,
                    simpleName       : data.simple_name,
                    nameElement      : data.name_element,
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
            "depth"              : 7,
            "display_html"       : "<data><scientific><name id='70914'><element class='Doodia'>Doodia</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific><citation>Parris, B.S. in McCarthy, P.M. (ed.) (1998), Doodia. <i>Flora of Australia</i> 48</citation></data>",
            "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2910041",
            "excluded"           : false,
            "instance_id"        : 578615,
            "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/578615",
            "name_element"       : "Doodia",
            "name_id"            : 70914,
            "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/70914",
            "name_path"          : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia",
            "parent_version_id"  : 146,
            "parent_element_id"  : 8032171,
            "previous_version_id": 145,
            "previous_element_id": 2910041,
            "profile"            : ["APC Dist.": ["value": "NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas", "source_id": 14274, "created_at": "2008-08-06T00:00:00+10:00", "created_by": "BRONWYNC", "updated_at": "2008-08-06T00:00:00+10:00", "updated_by": "BRONWYNC", "source_system": "APC_CONCEPT"], "APC Comment": ["value": "<i>Doodia</i> R.Br. is included in <i>Blechnum</i> L. in NSW.", "source_id": null, "created_at": "2016-06-10T15:22:41.742+10:00", "created_by": "blepschi", "updated_at": "2016-06-10T15:23:01.201+10:00", "updated_by": "blepschi", "source_system": null]],
            "rank"               : "Genus",
            "rank_path"          : ["Ordo": ["id": 223583, "name": "Polypodiales"], "Genus": ["id": 70914, "name": "Doodia"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Familia": ["id": 222592, "name": "Blechnaceae"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
            "simple_name"        : "Doodia",
            "source_element_link": null,
            "source_shard"       : "APNI",
            "synonyms"           : null,
            "synonyms_html"      : "<synonyms>Not Set</synonyms>",
            "tree_path"          : "9284583/9284582/9284581/9134301/9032536/8032171/2910041",
            "updated_at"         : "2017-07-11 00:00:00.000000",
            "updated_by"         : "import"
    ]

    private static Map blechnaceaeElementData = [
            "tree_version_id"    : 146,
            "tree_element_id"    : 8032171,
            "lock_version"       : 0,
            "depth"              : 6,
            "display_html"       : "<data><scientific><name id='222592'><element class='Blechnaceae'>Blechnaceae</element> <authors><author id='8244' title='Newman, E.'>Newman</author></authors></name></scientific><citation>CHAH (2009), <i>Australian Plant Census</i></citation></data>",
            "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8032171",
            "excluded"           : false,
            "instance_id"        : 651382,
            "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/651382",
            "name_element"       : "Blechnaceae",
            "name_id"            : 222592,
            "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/222592",
            "name_path"          : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae",
            "parent_version_id"  : 146,
            "parent_element_id"  : 9032536,
            "previous_version_id": 145,
            "previous_element_id": 8032171,
            "profile"            : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas, MI", "source_id": 22346, "created_at": "2009-10-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2009-10-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
            "rank"               : "Familia",
            "rank_path"          : ["Ordo": ["id": 223583, "name": "Polypodiales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Familia": ["id": 222592, "name": "Blechnaceae"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
            "simple_name"        : "Blechnaceae",
            "source_element_link": null,
            "source_shard"       : "APNI",
            "synonyms"           : null,
            "synonyms_html"      : "<synonyms>Not Set</synonyms>",
            "tree_path"          : "9284583/9284582/9284581/9134301/9032536/8032171",
            "updated_at"         : "2017-07-11 00:00:00.000000",
            "updated_by"         : "import"
    ]

    private static Map asperaElementData = [
            "tree_version_id"    : 146,
            "tree_element_id"    : 2895769,
            "lock_version"       : 0,
            "depth"              : 8,
            "display_html"       : "<data><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific><citation>CHAH (2014), <i>Australian Plant Census</i></citation></data>",
            "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2895769",
            "excluded"           : false,
            "instance_id"        : 781104,
            "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/781104",
            "name_element"       : "aspera",
            "name_id"            : 70944,
            "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/70944",
            "name_path"          : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia/aspera",
            "parent_version_id"  : 146,
            "parent_element_id"  : 2910041,
            "previous_version_id": 145,
            "previous_element_id": 2895769,
            "profile"            : ["APC Dist.": ["value": "Qld, NSW, LHI, NI, Vic, Tas", "source_id": 42500, "created_at": "2014-03-25T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2014-03-25T14:04:06+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"], "APC Comment": ["value": "Treated as <i>Blechnum neohollandicum</i> Christenh. in NSW.", "source_id": null, "created_at": "2016-06-10T15:21:38.135+10:00", "created_by": "blepschi", "updated_at": "2016-06-10T15:21:38.135+10:00", "updated_by": "blepschi", "source_system": null]],
            "rank"               : "Species",
            "rank_path"          : ["Ordo": ["id": 223583, "name": "Polypodiales"], "Genus": ["id": 70914, "name": "Doodia"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Familia": ["id": 222592, "name": "Blechnaceae"], "Species": ["id": 70944, "name": "aspera"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
            "simple_name"        : "Doodia aspera",
            "source_element_link": null,
            "source_shard"       : "APNI",
            "synonyms"           : ["Woodwardia aspera": ["type": "nomenclatural synonym", "name_id": 106698], "Blechnum neohollandicum": ["type": "taxonomic synonym", "name_id": 239547], "Doodia aspera var. aspera": ["type": "nomenclatural synonym", "name_id": 70967], "Doodia aspera var. angustifrons": ["type": "taxonomic synonym", "name_id": 70959]],
            "synonyms_html"      : "<synonyms>Not Set</synonyms>",
            "tree_path"          : "9284583/9284582/9284581/9134301/9032536/8032171/2910041/2895769",
            "updated_at"         : "2017-07-11 00:00:00.000000",
            "updated_by"         : "import"
    ]

    private static List<Map> testElementData() {
        [
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 9284583,
                        "lock_version"       : 0,
                        "depth"              : 1,
                        "display_html"       : "<data><scientific><name id='54717'><element class='Plantae'>Plantae</element> <authors><author id='3882' title='Haeckel, Ernst Heinrich Philipp August'>Haeckel</author></authors></name></scientific><citation>CHAH (2012), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/9284583",
                        "excluded"           : false,
                        "instance_id"        : 738442,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/738442",
                        "name_element"       : "Plantae",
                        "name_id"            : 54717,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/54717",
                        "name_path"          : "Plantae",
                        "parent_version_id"  : null,
                        "parent_element_id"  : null,
                        "previous_version_id": null,
                        "previous_element_id": null,
                        "profile"            : null,
                        "rank"               : "Regnum",
                        "rank_path"          : ["Regnum": ["id": 54717, "name": "Plantae"]],
                        "simple_name"        : "Plantae",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513225,
                        "lock_version"       : 0,
                        "depth"              : 2,
                        "display_html"       : "<data><scientific><name id='238892'><element class='Anthocerotophyta'>Anthocerotophyta</element> <authors><ex id='7053' title='Rothmaler, W.H.P.'>Rothm.</ex> ex <author id='5307' title='Stotler,R.E. &amp; Crandall-Stotler,B.J.'>Stotler & Crand.-Stotl.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513225",
                        "excluded"           : false,
                        "instance_id"        : 8509445,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8509445",
                        "name_element"       : "Anthocerotophyta",
                        "name_id"            : 238892,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/238892",
                        "name_path"          : "Plantae/Anthocerotophyta",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 9284583,
                        "previous_version_id": 145,
                        "previous_element_id": 8513225,
                        "profile"            : null,
                        "rank"               : "Division",
                        "rank_path"          : ["Regnum": ["id": 54717, "name": "Plantae"], "Division": ["id": 238892, "name": "Anthocerotophyta"]],
                        "simple_name"        : "Anthocerotophyta",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513223,
                        "lock_version"       : 0,
                        "depth"              : 3,
                        "display_html"       : "<data><scientific><name id='238893'><element class='Anthocerotopsida'>Anthocerotopsida</element> <authors><ex id='2484' title='de Bary, H.A.'>de Bary</ex> ex <author id='3443' title='Janczewski, E. von G.'>Jancz.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513223",
                        "excluded"           : false,
                        "instance_id"        : 8509444,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8509444",
                        "name_element"       : "Anthocerotopsida",
                        "name_id"            : 238893,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/238893",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513225,
                        "previous_version_id": 145,
                        "previous_element_id": 8513223,
                        "profile"            : null,
                        "rank"               : "Classis",
                        "rank_path"          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"]],
                        "simple_name"        : "Anthocerotopsida",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513221,
                        "lock_version"       : 0,
                        "depth"              : 4,
                        "display_html"       : "<data><scientific><name id='240384'><element class='Anthocerotidae'>Anthocerotidae</element> <authors><author id='6453' title='Rosenvinge, J.L.A.K.'>Rosenv.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513221",
                        "excluded"           : false,
                        "instance_id"        : 8509443,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8509443",
                        "name_element"       : "Anthocerotidae",
                        "name_id"            : 240384,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/240384",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513223,
                        "previous_version_id": 145,
                        "previous_element_id": 8513221,
                        "profile"            : null,
                        "rank"               : "Subclassis",
                        "rank_path"          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthocerotidae",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513220,
                        "lock_version"       : 0,
                        "depth"              : 5,
                        "display_html"       : "<data><scientific><name id='142301'><element class='Anthocerotales'>Anthocerotales</element> <authors><author id='2624' title='Limpricht, K.G.'>Limpr.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513220",
                        "excluded"           : false,
                        "instance_id"        : 8508886,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8508886",
                        "name_element"       : "Anthocerotales",
                        "name_id"            : 142301,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/142301",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513221,
                        "previous_version_id": 145,
                        "previous_element_id": 8513220,
                        "profile"            : null,
                        "rank"               : "Ordo",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthocerotales",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513200,
                        "lock_version"       : 0,
                        "depth"              : 6,
                        "display_html"       : "<data><scientific><name id='124939'><element class='Anthocerotaceae'>Anthocerotaceae</element> <authors>(<base id='2041' title='Gray, S.F.'>Gray</base>) <author id='6855' title='Dumortier, B.C.J.'>Dumort.</author></authors></name></scientific><citation>CHAH (2011), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513200",
                        "excluded"           : false,
                        "instance_id"        : 748950,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/748950",
                        "name_element"       : "Anthocerotaceae",
                        "name_id"            : 124939,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/124939",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513220,
                        "previous_version_id": 145,
                        "previous_element_id": 8513200,
                        "profile"            : ["APC Dist.": ["value": "WA, NT, Qld, NSW, Vic, Tas", "source_id": 35164, "created_at": "2012-05-21T00:00:00+10:00", "created_by": "KIRSTENC", "updated_at": "2012-05-21T00:00:00+10:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Familia",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthocerotaceae",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 6722223,
                        "lock_version"       : 0,
                        "depth"              : 7,
                        "display_html"       : "<data><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element> <authors><author id='1426' title='Linnaeus, C.'>L.</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/6722223",
                        "excluded"           : false,
                        "instance_id"        : 668637,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/668637",
                        "name_element"       : "Anthoceros",
                        "name_id"            : 121601,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/121601",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513200,
                        "previous_version_id": 145,
                        "previous_element_id": 6722223,
                        "profile"            : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, ACT, Vic, Tas", "source_id": 27645, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Genus",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthoceros",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/6722223",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2910349,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='144273'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='capricornii'>capricornii</element> <authors><author id='1771' title='Cargill, D.C. &amp; Scott, G.A.M.'>Cargill & G.A.M.Scott</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2910349",
                        "excluded"           : false,
                        "instance_id"        : 621662,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/621662",
                        "name_element"       : "capricornii",
                        "name_id"            : 144273,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/144273",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/capricornii",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 6722223,
                        "previous_version_id": 145,
                        "previous_element_id": 2910349,
                        "profile"            : ["APC Dist.": ["value": "WA, NT", "source_id": 27646, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 144273, "name": "capricornii"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthoceros capricornii",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Anthoceros adscendens": ["type": "pro parte misapplied", "name_id": 162382]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/6722223/2910349",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2909847,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='209869'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='ferdinandi-muelleri'>ferdinandi-muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2909847",
                        "excluded"           : false,
                        "instance_id"        : 621668,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/621668",
                        "name_element"       : "ferdinandi-muelleri",
                        "name_id"            : 209869,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/209869",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/ferdinandi-muelleri",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 6722223,
                        "previous_version_id": 145,
                        "previous_element_id": 2909847,
                        "profile"            : ["APC Dist.": ["value": "?Qld", "source_id": 27647, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 209869, "name": "ferdinandi-muelleri"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthoceros ferdinandi-muelleri",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/6722223/2909847",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2916003,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='142232'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fragilis'>fragilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>Cargill, D.C., Söderström, L., Hagborg, A. & Konrat, M. von (2013), Notes on Early Land Plants Today. 23. A new synonym in Anthoceros (Anthocerotaceae, Anthocerotophyta). <i>Phytotaxa</i> 76(3)</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2916003",
                        "excluded"           : false,
                        "instance_id"        : 760852,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/760852",
                        "name_element"       : "fragilis",
                        "name_id"            : 142232,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/142232",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/fragilis",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 6722223,
                        "previous_version_id": 145,
                        "previous_element_id": 2916003,
                        "profile"            : ["Type": ["value": "Australia. Queensland: Amalie Dietrich (holotype G-61292! [=G-24322!]).", "source_id": 214282, "created_at": "2013-01-16T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2013-01-16T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "CITATION_TEXT"], "APC Dist.": ["value": "Qld", "source_id": 38619, "created_at": "2013-02-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2013-02-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 142232, "name": "fragilis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthoceros fragilis",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Anthoceros fertilis": ["type": "taxonomic synonym", "name_id": 209870]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/6722223/2916003",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2891332,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='202233'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='laminifer'>laminifer</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2891332",
                        "excluded"           : false,
                        "instance_id"        : 621709,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/621709",
                        "name_element"       : "laminifer",
                        "name_id"            : 202233,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/202233",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/laminifer",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 6722223,
                        "previous_version_id": 145,
                        "previous_element_id": 2891332,
                        "profile"            : ["APC Dist.": ["value": "Qld", "source_id": 27650, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 202233, "name": "laminifer"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthoceros laminifer",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/6722223/2891332",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2901206,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='122138'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='punctatus'>punctatus</element> <authors><author id='1426' title='Linnaeus, C.'>L.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2901206",
                        "excluded"           : false,
                        "instance_id"        : 621713,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/621713",
                        "name_element"       : "punctatus",
                        "name_id"            : 122138,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/122138",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/punctatus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 6722223,
                        "previous_version_id": 145,
                        "previous_element_id": 2901206,
                        "profile"            : ["APC Dist.": ["value": "SA, Qld, NSW, ACT, Vic, Tas", "source_id": 27651, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 122138, "name": "punctatus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Anthoceros punctatus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/6722223/2901206",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2894485,
                        "lock_version"       : 0,
                        "depth"              : 7,
                        "display_html"       : "<data><scientific><name id='134990'><element class='Folioceros'>Folioceros</element> <authors><author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2894485",
                        "excluded"           : false,
                        "instance_id"        : 669233,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/669233",
                        "name_element"       : "Folioceros",
                        "name_id"            : 134990,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/134990",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513200,
                        "previous_version_id": 145,
                        "previous_element_id": 2894485,
                        "profile"            : ["APC Dist.": ["value": "Qld, NSW", "source_id": 27661, "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Genus",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 134990, "name": "Folioceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Folioceros",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/2894485",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2891695,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='143486'><scientific><name id='134990'><element class='Folioceros'>Folioceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors>(<base id='8996' title='Montagne, J.P.F.C.'>Mont.</base>) <author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>Bhardwaj, D.C. (1975), <i>Geophytology</i> 5</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2891695",
                        "excluded"           : false,
                        "instance_id"        : 621673,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/621673",
                        "name_element"       : "fuciformis",
                        "name_id"            : 143486,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/143486",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros/fuciformis",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2894485,
                        "previous_version_id": 145,
                        "previous_element_id": 2891695,
                        "profile"            : ["APC Dist.": ["value": "Qld", "source_id": 27662, "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 134990, "name": "Folioceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 143486, "name": "fuciformis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Folioceros fuciformis",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Anthoceros fuciformis": ["type": "basionym", "name_id": 142253]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/2894485/2891695",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2896010,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='134991'><scientific><name id='134990'><element class='Folioceros'>Folioceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2896010",
                        "excluded"           : false,
                        "instance_id"        : 669234,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/669234",
                        "name_element"       : "glandulosus",
                        "name_id"            : 134991,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/134991",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros/glandulosus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2894485,
                        "previous_version_id": 145,
                        "previous_element_id": 2896010,
                        "profile"            : ["APC Dist.": ["value": "NSW", "source_id": 27663, "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 134990, "name": "Folioceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 134991, "name": "glandulosus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        "simple_name"        : "Folioceros glandulosus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Anthoceros glandulosus": ["type": "nomenclatural synonym", "name_id": 129589], "Aspiromitus glandulosus": ["type": "nomenclatural synonym", "name_id": 209879]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513221/8513220/8513200/2894485/2896010",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513224,
                        "lock_version"       : 0,
                        "depth"              : 4,
                        "display_html"       : "<data><scientific><name id='240391'><element class='Dendrocerotidae'>Dendrocerotidae</element> <authors><author id='5261' title='Duff, R.J., Villarreal, J.C., Cargill, D.C. &amp; Renzaglia, K.S.'>Duff, J.C.Villarreal, Cargill & Renzaglia</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513224",
                        "excluded"           : false,
                        "instance_id"        : 8511897,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8511897",
                        "name_element"       : "Dendrocerotidae",
                        "name_id"            : 240391,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/240391",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513223,
                        "previous_version_id": 145,
                        "previous_element_id": 8513224,
                        "profile"            : null,
                        "rank"               : "Subclassis",
                        "rank_path"          : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        "simple_name"        : "Dendrocerotidae",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513222,
                        "lock_version"       : 0,
                        "depth"              : 5,
                        "display_html"       : "<data><scientific><name id='240393'><element class='Dendrocerotales'>Dendrocerotales</element> <authors><author id='3432' title='H&auml;ssel de Men&eacute;ndez, G.G.'>Hässel</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513222",
                        "excluded"           : false,
                        "instance_id"        : 8512151,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8512151",
                        "name_element"       : "Dendrocerotales",
                        "name_id"            : 240393,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/240393",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513224,
                        "previous_version_id": 145,
                        "previous_element_id": 8513222,
                        "profile"            : null,
                        "rank"               : "Ordo",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        "simple_name"        : "Dendrocerotales",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513219,
                        "lock_version"       : 0,
                        "depth"              : 6,
                        "display_html"       : "<data><scientific><name id='193461'><element class='Dendrocerotaceae'>Dendrocerotaceae</element> <authors>(<base id='2276' title='Milde, C.A.J.'>Milde</base>) <author id='3432' title='H&auml;ssel de Men&eacute;ndez, G.G.'>Hässel</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513219",
                        "excluded"           : false,
                        "instance_id"        : 8512407,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8512407",
                        "name_element"       : "Dendrocerotaceae",
                        "name_id"            : 193461,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/193461",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513222,
                        "previous_version_id": 145,
                        "previous_element_id": 8513219,
                        "profile"            : null,
                        "rank"               : "Familia",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        "simple_name"        : "Dendrocerotaceae",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513207,
                        "lock_version"       : 0,
                        "depth"              : 7,
                        "display_html"       : "<data><scientific><name id='240394'><scientific><name id='193461'><element class='Dendrocerotaceae'>Dendrocerotaceae</element></name></scientific> <element class='Dendrocerotoideae'>Dendrocerotoideae</element> <authors><author id='1751' title='Schuster, R.M.'>R.M.Schust.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513207",
                        "excluded"           : false,
                        "instance_id"        : 8512665,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8512665",
                        "name_element"       : "Dendrocerotoideae",
                        "name_id"            : 240394,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/240394",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513219,
                        "previous_version_id": 145,
                        "previous_element_id": 8513207,
                        "profile"            : null,
                        "rank"               : "Subfamilia",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendrocerotaceae Dendrocerotoideae",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2909398,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element> <authors><author id='6893' title='Nees von Esenbeck, C.G.D.'>Nees</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2909398",
                        "excluded"           : false,
                        "instance_id"        : 668662,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/668662",
                        "name_element"       : "Dendroceros",
                        "name_id"            : 129597,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/129597",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513207,
                        "previous_version_id": 145,
                        "previous_element_id": 2909398,
                        "profile"            : ["APC Dist.": ["value": "Qld, NSW", "source_id": 27652, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Genus",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2895623,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210308'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='australis'>australis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2895623",
                        "excluded"           : false,
                        "instance_id"        : 622818,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/622818",
                        "name_element"       : "australis",
                        "name_id"            : 210308,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210308",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/australis",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2895623,
                        "profile"            : ["APC Dist.": ["value": "NSW", "source_id": 27653, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210308, "name": "australis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros australis",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2895623",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2909090,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='178505'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='crispatus'>crispatus</element> <authors>(<base id='6851' title='Hooker, W.J.'>Hook.</base>) <author id='6893' title='Nees von Esenbeck, C.G.D.'>Nees</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2909090",
                        "excluded"           : false,
                        "instance_id"        : 622823,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/622823",
                        "name_element"       : "crispatus",
                        "name_id"            : 178505,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/178505",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/crispatus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2909090,
                        "profile"            : ["APC Dist.": ["value": "Qld", "source_id": 27654, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 178505, "name": "crispatus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros crispatus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Monoclea crispata": ["type": "nomenclatural synonym", "name_id": 210309]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2909090",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2897137,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210317'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='difficilis'>difficilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>Pócs, T. & Streimann, H. (1999), Epiphyllous liverworts from Queensland, Australia. <i>Bryobrothera</i> 5</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2897137",
                        "excluded"           : false,
                        "instance_id"        : 622849,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/622849",
                        "name_element"       : "difficilis",
                        "name_id"            : 210317,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210317",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/difficilis",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2897137,
                        "profile"            : ["APC Dist.": ["value": "Qld", "source_id": 41411, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "BLEPSCHI", "updated_at": "2013-10-24T12:18:26+11:00", "updated_by": "BLEPSCHI", "source_system": "APC_CONCEPT"], "APC Comment": ["value": "Listed as \"<i>Dendroceros</i> cf. <i>difficilis</i>\" by P.M.McCarthy, <i>Checkl. Austral. Liverworts and Hornworts</i> 35 (2003).", "source_id": 41411, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "BLEPSCHI", "updated_at": "2013-10-24T12:18:26+11:00", "updated_by": "BLEPSCHI", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210317, "name": "difficilis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros difficilis",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2897137",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2897129,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210311'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='granulatus'>granulatus</element> <authors><author id='1465' title='Mitten, W.'>Mitt.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2897129",
                        "excluded"           : false,
                        "instance_id"        : 622830,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/622830",
                        "name_element"       : "granulatus",
                        "name_id"            : 210311,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210311",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/granulatus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2897129,
                        "profile"            : ["APC Dist.": ["value": "Qld", "source_id": 27656, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210311, "name": "granulatus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros granulatus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2897129",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2916733,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210313'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='muelleri'>muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2916733",
                        "excluded"           : false,
                        "instance_id"        : 772246,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/772246",
                        "name_element"       : "muelleri",
                        "name_id"            : 210313,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210313",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/muelleri",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2916733,
                        "profile"            : ["APC Dist.": ["value": "Qld, NSW", "source_id": 41410, "created_at": "2013-10-24T00:00:00+11:00", "created_by": "BLEPSCHI", "updated_at": "2013-10-24T11:42:28+11:00", "updated_by": "BLEPSCHI", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210313, "name": "muelleri"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros muelleri",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Dendroceros ferdinandi-muelleri": ["type": "orthographic variant", "name_id": 210312]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2916733",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2917550,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210314'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='subtropicus'>subtropicus</element> <authors><author id='3814' title='Wild, C.J.'>C.J.Wild</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2917550",
                        "excluded"           : false,
                        "instance_id"        : 622837,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/622837",
                        "name_element"       : "subtropicus",
                        "name_id"            : 210314,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210314",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/subtropicus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2917550,
                        "profile"            : ["APC Dist.": ["value": "Qld", "source_id": 27658, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210314, "name": "subtropicus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros subtropicus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2917550",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2913739,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210315'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='wattsianus'>wattsianus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2913739",
                        "excluded"           : false,
                        "instance_id"        : 622841,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/622841",
                        "name_element"       : "wattsianus",
                        "name_id"            : 210315,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210315",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/wattsianus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 2909398,
                        "previous_version_id": 145,
                        "previous_element_id": 2913739,
                        "profile"            : ["APC Dist.": ["value": "NSW", "source_id": 27659, "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210315, "name": "wattsianus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Dendroceros wattsianus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/2909398/2913739",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 8513209,
                        "lock_version"       : 0,
                        "depth"              : 8,
                        "display_html"       : "<data><scientific><name id='124930'><element class='Megaceros'>Megaceros</element> <authors><author id='9964' title='Campbell, D.H.'>Campb.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/8513209",
                        "excluded"           : false,
                        "instance_id"        : 8513196,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/8513196",
                        "name_element"       : "Megaceros",
                        "name_id"            : 124930,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/124930",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513207,
                        "previous_version_id": 145,
                        "previous_element_id": 8513209,
                        "profile"            : null,
                        "rank"               : "Genus",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 124930, "name": "Megaceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Megaceros",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/8513209",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2917526,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='175653'><scientific><name id='124930'><element class='Megaceros'>Megaceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors>(<base id='1435' title='Stephani, F.'>Steph.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2917526",
                        "excluded"           : false,
                        "instance_id"        : 624477,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/624477",
                        "name_element"       : "carnosus",
                        "name_id"            : 175653,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/175653",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros/carnosus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513209,
                        "previous_version_id": 145,
                        "previous_element_id": 2917526,
                        "profile"            : ["APC Dist.": ["value": "NSW, Vic, Tas", "source_id": 27665, "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 124930, "name": "Megaceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 175653, "name": "carnosus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Megaceros carnosus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : ["Anthoceros carnosus": ["type": "nomenclatural synonym", "name_id": 175654]],
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/8513209/2917526",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ],
                [
                        "tree_version_id"    : 146,
                        "tree_element_id"    : 2909144,
                        "lock_version"       : 0,
                        "depth"              : 9,
                        "display_html"       : "<data><scientific><name id='210888'><scientific><name id='124930'><element class='Megaceros'>Megaceros</element></name></scientific> <element class='crassus'>crassus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        "element_link"       : "http://localhost:7070/nsl-mapper/tree/146/2909144",
                        "excluded"           : false,
                        "instance_id"        : 624478,
                        "instance_link"      : "http://localhost:7070/nsl-mapper/instance/apni/624478",
                        "name_element"       : "crassus",
                        "name_id"            : 210888,
                        "name_link"          : "http://localhost:7070/nsl-mapper/name/apni/210888",
                        "name_path"          : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros/crassus",
                        "parent_version_id"  : 146,
                        "parent_element_id"  : 8513209,
                        "previous_version_id": 145,
                        "previous_element_id": 2909144,
                        "profile"            : ["APC Dist.": ["value": "Tas", "source_id": 27666, "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_system": "APC_CONCEPT"]],
                        "rank"               : "Species",
                        "rank_path"          : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 124930, "name": "Megaceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210888, "name": "crassus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        "simple_name"        : "Megaceros crassus",
                        "source_element_link": null,
                        "source_shard"       : "APNI",
                        "synonyms"           : null,
                        "synonyms_html"      : "<synonyms>Not Set</synonyms>",
                        "tree_path"          : "9284583/8513225/8513223/8513224/8513222/8513219/8513207/8513209/2909144",
                        "updated_at"         : "2017-07-11 00:00:00.000000",
                        "updated_by"         : "import"
                ]
        ]
    }
}
