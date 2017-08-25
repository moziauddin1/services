package au.org.biodiversity.nsl

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(TreeService)
@Mock([NameRank, NameGroup])
class TreeServiceUnitSpec extends Specification {

    def setup() {
        TestUte.setUpNameGroups()
        TestUte.setUpNameRanks()
    }

    def cleanup() {
    }

    void "test filter synonyms"() {

        when: 'I filter a synonyms map'
        Set<String> result = service.filterSynonyms(new TaxonData(synonyms: synonyms))
        println synonyms
        println result

        then: 'I get the result'
        result != null
        names.empty ? result.empty : result.containsAll(names)

        where:
        names                                                                                                                                                      | synonyms
        ['Calandrinia sp. Murchison-Gascoyne (F.Obbens & F.Hort FO 49/04)']                                                                                        | ['Calandrinia sp. Murchison-Gascoyne (F.Obbens & F.Hort FO 49/04)': ['type': 'taxonomic synonym', 'name_id': 215806]]
        []                                                                                                                                                         | ['Deyeuxia setifolia': ['type': 'misapplied', 'name_id': 99882]]
        []                                                                                                                                                         | ['Pellaea falcata var. nana': ['type': 'misapplied', 'name_id': 115321]]
        ['Sida sp. Wiluna (A.Markey & S.Dillon 4126)']                                                                                                             | ['Sida sp. Wiluna (A.Markey & S.Dillon 4126)': ['type': 'taxonomic synonym', 'name_id': 236527]]
        ['Gossypium taitense', 'Gossypium punctatum', 'Gossypium hirsutum var. hirsutum', 'Gossypium hirsutum var. taitense', 'Gossypium hirsutum var. punctatum'] | ['Gossypium taitense': ['type': 'taxonomic synonym', 'name_id': 244516], 'Gossypium punctatum': ['type': 'taxonomic synonym', 'name_id': 244514], 'Gossypium hirsutum var. hirsutum': ['type': 'nomenclatural synonym', 'name_id': 106461], 'Gossypium hirsutum var. taitense': ['type': 'taxonomic synonym', 'name_id': 244517], 'Gossypium hirsutum var. punctatum': ['type': 'taxonomic synonym', 'name_id': 3950649]]
        ['Amoora spectabilis']                                                                                                                                     | ['Amoora spectabilis': ['type': 'basionym', 'name_id': 119129]]
        ['Kentia ramsayi', 'Gulubia ramsayi', 'Gronophyllum ramsayi']                                                                                              | ['Kentia ramsayi': ['type': 'nomenclatural synonym', 'name_id': 71489], 'Gulubia ramsayi': ['type': 'basionym', 'name_id': 56463], 'Gronophyllum ramsayi': ['type': 'nomenclatural synonym', 'name_id': 79642]]
        ['Amperea protensa var. genuina', 'Amperea protensa var. protensa', 'Amperea protensa var. tenuiramea']                                                    | ['Amperea protensa var. genuina': ['type': 'taxonomic synonym', 'name_id': 86321], 'Amperea protensa var. protensa': ['type': 'nomenclatural synonym', 'name_id': 86323], 'Amperea protensa var. tenuiramea': ['type': 'taxonomic synonym', 'name_id': 86326]]
        ['White Speargrass']                                                                                                                                       | ['White Speargrass': ['type': 'common name', 'name_id': 440044]]
        ['Zieria sp. G', 'Zieria buxijugum MS', 'Zieria buxijugum (Paris 9079)', 'Zieria sp. 14 (sp. \'P\'; Box Range North)']                                     | ['Zieria sp. G': ['type': 'taxonomic synonym', 'name_id': 189814], 'Zieria buxijugum MS': ['type': 'nomenclatural synonym', 'name_id': 194919], 'Zieria buxijugum (Paris 9079)': ['type': 'taxonomic synonym', 'name_id': 149417], 'Zieria sp. 14 (sp. \'P\'; Box Range North)': ['type': 'taxonomic synonym', 'name_id': 194610]]
        ['Gratiola pumila', 'Gratiola sexdentata', 'Gratiola peruviana var. pumila', 'Gratiola peruviana var. pumilo']                                             | ['Gratiola pumila': ['type': 'orthographic variant', 'name_id': 235185], 'Gratiola sexdentata': ['type': 'doubtful taxonomic synonym', 'name_id': 60451], 'Gratiola peruviana var. pumila': ['type': 'nomenclatural synonym', 'name_id': 232932], 'Gratiola peruviana var. pumilo': ['type': 'nomenclatural synonym', 'name_id': 60378]]
    }

