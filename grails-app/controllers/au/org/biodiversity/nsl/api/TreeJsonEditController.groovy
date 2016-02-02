package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.Namespace
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
        def msg = [msg: 'TreeJsonEditController']
        render msg as JSON
    }


    def createWorkspace(CreateWorkspaceParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { Error it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];
            return render(result as JSON)
        }

        String title = param.title ?: "${SecurityUtils.subject.principal} ${new Date()}"
        Namespace ns = Namespace.findByName(param.namespace)
        // todo: use the grails parameter validation to do this
        if (!ns) {
            def msg = [];

            msg += [msg: 'Namespace not found', status: 'warning', body: "Namespace \"${param.namespace}\" not found"]

            def result = [
                    success: false,
                    msg    : msg
            ];
            return render(result as JSON)
        }

        Arrangement a = userWorkspaceManagerService.createWorkspace(ns, SecurityUtils.subject.principal, title, param.description)

        def msg = [
                [msg: 'Created Workspace', body: a.title, status: 'success'],
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
    String title
    String description

    String toString() {
        return [
                namespace  : namespace,
                title      : title,
                description: description
        ].toString()
    }

    static constraints = {
        namespace nullable: false
        title nullable: false
        description nullable: true
    }
}
