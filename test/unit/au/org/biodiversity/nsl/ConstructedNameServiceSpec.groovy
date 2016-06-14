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
@Mock([NameGroup, NameRank, Name])
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
        Name name = new Name(simpleName: test, nameRank: NameRank.findByName(rank))
        String output = service.makeSortName(name, name.simpleName)

        then:
        output == result

        where:
        test                                          | rank         | result
        "Ptilotus"                                    | 'Genus'      | "ptilotus"
        "Ptilotus sect. Ptilotus"                     | 'Sectio'     | "ptilotus ptilotus"
        "Ptilotus ser. Ptilotus"                      | 'Series'     | "ptilotus ptilotus"
        "Ptilotus aervoides"                          | 'Species'    | "ptilotus aervoides"
        "Ptilotus albidus"                            | 'Species'    | "ptilotus albidus"
        "Ptilotus alexandri"                          | 'Species'    | "ptilotus alexandri"
        "Ptilotus alopecuroides"                      | 'Species'    | "ptilotus alopecuroides"
        "Ptilotus alopecuroideus"                     | 'Species'    | "ptilotus alopecuroideus"
        "Ptilotus alopecuroideus f. alopecuroideus"   | 'Forma'      | "ptilotus alopecuroideus alopecuroideus"
        "Ptilotus alopecuroideus var. alopecuroideus" | 'Varietas'   | "ptilotus alopecuroideus alopecuroideus"
        "Ptilotus alopecuroideus f. rubriflorus"      | 'Forma'      | "ptilotus alopecuroideus rubriflorus"
        "Ptilotus alopecuroideus var. longistachyus"  | 'Varietas'   | "ptilotus alopecuroideus longistachyus"
        "Ptilotus alopecuroideus var. rubriflorum"    | 'Varietas'   | "ptilotus alopecuroideus rubriflorum"
        "Ptilotus alopecuroideus var. rubriflorus"    | 'Varietas'   | "ptilotus alopecuroideus rubriflorus"
        "Ptilotus amabilis"                           | 'Species'    | "ptilotus amabilis"
        "Ptilotus andersonii"                         | 'Species'    | "ptilotus andersonii"
        "Ptilotus aphyllus"                           | 'Species'    | "ptilotus aphyllus"
        "Ptilotus appendiculatus"                     | 'Species'    | "ptilotus appendiculatus"
        "Ptilotus appendiculatus var. appendiculatus" | 'Varietas'   | "ptilotus appendiculatus appendiculatus"
        "Ptilotus appendiculatus var. minor"          | 'Varietas'   | "ptilotus appendiculatus minor"
        "Ptilotus aristatus"                          | 'Species'    | "ptilotus aristatus"
        "Ptilotus aristatus subsp. aristatus"         | 'Subspecies' | "ptilotus aristatus aristatus"
        "Ptilotus aristatus var. aristatus"           | 'Varietas'   | "ptilotus aristatus aristatus"
        "Ptilotus aristatus subsp. micranthus"        | 'Subspecies' | "ptilotus aristatus micranthus"
        "Ptilotus aristatus var. eichlerianus"        | 'Varietas'   | "ptilotus aristatus eichlerianus"
        "Ptilotus aristatus var. exilis"              | 'Varietas'   | "ptilotus aristatus exilis"
        "Ptilotus aristatus var. stenophyllus"        | 'Varietas'   | "ptilotus aristatus stenophyllus"


    }
}