    void "test check polynomial below parent"() {
        when: 'I check a good placement'
        NameRank species = NameRank.findByName('Species')
        String[] parentNameElements = ['Plantae', 'Charophyta', 'Equisetopsida', 'Polypodiidae', 'Polypodiales', 'Blechnaceae', 'Doodia']
        service.checkPolynomialsBelowNameParent('Doodia aspera', false, species, parentNameElements)

        then: 'it works'
        notThrown(BadArgumentsException)

        when: 'I check a bad placement'
        parentNameElements = ['Plantae', 'Charophyta', 'Equisetopsida', 'Polypodiidae', 'Polypodiales', 'Blechnaceae', 'Blechnum']
        service.checkPolynomialsBelowNameParent('Doodia aspera', false, species, parentNameElements)

        then: 'it throws a bad argument exception'
        def e = thrown(BadArgumentsException)
        println e.message

        when: 'I check a hybrid name placement placed under the first parent'
        parentNameElements = ['Plantae', 'Charophyta', 'Equisetopsida', 'Polypodiidae', 'Polypodiales', 'Blechnaceae', 'Blechnum']
        service.checkPolynomialsBelowNameParent('Blechnum cartilagineum Sw. x Doodia media R.Br. ', false, species, parentNameElements)

        then: 'it works'
        notThrown(BadArgumentsException)

        when: 'I check a hybrid name placement placed under the second parent'
        parentNameElements = ['Plantae', 'Charophyta', 'Equisetopsida', 'Polypodiidae', 'Polypodiales', 'Blechnaceae', 'Doodia']
        service.checkPolynomialsBelowNameParent('Blechnum cartilagineum Sw. x Doodia media R.Br. ', false, species, parentNameElements)

        then: 'it throws a bad argument exception'
        thrown(BadArgumentsException)
    }

    void "test finding the rank of an element"() {
        when: 'I get the rank from the rank path for Blechnum'
        Map rankPath = [
                "Ordo"      : ["id": 223583, "name": "Polypodiales"],
                "Genus"     : ["id": 56340, "name": "Blechnum"],
                "Regnum"    : ["id": 54717, "name": "Plantae"],
                "Classis"   : ["id": 223519, "name": "Equisetopsida"],
                "Familia"   : ["id": 222592, "name": "Blechnaceae"],
                "Division"  : ["id": 224706, "name": "Charophyta"],
                "Subclassis": ["id": 224852, "name": "Polypodiidae"]
        ]
        NameRank rank = service.rankOfElement(rankPath, 'Blechnum')

        then: 'I get Genus'
        rank
        rank.name == 'Genus'

        when: 'I get the ranks of Doodia aspera from rankPath'
        rank = null
        rankPath = [
                "Ordo"      : ["id": 223583, "name": "Polypodiales"],
                "Genus"     : ["id": 70914, "name": "Doodia"],
                "Regnum"    : ["id": 54717, "name": "Plantae"],
                "Classis"   : ["id": 223519, "name": "Equisetopsida"],
                "Familia"   : ["id": 222592, "name": "Blechnaceae"],
                "Species"   : ["id": 70944, "name": "aspera"],
                "Division"  : ["id": 224706, "name": "Charophyta"],
                "Subclassis": ["id": 224852, "name": "Polypodiidae"]
        ]
        rank = service.rankOfElement(rankPath, 'aspera')

        then: 'I get Species'
        rank
        rank.name == 'Species'
    }
}
