package au.org.biodiversity.nsl

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([NameGroup, NameRank])
@TestFor(ConstructedNameService)
class ConstructedNameServiceSpec extends Specification {

    def setup() {
        //create basic nameRanks
        List<Map> data = [
                [abbrev: 'reg.', name: 'Regnum', sortOrder: 10, descriptionHtml: '[description of <b>Regnum</b>]', rdfId: 'regnum'],
                [abbrev: 'div.', name: 'Division', sortOrder: 20, descriptionHtml: '[description of <b>Division</b>]', rdfId: 'division'],
                [abbrev: 'cl.', name: 'Classis', sortOrder: 30, descriptionHtml: '[description of <b>Classis</b>]', rdfId: 'classis'],
                [abbrev: 'subcl.', name: 'Subclassis', sortOrder: 40, descriptionHtml: '[description of <b>Subclassis</b>]', rdfId: 'subclassis'],
                [abbrev: 'superordo', name: 'Superordo', sortOrder: 50, descriptionHtml: '[description of <b>Superordo</b>]', rdfId: 'superordo'],
                [abbrev: 'ordo', name: 'Ordo', sortOrder: 60, descriptionHtml: '[description of <b>Ordo</b>]', rdfId: 'ordo'],
                [abbrev: 'subordo', name: 'Subordo', sortOrder: 70, descriptionHtml: '[description of <b>Subordo</b>]', rdfId: 'subordo'],
                [abbrev: 'fam.', name: 'Familia', sortOrder: 80, descriptionHtml: '[description of <b>Familia</b>]', rdfId: 'familia'],
                [abbrev: 'subfam.', name: 'Subfamilia', sortOrder: 90, descriptionHtml: '[description of <b>Subfamilia</b>]', rdfId: 'subfamilia'],
                [abbrev: 'trib.', name: 'Tribus', sortOrder: 100, descriptionHtml: '[description of <b>Tribus</b>]', rdfId: 'tribus'],
                [abbrev: 'subtrib.', name: 'Subtribus', sortOrder: 110, descriptionHtml: '[description of <b>Subtribus</b>]', rdfId: 'subtribus'],
                [abbrev: 'gen.', name: 'Genus', sortOrder: 120, descriptionHtml: '[description of <b>Genus</b>]', rdfId: 'genus'],
                [abbrev: 'subg.', name: 'Subgenus', sortOrder: 130, descriptionHtml: '[description of <b>Subgenus</b>]', rdfId: 'subgenus'],
                [abbrev: 'sect.', name: 'Sectio', sortOrder: 140, descriptionHtml: '[description of <b>Sectio</b>]', rdfId: 'sectio'],
                [abbrev: 'subsect.', name: 'Subsectio', sortOrder: 150, descriptionHtml: '[description of <b>Subsectio</b>]', rdfId: 'subsectio'],
                [abbrev: 'ser.', name: 'Series', sortOrder: 160, descriptionHtml: '[description of <b>Series</b>]', rdfId: 'series'],
                [abbrev: 'subser.', name: 'Subseries', sortOrder: 170, descriptionHtml: '[description of <b>Subseries</b>]', rdfId: 'subseries'],
                [abbrev: 'supersp.', name: 'Superspecies', sortOrder: 180, descriptionHtml: '[description of <b>Superspecies</b>]', rdfId: 'superspecies'],
                [abbrev: 'sp.', name: 'Species', sortOrder: 190, descriptionHtml: '[description of <b>Species</b>]', rdfId: 'species'],
                [abbrev: 'subsp.', name: 'Subspecies', sortOrder: 200, descriptionHtml: '[description of <b>Subspecies</b>]', rdfId: 'subspecies'],
                [abbrev: 'nothovar.', name: 'Nothovarietas', sortOrder: 210, descriptionHtml: '[description of <b>Nothovarietas</b>]', rdfId: 'nothovarietas'],
                [abbrev: 'var.', name: 'Varietas', sortOrder: 210, descriptionHtml: '[description of <b>Varietas</b>]', rdfId: 'varietas'],
                [abbrev: 'subvar.', name: 'Subvarietas', sortOrder: 220, descriptionHtml: '[description of <b>Subvarietas</b>]', rdfId: 'subvarietas'],
                [abbrev: 'f.', name: 'Forma', sortOrder: 230, descriptionHtml: '[description of <b>Forma</b>]', rdfId: 'forma'],
                [abbrev: 'subf.', name: 'Subforma', sortOrder: 240, descriptionHtml: '[description of <b>Subforma</b>]', rdfId: 'subforma'],
                [abbrev: 'form taxon', name: 'form taxon', sortOrder: 250, descriptionHtml: '[description of <b>form taxon</b>]', rdfId: 'form-taxon'],
                [abbrev: 'morph.', name: 'morphological var.', sortOrder: 260, descriptionHtml: '[description of <b>morphological var.</b>]', rdfId: 'morphological-var'],
                [abbrev: 'nothomorph', name: 'nothomorph.', sortOrder: 270, descriptionHtml: '[description of <b>nothomorph.</b>]', rdfId: 'nothomorph'],
                [abbrev: '[unranked]', name: '[unranked]', sortOrder: 500, descriptionHtml: '[description of <b>[unranked]</b>]', rdfId: 'unranked'],
                [abbrev: '[infrafamily]', name: '[infrafamily]', sortOrder: 500, descriptionHtml: '[description of <b>[infrafamily]</b>]', rdfId: 'infrafamily'],
                [abbrev: '[infragenus]', name: '[infragenus]', sortOrder: 500, descriptionHtml: '[description of <b>[infragenus]</b>]', rdfId: 'infragenus'],
                [abbrev: '[infrasp.]', name: '[infraspecies]', sortOrder: 500, descriptionHtml: '[description of <b>[infraspecies]</b>]', rdfId: 'infraspecies'],
                [abbrev: '[unknown]', name: '[unknown]', sortOrder: 500, descriptionHtml: '[description of <b>[unknown]</b>]', rdfId: 'unknown'],
                [abbrev: 'regio', name: 'Regio', sortOrder: 8, descriptionHtml: '[description of <b>Regio</b>]', rdfId: 'regio'],
                [abbrev: '[n/a]', name: '[n/a]', sortOrder: 500, descriptionHtml: '[description of <b>[n/a]</b>]', rdfId: 'n-a']
        ]
        NameGroup group = new NameGroup([name: 'group'])
        data.each { Map d ->
            NameRank r = new NameRank(d)
            r.nameGroup = group
            r.save()
        }
    }

