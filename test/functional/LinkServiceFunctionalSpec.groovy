import au.org.biodiversity.nsl.LinkService
import au.org.biodiversity.nsl.Name
import grails.test.spock.IntegrationSpec
import spock.lang.Shared

/**
 * User: pmcneil
 * Date: 11/02/16
 *
 */
class LinkServiceFunctionalSpec extends IntegrationSpec {

    @Shared
    def grailsApplication

    @Shared
    def linkService

    def setup() {

    }
    def cleanup() {

    }

    void "get mapper identity"() {

        when: 'I call the mapper with a URI that exists'
        def identity = linkService.getMapperIdentityForLink('http://localhost:7070/nsl-mapper/name/apni/92690')
        println "identity is $identity"

        then: 'identity should be found and match'
        identity instanceof Map
        identity.nameSpace == 'apni'
        identity.objectType == 'name'
        identity.idNumber == 92690

        when: 'We call the mapper for a URI that doesnt exists'
        identity = linkService.getMapperIdentityForLink('http://localhost:7070/nsl-mapper/name/apni/false')
        println "identity is $identity"

        then: 'identity should be null'
        identity == null
    }

    void "get an object for a uri"() {
        when: 'you ask the linkservice for the object identified by a uri'
        def object = linkService.getObjectForLink('http://localhost:7070/nsl-mapper/name/apni/92690')
        println object

        then: 'you get back a Name object'
        object instanceof Name
        object.fullName == 'Hakea ceratophylla var. tricuspis Meisn.'

    }
}
