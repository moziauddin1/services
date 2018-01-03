package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import grails.validation.ValidationException
import org.hibernate.engine.spi.Status
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
        Tree tree = service.createNewTree('aTree', 'aGroup', null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'It should work'
        tree
        tree.name == 'aTree'
        tree.groupName == 'aGroup'
        tree.referenceId == null
        tree.currentTreeVersion == null
        tree.defaultDraftTreeVersion == null
        tree.id != null

        when: 'I try and create another tree with the same name'
        service.createNewTree('aTree', 'aGroup', null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'It will fail with an exception'
        thrown ObjectExistsException

        when: 'I try and create another tree with null name'
        service.createNewTree(null, 'aGroup', null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'It will fail with an exception'
        thrown ValidationException

        when: 'I try and create another tree with null group name'
        service.createNewTree('aNotherTree', null, null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'It will fail with an exception'
        thrown ValidationException

        when: 'I try and create another tree with reference ID'
        Tree tree2 = service.createNewTree('aNotherTree', 'aGroup', 12345l, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'It will work'
        tree2
        tree2.name == 'aNotherTree'
        tree2.groupName == 'aGroup'
        tree2.referenceId == 12345l
    }

    void "Test editing tree"() {
        given:
        Tree atree = makeATestTree() //new Tree(name: 'aTree', groupName: 'aGroup').save()
        Tree btree = makeBTestTree() //new Tree(name: 'b tree', groupName: 'aGroup').save()
        Long treeId = atree.id

        expect:
        atree
        treeId
        atree.name == 'aTree'
        btree
        btree.name == 'bTree'

        when: 'I change the name of a tree'
        Tree tree2 = service.editTree(atree, 'A new name', atree.groupName, 123456, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'The name and referenceID are changed'
        atree == tree2
        atree.name == 'A new name'
        atree.groupName == 'aGroup'
        atree.referenceId == 123456

        when: 'I change nothing'

        Tree tree3 = service.editTree(atree, 'A new name', atree.groupName, 123456, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'everything remains the same'
        atree == tree3
        atree.name == 'A new name'
        atree.groupName == 'aGroup'
        atree.referenceId == 123456

        when: 'I change the group and referenceId'

        Tree tree4 = service.editTree(atree, atree.name, 'A different group', null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'changes as expected'
        atree == tree4
        atree.name == 'A new name'
        atree.groupName == 'A different group'
        atree.referenceId == null

        when: 'I give a null name'

        service.editTree(atree, null, atree.groupName, null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'I get a bad argument exception'
        thrown BadArgumentsException

        when: 'I give a null group name'

        service.editTree(atree, atree.name, null, null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'I get a bad argument exception'
        thrown BadArgumentsException

        when: 'I give a name that is the same as another tree'

        service.editTree(atree, btree.name, atree.groupName, null, '<p>A description</p>', 'http://trees.org/aTree', false)

        then: 'I get a object exists exception'
        thrown ObjectExistsException
    }

    def "test creating a tree version"() {
        given:
        Tree tree = makeATestTree()
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
        List<TreeElement> testElements = makeTestElements(version, testElementData(), testTreeVersionElementData())
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
        versionsAreEqual(version, version2)
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
        versionsAreEqual(version2, draftVersion)
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

    private static Boolean versionsAreEqual(TreeVersion v1, TreeVersion v2) {
        v1.treeVersionElements.size() == v2.treeVersionElements.size() &&
                v1.treeVersionElements.collect { it.treeElement.id }.containsAll(v2.treeVersionElements.collect {
                    it.treeElement.id
                }) &&
                v1.treeVersionElements.collect { it.taxonId }.containsAll(v2.treeVersionElements.collect { it.taxonId })
    }

    def "test making and deleting a tree"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
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

    def "test making and deleting a draft tree version"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
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
        versionsAreEqual(publishedVersion, draftVersion)

        when: 'I delete the tree version'
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
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, doodiaElementData, asperaElementData], [blechnaceaeTVEData, doodiaTVEData, asperaTVEData])
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
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, asperaElementData], [blechnaceaeTVEData, asperaTVEData])
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
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, asperaElementData], [blechnaceaeTVEData, asperaTVEData])
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
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, doodiaElementData, asperaElementData], [blechnaceaeTVEData, doodiaTVEData, asperaTVEData])
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
        service.linkService.bulkAddTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, [blechnaceaeElementData, doodiaElementData], [blechnaceaeTVEData, doodiaTVEData])
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft')

        Instance asperaInstance = Instance.get(781104)
        TreeVersionElement blechnaceaeElement = service.findElementBySimpleName('Blechnaceae', draftVersion)
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement nullAsperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)
        String instanceUri = 'http://localhost:7070/nsl-mapper/instance/apni/781104'
        Long blechnaceaeTaxonId = blechnaceaeElement.taxonId
        Long doodiaTaxonId = doodiaElement.taxonId

        //return a url that matches the name link of aspera
        service.linkService.getPreferredLinkForObject(asperaInstance.name) >> 'http://localhost:7070/nsl-mapper/name/apni/70944'
        service.linkService.getPreferredLinkForObject(asperaInstance) >> 'http://localhost:7070/nsl-mapper/instance/apni/781104'

        expect:
        tree
        draftVersion
        blechnaceaeElement
        blechnaceaeTaxonId
        TreeVersionElement.countByTaxonId(blechnaceaeElement.taxonId) > 2
        doodiaElement
        doodiaTaxonId
        asperaInstance
        !nullAsperaElement

        when: 'I try to place Doodia aspera under Doodia'
        Map result = service.placeTaxonUri(doodiaElement, instanceUri, false, 'A. User')
        println result

        then: 'It should work'
        1 * service.linkService.getObjectForLink(instanceUri) >> asperaInstance
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        3 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }
        result.childElement == service.findElementBySimpleName('Doodia aspera', draftVersion)
        result.warnings.empty
        //taxon id should be set to a unique/new positive value
        result.childElement.taxonId != 0
        TreeVersionElement.countByTaxonId(result.childElement.taxonId) == 1
        result.childElement.taxonLink == "http://localhost:7070/nsl-mapper/taxon/apni/${result.childElement.taxonId}"
        //taxon id for the taxon above has changed to new IDs
        blechnaceaeElement.taxonId != 0
        blechnaceaeElement.taxonId != blechnaceaeTaxonId
        TreeVersionElement.countByTaxonId(blechnaceaeElement.taxonId) == 1
        blechnaceaeElement.taxonLink == "http://localhost:7070/nsl-mapper/taxon/apni/$blechnaceaeElement.taxonId"
        doodiaElement.taxonId != 0
        doodiaElement.taxonId != doodiaTaxonId
        TreeVersionElement.countByTaxonId(doodiaElement.taxonId) == 1
        doodiaElement.taxonLink == "http://localhost:7070/nsl-mapper/taxon/apni/$doodiaElement.taxonId"

        println result.childElement.elementLink
    }

    def "test replace a taxon"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        List<TreeElement> testElements = makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft')
        TreeVersionElement anthocerotaceaeTve = service.findElementBySimpleName('Anthocerotaceae', draftVersion)
        TreeVersionElement anthocerosTve = service.findElementBySimpleName('Anthoceros', draftVersion)
        TreeVersionElement dendrocerotaceaeTve = service.findElementBySimpleName('Dendrocerotaceae', draftVersion)
        List<Long> originalDendrocerotaceaeParentTaxonIDs = service.getParentTreeVersionElements(dendrocerotaceaeTve).collect {
            it.taxonId
        }
        List<Long> originalAnthocerotaceaeParentTaxonIDs = service.getParentTreeVersionElements(anthocerotaceaeTve).collect {
            it.taxonId
        }
        Instance replacementAnthocerosInstance = Instance.get(753948)
        TreeElement anthocerosTe = anthocerosTve.treeElement
        Long dendrocerotaceaeInitialTaxonId = dendrocerotaceaeTve.taxonId

        printTve(dendrocerotaceaeTve)
        printTve(anthocerotaceaeTve)

        expect:
        tree
        testElements.size() == 30
        draftVersion.treeVersionElements.size() == 30
        !draftVersion.published
        anthocerotaceaeTve
        anthocerosTve
        anthocerosTve.parent == anthocerotaceaeTve
        dendrocerotaceaeTve
        originalDendrocerotaceaeParentTaxonIDs.size() == 6
        originalAnthocerotaceaeParentTaxonIDs.size() == 6

        when: 'I try to move a taxon, anthoceros under dendrocerotaceae'
        Map result = service.replaceTaxon(anthocerosTve, dendrocerotaceaeTve,
                'http://localhost:7070/nsl-mapper/instance/apni/753948',
                'test move taxon')
        println "\n*** $result\n"

        TreeVersionElement.withSession { s ->
            s.flush()
        }
        draftVersion.refresh()

        List<TreeVersionElement> anthocerosChildren = service.getAllChildElements(result.replacementElement)
        List<TreeVersionElement> dendrocerotaceaeChildren = service.getAllChildElements(dendrocerotaceaeTve)

        printTve(dendrocerotaceaeTve)
        printTve(anthocerotaceaeTve)

        then: 'It works'
        1 * service.linkService.bulkRemoveTargets(_) >> { List<TreeVersionElement> elements ->
            [success: true]
        }
        1 * service.linkService.getObjectForLink(_) >> replacementAnthocerosInstance
        1 * service.linkService.getPreferredLinkForObject(replacementAnthocerosInstance.name) >> 'http://localhost:7070/nsl-mapper/name/apni/121601'
        1 * service.linkService.getPreferredLinkForObject(replacementAnthocerosInstance) >> 'http://localhost:7070/nsl-mapper/instance/apni/753948'
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        10 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }
        deleted(anthocerosTve) //deleted
        !deleted(anthocerosTe) // not deleted because it's referenced elsewhere
        result.replacementElement
        result.replacementElement == service.findElementBySimpleName('Anthoceros', draftVersion)
        result.replacementElement.treeVersion == draftVersion
        result.replacementElement.treeElement != anthocerosTe
        dendrocerotaceaeTve.taxonId != dendrocerotaceaeInitialTaxonId
        draftVersion.treeVersionElements.size() == 30
        anthocerosChildren.size() == 5
        result.replacementElement.parent == dendrocerotaceaeTve
        anthocerosChildren[0].treeElement.nameElement == 'capricornii'
        anthocerosChildren[0].parent == result.replacementElement
        anthocerosChildren[1].treeElement.nameElement == 'ferdinandi-muelleri'
        anthocerosChildren[2].treeElement.nameElement == 'fragilis'
        anthocerosChildren[3].treeElement.nameElement == 'laminifer'
        anthocerosChildren[4].treeElement.nameElement == 'punctatus'
        dendrocerotaceaeChildren.containsAll(anthocerosChildren)
        // all the parent taxonIds should have been updated
        !service.getParentTreeVersionElements(dendrocerotaceaeTve).collect { it.taxonId }.find {
            originalDendrocerotaceaeParentTaxonIDs.contains(it)
        }
        !service.getParentTreeVersionElements(anthocerotaceaeTve).collect { it.taxonId }.find {
            originalAnthocerotaceaeParentTaxonIDs.contains(it)
        }

        when: 'I publish the version then try a move'
        service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        service.replaceTaxon(anthocerosTve, anthocerotaceaeTve,
                'http://localhost:7070/nsl-mapper/instance/apni/753948',
                'test move taxon')

        then: 'I get a PublishedVersionException'
        thrown(PublishedVersionException)
    }

    def "test replace a taxon with multiple child levels"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        List<TreeElement> testElements = makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft')

        TreeVersionElement anthocerotalesTve = service.findElementBySimpleName('Anthocerotales', draftVersion)
        TreeVersionElement dendrocerotidaeTve = service.findElementBySimpleName('Dendrocerotidae', draftVersion)
        TreeVersionElement anthocerotidaeTve = service.findElementBySimpleName('Anthocerotidae', draftVersion)
        List<TreeVersionElement> anthocerotalesChildren = service.getAllChildElements(anthocerotalesTve)
        List<Long> originalDendrocerotidaeTaxonIDs = service.getParentTreeVersionElements(dendrocerotidaeTve).collect {
            it.taxonId
        }
        List<Long> originalAnthocerotidaeTaxonIDs = service.getParentTreeVersionElements(anthocerotidaeTve).collect {
            it.taxonId
        }
        Instance replacementAnthocerotalesInstance = Instance.get(753978)
        printTve(anthocerotidaeTve)
        printTve(dendrocerotidaeTve)

        expect:
        tree
        testElements.size() == 30
        draftVersion.treeVersionElements.size() == 30
        !draftVersion.published
        anthocerotalesTve
        dendrocerotidaeTve
        replacementAnthocerotalesInstance
        anthocerotalesTve.parent == anthocerotidaeTve
        anthocerotalesChildren.size() == 10
        originalDendrocerotidaeTaxonIDs.size() == 4
        originalAnthocerotidaeTaxonIDs.size() == 4

        when: 'I move Anthocerotales under Dendrocerotidae'
        Map result = service.replaceTaxon(anthocerotalesTve, dendrocerotidaeTve,
                'http://localhost:7070/nsl-mapper/instance/apni/753978',
                'test move taxon')
        println "\n*** $result\n"
        List<TreeVersionElement> newAnthocerotalesChildren = service.getAllChildElements(result.replacementElement)
        List<TreeVersionElement> dendrocerotidaeChildren = service.getAllChildElements(dendrocerotidaeTve)
        printTve(anthocerotidaeTve)
        printTve(dendrocerotidaeTve)
        draftVersion.refresh()

        then: 'It works'
        1 * service.linkService.bulkRemoveTargets(_) >> { List<TreeVersionElement> elements ->
            [success: true]
        }
        1 * service.linkService.getObjectForLink(_) >> replacementAnthocerotalesInstance
        1 * service.linkService.getPreferredLinkForObject(replacementAnthocerotalesInstance.name) >> 'http://localhost:7070/nsl-mapper/name/apni/142301'
        1 * service.linkService.getPreferredLinkForObject(replacementAnthocerotalesInstance) >> 'http://localhost:7070/nsl-mapper/instance/apni/753978'
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        6 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }

        deleted(anthocerotalesTve) //deleted
        result.replacementElement
        result.replacementElement == service.findElementBySimpleName('Anthocerotales', draftVersion)
        result.replacementElement.treeVersion == draftVersion

        draftVersion.treeVersionElements.size() == 30
        newAnthocerotalesChildren.size() == 10
        dendrocerotidaeChildren.size() == 25
        // all the parent taxonIds should have been updated
        !service.getParentTreeVersionElements(dendrocerotidaeTve).collect { it.taxonId }.find {
            originalDendrocerotidaeTaxonIDs.contains(it)
        }
        !service.getParentTreeVersionElements(anthocerotidaeTve).collect { it.taxonId }.find {
            originalAnthocerotidaeTaxonIDs.contains(it)
        }

    }

    def "test remove a taxon"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        List<TreeElement> testElements = makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft')

        TreeVersionElement anthocerotaceae = service.findElementBySimpleName('Anthocerotaceae', draftVersion)
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)
        List<Long> originalAnthocerotaceaeTaxonIDs = service.getParentTreeVersionElements(anthocerotaceae).collect {
            it.taxonId
        }

        expect:
        tree
        testElements.size() == 30
        draftVersion.treeVersionElements.size() == 30
        !draftVersion.published
        anthocerotaceae
        anthoceros
        anthoceros.parent == anthocerotaceae
        originalAnthocerotaceaeTaxonIDs.size() == 6

        when: 'I try to move a taxon'
        int count = service.removeTreeVersionElement(anthoceros)

        then: 'It works'
        6 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }
        count == 6
        draftVersion.treeVersionElements.size() == 24
        service.findElementBySimpleName('Anthoceros', draftVersion) == null
        //The taxonIds for Anthoceros' parents should have changed
        !service.getParentTreeVersionElements(anthocerotaceae).collect { it.taxonId }.find {
            originalAnthocerotaceaeTaxonIDs.contains(it)
        }
    }

    def "test edit taxon profile"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft')
        TreeVersionElement anthocerosTve = service.findElementBySimpleName('Anthoceros', draftVersion)
        TreeVersionElement pubAnthocerosTve = service.findElementBySimpleName('Anthoceros', publishedVersion)

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        publishedVersion
        publishedVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == publishedVersion
        pubAnthocerosTve
        anthocerosTve.treeElement.profile == [
                "APC Dist.": [
                        value      : "WA, NT, SA, Qld, NSW, ACT, Vic, Tas",
                        created_at : "2011-01-27T00:00:00+11:00",
                        created_by : "KIRSTENC",
                        updated_at : "2011-01-27T00:00:00+11:00",
                        updated_by : "KIRSTENC",
                        source_link: "http://localhost:7070/nsl-mapper/instanceNote/apni/1117116"
                ]
        ]

        when: 'I update the profile on the published version'
        service.editProfile(pubAnthocerosTve, ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]], 'test edit profile')

        then: 'I get a PublishedVersionException'
        thrown(PublishedVersionException)

        when: 'I update a profile on the draft version'
        List<TreeVersionElement> childTves = TreeVersionElement.findAllByParent(anthocerosTve)
        TreeElement oldElement = anthocerosTve.treeElement
        Timestamp oldUpdatedAt = anthocerosTve.treeElement.updatedAt
        Long oldTaxonId = anthocerosTve.taxonId
        TreeVersionElement replacedAnthocerosTve = service.editProfile(anthocerosTve, ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]], 'test edit profile')
        childTves.each {
            it.refresh()
        }

        then: 'It updates the treeElement profile'
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        oldElement
        deleted(anthocerosTve)
        replacedAnthocerosTve
        childTves.findAll {
            it.parent == replacedAnthocerosTve
        }.size() == childTves.size()
        replacedAnthocerosTve.taxonId == oldTaxonId
        replacedAnthocerosTve.treeElement != oldElement
        replacedAnthocerosTve.treeElement.profile == ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]]
        replacedAnthocerosTve.treeElement.updatedBy == 'test edit profile'
        replacedAnthocerosTve.treeElement.updatedAt.after(oldUpdatedAt)

        when: 'I change a profile to the same thing'
        TreeVersionElement anthocerosCapricornii = service.findElementBySimpleName('Anthoceros capricornii', draftVersion)
        TreeElement oldACElement = anthocerosCapricornii.treeElement
        oldTaxonId = anthocerosCapricornii.taxonId
        Map oldProfile = new HashMap(anthocerosCapricornii.treeElement.profile)
        TreeVersionElement treeVersionElement1 = service.editProfile(anthocerosCapricornii, oldProfile, 'test edit profile')

        then: 'nothing changes'

        treeVersionElement1 == anthocerosCapricornii
        treeVersionElement1.taxonId == oldTaxonId
        treeVersionElement1.treeElement == oldACElement
        treeVersionElement1.treeElement.profile == oldProfile
    }

    def "test edit draft only taxon profile"() {
        given:
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
        TreeVersionElement anthoceros = service.findElementBySimpleName('Anthoceros', draftVersion)

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 30
        tree.defaultDraftTreeVersion == draftVersion
        tree.currentTreeVersion == null
        anthoceros.treeElement.profile == [
                "APC Dist.": [
                        value      : "WA, NT, SA, Qld, NSW, ACT, Vic, Tas",
                        created_at : "2011-01-27T00:00:00+11:00",
                        created_by : "KIRSTENC",
                        updated_at : "2011-01-27T00:00:00+11:00",
                        updated_by : "KIRSTENC",
                        source_link: "http://localhost:7070/nsl-mapper/instanceNote/apni/1117116"
                ]
        ]

        when: 'I update a profile on the draft version'
        TreeElement oldElement = anthoceros.treeElement
        Timestamp oldTimestamp = anthoceros.treeElement.updatedAt
        Long oldTaxonId = anthoceros.taxonId
        TreeVersionElement treeVersionElement = service.editProfile(anthoceros, ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]], 'test edit profile')

        then: 'It updates the treeElement and updates the profile and not the taxonId'
        treeVersionElement
        oldElement
        treeVersionElement == anthoceros
        treeVersionElement.taxonId == oldTaxonId
        treeVersionElement.treeElement == oldElement
        treeVersionElement.treeElement.profile == ['APC Dist.': [value: "WA, NT, SA, Qld, NSW"]]
        treeVersionElement.treeElement.updatedBy == 'test edit profile'
        treeVersionElement.treeElement.updatedAt > oldTimestamp
    }

    def "test edit taxon excluded status"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft')
        makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
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
        Long oldTaxonId = anthoceros.taxonId
        TreeElement oldElement = anthoceros.treeElement
        TreeVersionElement treeVersionElement = service.editExcluded(anthoceros, true, 'test edit status')

        then: 'It creates a new treeVersionElement and treeElement updates the children TVEs and updates the status'
        1 * service.linkService.bulkRemoveTargets(_) >> { List<TreeVersionElement> tves -> [success: true] }
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        treeVersionElement
        oldElement
        deleted(anthoceros)
        treeVersionElement.taxonId == oldTaxonId
        treeVersionElement.treeElement != oldElement
        treeVersionElement.treeElement.excluded
        treeVersionElement.treeElement.updatedBy == 'test edit status'

        when: 'I change status to the same thing'
        TreeVersionElement anthocerosCapricornii = service.findElementBySimpleName('Anthoceros capricornii', draftVersion)
        TreeElement oldACElement = anthocerosCapricornii.treeElement
        oldTaxonId = anthocerosCapricornii.taxonId
        TreeVersionElement treeVersionElement1 = service.editExcluded(anthocerosCapricornii, false, 'test edit status')

        then: 'nothing changes'
        treeVersionElement1 == anthocerosCapricornii
        treeVersionElement1.taxonId == oldTaxonId
        treeVersionElement1.treeElement == oldACElement
        !treeVersionElement1.treeElement.excluded
    }

    def "test update child tree path"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft')
        makeTestElements(draftVersion, testElementData(), testTreeVersionElementData())
        makeTestElements(draftVersion, [doodiaElementData], [doodiaTVEData]).first()
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft')

        TreeVersionElement anthocerosTve = service.findElementBySimpleName('Anthoceros', draftVersion)
        TreeVersionElement doodiaTve = service.findElementBySimpleName('Doodia', draftVersion)
        List<TreeVersionElement> anthocerosChildren = service.getAllChildElements(anthocerosTve)
        printTve(anthocerosTve)

        expect:
        tree
        draftVersion.treeVersionElements.size() == 31
        !draftVersion.published
        anthocerosTve
        doodiaTve
        anthocerosChildren.size() == 5
        anthocerosChildren.findAll { it.treePath.contains(anthocerosTve.treeElement.id.toString()) }.size() == 5

        when: "I update the tree path changing an element"
        service.updateChildTreePath(doodiaTve.treePath, anthocerosTve.treePath, anthocerosTve.treeVersion)

        List<TreeVersionElement> doodiaChildren = service.getAllChildElements(doodiaTve)

        then: "The tree paths of anthoceros kids have changed"
        doodiaChildren.size() == 5
        anthocerosChildren.containsAll(doodiaChildren)

    }


    private printTve(TreeVersionElement target) {
        println "*** Taxon $target.taxonId: $target.treeElement.name.simpleName Children ***"
        for (TreeVersionElement tve in service.getAllChildElements(target)) {
            tve.refresh()
            println "Taxon: $tve.taxonId, Names: $tve.namePath, Path: $tve.treePath"
        }
    }

    private static deleted(domainObject) {
        Name.withSession { session ->
            session.persistenceContext.getEntry(domainObject)?.status in [null, Status.DELETED]
        }
    }

    private Tree makeATestTree() {
        service.createNewTree('aTree', 'aGroup', null, '<p>A description</p>', 'http://trees.org/aTree', false)
    }

    private Tree makeBTestTree() {
        service.createNewTree('bTree', 'aGroup', null, '<p>B description</p>', 'http://trees.org/bTree', false)
    }

    private static List<TreeElement> makeTestElements(TreeVersion version, List<Map> elementData, List<Map> tveData) {
        List<TreeElement> elements = []
        Map<Long, Long> generatedIdMapper = [:]
        elementData.each { Map data ->
            data.remove('previousElementId')
            TreeElement e = new TreeElement(data)
            e.save()
            generatedIdMapper.put(data.id as Long, e.id as Long)
            elements.add(e)
        }

        Map<String, String> elementLinkMapper = [:]
        tveData.each { Map data ->

            TreeVersionElement tve = new TreeVersionElement(data)
            tve.treeVersion = version
            tve.treeElement = TreeElement.get(generatedIdMapper.get(data.treeElementId as Long) as Long)
            tve.elementLink = "http://localhost:7070/nsl-mapper/tree/$version.id/$data.treeElementId"
            tve.treePath = filterTreePath(generatedIdMapper, data.treePath)
            tve.parent = TreeVersionElement.get(elementLinkMapper[data.parentId])
            elementLinkMapper.put(data.elementLink, tve.elementLink)
            tve.save()
        }

        version.save(flush: true)
        version.refresh()
        return elements
    }

    private static filterTreePath(Map idMap, String treePath) {
        if (idMap.isEmpty()) {
            println "ID Map is empty!!!!!!!!"
        }
        idMap.each { k, v ->
            treePath = treePath.replaceAll("/$k", "/$v")
        }
        return treePath
    }


    private static Map doodiaElementData = [
            id               : 9788223,
            lockVersion      : 0,
            displayHtml      : "<data><scientific><name id='70914'><element class='Doodia'>Doodia</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific><citation>Parris, B.S. in McCarthy, P.M. (ed.) (1998), Doodia. <i>Flora of Australia</i> 48</citation></data>",
            excluded         : false,
            instanceId       : 578615,
            instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/578615",
            nameElement      : "Doodia",
            nameId           : 70914,
            nameLink         : "http://localhost:7070/nsl-mapper/name/apni/70914",
            previousElementId: null,
            profile          : ["APC Dist.": ["value": "NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas", "created_at": "2008-08-06T00:00:00+10:00", "created_by": "BRONWYNC", "updated_at": "2008-08-06T00:00:00+10:00", "updated_by": "BRONWYNC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1105068"], "APC Comment": ["value": "<i>Doodia</i> R.Br. is included in <i>Blechnum</i> L. in NSW.", "created_at": "2016-06-10T15:22:41.742+10:00", "created_by": "blepschi", "updated_at": "2016-06-10T15:23:01.201+10:00", "updated_by": "blepschi", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/6842406"]],
            rank             : "Genus",
            simpleName       : "Doodia",
            sourceElementLink: null,
            sourceShard      : "APNI",
            synonyms         : null,
            synonymsHtml     : "<synonyms></synonyms>",
            updatedAt        : "2018-01-01 15:22:55.387530",
            updatedBy        : "import"
    ]

    private static Map doodiaTVEData = [
            elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9788223",
            depth        : 7,
            namePath     : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia",
            parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9788187",
            taxonId      : 2910041,
            taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2910041",
            treeElementId: 9788223,
            treePath     : "/9787517/9787518/9787519/9787648/9787977/9788187/9788223",
            treeVersionId: 9787484
    ]

    private static Map blechnaceaeElementData = [
            id               : 9788187,
            lockVersion      : 0,
            displayHtml      : "<data><scientific><name id='222592'><element class='Blechnaceae'>Blechnaceae</element> <authors><author id='8244' title='Newman, E.'>Newman</author></authors></name></scientific><citation>CHAH (2009), <i>Australian Plant Census</i></citation></data>",
            excluded         : false,
            instanceId       : 651382,
            instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/651382",
            nameElement      : "Blechnaceae",
            nameId           : 222592,
            nameLink         : "http://localhost:7070/nsl-mapper/name/apni/222592",
            previousElementId: null,
            profile          : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, LHI, NI, ACT, Vic, Tas, MI", "created_at": "2009-10-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2009-10-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1111961"]],
            rank             : "Familia",
            simpleName       : "Blechnaceae",
            sourceElementLink: null,
            sourceShard      : "APNI",
            synonyms         : null,
            synonymsHtml     : "<synonyms></synonyms>",
            updatedAt        : "2018-01-01 15:22:55.387530",
            updatedBy        : "import"
    ]

    private static Map blechnaceaeTVEData = [
            elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9788187",
            depth        : 6,
            namePath     : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae",
            parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9787977",
            taxonId      : 8032171,
            taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8032171",
            treeElementId: 9788187,
            treePath     : "/9787517/9787518/9787519/9787648/9787977/9788187",
            treeVersionId: 9787484
    ]

    private static Map asperaElementData = [
            id               : 9788224,
            lockVersion      : 0,
            displayHtml      : "<data><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific><citation>CHAH (2014), <i>Australian Plant Census</i></citation></data>",
            excluded         : false,
            instanceId       : 781104,
            instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/781104",
            nameElement      : "aspera",
            nameId           : 70944,
            nameLink         : "http://localhost:7070/nsl-mapper/name/apni/70944",
            previousElementId: null,
            profile          : ["APC Dist.": ["value": "Qld, NSW, LHI, NI, Vic, Tas", "created_at": "2014-03-25T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2014-03-25T14:04:06+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1132306"], "APC Comment": ["value": "Treated as <i>Blechnum neohollandicum</i> Christenh. in NSW.", "created_at": "2016-06-10T15:21:38.135+10:00", "created_by": "blepschi", "updated_at": "2016-06-10T15:21:38.135+10:00", "updated_by": "blepschi", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/6842405"]],
            rank             : "Species",
            simpleName       : "Doodia aspera",
            sourceElementLink: null,
            sourceShard      : "APNI",
            synonyms         : ["Woodwardia aspera": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Mettenius, G.H. (1856), <i>Filices Horti Botanici Lipsiensis</i>", "name_id": 106698, "name_link": "http://localhost:7070/nsl-mapper/name/apni/106698", "full_name_html": "<scientific><name id='106698'><scientific><name id='106675'><element class='Woodwardia'>Woodwardia</element></name></scientific> <element class='aspera'>aspera</element> <authors>(<base id='1441' title='Brown, R.'>R.Br.</base>) <author id='7081' title='Mettenius, G.H.'>Mett.</author></authors></name></scientific>"], "Blechnum neohollandicum": ["mis": false, "nom": false, "tax": true, "type": "taxonomic synonym", "cites": "Christenhusz, M.J.M., Zhang, X.C. & Schneider, H. (2011), A linear sequence of extant families and genera of lycophytes and ferns. <i>Phytotaxa</i> 19", "name_id": 239547, "name_link": "http://localhost:7070/nsl-mapper/name/apni/239547", "full_name_html": "<scientific><name id='239547'><scientific><name id='56340'><element class='Blechnum'>Blechnum</element></name></scientific> <element class='neohollandicum'>neohollandicum</element> <authors><author id='6422' title='Christenhusz, M.J.M.'>Christenh.</author></authors></name></scientific>"], "Doodia aspera var. aspera": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Bailey, F.M. (1881), <i>The fern world of Australia</i>", "name_id": 70967, "name_link": "http://localhost:7070/nsl-mapper/name/apni/70967", "full_name_html": "<scientific><name id='70967'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific> <rank id='54412'>var.</rank> <element class='aspera'>aspera</element></name></scientific>"], "Doodia aspera var. angustifrons": ["mis": false, "nom": false, "tax": true, "type": "taxonomic synonym", "cites": "Domin, K. (1915), Beitrage zur Flora und Pflanzengeographie Australiens. <i>Bibliotheca Botanica</i> 20(85)", "name_id": 70959, "name_link": "http://localhost:7070/nsl-mapper/name/apni/70959", "full_name_html": "<scientific><name id='70959'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element></name></scientific> <rank id='54412'>var.</rank> <element class='angustifrons'>angustifrons</element> <authors><author id='6860' title='Domin, K.'>Domin</author></authors></name></scientific>"]],
            synonymsHtml     : "<synonyms><nom><scientific><name id='106698'><scientific><name id='106675'><element class='Woodwardia'>Woodwardia</element></name></scientific> <element class='aspera'>aspera</element> <authors>(<base id='1441' title='Brown, R.'>R.Br.</base>) <author id='7081' title='Mettenius, G.H.'>Mett.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom><nom><scientific><name id='70967'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element> <authors><author id='1441' title='Brown, R.'>R.Br.</author></authors></name></scientific> <rank id='54412'>var.</rank> <element class='aspera'>aspera</element></name></scientific> <type>nomenclatural synonym</type></nom><tax><scientific><name id='70959'><scientific><name id='70944'><scientific><name id='70914'><element class='Doodia'>Doodia</element></name></scientific> <element class='aspera'>aspera</element></name></scientific> <rank id='54412'>var.</rank> <element class='angustifrons'>angustifrons</element> <authors><author id='6860' title='Domin, K.'>Domin</author></authors></name></scientific> <type>taxonomic synonym</type></tax><tax><scientific><name id='239547'><scientific><name id='56340'><element class='Blechnum'>Blechnum</element></name></scientific> <element class='neohollandicum'>neohollandicum</element> <authors><author id='6422' title='Christenhusz, M.J.M.'>Christenh.</author></authors></name></scientific> <type>taxonomic synonym</type></tax></synonyms>",
            updatedAt        : "2018-01-01 15:22:55.387530",
            updatedBy        : "import"
    ]

    private static Map asperaTVEData = [
            elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9788224",
            depth        : 8,
            namePath     : "Plantae/Charophyta/Equisetopsida/Polypodiidae/Polypodiales/Blechnaceae/Doodia/aspera",
            parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9788223",
            taxonId      : 2895769,
            taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2895769",
            treeElementId: 9788224,
            treePath     : "/9787517/9787518/9787519/9787648/9787977/9788187/9788223/9788224",
            treeVersionId: 9787484
    ]

    private static List<Map> testTreeVersionElementData() {
        [
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9787517",
                        depth        : 1,
                        namePath     : "Plantae",
                        parentId     : null,
                        taxonId      : 9723412,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/9723412",
                        treeElementId: 9787517,
                        treePath     : "/9787517",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825378",
                        depth        : 2,
                        namePath     : "Plantae/Anthocerotophyta",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9787517",
                        taxonId      : 9434056,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/9434056",
                        treeElementId: 9825378,
                        treePath     : "/9787517/9825378",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825379",
                        depth        : 3,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825378",
                        taxonId      : 9434055,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/9434055",
                        treeElementId: 9825379,
                        treePath     : "/9787517/9825378/9825379",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825380",
                        depth        : 4,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825379",
                        taxonId      : 8513221,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513221",
                        treeElementId: 9825380,
                        treePath     : "/9787517/9825378/9825379/9825380",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825381",
                        depth        : 5,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825380",
                        taxonId      : 8513220,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513220",
                        treeElementId: 9825381,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825382",
                        depth        : 6,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825381",
                        taxonId      : 8513200,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513200",
                        treeElementId: 9825382,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825383",
                        depth        : 7,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825382",
                        taxonId      : 6722223,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/6722223",
                        treeElementId: 9825383,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825383",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825384",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/capricornii",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825383",
                        taxonId      : 2910349,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2910349",
                        treeElementId: 9825384,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825383/9825384",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825385",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/ferdinandi-muelleri",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825383",
                        taxonId      : 2909847,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2909847",
                        treeElementId: 9825385,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825383/9825385",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825388",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/fragilis",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825383",
                        taxonId      : 2916003,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2916003",
                        treeElementId: 9825388,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825383/9825388",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825386",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/laminifer",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825383",
                        taxonId      : 2891332,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2891332",
                        treeElementId: 9825386,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825383/9825386",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825387",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Anthoceros/punctatus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825383",
                        taxonId      : 2901206,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2901206",
                        treeElementId: 9825387,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825383/9825387",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825389",
                        depth        : 7,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825382",
                        taxonId      : 2894485,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2894485",
                        treeElementId: 9825389,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825389",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825390",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros/fuciformis",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825389",
                        taxonId      : 2891695,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2891695",
                        treeElementId: 9825390,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825389/9825390",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825391",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Anthocerotidae/Anthocerotales/Anthocerotaceae/Folioceros/glandulosus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825389",
                        taxonId      : 2896010,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2896010",
                        treeElementId: 9825391,
                        treePath     : "/9787517/9825378/9825379/9825380/9825381/9825382/9825389/9825391",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825408",
                        depth        : 4,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825379",
                        taxonId      : 8513224,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513224",
                        treeElementId: 9825408,
                        treePath     : "/9787517/9825378/9825379/9825408",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825409",
                        depth        : 5,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825408",
                        taxonId      : 8513222,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513222",
                        treeElementId: 9825409,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825410",
                        depth        : 6,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825409",
                        taxonId      : 8513219,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513219",
                        treeElementId: 9825410,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825411",
                        depth        : 7,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825410",
                        taxonId      : 8513207,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513207",
                        treeElementId: 9825411,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825411",
                        taxonId      : 2909398,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2909398",
                        treeElementId: 9825412,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825413",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/australis",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2895623,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2895623",
                        treeElementId: 9825413,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825413",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825414",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/crispatus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2909090,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2909090",
                        treeElementId: 9825414,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825414",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825418",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/difficilis",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2897137,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2897137",
                        treeElementId: 9825418,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825418",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825415",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/granulatus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2897129,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2897129",
                        treeElementId: 9825415,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825415",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825419",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/muelleri",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2916733,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2916733",
                        treeElementId: 9825419,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825419",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825416",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/subtropicus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2917550,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2917550",
                        treeElementId: 9825416,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825416",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825417",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Dendroceros/wattsianus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825412",
                        taxonId      : 2913739,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2913739",
                        treeElementId: 9825417,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825412/9825417",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825420",
                        depth        : 8,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825411",
                        taxonId      : 8513209,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/8513209",
                        treeElementId: 9825420,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825420",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825421",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros/carnosus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825420",
                        taxonId      : 2917526,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2917526",
                        treeElementId: 9825421,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825420/9825421",
                        treeVersionId: 9787484
                ],
                [
                        elementLink  : "http://localhost:7070/nsl-mapper/tree/9787484/9825422",
                        depth        : 9,
                        namePath     : "Plantae/Anthocerotophyta/Anthocerotopsida/Dendrocerotidae/Dendrocerotales/Dendrocerotaceae/Dendrocerotoideae/Megaceros/crassus",
                        parentId     : "http://localhost:7070/nsl-mapper/tree/9787484/9825420",
                        taxonId      : 2909144,
                        taxonLink    : "http://localhost:7070/nsl-mapper/node/apni/2909144",
                        treeElementId: 9825422,
                        treePath     : "/9787517/9825378/9825379/9825408/9825409/9825410/9825411/9825420/9825422",
                        treeVersionId: 9787484
                ]
        ]
    }


    private static List<Map> testElementData() {
        [
                [
                        id               : 9787517,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='54717'><element class='Plantae'>Plantae</element> <authors><author id='3882' title='Haeckel, Ernst Heinrich Philipp August'>Haeckel</author></authors></name></scientific><citation>CHAH (2012), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 738442,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/738442",
                        nameElement      : "Plantae",
                        nameId           : 54717,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/54717",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Regnum",
                        simpleName       : "Plantae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825378,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='238892'><element class='Anthocerotophyta'>Anthocerotophyta</element> <authors><ex id='7053' title='Rothmaler, W.H.P.'>Rothm.</ex> ex <author id='5307' title='Stotler,R.E. &amp; Crandall-Stotler,B.J.'>Stotler & Crand.-Stotl.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8509445,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8509445",
                        nameElement      : "Anthocerotophyta",
                        nameId           : 238892,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/238892",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Division",
                        simpleName       : "Anthocerotophyta",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825379,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='238893'><element class='Anthocerotopsida'>Anthocerotopsida</element> <authors><ex id='2484' title='de Bary, H.A.'>de Bary</ex> ex <author id='3443' title='Janczewski, E. von G.'>Jancz.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8509444,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8509444",
                        nameElement      : "Anthocerotopsida",
                        nameId           : 238893,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/238893",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Classis",
                        simpleName       : "Anthocerotopsida",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825380,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='240384'><element class='Anthocerotidae'>Anthocerotidae</element> <authors><author id='6453' title='Rosenvinge, J.L.A.K.'>Rosenv.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8509443,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8509443",
                        nameElement      : "Anthocerotidae",
                        nameId           : 240384,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240384",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Subclassis",
                        simpleName       : "Anthocerotidae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825381,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='142301'><element class='Anthocerotales'>Anthocerotales</element> <authors><author id='2624' title='Limpricht, K.G.'>Limpr.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8508886,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8508886",
                        nameElement      : "Anthocerotales",
                        nameId           : 142301,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/142301",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Ordo",
                        simpleName       : "Anthocerotales",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825382,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='124939'><element class='Anthocerotaceae'>Anthocerotaceae</element> <authors>(<base id='2041' title='Gray, S.F.'>Gray</base>) <author id='6855' title='Dumortier, B.C.J.'>Dumort.</author></authors></name></scientific><citation>CHAH (2011), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 748950,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/748950",
                        nameElement      : "Anthocerotaceae",
                        nameId           : 124939,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/124939",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "WA, NT, Qld, NSW, Vic, Tas", "created_at": "2012-05-21T00:00:00+10:00", "created_by": "KIRSTENC", "updated_at": "2012-05-21T00:00:00+10:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1124834"]],
                        rank             : "Familia",
                        simpleName       : "Anthocerotaceae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825383,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element> <authors><author id='1426' title='Linnaeus, C.'>L.</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 668637,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/668637",
                        nameElement      : "Anthoceros",
                        nameId           : 121601,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/121601",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "WA, NT, SA, Qld, NSW, ACT, Vic, Tas", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117116"]],
                        rank             : "Genus",
                        simpleName       : "Anthoceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825384,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='144273'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='capricornii'>capricornii</element> <authors><author id='1771' title='Cargill, D.C. &amp; Scott, G.A.M.'>Cargill & G.A.M.Scott</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621662,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621662",
                        nameElement      : "capricornii",
                        nameId           : 144273,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/144273",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "WA, NT", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117117"]],
                        rank             : "Species",
                        simpleName       : "Anthoceros capricornii",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros adscendens": ["mis": true, "nom": false, "tax": false, "type": "pro parte misapplied", "cites": "Lehmann, J.G.C. & Lindenberg, J.B.W. in Lehmann, J.G.C. (1832), <i>Novarum et Minus Cognitarum Stirpium Pugillus</i> 4", "name_id": 162382, "name_link": "http://localhost:7070/nsl-mapper/name/apni/162382", "full_name_html": "<scientific><name id='162382'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='adscendens'>adscendens</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><mis><scientific><name id='162382'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='adscendens'>adscendens</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific> <type>pro parte misapplied</type> by <citation>Lehmann, J.G.C. & Lindenberg, J.B.W. in Lehmann, J.G.C. (1832), <i>Novarum et Minus Cognitarum Stirpium Pugillus</i> 4</citation></mis></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825385,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='209869'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='ferdinandi-muelleri'>ferdinandi-muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621668,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621668",
                        nameElement      : "ferdinandi-muelleri",
                        nameId           : 209869,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/209869",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "?Qld", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117118"]],
                        rank             : "Species",
                        simpleName       : "Anthoceros ferdinandi-muelleri",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825388,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='142232'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fragilis'>fragilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>Cargill, D.C., Söderström, L., Hagborg, A. & Konrat, M. von (2013), Notes on Early Land Plants Today. 23. A new synonym in Anthoceros (Anthocerotaceae, Anthocerotophyta). <i>Phytotaxa</i> 76(3)</citation></data>",
                        excluded         : false,
                        instanceId       : 760852,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/760852",
                        nameElement      : "fragilis",
                        nameId           : 142232,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/142232",
                        previousElementId: null,
                        profile          : ["Type": ["value": "Australia. Queensland: Amalie Dietrich (holotype G-61292! [=G-24322!]).", "created_at": "2013-01-16T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2013-01-16T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1086559"], "APC Dist.": ["value": "Qld", "created_at": "2013-02-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2013-02-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1128261"]],
                        rank             : "Species",
                        simpleName       : "Anthoceros fragilis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros fertilis": ["mis": false, "nom": false, "tax": true, "type": "taxonomic synonym", "cites": "Stephani, F. (1916), <i>Species Hepaticarum</i> 5", "name_id": 209870, "name_link": "http://localhost:7070/nsl-mapper/name/apni/209870", "full_name_html": "<scientific><name id='209870'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fertilis'>fertilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><tax><scientific><name id='209870'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fertilis'>fertilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific> <type>taxonomic synonym</type></tax></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825386,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='202233'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='laminifer'>laminifer</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621709,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621709",
                        nameElement      : "laminifer",
                        nameId           : 202233,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/202233",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117121"]],
                        rank             : "Species",
                        simpleName       : "Anthoceros laminifer",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825387,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='122138'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='punctatus'>punctatus</element> <authors><author id='1426' title='Linnaeus, C.'>L.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 621713,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621713",
                        nameElement      : "punctatus",
                        nameId           : 122138,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/122138",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "SA, Qld, NSW, ACT, Vic, Tas", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117122"]],
                        rank             : "Species",
                        simpleName       : "Anthoceros punctatus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825389,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='134990'><element class='Folioceros'>Folioceros</element> <authors><author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 669233,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/669233",
                        nameElement      : "Folioceros",
                        nameId           : 134990,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/134990",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld, NSW", "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117130"]],
                        rank             : "Genus",
                        simpleName       : "Folioceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825390,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='143486'><scientific><name id='134990'><element class='Folioceros'>Folioceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors>(<base id='8996' title='Montagne, J.P.F.C.'>Mont.</base>) <author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>Bhardwaj, D.C. (1975), <i>Geophytology</i> 5</citation></data>",
                        excluded         : false,
                        instanceId       : 621673,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/621673",
                        nameElement      : "fuciformis",
                        nameId           : 143486,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/143486",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117131"]],
                        rank             : "Species",
                        simpleName       : "Folioceros fuciformis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros fuciformis": ["mis": false, "nom": true, "tax": false, "type": "basionym", "cites": "Montagne, J.P.F.C. (1843), <i>Annales des Sciences Naturelles; Botanique</i> 20", "name_id": 142253, "name_link": "http://localhost:7070/nsl-mapper/name/apni/142253", "full_name_html": "<scientific><name id='142253'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors><author id='8996' title='Montagne, J.P.F.C.'>Mont.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='142253'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='fuciformis'>fuciformis</element> <authors><author id='8996' title='Montagne, J.P.F.C.'>Mont.</author></authors></name></scientific> <type>basionym</type></nom></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825391,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='134991'><scientific><name id='134990'><element class='Folioceros'>Folioceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1941' title='Bhardwaj, D.C.'>D.C.Bhardwaj</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 669234,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/669234",
                        nameElement      : "glandulosus",
                        nameId           : 134991,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/134991",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW", "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117132"]],
                        rank             : "Species",
                        simpleName       : "Folioceros glandulosus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros glandulosus": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Lehmann, J.G.C. & Lindenberg, J.B.W. in Lehmann, J.G.C. (1832), <i>Novarum et Minus Cognitarum Stirpium Pugillus</i> 4", "name_id": 129589, "name_link": "http://localhost:7070/nsl-mapper/name/apni/129589", "full_name_html": "<scientific><name id='129589'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific>"], "Aspiromitus glandulosus": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Stephani, F. (1916), <i>Species Hepaticarum</i> 5", "name_id": 209879, "name_link": "http://localhost:7070/nsl-mapper/name/apni/209879", "full_name_html": "<scientific><name id='209879'><scientific><name id='172172'><element class='Aspiromitus'>Aspiromitus</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='129589'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors><author id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom><nom><scientific><name id='209879'><scientific><name id='172172'><element class='Aspiromitus'>Aspiromitus</element></name></scientific> <element class='glandulosus'>glandulosus</element> <authors>(<base id='1628' title='Lehmann, J.G.C. &amp; Lindenberg, J.B.W.'>Lehm. & Lindenb.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825408,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='240391'><element class='Dendrocerotidae'>Dendrocerotidae</element> <authors><author id='5261' title='Duff, R.J., Villarreal, J.C., Cargill, D.C. &amp; Renzaglia, K.S.'>Duff, J.C.Villarreal, Cargill & Renzaglia</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8511897,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8511897",
                        nameElement      : "Dendrocerotidae",
                        nameId           : 240391,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240391",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Subclassis",
                        simpleName       : "Dendrocerotidae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825409,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='240393'><element class='Dendrocerotales'>Dendrocerotales</element> <authors><author id='3432' title='H&auml;ssel de Men&eacute;ndez, G.G.'>Hässel</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8512151,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8512151",
                        nameElement      : "Dendrocerotales",
                        nameId           : 240393,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240393",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Ordo",
                        simpleName       : "Dendrocerotales",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825410,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='193461'><element class='Dendrocerotaceae'>Dendrocerotaceae</element> <authors>(<base id='2276' title='Milde, C.A.J.'>Milde</base>) <author id='3432' title='H&auml;ssel de Men&eacute;ndez, G.G.'>Hässel</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8512407,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8512407",
                        nameElement      : "Dendrocerotaceae",
                        nameId           : 193461,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/193461",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Familia",
                        simpleName       : "Dendrocerotaceae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825411,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='240394'><scientific><name id='193461'><element class='Dendrocerotaceae'>Dendrocerotaceae</element></name></scientific> <element class='Dendrocerotoideae'>Dendrocerotoideae</element> <authors><author id='1751' title='Schuster, R.M.'>R.M.Schust.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8512665,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8512665",
                        nameElement      : "Dendrocerotoideae",
                        nameId           : 240394,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/240394",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Subfamilia",
                        simpleName       : "Dendrocerotaceae Dendrocerotoideae",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825412,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element> <authors><author id='6893' title='Nees von Esenbeck, C.G.D.'>Nees</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 668662,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/668662",
                        nameElement      : "Dendroceros",
                        nameId           : 129597,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/129597",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld, NSW", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117123"]],
                        rank             : "Genus",
                        simpleName       : "Dendroceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825413,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210308'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='australis'>australis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622818,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622818",
                        nameElement      : "australis",
                        nameId           : 210308,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210308",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117124"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros australis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825414,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='178505'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='crispatus'>crispatus</element> <authors>(<base id='6851' title='Hooker, W.J.'>Hook.</base>) <author id='6893' title='Nees von Esenbeck, C.G.D.'>Nees</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622823,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622823",
                        nameElement      : "crispatus",
                        nameId           : 178505,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/178505",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117125"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros crispatus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Monoclea crispata": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Hooker, W.J. in Hooker, W.J. (ed.) (1830), <i>Botanical Miscellany</i> 1", "name_id": 210309, "name_link": "http://localhost:7070/nsl-mapper/name/apni/210309", "full_name_html": "<scientific><name id='210309'><scientific><name id='133731'><element class='Monoclea'>Monoclea</element></name></scientific> <element class='crispata'>crispata</element> <authors><author id='6851' title='Hooker, W.J.'>Hook.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='210309'><scientific><name id='133731'><element class='Monoclea'>Monoclea</element></name></scientific> <element class='crispata'>crispata</element> <authors><author id='6851' title='Hooker, W.J.'>Hook.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825418,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210317'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='difficilis'>difficilis</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>Pócs, T. & Streimann, H. (1999), Epiphyllous liverworts from Queensland, Australia. <i>Bryobrothera</i> 5</citation></data>",
                        excluded         : false,
                        instanceId       : 622849,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622849",
                        nameElement      : "difficilis",
                        nameId           : 210317,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210317",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "BLEPSCHI", "updated_at": "2013-10-24T12:18:26+11:00", "updated_by": "BLEPSCHI", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1131154"], "APC Comment": ["value": "Listed as \"<i>Dendroceros</i> cf. <i>difficilis</i>\" by P.M.McCarthy, <i>Checkl. Austral. Liverworts and Hornworts</i> 35 (2003).", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "BLEPSCHI", "updated_at": "2013-10-24T12:18:26+11:00", "updated_by": "BLEPSCHI", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1131153"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros difficilis",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825415,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210311'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='granulatus'>granulatus</element> <authors><author id='1465' title='Mitten, W.'>Mitt.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622830,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622830",
                        nameElement      : "granulatus",
                        nameId           : 210311,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210311",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117126"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros granulatus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825419,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210313'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='muelleri'>muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>CHAH (2010), <i>Australian Plant Census</i></citation></data>",
                        excluded         : false,
                        instanceId       : 772246,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/772246",
                        nameElement      : "muelleri",
                        nameId           : 210313,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210313",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld, NSW", "created_at": "2013-10-24T00:00:00+11:00", "created_by": "BLEPSCHI", "updated_at": "2013-10-24T11:42:28+11:00", "updated_by": "BLEPSCHI", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1131152"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros muelleri",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Dendroceros ferdinandi-muelleri": ["mis": false, "nom": false, "tax": false, "type": "orthographic variant", "cites": "Stephani, F. (1916), <i>Species Hepaticarum</i> 5", "name_id": 210312, "name_link": "http://localhost:7070/nsl-mapper/name/apni/210312", "full_name_html": "<scientific><name id='210312'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='ferdinandi-muelleri'>ferdinandi-muelleri</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825416,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210314'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='subtropicus'>subtropicus</element> <authors><author id='3814' title='Wild, C.J.'>C.J.Wild</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622837,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622837",
                        nameElement      : "subtropicus",
                        nameId           : 210314,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210314",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Qld", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117127"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros subtropicus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825417,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210315'><scientific><name id='129597'><element class='Dendroceros'>Dendroceros</element></name></scientific> <element class='wattsianus'>wattsianus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 622841,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/622841",
                        nameElement      : "wattsianus",
                        nameId           : 210315,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210315",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW", "created_at": "2011-01-27T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-27T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117128"]],
                        rank             : "Species",
                        simpleName       : "Dendroceros wattsianus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825420,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='124930'><element class='Megaceros'>Megaceros</element> <authors><author id='9964' title='Campbell, D.H.'>Campb.</author></authors></name></scientific><citation>Renzaglia, K.S., Villarreal, J.C. & Duff, R.J. in Goffinet, B. & Shaw, A.J. (ed.) (2009), New insights into morphology, anatomy and systematics of hornworts. <i>Bryophyte Biology</i> Edn. 2</citation></data>",
                        excluded         : false,
                        instanceId       : 8513196,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/8513196",
                        nameElement      : "Megaceros",
                        nameId           : 124930,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/124930",
                        previousElementId: null,
                        profile          : null,
                        rank             : "Genus",
                        simpleName       : "Megaceros",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825421,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='175653'><scientific><name id='124930'><element class='Megaceros'>Megaceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors>(<base id='1435' title='Stephani, F.'>Steph.</base>) <author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 624477,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/624477",
                        nameElement      : "carnosus",
                        nameId           : 175653,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/175653",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "NSW, Vic, Tas", "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117134"]],
                        rank             : "Species",
                        simpleName       : "Megaceros carnosus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : ["Anthoceros carnosus": ["mis": false, "nom": true, "tax": false, "type": "nomenclatural synonym", "cites": "Stephani, F. (1889), Hepaticae Australiae. <i>Hedwigia</i> 28", "name_id": 175654, "name_link": "http://localhost:7070/nsl-mapper/name/apni/175654", "full_name_html": "<scientific><name id='175654'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific>"]],
                        synonymsHtml     : "<synonyms><nom><scientific><name id='175654'><scientific><name id='121601'><element class='Anthoceros'>Anthoceros</element></name></scientific> <element class='carnosus'>carnosus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific> <type>nomenclatural synonym</type></nom></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ],
                [
                        id               : 9825422,
                        lockVersion      : 0,
                        displayHtml      : "<data><scientific><name id='210888'><scientific><name id='124930'><element class='Megaceros'>Megaceros</element></name></scientific> <element class='crassus'>crassus</element> <authors><author id='1435' title='Stephani, F.'>Steph.</author></authors></name></scientific><citation>McCarthy, P.M. (2003), <i>Catalogue of Australian Liverworts and Hornworts</i></citation></data>",
                        excluded         : false,
                        instanceId       : 624478,
                        instanceLink     : "http://localhost:7070/nsl-mapper/instance/apni/624478",
                        nameElement      : "crassus",
                        nameId           : 210888,
                        nameLink         : "http://localhost:7070/nsl-mapper/name/apni/210888",
                        previousElementId: null,
                        profile          : ["APC Dist.": ["value": "Tas", "created_at": "2011-01-31T00:00:00+11:00", "created_by": "KIRSTENC", "updated_at": "2011-01-31T00:00:00+11:00", "updated_by": "KIRSTENC", "source_link": "http://localhost:7070/nsl-mapper/instanceNote/apni/1117135"]],
                        rank             : "Species",
                        simpleName       : "Megaceros crassus",
                        sourceElementLink: null,
                        sourceShard      : "APNI",
                        synonyms         : null,
                        synonymsHtml     : "<synonyms></synonyms>",
                        updatedAt        : "2018-01-01 15:22:55.387530",
                        updatedBy        : "import"
                ]
        ]
    }
}
