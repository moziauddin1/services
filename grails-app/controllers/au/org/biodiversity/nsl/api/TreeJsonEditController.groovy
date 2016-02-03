package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.ArrangementType
import au.org.biodiversity.nsl.LinkService
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
    LinkService linkService

    def test() {
        def msg = [msg: 'TreeJsonEditController']
        render msg as JSON
    }


    def createWorkspace(CreateWorkspaceParam param) {
        if (!param.validate()) return renderValidationErrors(param)

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
            response.status = 400
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

        response.status = 201
        return render(result as JSON)
    }

    def deleteWorkspace(DeleteWorkspaceParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        Object o = linkService.getObjectForLink(param.uri)
        if (o == null) {
            def result = [
                    success: false,
                    msg    : [
                            [msg: 'Not Found', body: "Workspace \"${param.uri}\" not found", status: 'warning'],
                    ]
            ]
            response.status = 404
            return render(result as JSON)
        }
        if (!(o instanceof Arrangement) || ((Arrangement) o).arrangementType != ArrangementType.U) {
            def result = [
                    success: false,
                    msg    : [
                            [msg: 'Not Found', body: "\"${param.uri}\" is not a workspace", status: 'warning'],
                    ]
            ]
            response.status = 404
            return render(result as JSON)
        }

        Arrangement a = (Arrangement) o;

        if (o.owner != SecurityUtils.subject.principal) {
            def result = [
                    success: true,
                    msg    : [
                            [msg: 'Authorisation', body: "You do not have permission to delete workspace ${a.title}", status: 'warning'],
                    ]
            ]
            response.status = 403
            return render(result as JSON)
        }

        userWorkspaceManagerService.deleteWorkspace(a);

        response.status = 200
        return render([
                success: true,
                msg    : [
                        [msg: 'Deleted', body: "Workspace ${a.title} deleted", status: 'success']
                ]
        ] as JSON)
    }

    def updateWorkspace(UpdateWorkspaceParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        Object o = linkService.getObjectForLink(param.uri)
        if (o == null) {
            def result = [
                    success: false,
                    msg    : [
                            [msg: 'Not Found', body: "Workspace \"${param.uri}\" not found", status: 'warning'],
                    ]
            ]
            response.status = 404
            return render(result as JSON)
        }
        if (!(o instanceof Arrangement) || ((Arrangement) o).arrangementType != ArrangementType.U) {
            def result = [
                    success: false,
                    msg    : [
                            [msg: 'Not Found', body: "\"${param.uri}\" is not a workspace", status: 'warning'],
                    ]
            ]
            response.status = 404
            return render(result as JSON)
        }

        Arrangement a = (Arrangement) o;

        if (o.owner != SecurityUtils.subject.principal) {
            def result = [
                    success: true,
                    msg    : [
                            [msg: 'Authorisation', body: "You do not have permission to alter workspace ${a.title}", status: 'warning'],
                    ]
            ]
            response.status = 403
            return render(result as JSON)
        }

        userWorkspaceManagerService.updateWorkspace(a, param.title, param.description);

        response.status = 200
        return render([
                success: true,
                msg    : [
                        [msg: 'Updated', body: "Workspace ${a.title} updated", status: 'success']
                ]
        ] as JSON)

    }

    private renderValidationErrors(param) {
        def msg = [];
        msg += param.errors.globalErrors.collect { Error it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
        msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }
        response.status = 400
        return render([
                success: false,
                msg    : msg
        ] as JSON)
    }
}

@Validateable
class CreateWorkspaceParam {
    String namespace
    String title
    String description

    static constraints = {
        namespace nullable: false
        title nullable: false
        description nullable: true
    }
}

@Validateable
class UpdateWorkspaceParam {
    String uri
    String title
    String description

    static constraints = {
        uri nullable: false
        title nullable: false
        description nullable: true
    }
}

@Validateable
class DeleteWorkspaceParam {
    String uri
    static constraints = {
        uri nullable: false
    }
}
