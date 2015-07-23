package services

import au.org.biodiversity.nsl.*
import grails.converters.JSON
import spock.lang.Specification

import static org.springframework.http.HttpStatus.OK

/**
 *
 */
class ConstructedNameTestsSpec extends Specification {

    def constructedNameService

    def setup() {
    }

    def cleanup() {
    }

    void "Test makeFullName handles no name data"() {
        when: "no parameters are passed"
        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.contentType = "application/json"
        controller.request.content = ''.bytes
        controller.request.method = 'POST'
        controller.makeFullName()

        then: "? is returned"
        controller.response.status == OK.value
        controller.response.contentAsString == '{"fullMarkedUpName":"?","simpleMarkedUpName":"?","fullName":"?","simpleName":"?"}'
    }

    void "Test makeFullName generates a name using parent"() {
        when: "A JSON map with name parameters and a parent is supplied"
        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.method = 'POST'
        controller.request.contentType = "application/json"
        String json = (nameMap() << [parent: [id: Name.findByNameElement('Poa')?.id]] as JSON).toString(true)
        controller.request.content = json.bytes

        controller.makeFullName()

        println json
        println controller.response.text

        then: "A JSON response with the name values is returned"
        controller.response.status == OK.value
        controller.response.text.contains('"fullName":"Poa brownii Kunth","simpleName":"Poa brownii"')
    }

    void "Test makeFullName generates a name when parent is null"() {
        when: "A JSON map with name parameters and parent is null"

        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.method = 'POST'
        controller.request.contentType = "application/json"
        String json = (nameMap() << [parent: [id: null]] as JSON).toString(true)
        controller.request.content = json.bytes

        controller.makeFullName()

        println json
        println controller.response.text

        then: "A JSON response with the name values is returned"
        controller.response.status == OK.value
        controller.response.text.contains('"fullName":"brownii Kunth","simpleName":"brownii"')
    }

    void "Test makeFullName generates a name when author is null"() {
        when:

        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.method = 'POST'
        controller.request.contentType = "application/json"
        Map nameMap = new HashMap(nameMap())
        nameMap.Name.authorId = null;
        nameMap.parent = [id: null]
        String json = (nameMap as JSON).toString(true)
        controller.request.content = json.bytes

        controller.makeFullName()

        println json
        println controller.response.text

        then: "A JSON response with the name values is returned"
        controller.response.status == OK.value
        controller.response.text.contains('"fullName":"brownii","simpleName":"brownii"')
    }

    void "Test makeFullName generates a name with just name element"() {
        when:

        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.method = 'POST'
        controller.request.contentType = "application/json"
        Map nameMap = new HashMap(nameMap())
        nameMap.Name.authorId = null;
        nameMap.Name.nameRankId = null;
        nameMap.Name.nameStatusId = null;
        nameMap.Name.nameTypeId = null;
        nameMap.Name.namespaceId = null;
        nameMap.parent = [id: null]
        String json = (nameMap as JSON).toString(true)
        controller.request.content = json.bytes

        controller.makeFullName()

        println json
        println controller.response.text

        then: "A JSON response with the name values is returned"
        controller.response.status == OK.value
        controller.response.text.contains('"fullName":"brownii","simpleName":"brownii"')
    }

    void "Test makeFullName generates a scientific name with name element and type"() {
        when:

        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.method = 'POST'
        controller.request.contentType = "application/json"
        Map nameMap = new HashMap(nameMap())
        nameMap.Name.authorId = null;
        nameMap.Name.nameRankId = null;
        nameMap.Name.nameStatusId = null;
        nameMap.Name.namespaceId = null;
        nameMap.parent = [id: null]
        String json = (nameMap as JSON).toString(true)
        controller.request.content = json.bytes

        controller.makeFullName()

        println json
        println controller.response.text

        then: "A JSON response with the name values is returned"
        controller.response.status == OK.value
        controller.response.text.contains('"fullName":"brownii","simpleName":"brownii"')
    }

    void "Test makeFullName generates a name ? when all data is missing"() {
        when:

        def controller = new au.org.biodiversity.nsl.api.NameController()
        controller.request.method = 'POST'
        controller.request.contentType = "application/json"
        Map nameMap = new HashMap(nameMap())
        nameMap.Name.authorId = null
        nameMap.Name.nameRankId = null
        nameMap.Name.nameStatusId = null
        nameMap.Name.nameTypeId = null
        nameMap.Name.namespaceId = null
        nameMap.Name.nameElement = ''
        nameMap.parent = [id: null]
        String json = (nameMap as JSON).toString(true)
        controller.request.content = json.bytes

        controller.makeFullName()

        println json
        println controller.response.text

        then: "A JSON response with the name values is returned"
        controller.response.status == OK.value
        controller.response.text.contains('"fullName":"?","simpleName":"?"')
    }

    private static Map nameMap() {
        [
                Name: [
                        authorId      : Author.findByAbbrev('Kunth')?.id,
                        baseAuthorId  : null,
                        exAuthorId    : null,
                        exBaseAuthorId: null,
                        manuscript    : false,
                        nameElement   : "brownii",
                        nameRankId    : NameRank.findByAbbrev('sp.')?.id,
                        nameStatusId  : NameStatus.findByName('legitimate')?.id,
                        nameTypeId    : NameType.findByName('scientific')?.id,
                        namespaceId   : Namespace.findAllByName('APNI')?.id,
                        orthVar       : false
                ]
        ]
    }
}
