package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.ArrangementType
import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.JsonRendererService
import au.org.biodiversity.nsl.Link
import au.org.biodiversity.nsl.LinkService
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Namespace
import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.Reference
import au.org.biodiversity.nsl.tree.BasicOperationsService
import au.org.biodiversity.nsl.tree.DomainUtils
import au.org.biodiversity.nsl.tree.QueryService
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
    QueryService queryService

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
        response.status = 200 // default

        if (!param.validate()) return renderValidationErrors(param)

        Arrangement ws = (Arrangement) linkService.getObjectForLink(param.wsUri as String)
        Node focus = (Node) linkService.getObjectForLink(param.focus as String)

        def msgV = [];

        if (ws.arrangementType != ArrangementType.U) {
            msgV << [msg: "Illegal Argument", body: "${param.wsUri} is not a workspace", status: 'danger']
        }

        if (!msgV.isEmpty()) {
            response.status = 400
            def result = [
                    success: false,
                    msg    : msgV
            ]
            return render(result as JSON)
        }

        if (ws.owner != SecurityUtils.subject.principal) {
            def result = [
                    success: false,
                    msg    : [
                            [msg: 'Authorisation', body: "You do not have permission to alter workspace ${ws.title}", status: 'warning'],
                    ]
            ]
            response.status = 403
            return render(result as JSON)
        }



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
            newFocus = userWorkspaceManagerService.addNamesToNode(ws, focus, names).target;
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

    def dropUrisOntoNode(DropUrisOntoNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        response.status = 200; // default

        Node wsNode = (Node) linkService.getObjectForLink(param.wsNode as String)
        Arrangement ws = wsNode.root
        Node target = (Node) linkService.getObjectForLink(param.target as String)
        Node focus = (Node) linkService.getObjectForLink(param.focus as String)


        if (ws.arrangementType != ArrangementType.U) {
            response.status = 400
            def result = [
                    success: false,
                    msg    : [msg: "Illegal Argument", body: "${param.wsNode} is not a workspace root", status: 'danger']
            ]
            return render(result as JSON)
        }

        if (ws.owner != SecurityUtils.subject.principal) {
            def result = [
                    success: false,
                    msg    : [msg: 'Authorisation', body: "You do not have permission to alter workspace ${ws.title}", status: 'danger']
            ]
            response.status = 403
            return render(result as JSON)
        }

        // ok, at this point I need to work out what the hell to do
        // I'll handle dropping one thing at time, I think.

        if (!param.uris || param.uris.isEmpty()) {
            return render([
                    success: false,
                    //newFocus: linkService.getPreferredLinkForObject(newFocus),
                    msg    : [msg: 'No Drop', body: "nothing appears to have been dropped", status: 'info']
            ] as JSON)
        }

        if (param.uris.size() > 1) {
            return render([
                    success: false,
                    //newFocus: linkService.getPreferredLinkForObject(newFocus),
                    msg    : [msg: 'Multiple', body: "Multiple drops is not implemented yet", status: 'info']
            ] as JSON)
        }

        Object o = linkService.getObjectForLink(param.uris.get(0) as String)

        if (!o) {
            def result = [
                    success: false,
                    msg    : [msg: 'Unrecognised URI', body: "${param.uris.get(0)} does not appear to be a uri from this NSL shard", status: 'danger'],
            ];

            return render(result as JSON)

        }

        try {
            if (o instanceof Name) {
                return render(dropNameOntoNode(ws, focus, target, o as Name) as JSON)
            } else if (o instanceof Instance) {
                return render(dropInstanceOntoNode(ws, focus, target, o as Instance) as JSON)
            } else if (o instanceof Reference) {
                return render(dropReferenceOntoNode(ws, focus, target, o as Reference) as JSON)
            } else if (o instanceof Node) {
                return render(dropNodeOntoNode(ws, focus, target, o as Node) as JSON)
            } else {
                def result = [
                        success             : false,
                        msg                 : [msg: 'Cannot handle drop', body: o as String, status: 'danger'],
                        treeServiceException: ex,
                ];

                return render(result as JSON)

            }
        }
        catch (ServiceException ex) {
            response.status = 400

            def result = [
                    success             : false,
                    msg                 : [msg: 'Could not handle drop', body: ex.getLocalizedMessage(), status: 'warning'],
                    treeServiceException: ex,
            ];

            return render(result as JSON)
        }
    }


    private def dropNameOntoNode(Arrangement ws, Node focus, Node target, Name name) {
        return [
                success     : false,
                //newFocus: linkService.getPreferredLinkForObject(newFocus),
                msg         : [msg: 'TODO!', body: "implement dropNameOntoNode", status: 'info'],
                chooseAction: [
                        [
                                msg   : [msg: 'Danger', body: "this is a danger message", status: "danger"],
                                action: "DO THING a"
                        ],
                        [
                                msg   : [msg: 'Warning', body: "this is a warning message", status: "warning"],
                                action: "DO THING b"
                        ],
                        [
                                msg   : [msg: 'Info', body: "this is an info message", status: "info"],
                                action: "DO THING c"
                        ],
                        [
                                msg   : [msg: 'Success', body: "this is a success message", status: "success"],
                                action: "DO THING d"
                        ],
                        [
                                msg   : [msg: 'Default', body: "this is a default message"],
                                action: "DO THING e"
                        ]
                ]
        ]
    }

    private def dropInstanceOntoNode(Arrangement ws, Node focus, Node target, Instance nstance) {
        return [
                success     : false,
                //newFocus: linkService.getPreferredLinkForObject(newFocus),
                msg         : [msg: 'TODO!', body: "implement dropInstanceOntoNode", status: 'info'],
                chooseAction: [
                        [
                                msg   : [msg: 'Danger', body: "this is a danger message", status: "danger"],
                                action: "DO THING a"
                        ],
                        [
                                msg   : [msg: 'Warning', body: "this is a warning message", status: "warning"],
                                action: "DO THING b"
                        ],
                        [
                                msg   : [msg: 'Info', body: "this is an info message", status: "info"],
                                action: "DO THING c"
                        ],
                        [
                                msg   : [msg: 'Success', body: "this is a success message", status: "success"],
                                action: "DO THING d"
                        ],
                        [
                                msg   : [msg: 'Default', body: "this is a default message"],
                                action: "DO THING e"
                        ]
                ]
        ]
    }

    private def dropReferenceOntoNode(Arrangement ws, Node focus, Node target, Reference reference) {
        return [
                success     : false,
                //newFocus: linkService.getPreferredLinkForObject(newFocus),
                msg         : [msg: 'TODO!', body: "implement dropReferenceOntoNode", status: 'info'],
                chooseAction: [
                        [
                                msg   : [msg: 'Danger', body: "this is a danger message", status: "danger"],
                                action: "DO THING a"
                        ],
                        [
                                msg   : [msg: 'Warning', body: "this is a warning message", status: "warning"],
                                action: "DO THING b"
                        ],
                        [
                                msg   : [msg: 'Info', body: "this is an info message", status: "info"],
                                action: "DO THING c"
                        ],
                        [
                                msg   : [msg: 'Success', body: "this is a success message", status: "success"],
                                action: "DO THING d"
                        ],
                        [
                                msg   : [msg: 'Default', body: "this is a default message"],
                                action: "DO THING e"
                        ]
                ]
        ]
    }

    private def dropNodeOntoNode(Arrangement ws, Node focus, Node target, Node node) {
        if (node == target) {
            return render([
                    success: false,
                    msg    : [msg: 'Cannot drop', body: "Cannot drop a node onto itself.", status: 'info']
            ] as JSON)
        }

        def path = queryService.findPath(node, target)

        if (path) {
            return [
                    success: false,
                    msg    : [msg: 'Cannot drop', body: "Cannot drop a node onto a subnode of itself.", status: 'info']
            ]
        }

        def pathToNode = queryService.findPath(ws.node, node);
        def pathToTarget = queryService.findPath(ws.node, target);

        def result = null

        if (pathToTarget && pathToNode) {
            result = userWorkspaceManagerService.moveWorkspaceNode(ws, target, node);
        } else if (pathToTarget && DomainUtils.isCheckedIn(node)) {
            result = userWorkspaceManagerService.adoptNode(ws, target, node);
        }

        Node newFocus = ws.node.id == focus.id ? focus : queryService.findNodeCurrentOrCheckedout(ws.node, focus).subnode;

        return [
                success  : true,
                focusPath: queryService.findPath(ws.node, newFocus).collect { Node it -> linkService.getPreferredLinkForObject(it) },
                refetch  : result.modified.collect { Node it ->
                    queryService.findPath(newFocus, it).collect { Node it2 -> linkService.getPreferredLinkForObject(it2) }
                }
        ]
    }

    def revertNode(RevertRemoveNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        Node wsNode = (Node) linkService.getObjectForLink(param.wsNode as String)
        Arrangement ws = wsNode.root
        Node target = (Node) linkService.getObjectForLink(param.target as String)
        Node focus = (Node) linkService.getObjectForLink(param.focus as String)

        if (ws.arrangementType != ArrangementType.U) {
            response.status = 400
            def result = [
                    success: false,
                    msg    : [msg: "Illegal Argument", body: "${param.wsNode} is not a workspace root", status: 'danger']
            ]
            return render(result as JSON)
        }

        if (ws.owner != SecurityUtils.subject.principal) {
            def result = [
                    success: false,
                    msg    : [msg: 'Authorisation', body: "You do not have permission to alter workspace ${ws.title}", status: 'danger']
            ]
            response.status = 403
            return render(result as JSON)
        }

        if (DomainUtils.isCheckedIn(target)) {
            response.status = 400
            def result = [
                    success: false,
                    msg    : [msg: "Not draft", body: "${param.wsNode} is not a draft node", status: 'danger']
            ]
            return render(result as JSON)
        }

        if (!target.prev) {
            response.status = 400
            def result = [
                    success: false,
                    msg    : [msg: "Not an edited node", body: "${param.wsNode} is not an edited version of some persistent node", status: 'danger']
            ]
            return render(result as JSON)
        }

        Node curr;
        for(curr = target.prev; curr && !DomainUtils.isCurrent(curr); curr = curr.next);

        if (!DomainUtils.isCurrent(target.prev)) {
            response.status = 400

            if(curr && !DomainUtils.isEndNode(curr)) {
                if(param.confirm == 'USE_CURRENT_VERSION') {
                    // great!
                }
                else {
                    def result = [
                            success: false,
                            msg    : [msg: "Previous node is not current", body: "${param.wsNode} is an edited version of a node that is no longer current", status: 'warning'],
                            chooseAction: [
                                    [
                                            msg   : [msg: 'Use new version', body: "Revert to the current version of this node", status: "success"],
                                            action: 'USE_CURRENT_VERSION'
                                    ],
                            ]
                    ]
                    return render(result as JSON)
                }
            }
            else {
                def result = [
                        success: false,
                        msg    : [msg: "Previous node is not current", body: "${param.wsNode} is an edited version of a node that is no longer current and has been deleted. This draft node can be removed, but it cannot be reverted.", status: 'info'],
                ]
                return render(result as JSON)
            }

        }

        // ok. delete the draft node and insert the current replacement node into the tree

        Link parentLink = userWorkspaceManagerService.replaceDraftNodeWith(target, curr);

        // if the parent is the focus, then reset the focus path to point at the replacement (curr)
        // refetch the parent node

        return render([
                success     : true,
                focusPath: target == focus ? queryService.findPath(ws.node, curr).collect { Node it -> linkService.getPreferredLinkForObject(it) } : null,
                refetch: [
                        [ linkService.getPreferredLinkForObject(parentLink.supernode) ]
                ]
        ] as JSON)

    }

    def removeNode(RevertRemoveNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)
        return render([
                success     : false,
                msg         : [msg: 'TODO!', body: "implement removeNode", status: 'info'],
                chooseAction: [
                        [
                                msg   : [msg: 'Danger', body: "this is a danger message", status: "danger"],
                                action: "DO THING a"
                        ],
                        [
                                msg   : [msg: 'Warning', body: "this is a warning message", status: "warning"],
                                action: "DO THING b"
                        ],
                        [
                                msg   : [msg: 'Info', body: "this is an info message", status: "info"],
                                action: "DO THING c"
                        ],
                        [
                                msg   : [msg: 'Success', body: "this is a success message", status: "success"],
                                action: "DO THING d"
                        ],
                        [
                                msg   : [msg: 'Default', body: "this is a default message"],
                                action: "DO THING e"
                        ]
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
    String wsNode
    String focus
    List<String> names
    static constraints = {
        wsNode nullable: false
        focus nullable: false
    }
}


@Validateable
class DropUrisOntoNodeParam {
    String wsNode
    String focus
    String target
    String confirm
    List<String> uris
    static constraints = {
        wsNode nullable: false
        target nullable: false
        focus nullable: false
        uris nullable: true
        confirm nullable: true
    }
}

@Validateable
class RevertRemoveNodeParam {
    String wsNode
    String focus
    String target
    String confirm
    static constraints = {
        wsNode nullable: false
        target nullable: false
        focus nullable: false
        confirm nullable: true
    }
}