    def cleanup() {
    }

    void "test makeSortName returns expected results"() {
        when: "we convert a simple name string"
        String output = service.makeSortName(test)

        then:
        output == result

        where:
        test                                          | result
        "Ptilotus"                                    | "ptilotus"
        "Ptilotus sect. Ptilotus"                     | "ptilotus O ptilotus"
        "Ptilotus ser. Ptilotus"                      | "ptilotus Q ptilotus"
        "Ptilotus aervoides"                          | "ptilotus aervoides"
        "Ptilotus albidus"                            | "ptilotus albidus"
        "Ptilotus alexandri"                          | "ptilotus alexandri"
        "Ptilotus alopecuroides"                      | "ptilotus alopecuroides"
        "Ptilotus alopecuroideus"                     | "ptilotus alopecuroideus"
        "Ptilotus alopecuroideus f. alopecuroideus"   | "ptilotus alopecuroideus Y alopecuroideus"
        "Ptilotus alopecuroideus var. alopecuroideus" | "ptilotus alopecuroideus W alopecuroideus"
        "Ptilotus alopecuroideus f. rubriflorus"      | "ptilotus alopecuroideus Y rubriflorus"
        "Ptilotus alopecuroideus var. longistachyus"  | "ptilotus alopecuroideus W longistachyus"
        "Ptilotus alopecuroideus var. rubriflorum"    | "ptilotus alopecuroideus W rubriflorum"
        "Ptilotus alopecuroideus var. rubriflorus"    | "ptilotus alopecuroideus W rubriflorus"
        "Ptilotus amabilis"                           | "ptilotus amabilis"
        "Ptilotus andersonii"                         | "ptilotus andersonii"
        "Ptilotus aphyllus"                           | "ptilotus aphyllus"
        "Ptilotus appendiculatus"                     | "ptilotus appendiculatus"
        "Ptilotus appendiculatus var. appendiculatus" | "ptilotus appendiculatus W appendiculatus"
        "Ptilotus appendiculatus var. minor"          | "ptilotus appendiculatus W minor"
        "Ptilotus aristatus"                          | "ptilotus aristatus"
        "Ptilotus aristatus subsp. aristatus"         | "ptilotus aristatus U aristatus"
        "Ptilotus aristatus var. aristatus"           | "ptilotus aristatus W aristatus"
        "Ptilotus aristatus subsp. micranthus"        | "ptilotus aristatus U micranthus"
        "Ptilotus aristatus var. eichlerianus"        | "ptilotus aristatus W eichlerianus"
        "Ptilotus aristatus var. exilis"              | "ptilotus aristatus W exilis"
        "Ptilotus aristatus var. stenophyllus"        | "ptilotus aristatus W stenophyllus"


    }
}
