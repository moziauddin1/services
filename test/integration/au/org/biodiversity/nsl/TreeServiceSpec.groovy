package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import grails.validation.ValidationException
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Timestamp

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
        service.linkService.bulkAddTargets(_) >> [success: true]

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
        List<TreeElement> testElements = makeTestElements(version, testElementData())
        println version.treeVersionElements

        then: 'It should have 30 tree elements'
        testElements.size() == 30
        version.treeVersionElements.size() == 30
        version.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[3], version))
        version.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[13], version))
        version.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[23], version))

        when: 'I make a new version from this version'
        TreeVersion version2 = service.createTreeVersion(tree, version, 'my second draft')
        println version2.treeVersionElements

        then: 'It should copy the elements and set the previous version'
        version2
        version2.draftName == 'my second draft'
        version != version2
        version.id != version2.id
        version2.previousVersion == version
        version2.treeVersionElements.size() == 30
        version2.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[3], version2))
        version2.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[13], version2))
        version2.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[23], version2))

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
        draftVersion.treeVersionElements.size() == 30
        draftVersion.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[3], draftVersion))
        draftVersion.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[13], draftVersion))
        draftVersion.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[23], draftVersion))

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
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        publishedVersion
        publishedVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion

        when: 'I delete the tree'
        service.deleteTree(tree)

        then: 'I get a published version exception'
        thrown(PublishedVersionException)

        when: 'I unpublish the published version and then delete the tree'
        publishedVersion.published = false
        publishedVersion.save()
        service.deleteTree(tree)

        then: "the tree, it's versions and their elements are gone"
        Tree.get(tree.id) == null
        Tree.findByName('aTree') == null
        TreeVersion.get(draftVersion.id) == null
        TreeVersion.get(publishedVersion.id) == null
        TreeVersionElement.findByTreeVersion(draftVersion) == null
    }

    def "test making and deleting a tree version"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        publishedVersion
        publishedVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion

        when: 'I delete the tree'
        tree = service.deleteTreeVersion(draftVersion)
        publishedVersion.refresh() //the refresh() is required by deleteTreeVersion

        then: "the draft version and it's elements are gone"
        tree
        tree.currentTreeVersion == publishedVersion
        //the below should no longer exist.
        tree.defaultDraftTreeVersion == null
        TreeVersion.get(draftVersion.id) == null
        TreeVersionElement.executeQuery('select element from TreeVersionElement element where treeVersion.id = :draftVersionId',
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
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement asperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)

        //return a url that matches the name link of aspera
        service.linkService.getPreferredLinkForObject(asperaInstance.name) >> 'http://something that does not match the name'
        service.linkService.getPreferredLinkForObject(asperaInstance) >> asperaElement.treeElement.instanceLink

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
        e.message == "$tree.name version $draftVersion.id already contains taxon $asperaElement.treeElement.instanceLink. See $asperaElement.elementLink"

    }

    def "test check validation, ranked above parent"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, asperaElementData])
        Instance doodiaInstance = Instance.get(578615)
        TreeVersionElement blechnaceaeElement = service.findElementBySimpleName('Blechnaceae', draftVersion)
        TreeVersionElement asperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)

        //these shouldn't matter so long as they're not on the draft tree
        service.linkService.getPreferredLinkForObject(doodiaInstance.name) >> 'http://blah/name/apni/70914'
        service.linkService.getPreferredLinkForObject(doodiaInstance) >> 'http://blah/instance/apni/578615'

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 2
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
        TreeVersionElement blechnaceaeElement = service.findElementBySimpleName('Blechnaceae', draftVersion)

        //these shouldn't matter so long as they're not on the draft tree
        service.linkService.getPreferredLinkForObject(doodiaInstance.name) >> 'http://blah/name/apni/70914'
        service.linkService.getPreferredLinkForObject(doodiaInstance) >> 'http://blah/instance/apni/578615'

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 2
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
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement asperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)

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

    def "test place taxon"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, doodiaElementData])
        Instance asperaInstance = Instance.get(781104)
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement asperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)
        String instanceUri = 'http://localhost:7070/nsl-mapper/instance/apni/781104'

        //return a url that matches the name link of aspera
        service.linkService.getPreferredLinkForObject(asperaInstance.name) >> 'http://localhost:7070/nsl-mapper/name/apni/70944'
        service.linkService.getPreferredLinkForObject(asperaInstance) >> 'http://localhost:7070/nsl-mapper/instance/apni/781104'

        expect:
        tree
        draftVersion
        doodiaElement
        asperaInstance
        !asperaElement

        when: 'I try to place Doodia aspera under Doodia'
        Map result = service.placeTaxonUri(doodiaElement, instanceUri, false, 'A. User')
        println result

        then: 'It should work'
        1 * service.linkService.getObjectForLink(instanceUri) >> asperaInstance
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        result.childElement == service.findElementBySimpleName('Doodia aspera', draftVersion)
        result.warnings.empty
        println result.childElement.elementLink
    }

    def "test move a taxon"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        List<TreeElement> testElements = makeTestElements(draftVersion, testElementData())
        TreeVersionElement anthocerotaceae = service.findElementBySimpleName('Anthocerotaceae', draftVersion)
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)
        TreeVersionElement dendrocerotaceae = service.findElementBySimpleName('Dendrocerotaceae', draftVersion)

        expect:
        tree
        testElements.size() == 30
        draftVersion.treeVersionElements.size() == 30
        !draftVersion.published
        anthocerotaceae
        anthoceros
        anthoceros.treeElement.parentElement == anthocerotaceae.treeElement
        dendrocerotaceae

        when: 'I try to move a taxon'
        TreeVersionElement newAnthoceros = service.moveTaxon(anthoceros, dendrocerotaceae, 'test move taxon')
        List<TreeVersionElement> anthocerosChildren = service.getAllChildElements(newAnthoceros)
        List<TreeVersionElement> dendrocerotaceaeChildren = service.getAllChildElements(dendrocerotaceae)
        for (TreeVersionElement tve in dendrocerotaceaeChildren) {
            println tve.treeElement.namePath
        }
        draftVersion.refresh()

        then: 'It works'
        1 * service.linkService.bulkRemoveTargets(_) >> { List<TreeVersionElement> elements ->
            [success: true]
        }
        6 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        newAnthoceros
        newAnthoceros != anthoceros
        TreeVersionElement.get(anthoceros.elementLink) == null
        newAnthoceros == service.findElementBySimpleName('Anthoceros', draftVersion)
        newAnthoceros.treeVersion == draftVersion
        newAnthoceros.treeElement != anthoceros.treeElement
        draftVersion.treeVersionElements.size() == 30
        anthocerosChildren.size() == 5
        newAnthoceros.treeElement.parentElement == dendrocerotaceae.treeElement
        anthocerosChildren[0].treeElement.nameElement == 'capricornii'
        anthocerosChildren[1].treeElement.nameElement == 'ferdinandi-muelleri'
        anthocerosChildren[2].treeElement.nameElement == 'fragilis'
        anthocerosChildren[3].treeElement.nameElement == 'laminifer'
        anthocerosChildren[4].treeElement.nameElement == 'punctatus'
        dendrocerotaceaeChildren.containsAll(anthocerosChildren)

        when: 'I publish the version then try a move'
        service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        service.moveTaxon(anthoceros, anthocerotaceae, 'test move taxon')

        then: 'I get a PublishedVersionException'
        thrown(PublishedVersionException)
    }

    def "test move a taxon with multiple child levels"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        List<TreeElement> testElements = makeTestElements(draftVersion, testElementData())
        TreeVersionElement anthocerotales = service.findElementBySimpleName('Anthocerotales', draftVersion)
        TreeVersionElement dendrocerotidae = service.findElementBySimpleName('Dendrocerotidae', draftVersion)
        TreeVersionElement anthocerotidae = service.findElementBySimpleName('Anthocerotidae', draftVersion)
        List<TreeVersionElement> anthocerotalesChildren = service.getAllChildElements(anthocerotales)

        expect:
        tree
        testElements.size() == 30
        draftVersion.treeVersionElements.size() == 30
        !draftVersion.published
        anthocerotales
        dendrocerotidae
        anthocerotales.treeElement.parentElement == anthocerotidae.treeElement
        anthocerotalesChildren.size() == 10

        when: 'I move Anthocerotales under Dendrocerotidae'
        TreeVersionElement newAnthocerotales = service.moveTaxon(anthocerotales, dendrocerotidae, 'test move taxon')
        List<TreeVersionElement> newAnthocerotalesChildren = service.getAllChildElements(newAnthocerotales)
        List<TreeVersionElement> dendrocerotidaeChildren = service.getAllChildElements(dendrocerotidae)
        for (TreeVersionElement tve in dendrocerotidaeChildren) {
            println tve.treeElement.namePath
        }
        draftVersion.refresh()

        then: 'It works'
        1 * service.linkService.bulkRemoveTargets(_) >> { List<TreeVersionElement> elements ->
            [success: true]
        }
        11 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        newAnthocerotales
        newAnthocerotales != anthocerotales
        newAnthocerotales == service.findElementBySimpleName('Anthocerotales', draftVersion)
        draftVersion.treeVersionElements.size() == 30
        newAnthocerotalesChildren.size() == 10
        dendrocerotidaeChildren.size() == 25
    }

    def "test remove a taxon"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        List<TreeElement> testElements = makeTestElements(draftVersion, testElementData())
        TreeVersionElement anthocerotaceae = service.findElementBySimpleName('Anthocerotaceae', draftVersion)
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)

        expect:
        tree
        testElements.size() == 30
        draftVersion.treeVersionElements.size() == 30
        !draftVersion.published
        anthocerotaceae
        anthoceros
        anthoceros.treeElement.parentElement == anthocerotaceae.treeElement

        when: 'I try to move a taxon'
        int count = service.removeTreeVersionElement(anthoceros)

        then: 'It works'
        count == 6
        draftVersion.treeVersionElements.size() == 24
        service.findElementBySimpleName('Anthoceros', draftVersion) == null
    }

    def "test edit taxon profile"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)
        TreeVersionElement pubAnthoceros = service.findElementBySimpleName('Anthoceros', publishedVersion)

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        publishedVersion
        publishedVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion
        pubAnthoceros
        anthoceros.treeElement.profile == ["APC Dist.":
                                                   [
                                                           "value"       : "WA, NT, SA, Qld, NSW, ACT, Vic, Tas",
                                                           "sourceid"    : 27645,
                                                           "createdat"   : "2011-01-27T00:00:00+11:00",
                                                           "createdby"   : "KIRSTENC",
                                                           "updatedat"   : "2011-01-27T00:00:00+11:00",
                                                           "updatedby"   : "KIRSTENC",
                                                           "sourcesystem": "APCCONCEPT"
                                                   ]
        ]

        when: 'I update the profile on the published version'
        service.editProfile(pubAnthoceros, ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]], 'test edit profile')

        then: 'I get a PublishedVersionException'
        thrown(PublishedVersionException)

        when: 'I update a profile on the draft version'
        TreeElement oldElement = anthoceros.treeElement
        TreeVersionElement treeVersionElement = service.editProfile(anthoceros, ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]], 'test edit profile')

        then: 'It creates a new treeElement and updates the profile'
        treeVersionElement
        oldElement
        treeVersionElement == anthoceros
        treeVersionElement.treeElement != oldElement
        treeVersionElement.treeElement.profile == ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]]
        treeVersionElement.treeElement.updatedBy == 'test edit profile'

        when: 'I change a profile to the same thing'
        TreeVersionElement anthocerosCapricornii = service.findElementBySimpleName('Anthoceros capricornii', draftVersion)
        TreeVersionElement treeVersionElement1 = service.editProfile(anthocerosCapricornii, ["APC Dist.":
                                                                                                     [
                                                                                                             "value"       : "WA, NT",
                                                                                                             "sourceid"    : 27646,
                                                                                                             "createdat"   : "2011-01-27T00:00:00+11:00",
                                                                                                             "createdby"   : "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00",
                                                                                                             "updatedby"   : "KIRSTENC",
                                                                                                             "sourcesystem": "APCCONCEPT"
                                                                                                     ]
        ], 'test edit profile')

        then: 'nothing changes'
        treeVersionElement1 == anthocerosCapricornii
        treeVersionElement1.treeElement.updatedBy == 'import'
        treeVersionElement1.treeElement.profile == ["APC Dist.":
                                                            [
                                                                    "value"       : "WA, NT",
                                                                    "sourceid"    : 27646,
                                                                    "createdat"   : "2011-01-27T00:00:00+11:00",
                                                                    "createdby"   : "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00",
                                                                    "updatedby"   : "KIRSTENC",
                                                                    "sourcesystem": "APCCONCEPT"
                                                            ]
        ]
    }

    def "test edit draft only taxon profile"() {
        given:
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == null
        anthoceros.treeElement.profile == ["APC Dist.":
                                                   [
                                                           "value"       : "WA, NT, SA, Qld, NSW, ACT, Vic, Tas",
                                                           "sourceid"    : 27645,
                                                           "createdat"   : "2011-01-27T00:00:00+11:00",
                                                           "createdby"   : "KIRSTENC",
                                                           "updatedat"   : "2011-01-27T00:00:00+11:00",
                                                           "updatedby"   : "KIRSTENC",
                                                           "sourcesystem": "APCCONCEPT"
                                                   ]
        ]

        when: 'I update a profile on the draft version'
        TreeElement oldElement = anthoceros.treeElement
        Timestamp oldTimestamp = anthoceros.treeElement.updatedAt
        TreeVersionElement treeVersionElement = service.editProfile(anthoceros, ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]], 'test edit profile')

        then: 'It creates a new treeElement and updates the profile'
        treeVersionElement
        oldElement
        treeVersionElement == anthoceros
        treeVersionElement.treeElement == oldElement
        treeVersionElement.treeElement.profile == ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]]
        treeVersionElement.treeElement.updatedBy == 'test edit profile'
        treeVersionElement.treeElement.updatedAt > oldTimestamp
    }

    def "test edit taxon excluded status"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = service.createNewTree('aTree', 'aGroup', null)
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)
        TreeVersionElement pubAnthoceros = service.findElementBySimpleName('Anthoceros', publishedVersion)

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        publishedVersion
        publishedVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion
        pubAnthoceros
        !anthoceros.treeElement.excluded

        when: 'I update the profile on the published version'
        service.editExcluded(pubAnthoceros, true, 'test edit profile')

        then: 'I get a PublishedVersionException'
        thrown(PublishedVersionException)

        when: 'I update a profile on the draft version'
        TreeElement oldElement = anthoceros.treeElement
        TreeVersionElement treeVersionElement = service.editExcluded(anthoceros, true, 'test edit profile')

        then: 'It creates a new treeElement and updates the profile'
        treeVersionElement
        oldElement
        treeVersionElement == anthoceros
        treeVersionElement.treeElement != oldElement
        treeVersionElement.treeElement.excluded
        treeVersionElement.treeElement.updatedBy == 'test edit profile'

        when: 'I change a profile to the same thing'
        TreeVersionElement anthocerosCapricornii = service.findElementBySimpleName('Anthoceros capricornii', draftVersion)
        TreeVersionElement treeVersionElement1 = service.editExcluded(anthocerosCapricornii, false, 'test edit profile')

        then: 'nothing changes'
        treeVersionElement1 == anthocerosCapricornii
        treeVersionElement1.treeElement.updatedBy == 'import'
        !treeVersionElement1.treeElement.excluded
    }

    private static List<TreeElement> makeTestElements(TreeVersion version, List<Map> elementData) {
        List<TreeElement> elements = []
        Map<Long, Long> generatedIdMapper = [:]
        elementData.each { Map data ->
            if (data.parentElementId) {
                data.parentElement = TreeElement.get(generatedIdMapper[data.parentElementId as Long])
            }
            data.remove('previousElementId')
            TreeElement e = new TreeElement(data)
            e.save()
            generatedIdMapper.put(data.id as Long, e.id as Long)
            TreeVersionElement tve = new TreeVersionElement(treeVersion: version, treeElement: e, elementLink: "http://localhost:7070/nsl-mapper/tree/$version.id/$e.id", taxonLink: 'http://localhost:7070/nsl-mapper/node/apni/12345')
            tve.save()
            elements.add(e)
        }
        version.save(flush: true)
        version.refresh()
        return elements
    }

    private static Map doodiaElementData = [
            id               : 9435080,
            lockVersion      : 0,
            depth            : 7,
            displayHtml      : "<data><scientific><name id='70914'><element class='Doodia'>Doodia</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific><citation>Parris, B.S. in McCarthy, P.M. (ed.) (1998), Doodia. <i>Flora of Australia</i> 48</citation></data>",
            excluded         : false,
            instanceId       : 578615,
            instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/578615",
            nameElement      : "Doodia",
            nameId           : 70914,
            nameLink         : "http://localhost:7070/nsl-mapper/name/apni/70914",
            namePath         : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia",
            parentElementId  : 9435044,
            previousElementId: null,
            profile          : ["APC Dist.": ["value": "NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas", "sourceid": 14274, "createdat": "2008-08-06T00:00:00+10:00", "createdby": "BRONWYNC", "updatedat": "2008-08-06T00:00:00+10:00", "updatedby": "BRONWYNC", "sourcesystem": "APCCONCEPT"], "APC Comment": ["value": "<i>Doodia</i> R.Br. is included in <i>Blechnum</i> L. in NSW.", "sourceid": null, "createdat": "2016-06-10T15:22:41.742+10:00", "createdby": "blepschi", "updatedat": "2016-06-10T15:23:01.201+10:00", "updatedby": "blepschi", "sourcesystem": null]],
            rank             : "Genus",
            rankPath         : ["Ordo": ["id": 223583, "name": "Polypodiales"], "Genus": ["id": 70914, "name": "Doodia"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Familia": ["id": 222592, "name": "Blechnaceae"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
            simpleName       : "Doodia",
            sourceElementLink: null,
            sourceShard      : "APNI",
            synonyms         : null,
            synonymsHtml     : "<synonyms></synonyms>",
            treePath         : "/9434380/9434381/9434382/9434511/9434834/9435044/9435080",
            updatedAt        : "2017-09-07 10:54:20.736603",
            updatedBy        : "import"
    ]

    private static Map blechnaceaeElementData = [
            id               : 9435044,
            lockVersion      : 0,
            depth            : 6,
            displayHtml      : "<data><scientific><name id='222592'><element class='Blechnaceae'>Blechnaceae</element> <authors><author id='8244' title='Newman, E.'>Newman</author></authors></name></scientific><citation>CHAH (2009), <i>Australian Plant Census</i></citation></data>",
            excluded         : false,
            instanceId       : 651382,
            instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/651382",
            nameElement      : "Blechnaceae",
            nameId           : 222592,
            nameLink         : "http://localhost:7070/nsl-mapper/name/apni/222592",
            namePath         : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae",
            parentElementId  : 9434834,
            previousElementId: null,
            profile          : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas, MI", "sourceid": 22346, "createdat": "2009-10-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2009-10-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
            rank             : "Familia",
            rankPath         : ["Ordo": ["id": 223583, "name": "Polypodiales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Familia": ["id": 222592, "name": "Blechnaceae"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
            simpleName       : "Blechnaceae",
            sourceElementLink: null,
            sourceShard      : "APNI",
            synonyms         : null,
            synonymsHtml     : "<synonyms></synonyms>",
            treePath         : "/9434380/9434381/9434382/9434511/9434834/9435044",
            updatedAt        : "2017-09-07 10:54:20.736603",
            updatedBy        : "import"
    ]

    private static Map asperaElementData = [
            id               : 9435081,
            lockVersion      : 0,
            depth            : 8,
            displayHtml      : "<data><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific><citation>CHAH (2014), <i>Australian Plant Census</i></citation></data>",
            excluded         : false,
            instanceId       : 781104,
            instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/781104",
            nameElement      : "aspera",
            nameId           : 70944,
            nameLink         : "http://localhost:7070/nsl-mapper/name/apni/70944",
            namePath         : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia/aspera",
            parentElementId  : 9435080,
            previousElementId: null,
            profile          : ["APC Dist.": ["value": "Qld, NSW, LHI, NI, Vic, Tas", "sourceid": 42500, "createdat": "2014-03-25T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2014-03-25T14:04:06+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"], "APC Comment": ["value": "Treated as <i>Blechnum neohollandicum</i> Christenh. in NSW.", "sourceid": null, "createdat": "2016-06-10T15:21:38.135+10:00", "createdby": "blepschi", "updatedat": "2016-06-10T15:21:38.135+10:00", "updatedby": "blepschi", "sourcesystem": null]],
            rank             : "Species",
            rankPath         : ["Ordo": ["id": 223583, "name": "Polypodiales"], "Genus": ["id": 70914, "name": "Doodia"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 223519, "name": "Equisetopsida"], "Familia": ["id": 222592, "name": "Blechnaceae"], "Species": ["id": 70944, "name": "aspera"], "Division": ["id": 224706, "name": "Charophyta"], "Subclassis": ["id": 224852, "name": "Polypodiidae"]],
            simpleName       : "Doodia aspera",
            sourceElementLink: null,
            sourceShard      : "APNI",
            synonyms         : ["Woodwardia aspera": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Mettenius, G.H. (1856), <i>Filices Horti Botanici Lipsiensis</i>", "nameid": 106698, "fullnamehtml": "<scientific><name id='106698'><scientific><name id='106675'><element class='Woodwardia'>Woodwardia</element></name></scientific> <element class='aspera'>aspera</element> <authors>(<base id='1441' title='Brown, R.'>R.Br.</base>) <author id='7081' title='Mettenius, G.H.'>Mett.</author></authors></name></scientific>"], "Blechnum neohollandicum": ["mis": false, "nom": false, "tax": true, "type": "taxonomic synonym", "cites": "Christenhusz, M.J.M., Zhang, X.C. & Schneider, H. (2011), A linear sequence of extant families and genera of lycophytes and ferns. <i>Phytotaxa</i> 19", "nameid": 239547, "fullnamehtml": "<scientific><name id='239547'><scientific><name id='56340'><element class='Blechnum'>Blechnum</element></name></scientific> <element class='neohollandicum'>neohollandicum</element> <authors><author id='6422' title='Christenhusz, M.J.M.'>Christenh.</author></authors></name></scientific>"], "Doodia aspera var. aspera": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Bailey, F.M. (1881), <i>The fern world of Australia</i>", "nameid": 70967, "fullnamehtml": "<scientific><name id='70967'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific> <rank id='54412'>var.</rank> <element class='aspera'>aspera</element></name></scientific>"], "Doodia aspera var. angustifrons": ["mis": false, "nom": false, "tax": true, "type": "taxonomic synonym", "cites": "Domin, K. (1915), Beitrage zur Flora und Pflanzengeographie Australiens. <i>Bibliotheca Botanica</i> 20(85)", "nameid": 70959, "fullnamehtml": "<scientific><name id='70959'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element></name></scientific> <rank id='54412'>var.</rank> <element class='angustifrons'>angustifrons</element> <authors><author id='6860' title='Domin, K.'>Domin</author></authors></name></scientific>"]],
            synonymsHtml     : "<synonyms><nom><scientific><name id='106698'><scientific><name id='106675'><element class='Woodwardia'>Woodwardia</element></name></scientific> <element class='aspera'>aspera</element> <authors>(<base id='1441' title='Brown, R.'>R.Br.</base>) <author id='7081' title='Mettenius, G.H.'>Mett.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom><nom><scientific><name id='70967'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific> <rank id='54412'>var.</rank> <element class='aspera'>aspera</element></name></scientific> <type>nomenclatural synonym</type></nom><tax><scientific><name id='70959'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element></name></scientific> <rank id='54412'>var.</rank> <element class='angustifrons'>angustifrons</element> <authors><author id='6860' title='Domin, K.'>Domin</author></authors></name></scientific> <type>taxonomic synonym</type></tax><tax><scientific><name id='239547'><scientific><name id='56340'><element class='Blechnum'>Blechnum</element></name></scientific> <element class='neohollandicum'>neohollandicum</element> <authors><author id='6422' title='Christenhusz, M.J.M.'>Christenh.</author></authors></name></scientific> <type>taxonomic synonym</type></tax></synonyms>",
            treePath         : "/9434380/9434381/9434382/9434511/9434834/9435044/9435080/9435081",
            updatedAt        : "2017-09-07 10:54:20.736603",
            updatedBy        : "import"
    ]

    private static List<Map> testElementData() {
        [
                [
                        id               : 9434380,
                        lockVersion      : 0,
                        depth            : 1,
                        displayHtml      : "<data><scientific><name id='54717'><element class='Plantae'>Plantae</element> <authors><author id='3882' title='Haeckel, Ernst Heinrich Philipp August'>Haeckel</author></authors></name></scientific><citation>CHAH (2012), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 738442,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/738442",
                        nameElement      : "Plantae",
                        nameId           : 54717,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/54717",
                        namePath         : "Plantae",
                        parentElementId  : null,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Regnum",
                        rankPath         : ["Regnum": ["id": 54717, "name": "Plantae"]],
                        simpleName       : "Plantae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472131,
                        lockVersion      : 0,
                        depth            : 2,
                        displayHtml      : "<data><scientific><name id='238892'><element class='Anthocerotophyta'>Anthocerotophyta</element> <authors><ex id='7053' title='Rothmaler, W.H.P.'>Rothm.</ex> ex <author id='5307' title='Stotler,R.E. &amp; Crandall-Stotler,B.J.'>Stotler & Crand.-Stotl.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8509445,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8509445",
                        nameElement      : "Anthocerotophyta",
                        nameId           : 238892,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/238892",
                        namePath         : "Plantae/Anthocerotophyta",
                        parentElementId  : 9434380,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Division",
                        rankPath         : ["Regnum": ["id": 54717, "name": "Plantae"], "Division": ["id": 238892, "name": "Anthocerotophyta"]],
                        simpleName       : "Anthocerotophyta",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472132,
                        lockVersion      : 0,
                        depth            : 3,
                        displayHtml      : "<data><scientific><name id='238893'><element class='Anthocerotopsida'>Anthocerotopsida</element> <authors><ex id='2484' title='de Bary, H.A.'>de Bary</ex> ex <author id='3443' title='Janczewski, E. von G.'>Jancz.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8509444,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8509444",
                        nameElement      : "Anthocerotopsida",
                        nameId           : 238893,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/238893",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida",
                        parentElementId  : 9472131,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Classis",
                        rankPath         : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"]],
                        simpleName       : "Anthocerotopsida",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472133,
                        lockVersion      : 0,
                        depth            : 4,
                        displayHtml      : "<data><scientific><name id='240384'><element class='Anthocerotidae'>Anthocerotidae</element> <authors><author id='6453' title='Rosenvinge, J.L.A.K.'>Rosenv.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8509443,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8509443",
                        nameElement      : "Anthocerotidae",
                        nameId           : 240384,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240384",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae",
                        parentElementId  : 9472132,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Subclassis",
                        rankPath         : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthocerotidae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472134,
                        lockVersion      : 0,
                        depth            : 5,
                        displayHtml      : "<data><scientific><name id='142301'><element class='Anthocerotales'>Anthocerotales</element> <authors><author id='2624' title='Limpricht, K.G.'>Limpr.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8508886,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8508886",
                        nameElement      : "Anthocerotales",
                        nameId           : 142301,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/142301",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales",
                        parentElementId  : 9472133,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Ordo",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthocerotales",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472135,
                        lockVersion      : 0,
                        depth            : 6,
                        displayHtml      : "<data><scientific><name id='124939'><element class='Anthocerotaceae'>Anthocerotaceae</element> <authors>(<base id='2041' title='Gray, S.F.'>Gray</base>) <author id='6855' title='Dumortier, B.C.J.'>Dumort.</author></authors></name></scientific><citation>CHAH (2011), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 748950,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/748950",
                        nameElement      : "Anthocerotaceae",
                        nameId           : 124939,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/124939",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae",
                        parentElementId  : 9472134,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "WA, NT, Qld, NSW, Vic, Tas", "sourceid": 35164, "createdat": "2012-05-21T00:00:00+10:00", "createdby": "KIRSTENC", "updatedat": "2012-05-21T00:00:00+10:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Familia",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthocerotaceae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472136,
                        lockVersion      : 0,
                        depth            : 7,
                        displayHtml      : "<data><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element> <authors><author id='1426' title='Linnaeus, C.'>L.</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 668637,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/668637",
                        nameElement      : "Anthoceros",
                        nameId           : 121601,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/121601",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros",
                        parentElementId  : 9472135,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, ACT, Vic, Tas", "sourceid": 27645, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Genus",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthoceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472136",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472137,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='144273'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='capricornii'>capricornii</element> <authors><author id='1771' title='Cargill, D.C. &amp; Scott, G.A.M.'>Cargill & G.A.M.Scott</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621662,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621662",
                        nameElement      : "capricornii",
                        nameId           : 144273,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/144273",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/capricornii",
                        parentElementId  : 9472136,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "WA, NT", "sourceid": 27646, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 144273, "name": "capricornii"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthoceros capricornii",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros adscendens": ["mis": true, "nom": false, "tax": false, "type": "pro parte misapplied", "cites": "Lehmann, J.G.C. & Lindenberg, J.B.W. in Lehmann, J.G.C. (1832), <i>Novarum et Minus Cognitarum Stirpium Pugillus</i> 4", "nameid": 162382, "fullnamehtml": "<scientific><name id='162382'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='adscendens'>adscendens</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><mis><scientific><name id='162382'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='adscendens'>adscendens</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific> <type>pro parte misapplied</type> by <citation>Lehmann, J.G.C. & Lindenberg, J.B.W. in Lehmann, J.G.C. (1832), <i>Novarum et Minus Cognitarum Stirpium Pugillus</i> 4</citation></mis></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472136/9472137",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472138,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='209869'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='ferdinandi-muelleri'>ferdinandi-muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621668,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621668",
                        nameElement      : "ferdinandi-muelleri",
                        nameId           : 209869,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/209869",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/ferdinandi-muelleri",
                        parentElementId  : 9472136,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "?Qld", "sourceid": 27647, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 209869, "name": "ferdinandi-muelleri"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthoceros ferdinandi-muelleri",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472136/9472138",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472141,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='142232'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fragilis'>fragilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>Cargill, D.C., Söderström, L., Hagborg, A. & Konrat, M. von (2013), Notes on Early Land Plants Today. 23. A new synonym in Anthoceros (Anthocerotaceae, Anthocerotophyta). <i>Phytotaxa</i> 76(3)</citation></data>",
                        excluded         : false,
                        instanceId       : 760852,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/760852",
                        nameElement      : "fragilis",
                        nameId           : 142232,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/142232",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/fragilis",
                        parentElementId  : 9472136,
                        previousElementId: null,
                        profile          : ["Type": ["value": "Australia. Queensland: Amalie Dietrich (holotype G-61292! [=G-24322!]).", "sourceid": 214282, "createdat": "2013-01-16T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2013-01-16T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "CITATIONTEXT"], "APC Dist.": ["value": "Qld", "sourceid": 38619, "createdat": "2013-02-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2013-02-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 142232, "name": "fragilis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthoceros fragilis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros fertilis": ["mis": false, "nom": false, "tax": true, "type": "taxonomic synonym", "cites": "Stephani, F. (1916), <i>Species Hepaticarum</i> 5", "nameid": 209870, "fullnamehtml": "<scientific><name id='209870'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fertilis'>fertilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><tax><scientific><name id='209870'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fertilis'>fertilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific> <type>taxonomic synonym</type></tax></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472136/9472141",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472139,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='202233'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='laminifer'>laminifer</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621709,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621709",
                        nameElement      : "laminifer",
                        nameId           : 202233,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/202233",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/laminifer",
                        parentElementId  : 9472136,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "sourceid": 27650, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 202233, "name": "laminifer"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthoceros laminifer",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472136/9472139",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472140,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='122138'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='punctatus'>punctatus</element> <authors><author id='1426' title='Linnaeus, C.'>L.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621713,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621713",
                        nameElement      : "punctatus",
                        nameId           : 122138,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/122138",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/punctatus",
                        parentElementId  : 9472136,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "SA, Qld, NSW, ACT, Vic, Tas", "sourceid": 27651, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 121601, "name": "Anthoceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 122138, "name": "punctatus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Anthoceros punctatus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472136/9472140",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472142,
                        lockVersion      : 0,
                        depth            : 7,
                        displayHtml      : "<data><scientific><name id='134990'><element class='Folioceros'>Folioceros</element> <authors><author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 669233,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/669233",
                        nameElement      : "Folioceros",
                        nameId           : 134990,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/134990",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros",
                        parentElementId  : 9472135,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld, NSW", "sourceid": 27661, "createdat": "2011-01-31T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-31T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Genus",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 134990, "name": "Folioceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Folioceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472142",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472143,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='143486'><scientific><name id='134990'><element class='Folioceros'>Folioceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors>(<base id='8996' title='Montagne, J.P.F.C.'>Mont.</base>) <author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>Bhardwaj, D.C. (1975), <i>Geophytology</i> 5</citation></data>",
                        excluded         : false,
                        instanceId       : 621673,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621673",
                        nameElement      : "fuciformis",
                        nameId           : 143486,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/143486",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros/fuciformis",
                        parentElementId  : 9472142,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "sourceid": 27662, "createdat": "2011-01-31T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-31T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 134990, "name": "Folioceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 143486, "name": "fuciformis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Folioceros fuciformis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros fuciformis": ["mis": false, "nom": true, "tax": false, "type": "basionym", "cites": "Montagne, J.P.F.C. (1843), <i>Annales des Sciences Naturelles; Botanique</i> 20", "nameid": 142253, "fullnamehtml": "<scientific><name id='142253'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors><author id='8996' title='Montagne, J.P.F.C.'>Mont.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='142253'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors><author id='8996' title='Montagne, J.P.F.C.'>Mont.</author></authors></name></scientific> <type>basionym</type></nom></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472142/9472143",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472144,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='134991'><scientific><name id='134990'><element class='Folioceros'>Folioceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 669234,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/669234",
                        nameElement      : "glandulosus",
                        nameId           : 134991,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/134991",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros/glandulosus",
                        parentElementId  : 9472142,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW", "sourceid": 27663, "createdat": "2011-01-31T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-31T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 142301, "name": "Anthocerotales"], "Genus": ["id": 134990, "name": "Folioceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 124939, "name": "Anthocerotaceae"], "Species": ["id": 134991, "name": "glandulosus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240384, "name": "Anthocerotidae"]],
                        simpleName       : "Folioceros glandulosus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros glandulosus": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Lehmann, J.G.C. & Lindenberg, J.B.W. in Lehmann, J.G.C. (1832), <i>Novarum et Minus Cognitarum Stirpium Pugillus</i> 4", "nameid": 129589, "fullnamehtml": "<scientific><name id='129589'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific>"], "Aspiromitus glandulosus": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Stephani, F. (1916), <i>Species Hepaticarum</i> 5", "nameid": 209879, "fullnamehtml": "<scientific><name id='209879'><scientific><name id='172172'><element class='Aspiromitus'>Aspiromitus</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='129589'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom><nom><scientific><name id='209879'><scientific><name id='172172'><element class='Aspiromitus'>Aspiromitus</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472133/9472134/9472135/9472142/9472144",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472155,
                        lockVersion      : 0,
                        depth            : 4,
                        displayHtml      : "<data><scientific><name id='240391'><element class='Dendrocerotidae'>Dendrocerotidae</element> <authors><author id='5261' title='Duff, R.J., Villarreal, J.C., Cargill, D.C. &amp; Renzaglia, K.S.'>Duff, J.C.Villarreal, Cargill & Renzaglia</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8511897,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8511897",
                        nameElement      : "Dendrocerotidae",
                        nameId           : 240391,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240391",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae",
                        parentElementId  : 9472132,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Subclassis",
                        rankPath         : ["Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        simpleName       : "Dendrocerotidae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472156,
                        lockVersion      : 0,
                        depth            : 5,
                        displayHtml      : "<data><scientific><name id='240393'><element class='Dendrocerotales'>Dendrocerotales</element> <authors><author id='3432' title='H&auml;ssel de Men&eacute;ndez, G.G.'>Hässel</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8512151,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8512151",
                        nameElement      : "Dendrocerotales",
                        nameId           : 240393,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240393",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales",
                        parentElementId  : 9472155,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Ordo",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        simpleName       : "Dendrocerotales",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472157,
                        lockVersion      : 0,
                        depth            : 6,
                        displayHtml      : "<data><scientific><name id='193461'><element class='Dendrocerotaceae'>Dendrocerotaceae</element> <authors>(<base id='2276' title='Milde, C.A.J.'>Milde</base>) <author id='3432' title='H&auml;ssel de Men&eacute;ndez, G.G.'>Hässel</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8512407,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8512407",
                        nameElement      : "Dendrocerotaceae",
                        nameId           : 193461,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/193461",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae",
                        parentElementId  : 9472156,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Familia",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"]],
                        simpleName       : "Dendrocerotaceae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472158,
                        lockVersion      : 0,
                        depth            : 7,
                        displayHtml      : "<data><scientific><name id='240394'><scientific><name id='193461'><element class='Dendrocerotaceae'>Dendrocerotaceae</element></name></scientific> <element class='Dendrocerotoideae'>Dendrocerotoideae</element> <authors><author id='1751' title='Schuster, R.M.'>R.M.Schust.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8512665,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8512665",
                        nameElement      : "Dendrocerotoideae",
                        nameId           : 240394,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240394",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae",
                        parentElementId  : 9472157,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Subfamilia",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendrocerotaceae Dendrocerotoideae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472159,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element> <authors><author id='6893' title='Nees von Esenbeck, C.G.D.'>Nees</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 668662,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/668662",
                        nameElement      : "Dendroceros",
                        nameId           : 129597,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/129597",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros",
                        parentElementId  : 9472158,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld, NSW", "sourceid": 27652, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Genus",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472160,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210308'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='australis'>australis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622818,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622818",
                        nameElement      : "australis",
                        nameId           : 210308,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210308",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/australis",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW", "sourceid": 27653, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210308, "name": "australis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros australis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472160",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472161,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='178505'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='crispatus'>crispatus</element> <authors>(<base id='6851' title='Hooker, W.J.'>Hook.</base>) <author id='6893' title='Nees von Esenbeck, C.G.D.'>Nees</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622823,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622823",
                        nameElement      : "crispatus",
                        nameId           : 178505,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/178505",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/crispatus",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "sourceid": 27654, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 178505, "name": "crispatus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros crispatus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Monoclea crispata": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Hooker, W.J. in Hooker, W.J. (ed.) (1830), <i>Botanical Miscellany</i> 1", "nameid": 210309, "fullnamehtml": "<scientific><name id='210309'><scientific><name id='133731'><element class='Monoclea'>Monoclea</element></name></scientific> <element class='crispata'>crispata</element> <authors><author id='6851' title='Hooker, W.J.'>Hook.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='210309'><scientific><name id='133731'><element class='Monoclea'>Monoclea</element></name></scientific> <element class='crispata'>crispata</element> <authors><author id='6851' title='Hooker, W.J.'>Hook.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472161",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472165,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210317'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='difficilis'>difficilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>Pócs, T. & Streimann, H. (1999), Epiphyllous liverworts from Queensland, Australia. <i>Bryobrothera</i> 5</citation></data>",
                        excluded         : false,
                        instanceId       : 622849,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622849",
                        nameElement      : "difficilis",
                        nameId           : 210317,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210317",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/difficilis",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "sourceid": 41411, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "BLEPSCHI", "updatedat": "2013-10-24T12:18:26+11:00", "updatedby": "BLEPSCHI", "sourcesystem": "APCCONCEPT"], "APC Comment": ["value": "Listed as \"< i > Dendroceros < /i> cf. <i>difficilis</ i >\" by P.M.McCarthy, <i>Checkl. Austral. Liverworts and Hornworts</i> 35 (2003).", "sourceid": 41411, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "BLEPSCHI", "updatedat": "2013-10-24T12:18:26+11:00", "updatedby": "BLEPSCHI", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210317, "name": "difficilis"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros difficilis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472165",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472162,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210311'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='granulatus'>granulatus</element> <authors><author id='1465' title='Mitten, W.'>Mitt.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622830,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622830",
                        nameElement      : "granulatus",
                        nameId           : 210311,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210311",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/granulatus",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "sourceid": 27656, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210311, "name": "granulatus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros granulatus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472162",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472166,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210313'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='muelleri'>muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 772246,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/772246",
                        nameElement      : "muelleri",
                        nameId           : 210313,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210313",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/muelleri",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld, NSW", "sourceid": 41410, "createdat": "2013-10-24T00:00:00+11:00", "createdby": "BLEPSCHI", "updatedat": "2013-10-24T11:42:28+11:00", "updatedby": "BLEPSCHI", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210313, "name": "muelleri"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros muelleri",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Dendroceros ferdinandi-muelleri": ["mis": false, "nom": false, "tax": false, "type": "orthographic variant", "cites": "Stephani, F. (1916), <i>Species Hepaticarum</i> 5", "nameid": 210312, "fullnamehtml": "<scientific><name id='210312'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='ferdinandi-muelleri'>ferdinandi-muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472166",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472163,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210314'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='subtropicus'>subtropicus</element> <authors><author id='3814' title='Wild, C.J.'>C.J.Wild</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622837,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622837",
                        nameElement      : "subtropicus",
                        nameId           : 210314,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210314",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/subtropicus",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "sourceid": 27658, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210314, "name": "subtropicus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros subtropicus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472163",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472164,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210315'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='wattsianus'>wattsianus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622841,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622841",
                        nameElement      : "wattsianus",
                        nameId           : 210315,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210315",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/wattsianus",
                        parentElementId  : 9472159,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW", "sourceid": 27659, "createdat": "2011-01-27T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-27T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 129597, "name": "Dendroceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210315, "name": "wattsianus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Dendroceros wattsianus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472159/9472164",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472167,
                        lockVersion      : 0,
                        depth            : 8,
                        displayHtml      : "<data><scientific><name id='124930'><element class='Megaceros'>Megaceros</element> <authors><author id='9964' title='Campbell, D.H.'>Campb.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8513196,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8513196",
                        nameElement      : "Megaceros",
                        nameId           : 124930,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/124930",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros",
                        parentElementId  : 9472158,
                        previousElementId: null,
                        profile          : null,
                        rank             : "Genus",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 124930, "name": "Megaceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Megaceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472167",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472168,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='175653'><scientific><name id='124930'><element class='Megaceros'>Megaceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors>(<base id='1435' title='Stephani, F.'>Steph.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 624477,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/624477",
                        nameElement      : "carnosus",
                        nameId           : 175653,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/175653",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros/carnosus",
                        parentElementId  : 9472167,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW, Vic, Tas", "sourceid": 27665, "createdat": "2011-01-31T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-31T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 124930, "name": "Megaceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 175653, "name": "carnosus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Megaceros carnosus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros carnosus": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Stephani, F. (1889), Hepaticae Australiae. <i>Hedwigia</i> 28", "nameid": 175654, "fullnamehtml": "<scientific><name id='175654'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='175654'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472167/9472168",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ],
                [
                        id               : 9472169,
                        lockVersion      : 0,
                        depth            : 9,
                        displayHtml      : "<data><scientific><name id='210888'><scientific><name id='124930'><element class='Megaceros'>Megaceros</element></name></scientific> <element class='crassus'>crassus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 624478,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/624478",
                        nameElement      : "crassus",
                        nameId           : 210888,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210888",
                        namePath         : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros/crassus",
                        parentElementId  : 9472167,
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Tas", "sourceid": 27666, "createdat": "2011-01-31T00:00:00+11:00", "createdby": "KIRSTENC", "updatedat": "2011-01-31T00:00:00+11:00", "updatedby": "KIRSTENC", "sourcesystem": "APCCONCEPT"]],
                        rank             : "Species",
                        rankPath         : ["Ordo": ["id": 240393, "name": "Dendrocerotales"], "Genus": ["id": 124930, "name": "Megaceros"], "Regnum": ["id": 54717, "name": "Plantae"], "Classis": ["id": 238893, "name": "Anthocerotopsida"], "Familia": ["id": 193461, "name": "Dendrocerotaceae"], "Species": ["id": 210888, "name": "crassus"], "Division": ["id": 238892, "name": "Anthocerotophyta"], "Subclassis": ["id": 240391, "name": "Dendrocerotidae"], "Subfamilia": ["id": 240394, "name": "Dendrocerotoideae"]],
                        simpleName       : "Megaceros crassus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        treePath         : "/9434380/9472131/9472132/9472155/9472156/9472157/9472158/9472167/9472169",
                        updatedAt        : "2017-09-07 10:54:20.736603",
                        updatedBy        : "import"
                ]
        ]
    }
}
