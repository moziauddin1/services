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
import au.org.biodiversity.nsl.NodeInternalType
import au.org.biodiversity.nsl.Reference
import au.org.biodiversity.nsl.UriNs
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

        handleException { handleExceptionIgnore ->

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

            Arrangement a = userWorkspaceManagerService.createWorkspace(ns, SecurityUtils.subject.principal, param.shared == null ? false : param.shared, title, param.description, checkout)

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
    }

    def deleteWorkspace(DeleteWorkspaceParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

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

            userWorkspaceManagerService.deleteWorkspace(a);

            response.status = 200
            return render([
                    success: true,
                    msg    : [
                            [msg: 'Deleted', body: "Workspace ${a.title} deleted", status: 'success']
                    ]
            ] as JSON)
        }
    }

    def updateWorkspace(UpdateWorkspaceParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

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

            userWorkspaceManagerService.updateWorkspace(a, param.shared == null ? false : param.shared, param.title, param.description);

            response.status = 200
            return render([
                    success: true,
                    msg    : [
                            [msg: 'Updated', body: "Workspace ${a.title} updated", status: 'success']
                    ]
            ] as JSON)
        }
    }

    def addNamesToNode(AddNamesToNodeParam param) {
        response.status = 200 // default

        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

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

            Node newFocus = userWorkspaceManagerService.addNamesToNode(ws, focus, names).target;
            newFocus = DomainUtils.refetchNode(newFocus);

            response.status = 200
            return render([
                    success : true,
                    newFocus: linkService.getPreferredLinkForObject(newFocus),
                    msg     : [
                            [msg: 'Updated', body: "Names added", status: 'success']
                    ]
            ] as JSON)
        }
    }

    def dropUrisOntoNode(DropUrisOntoNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

            response.status = 200; // default

            Node wsNode = (Node) linkService.getObjectForLink(param.wsNode as String)

            if (!wsNode) {
                response.status = 400
                def result = [
                        success: false,
                        msg    : [msg: "Illegal Argument", body: "${param.wsNode} not found", status: 'danger']
                ]
                return render(result as JSON)
            }

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

            if (o instanceof Name) {
                return render(dropNameOntoNode(ws, focus, target, o as Name, param) as JSON)
            } else if (o instanceof Instance) {

                if (param.relationshipType == 'citing') {
                    o = (o as Instance).cites
                    if (!o) {
                        def result = [
                                success: false,
                                msg    : [msg: 'Not citing', body: "${param.uris.get(0)} does not appear to be a citing instance", status: 'danger'],
                        ];

                        return render(result as JSON)
                    }
                }
                if (param.relationshipType == 'cited') {
                    o = (o as Instance).citedBy
                    if (!o) {
                        def result = [
                                success: false,
                                msg    : [msg: 'Not citing', body: "${param.uris.get(0)} does not appear to be a citing instance", status: 'danger'],
                        ];

                        return render(result as JSON)
                    }
                }
                if((o as Instance).citedBy) {
                    if (!o) {
                        def result = [
                                success: false,
                                msg    : [msg: 'Not standalone', body: "${param.uris.get(0)} does not appear to be a standalone instance", status: 'danger'],
                        ];

                        return render(result as JSON)
                    }
                }

                return render(dropInstanceOntoNode(ws, focus, target, o as Instance, param) as JSON)
            } else if (o instanceof Reference) {
                return render(dropReferenceOntoNode(ws, focus, target, o as Reference, param) as JSON)
            } else if (o instanceof Node) {
                return render(dropNodeOntoNode(ws, focus, target, o as Node, param) as JSON)
            } else {
                def result = [
                        success             : false,
                        msg                 : [msg: 'Cannot handle drop', body: o as String, status: 'danger'],
                        treeServiceException: ex,
                ];

                return render(result as JSON)

            }
        }
    }

    private def dropNameOntoNode(Arrangement ws, Node focus, Node target, Name name, DropUrisOntoNodeParam param) {
        return [
                success: false,
                msg    : [msg: 'Not supported', body: "Names cannot be dropped onto trees - an instance must be specified", status: 'warning']
        ]
    }

    static enum DropInstanceOntoNodeEnum {
        AddNewSubnode, ChangeNodeInstance(true);

        final boolean needsWarning;

        DropInstanceOntoNodeEnum() { needsWarning = false; }

        DropInstanceOntoNodeEnum(boolean w) { needsWarning = w; }
    }


    private
    def dropInstanceOntoNode(Arrangement ws, Node focus, Node target, Instance instance, DropUrisOntoNodeParam param) {
        if (queryService.countPaths(ws.node, target) > 1 && DomainUtils.isCheckedIn(target)) {
            return [
                    success: false,
                    msg    : [msg: 'Cannot drop', body: "Cannot check out a node which appears more than once in the workspace", status: 'warning']
            ]
        }

        // WORK OUT WHAT THINGS MIGHT BE BEING DONE

        Set<DropInstanceOntoNodeEnum> possibleActions = new HashSet<DropInstanceOntoNodeEnum>(EnumSet.allOf(DropInstanceOntoNodeEnum));

        if (target.root.node == target) {
            possibleActions.remove(DropInstanceOntoNodeEnum.ChangeNodeInstance);
        }

        if (possibleActions.isEmpty()) {
            return [
                    success: false,
                    msg    : [msg: "Cannot drop", body: "${instance.name.fullName} cannot be placed here", status: 'danger']
            ]
        }

        // HANDLE what the user has requested be done

        final DropInstanceOntoNodeEnum paramAction;

        if (param.dropAction) {
            try {
                paramAction = DropInstanceOntoNodeEnum.valueOf(param.dropAction);
            }
            catch (IllegalArgumentException ex) {
                // what a pain! You can ask an enum if it knows about a value, you have to catch the illegal argument exception.
                return [
                        success: false,
                        msg    : [msg: "Unrecognised action", body: "${param.dropAction} not recognised. Recognised actions are ${EnumSet.allOf(DropInstanceOntoNodeEnum)}.", status: 'danger']
                ]

            }
        } else paramAction = null;

        if (paramAction && !possibleActions.contains(paramAction)) {
            return [
                    success: false,
                    msg    : [msg: paramAction, body: "cannot perform this action", status: 'danger']
            ]
        }

        // if what the user wants is ok, or there is only one thing that can be done and it doesn't need a warning,
        // do that.

        if (paramAction || (possibleActions.size() == 1 && !possibleActions.first().needsWarning)) {
            DropInstanceOntoNodeEnum action = paramAction ?: possibleActions.first();

            def result
            switch (action) {
                case DropInstanceOntoNodeEnum.ChangeNodeInstance:
                    result = userWorkspaceManagerService.changeNodeInstance(ws, target, instance);
                    break;
                case DropInstanceOntoNodeEnum.AddNewSubnode:
                    result = userWorkspaceManagerService.addNodeSubinstance(ws, target, instance);
                    break;
                default:
                    throw new IllegalStateException("No operation for ${action}")
            }

            Node newFocus = ws.node.id == focus.id ? focus : queryService.findNodeCurrentOrCheckedout(ws.node, focus).subnode;

            return [
                    success  : true,
                    focusPath: queryService.findPath(ws.node, newFocus).collect { Node it -> linkService.getPreferredLinkForObject(it) },
                    refetch  : result.modified.collect { Node it ->
                        queryService.findPath(newFocus, it).collect { Node it2 -> linkService.getPreferredLinkForObject(it2) }
                    }
            ]
        } else {
            // an array, so that they are displayed in the order that I specify them here.
            def options = [];

            if (possibleActions.contains(DropInstanceOntoNodeEnum.ChangeNodeInstance)) {
                options << [msg         : 'Change',
                            body        : instance.name == target.name ?
                                    "Change the reference at this placement to ${instance.reference.citation}" :
                                    "Change the instance at this placement to ${instance.name.simpleName} s. ${instance.reference.citation}",
                            whenSelected: DropInstanceOntoNodeEnum.ChangeNodeInstance.name()
                ]
            }

            if (possibleActions.contains(DropInstanceOntoNodeEnum.AddNewSubnode)) {
                options << [msg         : 'Place',
                            body        : "Place ${instance.name.simpleName} s. ${instance.reference.citation} under ${target.name?.fullName}",
                            whenSelected: DropInstanceOntoNodeEnum.AddNewSubnode.name()
                ]
            }

            return [
                    success       : false,
                    moreInfoNeeded: [
                            [
                                    name    : 'dropAction',
                                    msg     : [
                                            msg: possibleActions.size() == 1 ? 'Confirm' : 'Multiple options'
                                    ],
                                    options : options,
                                    selected: null
                            ]
                    ]
            ]
        }
    }

    private
    def dropReferenceOntoNode(Arrangement ws, Node focus, Node target, Reference reference, DropUrisOntoNodeParam param) {
        return [
                success: false,
                //newFocus: linkService.getPreferredLinkForObject(newFocus),
                msg    : [msg: 'Not supported!', body: "References cannot be dropped onto trees", status: 'warning'],
        ]
    }

    private def dropNodeOntoNode(Arrangement ws, Node focus, Node target, Node node, DropUrisOntoNodeParam param) {

        // TODO: if a node is dropped onto a node at the same rank, then offer to move all the child nodes of
        // A onto B
        // unless its generic or subgeneric, in which case it won't be valid to do that.

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

        if (queryService.countPaths(ws.node, target) > 1 && DomainUtils.isCheckedIn(target)) {
            return [
                    success: false,
                    msg    : [msg: 'Cannot drop', body: "Cannot check out a node which appears more than once in the workspace", status: 'warning']
            ]
        }

        if (queryService.countPaths(ws.node, node) > 1) {
            return [
                    success: false,
                    msg    : [msg: 'Cannot drop', body: "Cannot move a node which appears more than once in the workspace", status: 'warning']
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

    def revertNode(RevertNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

            Node wsNode = (Node) linkService.getObjectForLink(param.wsNode as String)
            Arrangement ws = wsNode.root
            Node target = (Node) linkService.getObjectForLink(param.target as String)
            Node focus = (Node) linkService.getObjectForLink(param.focus as String)

            if (!wsNode) throw new IllegalArgumentException("null wsNode");
            if (!ws) throw new IllegalArgumentException("null ws");
            if (!target) throw new IllegalArgumentException("null target");
            if (!focus) throw new IllegalArgumentException("null focus");

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

            int draftNodeCount = queryService.countDraftNodes(target)

            if (draftNodeCount > 1 && param.confirm != 'CONFIRM-REVERT-NODE') {
                return render([
                        success       : false,
                        moreInfoNeeded: [
                                [
                                        name    : 'confirm',
                                        options : [
                                                [msg         : 'Confirm',
                                                 body        : "Are you sure you want to revert edits to ${draftNodeCount} draft placement${draftNodeCount > 1 ? 's' : ''}? This operation cannot be undone",
                                                 status      : 'danger',
                                                 whenSelected: 'CONFIRM-REVERT-NODE'
                                                ]

                                        ],
                                        selected: null
                                ]
                        ]
                ] as JSON)
            }


            Node curr;
            for (curr = target.prev; curr && !DomainUtils.isCurrent(curr); curr = curr.next);

            if (!DomainUtils.isCurrent(target.prev)) {
                response.status = 400

                if (curr && !DomainUtils.isEndNode(curr)) {
                    if (param.useCurrentVersion == 'USE_CURRENT_VERSION') {
                        // great!
                    } else {
                        def result = [
                                success       : false,
                                msg           : [
                                        msg   : "Previous node is not current",
                                        body  : "${param.wsNode} is an edited version of a node that is no longer current",
                                        status: 'warning'],
                                moreInfoNeeded: [
                                        [name: 'confirm', selected: param.confirm['confirm']],
                                        [
                                                name    : 'useCurrentVersion',
                                                msg     : [
                                                        msg   : 'Previous node is not current',
                                                        body  : "${param.wsNode} is an edited version of a node that is no longer current",
                                                        status: 'warning'],
                                                options : [
                                                        [
                                                                msg         : 'Use new version',
                                                                body        : 'Revert to the current version of this node',
                                                                status      : 'success',
                                                                whenSelected: 'USE-CURRENT-VERSION'
                                                        ]

                                                ],
                                                selected: null
                                        ]
                                ]
                        ]
                        return render(result as JSON)
                    }
                } else {
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
                    success  : true,
                    focusPath: target == focus ? queryService.findPath(ws.node, curr).collect { Node it2 -> linkService.getPreferredLinkForObject(it2) } : null,
                    refetch  : [
                            [linkService.getPreferredLinkForObject(parentLink.supernode)]
                    ]
            ] as JSON)
        }
    }

    def removeNode(RemoveNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->
            Node wsNode = (Node) linkService.getObjectForLink(param.wsNode as String)
            Arrangement ws = wsNode.root
            Node focus = (Node) linkService.getObjectForLink(param.focus as String)
            Node linkSuper = linkService.getObjectForLink(param.linkSuper as String)
            Link link = Link.findBySupernodeAndLinkSeq(linkSuper, param.linkSeq)

            if (!wsNode) throw new IllegalArgumentException("null wsNode");
            if (!ws) throw new IllegalArgumentException("null ws");
            if (!linkSuper) throw new IllegalArgumentException("null super");
            if (!link) throw new IllegalArgumentException("linkSeq not found");
            if (!focus) throw new IllegalArgumentException("null focus");

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

            if (queryService.countPaths(ws.node, linkSuper) > 1 && DomainUtils.isCheckedIn(linkSuper)) {
                response.status = 400
                return render([
                        success: false,
                        msg    : [msg: 'Cannot delete', body: "Cannot check out a node which appears more than once in the workspace", status: 'warning']
                ] as JSON)
            }

            int draftNodeCount = queryService.countDraftNodes(link.subnode)

            if (draftNodeCount > 0 && param.confirm != 'CONFIRM-REMOVE-NODE') {
                return render([
                        success       : false,
                        moreInfoNeeded: [
                                [
                                        name    : 'confirm',
                                        options : [
                                                [msg         : 'Confirm',
                                                 body        : "Are you sure you want to delete ${draftNodeCount} draft placement${draftNodeCount > 1 ? 's' : ''}? This operation cannot be undone",
                                                 status      : 'danger',
                                                 whenSelected: 'CONFIRM-REMOVE-NODE'
                                                ]

                                        ],
                                        selected: null
                                ]
                        ]
                ] as JSON)
            }

            Node newCheckout = userWorkspaceManagerService.removeLink(ws, link);

            Node newFocus = ws.node.id == focus.id ? focus : queryService.findNodeCurrentOrCheckedout(ws.node, focus).subnode;

            def focusPath = queryService.findPath(ws.node, newFocus).collect { Node it2 ->
                linkService.getPreferredLinkForObject(it2)
            }
            def targetPath = queryService.findPath(newFocus, newCheckout).collect { Node it2 ->
                linkService.getPreferredLinkForObject(it2)
            }

            return render([
                    success  : true,
                    focusPath: focusPath,
                    refetch  : [targetPath]
            ] as JSON)
        }
    }

    def setNodeType(SetNodeTypeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

            Node wsNode = (Node) linkService.getObjectForLink(param.wsNode as String)
            Arrangement ws = wsNode.root
            Node focus = (Node) linkService.getObjectForLink(param.focus as String)
            Node target = (Node) linkService.getObjectForLink(param.target as String)
            UriNs ns = UriNs.findByLabel(param.nsPart)

            if (!wsNode) throw new IllegalArgumentException("null wsNode");
            if (!ws) throw new IllegalArgumentException("null ws");
            if (!focus) throw new IllegalArgumentException("null focus");
            if (!target) throw new IllegalArgumentException("null target");

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

            if (target.internalType != NodeInternalType.T) {
                def result = [
                        success: false,
                        msg    : [msg: 'Target', body: "${param.target} is not a taxonomic node", status: 'danger']
                ]
                response.status = 400
                return render(result as JSON)
            }

            if (!ns) {
                response.status = 400
                return render([
                        success: false,
                        msg    : [msg: 'No match', body: "Unrecognised uri namespace {{param.nsPart}}", status: 'info']
                ] as JSON)
            }

            def result = userWorkspaceManagerService.setNodeType(ws, target, new au.org.biodiversity.nsl.tree.Uri(ns, param.idPart));

            Node newFocus = ws.node.id == focus.id ? focus : queryService.findNodeCurrentOrCheckedout(ws.node, focus).subnode;

            return render([
                    success  : true,
                    focusPath: queryService.findPath(ws.node, newFocus).collect { Node it1 -> linkService.getPreferredLinkForObject(it1) },
                    refetch  : result.modified.collect { Node it1 ->
                        queryService.findPath(newFocus, it1).collect { Node it2 -> linkService.getPreferredLinkForObject(it2) }
                    }
            ] as JSON)
        }
    }

    def verifyCheckin(CheckinNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

            Node node = (Node) linkService.getObjectForLink(param.uri as String)

            /*
        We get passed a uri.

        The uri must be a taxonomic node
        It must be a draft node
        the draft node must belong to a workspace
        the workspace must be one the user owns
        the node must have a prev
        the prev must belong to a classification tree (?)
        the classification tree must be one the user can edit

        we then pass this down to the tree service for more validation checks

         */


            return render([
                    success            : true,
                    msg                : [[msg: 'TODO', body: 'actually verify the checkin', status: 'info']],
                    verificationResults: [
                            goodToGo: true
                    ]
            ] as JSON)
        }
    }

    def performCheckin(CheckinNodeParam param) {
        if (!param.validate()) return renderValidationErrors(param)

        handleException { handleExceptionIgnore ->

            Node node = (Node) linkService.getObjectForLink(param.uri as String)

            // doing no validation at all
            // TODO: security, etc

            def result = userWorkspaceManagerService.performCheckin(node);

            return render([
                    success       : true,
                    msg           : [[msg: 'Ok', body: 'Checkin performed. Please refresh your browser.', status: 'success']],
                    refetch       : result.modified.collect { Node it2 -> linkService.getPreferredLinkForObject(it2) },
                    checkinResults: [
                    ]
            ] as JSON)
        }
    }


    private renderValidationErrors(param) {
        def msg = [];
        msg += param.errors.globalErrors.collect { it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, (Locale)null)] }
        msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, (Locale)null)] }
        response.status = 400
        return render([
                success: false,
                msg    : msg
        ] as JSON)
    }

    private handleException(Closure doIt) {
        try {
            return doIt();
        }
        catch (ServiceException ex) {
            doIt.delegate.response.status = 400

            return render([
                    success             : false,
                    msg                 : [msg       : ex.class.simpleName,
                                           body      : ex.getMessage(),
                                           status    : 'danger',
                                           stackTrace: ex.getStackTrace().findAll {
                                               StackTraceElement it -> it.fileName && it.lineNumber != -1 && it.className.startsWith('au.org.biodiversity.nsl.')
                                           }.collect {
                                               StackTraceElement it -> [file: it.fileName, line: it.lineNumber, method: it.methodName, clazz: it.className]
                                           }],
                    treeServiceException: ex,
            ] as JSON)
        }
        catch (Exception ex) {
            doIt.delegate.response.status = 500
            return render([
                    success: false,
                    msg    : [msg       : ex.class.simpleName,
                              body      : ex.getMessage(),
                              status    : 'danger',
                              stackTrace: ex.getStackTrace().findAll {
                                  StackTraceElement it -> it.fileName && it.lineNumber != -1 && it.className.startsWith('au.org.biodiversity.nsl.')
                              }.collect {
                                  StackTraceElement it -> [file: it.fileName, line: it.lineNumber, method: it.methodName, clazz: it.className]
                              }
                    ]
            ] as JSON)
        }

    }
}


