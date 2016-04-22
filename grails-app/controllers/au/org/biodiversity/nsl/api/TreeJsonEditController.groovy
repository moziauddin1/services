package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.ArrangementType
import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.JsonRendererService
import au.org.biodiversity.nsl.LinkService
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Namespace
import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.tree.DomainUtils
import au.org.biodiversity.nsl.tree.ServiceException
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
    JsonRendererService jsonRendererService

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

        Node checkout = null;

        if (param.checkout) {
            Object o = linkService.getObjectForLink(param.checkout)
            if (o == null) {
                def result = [
                        success: false,
                        msg    : [
                                [msg: 'Not Found', body: "Node \"${param.checkout}\" not found", status: 'warning'],
                        ]
                ]
                response.status = 404
                return render(result as JSON)
            }
            if (!(o instanceof Node)) {
                def result = [
                        success: false,
                        msg    : [
                                [msg: 'Not Found', body: "\"${param.node}\" is not a node", status: 'warning'],
                        ]
                ]
                response.status = 404
                return render(result as JSON)
            }
            checkout = o as Node
        }

        Arrangement a;
        try {
            a = userWorkspaceManagerService.createWorkspace(ns, SecurityUtils.subject.principal, title, param.description, checkout)
        }
        catch (ServiceException ex) {
            response.status = 400

            def result = [
                    success             : false,
                    msg                 : [
                            [msg: 'Could not create workspace', body: ex.getLocalizedMessage(), status: 'warning'],
                    ],
                    treeServiceException: ex,
                    uri                 : null
            ];

            return render(result as JSON)
        }

        def result = [
                success: true,
                msg    : [
                        [msg: 'Created Workspace', body: a.title, status: 'success'],
                ],
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
                    success: false,
                    msg    : [
                            [msg: 'Authorisation', body: "You do not have permission to delete workspace ${a.title}", status: 'warning'],
                    ]
            ]
            response.status = 403
            return render(result as JSON)
        }

        try {
            userWorkspaceManagerService.deleteWorkspace(a);
        }
        catch (ServiceException ex) {
            response.status = 400

            def result = [
                    success             : false,
                    msg                 : [
                            [msg: 'Could not delete workspace', body: ex.getLocalizedMessage(), status: 'warning'],
                    ],
                    treeServiceException: ex,
                    uri                 : null
            ];

            return render(result as JSON)
        }

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
                    success: false,
                    msg    : [
                            [msg: 'Authorisation', body: "You do not have permission to alter workspace ${a.title}", status: 'warning'],
                    ]
            ]
            response.status = 403
            return render(result as JSON)
        }

        try {
            userWorkspaceManagerService.updateWorkspace(a, param.title, param.description);
        }
        catch (ServiceException ex) {
            response.status = 400

            def result = [
                    success             : false,
                    msg                 : [
                            [msg: 'Could not delete workspace', body: ex.getLocalizedMessage(), status: 'warning'],
                    ],
                    treeServiceException: ex,
                    uri                 : null
            ];

            return render(result as JSON)
        }

        response.status = 200
        return render([
                success: true,
                msg    : [
                        [msg: 'Updated', body: "Workspace ${a.title} updated", status: 'success']
                ]
        ] as JSON)

    }

    def addNamesToNode(AddNamesToNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        Node root = (Node) linkService.getObjectForLink(param.root as String)
        Node focus = (Node) linkService.getObjectForLink(param.focus as String)

        def names = [];

        // TODO better error handling here.

        for (uri in param.names) {
            def o = linkService.getObjectForLink(uri);
            if (!(o instanceof Name) && !(o instanceof Instance)) {
                throw new IllegalArgumentException(uri);
            }
            names.add(o)
        }

        Node newFocus
        try {
            newFocus = userWorkspaceManagerService.addNamesToNode(root.root, focus, names);
            newFocus = DomainUtils.refetchNode(newFocus);
        }
        catch (ServiceException ex) {
            response.status = 400

            def result = [
                    success             : false,
                    msg                 : [
                            [msg: 'Could not add names to node', body: ex.getLocalizedMessage(), status: 'warning'],
                    ],
                    treeServiceException: ex,
                    uri                 : null
            ];

            return render(result as JSON)
        }

        response.status = 200
        return render([
                success : true,
                newFocus: linkService.getPreferredLinkForObject(newFocus),
                msg     : [
                        [msg: 'Updated', body: "Names added", status: 'success']
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
    String checkout

    static constraints = {
        namespace nullable: false
        title nullable: false
        description nullable: true
        checkout checkout: true
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

@Validateable
class AddNamesToNodeParam {
    String root
    String focus
    List<String> names
    static constraints = {
        root nullable: false
        focus nullable: false
    }
}
