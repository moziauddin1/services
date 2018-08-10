package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import grails.validation.ValidationException
import org.hibernate.engine.spi.Status
import spock.lang.Specification
import spock.lang.Unroll

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
        service.eventService = Mock(EventService)
        service.treeReportService = new TreeReportService()
        service.treeReportService.transactionManager = getTransactionManager()
        service.treeReportService.dataSource_nsl = dataSource_nsl
        service.linkService.getPreferredHost() >> 'http://localhost:7070/nsl-mapper'
        service.eventService.createDraftTreeEvent(_, _) >> { data, user ->
            return new EventRecord(data: data, dealtWith: false, updatedBy: user, createdBy: user)
        }
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
        tree2.hostName == 'http://localhost:7070/nsl-mapper'
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
        TreeVersion version = service.createTreeVersion(tree, null, 'my first draft', 'irma', 'This is a log entry')

        then: 'A new version is created on that tree'
        version
        version.tree == tree
        version.draftName == 'my first draft'
        tree.treeVersions.contains(version)

        when: 'I add some test elements to the version'
        List<TreeElement> testElements = TreeTstHelper.makeTestElements(version, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        println version.treeVersionElements

        then: 'It should have 30 tree elements'
        testElements.size() == 30
        version.treeVersionElements.size() == 30
        version.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[3], version))
        version.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[13], version))
        version.treeVersionElements.contains(TreeVersionElement.findByTreeElementAndTreeVersion(testElements[23], version))

        when: 'I make a new version from this version'
        TreeVersion version2 = service.createTreeVersion(tree, version, 'my second draft', 'irma', 'This is a log entry')
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')

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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft', 'irma', 'This is a log entry')

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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft', 'irma', 'This is a log entry')

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

    def "test getting synonyms from instance"() {
        given:
        Tree tree = Tree.findByName('APC')
        TreeVersion treeVersion = tree.currentTreeVersion
        Instance ficusVirens = Instance.get(781547)
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

        expect:
        tree
        treeVersion
        ficusVirens

        when: 'I get element data for ficus virens'
        TaxonData taxonData = service.elementDataFromInstance(ficusVirens)
        println taxonData.synonymsHtml
        println taxonData.synonyms.asMap()

        then: 'I get 20 synonyms'
        taxonData.synonyms.size() == 20
        taxonData.synonymsHtml.startsWith('<synonyms><tax><scientific><name data-id=\'90571\'><scientific><name data-id=\'73030\'>')
    }

    def "test check synonyms"() {
        given:
        Tree tree = Tree.findByName('APC')
        TreeVersion treeVersion = tree.currentTreeVersion
        Instance ficusVirensSublanceolata = Instance.get(692695)
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

        expect:
        tree
        treeVersion
        ficusVirensSublanceolata

        when: 'I try to place Ficus virens var. sublanceolata sensu Jacobs & Packard (1981)'
        TaxonData taxonData = service.elementDataFromInstance(ficusVirensSublanceolata)
        List<Long> nameIdList = taxonData.synonyms.filtered().collect { it.nameId } + [taxonData.nameId]
        List<Map> existingSynonyms = service.checkNameIdsAgainstAllSynonyms(nameIdList, treeVersion, [])
        println existingSynonyms

        then: 'I get two found synonyms'
        existingSynonyms != null
        !existingSynonyms.empty
        existingSynonyms.size() == 2
        existingSynonyms.first().simpleName == 'Ficus virens'
    }

    def "test check synonyms of this taxon not on tree"() {
        given:
        Tree tree = Tree.findByName('APC')
        TreeVersion treeVersion = tree.currentTreeVersion
        Instance xanthosiaPusillaBunge = Instance.get(712692)
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

        expect:
        tree
        treeVersion
        xanthosiaPusillaBunge

        when: 'I try to place Xanthosia pusilla Bunge'
        TaxonData taxonData = service.elementDataFromInstance(xanthosiaPusillaBunge)
        service.checkSynonymsOfNameNotOnTheTree(taxonData, treeVersion, null)

        then: 'I get en error'
        def e = thrown(BadArgumentsException)
        println e.message
        e.message.startsWith("Can’t place this concept - synonym")
        e.message.contains("Xanthosia")
        e.message.contains("tasmanica")
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion,
                [TreeTstHelper.blechnaceaeElementData,
                 TreeTstHelper.doodiaElementData,
                 TreeTstHelper.asperaElementData],
                [TreeTstHelper.blechnaceaeTVEData,
                 TreeTstHelper.doodiaTVEData,
                 TreeTstHelper.asperaTVEData])
        Instance asperaInstance = Instance.get(781104)
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement asperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

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
        e.message.startsWith("Can’t place this concept")
        e.message.contains("Doodia")
        e.message.contains("aspera")

    }

    def "test check validation, ranked above parent"() {
        given:
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, [TreeTstHelper.blechnaceaeElementData, TreeTstHelper.asperaElementData],
                [TreeTstHelper.blechnaceaeTVEData, TreeTstHelper.asperaTVEData])
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, [TreeTstHelper.blechnaceaeElementData, TreeTstHelper.asperaElementData],
                [TreeTstHelper.blechnaceaeTVEData, TreeTstHelper.asperaTVEData])
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, [TreeTstHelper.blechnaceaeElementData, TreeTstHelper.doodiaElementData, TreeTstHelper.asperaElementData],
                [TreeTstHelper.blechnaceaeTVEData, TreeTstHelper.doodiaTVEData, TreeTstHelper.asperaTVEData])
        Instance asperaInstance = Instance.get(781104)
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement asperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

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
        e.message.startsWith("Can’t place this concept - <data><scientific><name data-id='70944'>")
    }

    def "test place taxon"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, [TreeTstHelper.blechnaceaeElementData, TreeTstHelper.doodiaElementData], [TreeTstHelper.blechnaceaeTVEData, TreeTstHelper.doodiaTVEData])
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft', 'irma', 'This is a log entry')

        Instance asperaInstance = Instance.get(781104)
        TreeVersionElement blechnaceaeElement = service.findElementBySimpleName('Blechnaceae', draftVersion)
        TreeVersionElement doodiaElement = service.findElementBySimpleName('Doodia', draftVersion)
        TreeVersionElement nullAsperaElement = service.findElementBySimpleName('Doodia aspera', draftVersion)
        String instanceUri = 'http://localhost:7070/nsl-mapper/instance/apni/781104'
        Long blechnaceaeTaxonId = blechnaceaeElement.taxonId
        Long doodiaTaxonId = doodiaElement.taxonId
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

        println TreeVersionElement.findAllByTaxonId(blechnaceaeElement.taxonId)
        printTve(blechnaceaeElement)

        expect:
        tree
        draftVersion
        blechnaceaeElement
        blechnaceaeTaxonId
        TreeVersionElement.findAllByTaxonId(blechnaceaeElement.taxonId).size() > 1
        TreeVersionElement.countByTaxonId(blechnaceaeElement.taxonId) > 1
        doodiaElement
        doodiaTaxonId
        asperaInstance
        !nullAsperaElement

        when: 'I try to place Doodia aspera under Doodia'
        Map result = service.placeTaxonUri(doodiaElement, instanceUri, false, null, 'A. User')
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
        result.childElement.taxonLink == "/taxon/apni/${result.childElement.taxonId}"
        //taxon id for the taxon above has changed to new IDs
        blechnaceaeElement.taxonId != 0
        blechnaceaeElement.taxonId != blechnaceaeTaxonId
        TreeVersionElement.countByTaxonId(blechnaceaeElement.taxonId) == 1
        blechnaceaeElement.taxonLink == "/taxon/apni/$blechnaceaeElement.taxonId"
        doodiaElement.taxonId != 0
        doodiaElement.taxonId != doodiaTaxonId
        TreeVersionElement.countByTaxonId(doodiaElement.taxonId) == 1
        doodiaElement.taxonLink == "/taxon/apni/$doodiaElement.taxonId"

        println result.childElement.elementLink
    }

    def "test replace a taxon"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft', 'irma', 'This is a log entry')
        List<TreeElement> testElements = TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft', 'irma', 'This is a log entry')
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
        service.treeReportService

        when: 'I try to move a taxon, anthoceros under dendrocerotaceae'
        Map result = service.replaceTaxon(anthocerosTve, dendrocerotaceaeTve,
                'http://localhost:7070/nsl-mapper/instance/apni/753948',
                anthocerosTve.treeElement.excluded,
                anthocerosTve.treeElement.profile,
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
                true,
                [:],
                'test move taxon')

        then: 'I get a PublishedVersionException'
        thrown(PublishedVersionException)
    }

    def "test replace a taxon with multiple child levels"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft', 'irma', 'This is a log entry')
        List<TreeElement> testElements = TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft', 'irma', 'This is a log entry')

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
                anthocerotalesTve.treeElement.excluded,
                anthocerotalesTve.treeElement.profile,
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

    def "test place taxon without a parent"() {
        given:
        service.linkService.bulkAddTargets(_) >> [success: true]
        Tree tree = makeATestTree()
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')

        Instance asperaInstance = Instance.get(781104)
        String instanceUri = 'http://localhost:7070/nsl-mapper/instance/apni/781104'
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }
        draftVersion.refresh()

        expect:
        tree
        draftVersion
        draftVersion.treeVersionElements.size() == 0
        asperaInstance

        when: 'I try to place Doodia aspera in the version without a parent'
        Map result = service.placeTaxonUri(draftVersion, instanceUri, false, null, 'A. User')
        println result

        then: 'It should work'
        1 * service.linkService.getObjectForLink(instanceUri) >> asperaInstance
        1 * service.linkService.addTargetLink(_) >> { TreeVersionElement tve -> "http://localhost:7070/nsl-mapper/tree/$tve.treeVersion.id/$tve.treeElement.id" }
        1 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }
        result.childElement == service.findElementBySimpleName('Doodia aspera', draftVersion)
        result.warnings.empty
        //taxon id should be set to a unique/new positive value
        result.childElement.taxonId != 0
        TreeVersionElement.countByTaxonId(result.childElement.taxonId) == 1
        result.childElement.taxonLink == "/taxon/apni/${result.childElement.taxonId}"
        println result.childElement.elementLink
    }

    def "test remove a taxon"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        service.linkService.bulkRemoveTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft', 'irma', 'This is a log entry')
        List<TreeElement> testElements = TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft', 'irma', 'This is a log entry')

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

        when: 'I try to remove a taxon'
        Map result = service.removeTreeVersionElement(anthoceros)

        then: 'It works'
        6 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }
        result.count == 6
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft', 'irma', 'This is a log entry')
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
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
        TreeVersion draftVersion = service.createDefaultDraftVersion(tree, null, 'my default draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        TreeVersion publishedVersion = service.publishTreeVersion(draftVersion, 'tester', 'publishing to delete')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my next draft', 'irma', 'This is a log entry')
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
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft', 'irma', 'This is a log entry')
        TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        TreeTstHelper.makeTestElements(draftVersion, [TreeTstHelper.doodiaElementData], [TreeTstHelper.doodiaTVEData]).first()
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft', 'irma', 'This is a log entry')

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

    def "test change a taxons parent"() {
        given:
        Tree tree = makeATestTree()
        service.linkService.bulkAddTargets(_) >> [success: true]
        TreeVersion draftVersion = service.createTreeVersion(tree, null, 'my first draft', 'irma', 'This is a log entry')
        List<TreeElement> testElements = TreeTstHelper.makeTestElements(draftVersion, TreeTstHelper.testElementData(), TreeTstHelper.testTreeVersionElementData())
        service.publishTreeVersion(draftVersion, 'testy mctestface', 'Publishing draft as a test')
        draftVersion = service.createDefaultDraftVersion(tree, null, 'my new default draft', 'irma', 'This is a log entry')
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
        service.treeReportService

        when: 'I try to move a taxon, anthoceros under dendrocerotaceae'
        Map result = service.changeParentTaxon(anthocerosTve, dendrocerotaceaeTve, 'test move taxon')
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
//        1 * service.linkService.bulkRemoveTargets(_) >> { List<TreeVersionElement> elements ->
//            [success: true]
//        }
        1 * service.linkService.getObjectForLink(_) >> replacementAnthocerosInstance
        1 * service.linkService.getPreferredLinkForObject(replacementAnthocerosInstance.name) >> 'http://localhost:7070/nsl-mapper/name/apni/121601'
        1 * service.linkService.getPreferredLinkForObject(replacementAnthocerosInstance) >> 'http://localhost:7070/nsl-mapper/instance/apni/753948'
        9 * service.linkService.addTaxonIdentifier(_) >> { TreeVersionElement tve ->
            println "Adding taxonIdentifier for $tve"
            "http://localhost:7070/nsl-mapper/taxon/apni/$tve.taxonId"
        }
        !deleted(anthocerosTve)// changing the parent simply changes the parent
        !deleted(anthocerosTe) // We are using the same tree element
        result.replacementElement
        result.replacementElement == service.findElementBySimpleName('Anthoceros', draftVersion)
        result.replacementElement.elementLink == anthocerosTve.elementLink //haven't changed tree version elements
        result.replacementElement.treeVersion == draftVersion
        result.replacementElement.treeElement == anthocerosTe
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
    }

    def "test find tree element"() {
        given:
        TreeElement doodiaDissecta = TreeElement.findBySimpleNameAndUpdatedBy('Doodia dissecta', 'import')
        service.linkService.getPreferredLinkForObject(_) >> {
            String url = "http://localhost:7070/nsl-mapper/${it[0].class.simpleName.toLowerCase()}/apni/${it[0].id}"
            println url
            return url
        }

        expect:
        doodiaDissecta

        when: 'I try finding the element using its data'
        Map edfe = service.comparators(doodiaDissecta)
        TreeElement found1 = service.findTreeElement(edfe)

        then:
        found1
        found1.id == doodiaDissecta.id

        when: 'I compare element data to taxon data it matches'
        TaxonData taxonData = service.elementDataFromInstance(doodiaDissecta.instance)
        taxonData.excluded = doodiaDissecta.excluded
        taxonData.profile = doodiaDissecta.profile
        Map edfi = service.comparators(taxonData)

        then:
        2 * service.linkService.getPreferredHost() >> 'http://localhost:7070/nsl-mapper'

        edfi.instanceId == edfe.instanceId
        edfi.nameId == edfe.nameId
        edfi.excluded == edfe.excluded
        edfi.simpleName == edfe.simpleName
        edfi.nameElement == edfe.nameElement
        edfi.sourceShard == edfe.sourceShard
        edfi.synonymsHtml == edfe.synonymsHtml
        edfi.profile == edfe.profile

        when: 'I get the TaxonData from the instance, it still works'
        TreeElement found2 = service.findTreeElement(taxonData)

        then:
        found2
        found2.id == doodiaDissecta.id
    }

    @Unroll("test repeat #i")
    def "test db synonymy against app synonymy"() {
        given:
        TreeElement physalisHederifolia = TreeElement.findBySimpleName('Physalis hederifolia')
        TreeElement abrotanellaScapigera = TreeElement.findBySimpleName('Abrotanella scapigera')
        TreeElement hibbertiaHirticalyx = TreeElement.findBySimpleName('Hibbertia hirticalyx')
        TreeElement cardamineLilacina = TreeElement.findBySimpleName('Cardamine lilacina')

        expect:
        physalisHederifolia
        abrotanellaScapigera
        hibbertiaHirticalyx
        cardamineLilacina

        when: "we generate the synonyms html for physalis"
        String physalisSynonymsDb = service.getSynonymsHtmlViaDBFunction(physalisHederifolia.instanceId)
        String physalisSynonymsHtml = service.getSynonyms(physalisHederifolia.instance).html()

        then: "we get them and they are equal"
        physalisSynonymsDb
        physalisSynonymsHtml
        physalisSynonymsDb == physalisSynonymsHtml

        when: "we generate the synonyms html for abrotanella"
        String abrotanellaSynonymsDb = service.getSynonymsHtmlViaDBFunction(abrotanellaScapigera.instanceId)
        String abrotanellaSynonymsHtml = service.getSynonyms(abrotanellaScapigera.instance).html()

        then: "we get them and they are equal"
        abrotanellaSynonymsDb
        abrotanellaSynonymsHtml
        abrotanellaSynonymsDb == abrotanellaSynonymsHtml

        when: "we generate the synonyms html for hibbertia"
        String hibbertiaSynonymsDb = service.getSynonymsHtmlViaDBFunction(hibbertiaHirticalyx.instanceId)
        String hibbertiaSynonymsHtml = service.getSynonyms(hibbertiaHirticalyx.instance).html()
        println hibbertiaSynonymsDb
        println hibbertiaSynonymsHtml

        then: "we get them and they are equal"
        hibbertiaSynonymsDb
        hibbertiaSynonymsHtml
        hibbertiaSynonymsDb == hibbertiaSynonymsHtml

        when: "we generate the synonyms html for cardamine"
        String cardamineSynonymsDb = service.getSynonymsHtmlViaDBFunction(cardamineLilacina.instanceId)
        String cardamineSynonymsHtml = service.getSynonyms(cardamineLilacina.instance).html()

        then: "we get them and they are equal"
        cardamineSynonymsDb
        cardamineSynonymsHtml
        cardamineSynonymsDb == cardamineSynonymsHtml

        where:

        i << (1..10)
    }

    static deleted(domainObject) {
        Name.withSession { session ->
            session.persistenceContext.getEntry(domainObject)?.status in [null, Status.DELETED]
        }
    }

    private Tree makeATestTree() {
        service.createNewTree('aTree', 'aGroup', null, '<p>A description</p>',
                'http://trees.org/aTree', false)
    }

    private Tree makeBTestTree() {
        service.createNewTree('bTree', 'aGroup', null, '<p>B description</p>',
                'http://trees.org/bTree', false)
    }

    private printTve(TreeVersionElement target) {
        println "*** Taxon $target.taxonId: $target.treeElement.name.simpleName Children ***"
        for (TreeVersionElement tve in service.getAllChildElements(target)) {
            tve.refresh()
            println "Taxon: $tve.taxonId, Names: $tve.namePath, Path: $tve.treePath"
        }
    }


}