@Validateable
class CreateWorkspaceParam {
    String namespace
    String title
    String description
    String checkout
    Boolean shared

    static constraints = {
        namespace nullable: false
        title nullable: false
        description nullable: true
        checkout nullable: true
        shared nullable: true
    }
}

@Validateable
class UpdateWorkspaceParam {
    String uri
    String title
    String description
    Boolean shared

    static constraints = {
        uri nullable: false
        title nullable: false
        description nullable: true
        shared nullable: true
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
class CheckinNodeParam {
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
    String relationshipType
    String dropAction
    static constraints = {
        wsNode nullable: false
        target nullable: false
        focus nullable: false
        uris nullable: true
        relationshipType nullable: true
        confirm nullable: true
        dropAction nullable: true
    }
}

@Validateable
class RevertNodeParam {
    String wsNode
    String focus
    String target
    String useCurrentVersion
    String confirm
    static constraints = {
        wsNode nullable: false
        target nullable: false
        focus nullable: false
        useCurrentVersion nullable: true
        confirm nullable: true
    }
}

@Validateable
class RemoveNodeParam {
    String wsNode
    String focus
    String linkSuper
    Integer linkSeq
    String confirm
    static constraints = {
        wsNode nullable: false
        focus nullable: false
        linkSuper nullable: false
        linkSeq nullable: false
        confirm nullable: true
    }
}

@Validateable
class SetNodeTypeParam {
    String wsNode
    String focus
    String target
    String nsPart
    String idPart
    static constraints = {
        wsNode nullable: false
        focus nullable: false
        target nullable: false
        nsPart nullable: false
        idPart nullable: false
    }
}