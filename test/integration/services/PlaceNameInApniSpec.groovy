package services

import au.org.biodiversity.nsl.ClassificationService
import au.org.biodiversity.nsl.ConstructedNameService
import au.org.biodiversity.nsl.Name
import spock.lang.Specification

class PlaceNameInApniSpec extends Specification {
    ClassificationService classificationService
    ConstructedNameService constructedNameService
    def rulesEngineService
    def jsonRendererService

    def setup() {
        jsonRendererService.registerObjectMashallers()
    }

    def cleanup() {
    }


    def "test one name is where I expect it"() {
        /*
         * This test may fail if the underlying science changes ... but I can't see how it could
         */

        when: "Find sample names"
        Name doodia = Name.findByFullName('Doodia R.Br.')
        Name doodiaAspera = Name.findByFullName('Doodia aspera R.Br.')
        Name doodiaAsperaVarMedia = Name.findByFullName('Doodia aspera var. media F.M.Bailey')

        then: "names exist"
        doodia
        doodia.fullName == 'Doodia R.Br.'
        doodiaAspera
        doodiaAspera.fullName == 'Doodia aspera R.Br.'
        doodiaAsperaVarMedia
        doodiaAsperaVarMedia.fullName == 'Doodia aspera var. media F.M.Bailey'

        when: "get path"
        List<Name> currentLocation = classificationService.getPath(doodiaAsperaVarMedia)

        then: "path looks like I expect"
        currentLocation.size() >= 1
        currentLocation.get(currentLocation.size() - 1) == doodiaAsperaVarMedia
        currentLocation.size() >= 2
        currentLocation.get(currentLocation.size() - 2) == doodiaAspera
        currentLocation.size() >= 3
        currentLocation.get(currentLocation.size() - 3) == doodia

    }

    def "test place a name and subname in APNI"() {
        when: "Find sample names"

        Name doodiaMaxima = Name.findByFullName('Doodia maxima J.Sm.')
        Name doodiaAspera = Name.findByFullName('Doodia aspera R.Br.')
        Name doodiaAsperaVarMedia = Name.findByFullName('Doodia aspera var. media F.M.Bailey')

        then: "names exist"
        doodiaMaxima
        doodiaMaxima.fullName == 'Doodia maxima J.Sm.'
        doodiaAspera
        doodiaAspera.fullName == 'Doodia aspera R.Br.'
        doodiaAsperaVarMedia
        doodiaAsperaVarMedia.fullName == 'Doodia aspera var. media F.M.Bailey'

        when: "find where D. aspera var media currently is"
        List<Name> currentLocation = classificationService.getPath(doodiaAsperaVarMedia)

        then: "check D. aspera var media is currently under D. aspera"
        currentLocation.size() >= 2
        currentLocation.get(currentLocation.size() - 2).equals(doodiaAspera)

        when: "put D. aspera var media under D. maxima"
        classificationService.placeNameInAPNI(doodiaMaxima, doodiaAsperaVarMedia)
        currentLocation = classificationService.getPath(doodiaAsperaVarMedia)

        then: "check D. aspera var media is currently under D. maxima"
        currentLocation.size() >= 2
        currentLocation.get(currentLocation.size() - 2).equals(doodiaMaxima)
    }

    def "test place name and subname in APNI using controller"() {
        when:
        Name doodiaMaxima = Name.findByFullName('Doodia maxima J.Sm.')
        Name doodiaAspera = Name.findByFullName('Doodia aspera R.Br.')
        Name doodiaAsperaVarMedia = Name.findByFullName('Doodia aspera var. media F.M.Bailey')

        def controller = new au.org.biodiversity.nsl.api.NameController()

        controller.request.contentType = "application/json"
        controller.request.content = "{ nameId:${doodiaAsperaVarMedia.id}, supernameId: ${doodiaMaxima.id} }".bytes
        controller.request.method = 'POST'
        controller.placeNameInApni()
        List<Name> currentLocation = classificationService.getPath(doodiaAsperaVarMedia)

        then:
        currentLocation.size() >= 2
        currentLocation.get(currentLocation.size() - 2).equals(doodiaMaxima)
    }

    def "test place name at APNI root using controller"() {
        when:
        Name doodiaMaxima = Name.findByFullName('Doodia maxima J.Sm.')
        Name doodiaAspera = Name.findByFullName('Doodia aspera R.Br.')
        Name doodiaAsperaVarMedia = Name.findByFullName('Doodia aspera var. media F.M.Bailey')

        def controller = new au.org.biodiversity.nsl.api.NameController()

        controller.request.contentType = "application/json"
        controller.request.content = "{ nameId:${doodiaAsperaVarMedia.id} }".bytes
        controller.request.method = 'POST'
        controller.placeNameInApni()
        List<Name> currentLocation = classificationService.getPath(doodiaAsperaVarMedia)

        then:
        currentLocation.size() == 2
        currentLocation.get(0) == null
        currentLocation.get(1).equals(doodiaAsperaVarMedia)
    }

    def "Test calling Rules Engine"() {
        when:
        Map grailsApplication = [config: [
                nslServices: [rulesEngine: [uri: 'http://155.187.10.62:1521/rulesEngine/']],
                grails     : [serverURL: 'http://localhost/test']
        ]]
        rulesEngineService.grailsApplication = grailsApplication
        jsonRendererService.grailsApplication = grailsApplication

        Name name = Name.findByFullName('Doodia maxima J.Sm.')
        Name supername = Name.findByFullName('Doodia aspera R.Br.')
        Map facts = [supername: supername, name: name]
        facts = rulesEngineService.ask('TreePlacement', facts)

        then:
        facts.containsKey('accept')
        facts.accept == false

        when:
        facts.supername = Name.findByFullName('Doodia R.Br.')
        facts = rulesEngineService.ask('TreePlacement', facts)

        then:
        facts.containsKey('accept')
        facts.accept == true

    }


}
