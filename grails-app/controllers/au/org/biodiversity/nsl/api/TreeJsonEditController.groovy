package au.org.biodiversity.nsl.api
import grails.converters.JSON
import org.apache.shiro.SecurityUtils
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


    def createWorkspace() {
        def msg = [
                [msg: 'ok', body: 'workspace created',  status: 'success'],
                [msg: 'TS', body: new Date(), status: 'info'],
        ];

        def result = [
            msg: msg,
            created: [ foo: 'here is a thing we created'],
            you: SecurityUtils.subject?.principal ?: 'guest'
        ];

        render result as JSON
    }
}
