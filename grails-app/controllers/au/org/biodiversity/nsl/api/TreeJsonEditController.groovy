package au.org.biodiversity.nsl.api
import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authz.annotation.RequiresRoles
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * Created by ibis on 14/01/2016.
 */

@RequiresRoles('treebuilder')

class TreeJsonEditController {
    GrailsApplication grailsApplication

    def test() {
        def msg =  [ msg: 'TreeJsonEditController' ]
        render msg as JSON
    }


    def createWorkspace() {
        def msg = [
                [msg: 'ok', body: 'workspace created',  status: 'success'],
                [msg: 'DEV', body: 'well, actualy we din\'t create it, because I haven\'t written that stuff yet', status: 'warning'],
                [msg: 'TS', body: new Date(), status: 'info'],
        ];

        def result = [
                success: true,
                msg    : msg,
                created: [foo: 'here is a thing we created']
        ];

        render result as JSON
    }
}
