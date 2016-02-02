package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.tree.UserWorkspaceManagerService
import grails.converters.JSON
import grails.validation.Validateable
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authz.annotation.RequiresRoles
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * Created by ibis on 14/01/2016.
 */

@RequiresRoles('treebuilder')

class TreeJsonEditController {
    GrailsApplication grailsApplication
    UserWorkspaceManagerService userWorkspaceManagerService
    MessageSource messageSource

    def test() {
        def msg =  [ msg: 'TreeJsonEditController' ]
        render msg as JSON
    }


    def createWorkspace(CreateWorkspaceParam param) {
        if(!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { Error it -> [ msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)]}
            msg += param.errors.fieldErrors.collect { FieldError it -> [ msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)]}

            def result = [
                    success: false,
                    msg    : msg,
                    errors    : param.errors,
            ];
            return render(result as JSON)
        }

        String description = param.description ?: "Workspace ${SecurityUtils.subject.principal} ${new Date()}"

        def msg = [
                [msg: 'Created Workspace', body: description,  status: 'success'],
                [msg: 'DEV', body: 'well, actualy we didn\'t create it, because I haven\'t written that stuff yet', status: 'warning'],
                [msg: 'TS', body: new Date(), status: 'info'],
        ];

        def result = [
                success: true,
                msg    : msg,
                uri    : null
        ];

        render result as JSON
    }
}

@Validateable
class CreateWorkspaceParam {
    String namespace
    String description

    String toString() {
        return [
            namespace: namespace,
            description: description
        ].toString()
    }

    static constraints = {
        namespace nullable: false
        description nullable: true
    }
}
