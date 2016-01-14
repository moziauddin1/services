package au.org.biodiversity.nsl.api
import grails.converters.JSON
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Created by ibis on 14/01/2016.
 */
class TreeJsonEditController {
    GrailsApplication grailsApplication

    def test() {
        def msg =  [ msg: 'TreeJsonEditController' ]
        render msg as JSON
    }

}
