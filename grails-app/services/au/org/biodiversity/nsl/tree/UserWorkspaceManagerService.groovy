package au.org.biodiversity.nsl.tree

import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional
import org.hibernate.SessionFactory
import org.hibernate.jdbc.Work

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

@Transactional(rollbackFor = [ServiceException])
class UserWorkspaceManagerService implements ValidationUtils {
    QueryService queryService
    BasicOperationsService basicOperationsService
    VersioningService versioningService
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl


    Arrangement createWorkspace(Namespace namespace, Arrangement baseTree, String owner, boolean shared, String title, String description) {
        mustHave(owner: owner, title: title, baseTree: baseTree) {

            if (baseTree.arrangementType != ArrangementType.P) {
                throw new IllegalArgumentException("baseTree must be a classifcation")
            }

            Event e = basicOperationsService.newEvent(namespace, "Create workspace on ${baseTree.label} for ${owner}", owner)
            baseTree = DomainUtils.refetchArrangement(baseTree)

            Arrangement ws = basicOperationsService.createWorkspace(e, baseTree, owner, shared, title, description)

            baseTree = DomainUtils.refetchArrangement(baseTree)

            Node checkout = DomainUtils.getSingleSubnode(baseTree.node)

            basicOperationsService.adoptNode(ws.node, checkout, VersioningMethod.V, linkType: DomainUtils.getBoatreeUri('workspace-top-node'))

            ws = DomainUtils.refetchArrangement(ws)

            return ws
        } as Arrangement
    }

    void deleteWorkspace(Arrangement arrangement) {
        if (!arrangement) throw new IllegalArgumentException("arrangement may not be null")
        if (arrangement.arrangementType != ArrangementType.U) throw new IllegalArgumentException("arrangement must be a workspace")
        basicOperationsService.deleteArrangement(arrangement)
    }


    void updateWorkspace(Arrangement arrangement, boolean shared, String title, String description) {
        if (!arrangement) throw new IllegalArgumentException("arrangement may not be null")
        if (arrangement.arrangementType != ArrangementType.U) throw new IllegalArgumentException("arrangement must be a workspace")
        basicOperationsService.updateWorkspace(arrangement, shared, title, description)
    }

    def moveWorkspaceNode(Arrangement ws, Node target, Node node) {
        if (target == node) throw new IllegalArgumentException("node == target")

        if (node == ws.node) throw new IllegalArgumentException("node == ws.node")

        List<Node> reversePath = queryService.findPath(node, target)

        if (reversePath && !reversePath.isEmpty()) throw new IllegalArgumentException("node is supernode of target")

        if (DomainUtils.isCheckedIn(target)) {
            List<Node> pathToTarget = queryService.findPath(ws.node, target)
            if (pathToTarget.isEmpty()) throw new IllegalArgumentException("target not in workspace")

            target = basicOperationsService.checkoutNode(ws.node, target)

            ws = DomainUtils.refetchArrangement(ws)
            target = DomainUtils.refetchNode(target)
            node = DomainUtils.refetchNode(node)
        }


        List<Link> pathToNode = queryService.findPathLinks(ws.node, node)

        if (pathToNode.isEmpty()) throw new IllegalArgumentException("node not in workspace")

        Link parentLink = pathToNode.last()
        Node currentParent = parentLink.supernode

        if (DomainUtils.isCheckedIn(currentParent)) {
            ws = DomainUtils.refetchArrangement(ws)
            target = DomainUtils.refetchNode(target)
            DomainUtils.refetchNode(node)
            currentParent = DomainUtils.refetchNode(currentParent)
            parentLink = DomainUtils.refetchLink(parentLink)

            currentParent = basicOperationsService.checkoutNode(ws.node, currentParent)
            currentParent = DomainUtils.refetchNode(currentParent)
            parentLink = DomainUtils.refetchLink(parentLink)

            parentLink = Link.findBySupernodeAndLinkSeq(currentParent, parentLink.linkSeq)

        }

        basicOperationsService.simpleMoveDraftLink(parentLink, target)

        return [
                target  : target,
                modified: [target, currentParent]
        ]

    }

    def moveWorkspaceSubnodes(Arrangement ws, Node target, Node node) {
        if (target == node) throw new IllegalArgumentException("node == target")

        if (node == ws.node) throw new IllegalArgumentException("node == ws.node")

        List<Node> reversePath = queryService.findPath(node, target)

        if (reversePath && !reversePath.isEmpty()) throw new IllegalArgumentException("node is supernode of target")

        if (DomainUtils.isCheckedIn(target)) {
            List<Node> pathToTarget = queryService.findPath(ws.node, target)
            if (pathToTarget.isEmpty()) throw new IllegalArgumentException("target not in workspace")

            target = basicOperationsService.checkoutNode(ws.node, target)

            ws = DomainUtils.refetchArrangement(ws)
            target = DomainUtils.refetchNode(target)
            node = DomainUtils.refetchNode(node)
        }

        if (DomainUtils.isCheckedIn(node)) {
            List<Node> pathToNode = queryService.findPath(ws.node, node)
            if (pathToNode.isEmpty()) throw new IllegalArgumentException("node not in workspace")

            node = basicOperationsService.checkoutNode(ws.node, node)

            ws = DomainUtils.refetchArrangement(ws)
            target = DomainUtils.refetchNode(target)
            node = DomainUtils.refetchNode(node)
        }

        Set<Link> links = new HashSet<Link>(node.subLink.findAll { it.subnode.internalType == NodeInternalType.T })

        Link prevLink = null

        for (Link l : links) {
            l = DomainUtils.refetchLink(l)
            if (DomainUtils.isCheckedIn(l.subnode)) {
                prevLink = basicOperationsService.adoptNode(target, l.subnode, l.versioningMethod, linkType: DomainUtils.getRawLinkTypeUri(l), prevLink: prevLink)
                l = DomainUtils.refetchLink(l)
                basicOperationsService.deleteLink(l.supernode, l.linkSeq)
            } else {
                // this is failing when more than one link needs doing, becaue
                prevLink = basicOperationsService.simpleMoveDraftLink(l, DomainUtils.refetchNode(target), prevLink: prevLink)
            }

            ws = DomainUtils.refetchArrangement(ws)
            target = DomainUtils.refetchNode(target)
            node = DomainUtils.refetchNode(node)
        }

        return [
                target  : target,
                modified: [target, node]
        ]


    }

    Map adoptNode(Arrangement workSpace, Node target, Node node) {
        if (target == node) throw new IllegalArgumentException("node == target")

        if (node == workSpace.node) throw new IllegalArgumentException("node == workSpace.node")

        if (!DomainUtils.isCheckedIn(node)) throw new IllegalArgumentException("cannot adopt draft node")
        if (DomainUtils.isReplaced(node)) throw new IllegalArgumentException("cannot adopt outdated node")

        List<Node> reversePath = queryService.findPath(node, target)

        if (reversePath && !reversePath.isEmpty()) throw new IllegalArgumentException("node is supernode of target")

        if (DomainUtils.isCheckedIn(target)) {
            List<Node> pathToTarget = queryService.findPath(workSpace.node, target)
            if (pathToTarget.isEmpty()) throw new IllegalArgumentException("target not in workspace")

            target = basicOperationsService.checkoutNode(workSpace.node, target)

            workSpace = DomainUtils.refetchArrangement(workSpace)
            target = DomainUtils.refetchNode(target)
            node = DomainUtils.refetchNode(node)
        }

        List<Link> pathToNode = queryService.findPathLinks(workSpace.node, node)

        if (!pathToNode.isEmpty()) throw new IllegalArgumentException("node already in workspace")

        basicOperationsService.adoptNode(target, node, VersioningMethod.V)

        return [
                target  : target,
                modified: [target]
        ]
    }

    Map addNamesToNode(Arrangement ws, Node focus, List<?> names) {
        log.debug('addNamesToNode')
        if (!ws) throw new IllegalArgumentException("root may not be null")
        if (!focus) throw new IllegalArgumentException("focus may not be null")

        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("root must belong to a workspace")

        if (ws.node.checkedInAt) {
            throw new IllegalStateException("Workspace root nodes are never checked in")
        }

        if (focus.checkedInAt) {
            log.debug("about to checkout ${focus} in ${ws}")
            focus = basicOperationsService.checkoutNode(ws.node, focus)
            log.debug("checkout ok. New node is ${focus}")
        } else {
            log.debug("${focus} is already checked out")
        }

        names.each {
            ws = DomainUtils.refetchArrangement(ws)
            focus = DomainUtils.refetchNode(focus)

            if (it instanceof Name) {
                // TODO: DO NOT IGNORE THIS TODO
                // TODO: find if this name is already in the tree. If it is, check out the supernode and delete the link.
                Name n = DomainUtils.refetchName((Name) it)
                basicOperationsService.createDraftNode(focus, VersioningMethod.V, NodeInternalType.T, nslName: n)
            } else if (it instanceof Instance) {
                // TODO: DO NOT IGNORE THIS TODO
                // TODO: find if this name is already in the tree. If it is, check out the supernode and delete the link.
                Instance inst = DomainUtils.refetchInstance((Instance) it)
                basicOperationsService.createDraftNode(focus, VersioningMethod.V, NodeInternalType.T, nslInstance: inst)
            } else {
                log.debug('I don\'t know what this is. throw an exception')
                throw new IllegalArgumentException("dont know how to add a ${it.class} to a node")
            }
            log.debug('added element ok')

        }
        log.debug('added all elements ok')

        return [
                target  : focus,
                modified: []
        ]
    }

    def replaceDraftNodeWith(Node target, Node replacement) {
        log.debug('replaceDraftNodeWith')
        if (!target) throw new IllegalArgumentException("target may not be null")
        if (!replacement) throw new IllegalArgumentException("replacement may not be null")

        if (DomainUtils.isCheckedIn(target)) throw new IllegalArgumentException("target must be a draft node")
        if (target.root.node == target) throw new IllegalArgumentException("target must not be the root of a workspace")
        if (target.root.arrangementType != ArrangementType.U) throw new IllegalArgumentException("target must belong to a workspace")

        if (!DomainUtils.isCurrent(replacement)) throw new IllegalArgumentException("replacement must be current")
        if (DomainUtils.isEndNode(replacement)) throw new IllegalArgumentException("replacement must not be the end node")

        Link existingLink = DomainUtils.getDraftNodeSuperlink(target)

        Node supernode = existingLink.supernode
        VersioningMethod vm = existingLink.getVersioningMethod()
        int seq = existingLink.getLinkSeq()
        Uri type = DomainUtils.getLinkTypeUri(existingLink)

        if (existingLink == null) {
            throw new IllegalStateException("draft node ${target} has no superlink!")
        }

        basicOperationsService.deleteDraftTree(target)

        supernode = DomainUtils.refetchNode(supernode)
        replacement = DomainUtils.refetchNode(replacement)
        type = DomainUtils.refetchUri(type)

        return basicOperationsService.adoptNode(
                supernode,
                replacement,
                vm,
                seq: seq,
                linkType: type
        )
    }

    def removeLink(Arrangement ws, Link link) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!link) throw new IllegalArgumentException("null link")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")
        if (ws.node.checkedInAt) {
            throw new IllegalStateException("Workspace root nodes are never checked in")
        }

        int ct = queryService.countPaths(ws.node, link.supernode)
        if (ct != 1) throw new IllegalArgumentException("supernode must appear only once in the workspace")

        Node focus = link.supernode
        if (DomainUtils.isCheckedIn(focus)) {
            log.debug("about to checkout ${focus} in ${ws}")
            focus = basicOperationsService.checkoutNode(ws.node, focus)
            log.debug("checkout ok. New node is ${focus}")

            link = DomainUtils.refetchLink(link)
        } else {
            log.debug("${focus} is already checked out")
        }

        if (DomainUtils.isCheckedIn(link.subnode)) {
            basicOperationsService.deleteLink(focus, link.linkSeq)
        } else {
            basicOperationsService.deleteDraftTree(link.subnode)
        }

        focus = DomainUtils.refetchNode(focus)
        return focus
    }

    def emptyCheckout(Arrangement ws, Node target) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!target) throw new IllegalArgumentException("null target")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        if (DomainUtils.isCheckedIn(target)) {
            int paths = queryService.countPaths(ws.node, target)
            if (paths == 0) throw new IllegalArgumentException("target not in workspace")
            if (paths > 1) throw new IllegalArgumentException("target in workspace multiple places")
            target = basicOperationsService.checkoutNode(ws.node, target)
            target = DomainUtils.refetchNode(target)
        }

        return [
                target  : target,
                modified: [target]
        ]
    }


    def changeNodeName(Arrangement ws, Node target, Name name) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!target) throw new IllegalArgumentException("null target")
        if (!name) throw new IllegalArgumentException("null name")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        if (DomainUtils.isCheckedIn(target)) {
            int paths = queryService.countPaths(ws.node, target)
            if (paths == 0) throw new IllegalArgumentException("target not in workspace")
            if (paths > 1) throw new IllegalArgumentException("target in workspace multiple places")
            target = basicOperationsService.checkoutNode(ws.node, target)
            target = DomainUtils.refetchNode(target)
            name = DomainUtils.refetchName(name)
        }


        basicOperationsService.updateDraftNode(target, nslName: name, nslInstance: null)

        return [
                target  : target,
                modified: [target]
        ]
    }

    def addNodeSubname(Arrangement ws, Node target, Name name) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!target) throw new IllegalArgumentException("null target")
        if (!name) throw new IllegalArgumentException("null name")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        if (DomainUtils.isCheckedIn(target)) {
            int paths = queryService.countPaths(ws.node, target)
            if (paths == 0) throw new IllegalArgumentException("target not in workspace")
            if (paths > 1) throw new IllegalArgumentException("target in workspace multiple places")
            target = basicOperationsService.checkoutNode(ws.node, target)
            target = DomainUtils.refetchNode(target)
            name = DomainUtils.refetchName(name)
        }

        Node n = basicOperationsService.createDraftNode(target, VersioningMethod.V, NodeInternalType.T,
                // TODO: NODE TYPE
                nslName: name, nodeType: DomainUtils.uri('apc-voc', 'ApcConcept')
        )

        target = DomainUtils.refetchNode(target)

        return [
                target  : target,
                modified: [n]
        ]
    }

    def changeNodeInstance(Arrangement ws, Node target, Instance instance) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!target) throw new IllegalArgumentException("null target")
        if (!instance) throw new IllegalArgumentException("null instance")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        if (DomainUtils.isCheckedIn(target)) {
            int paths = queryService.countPaths(ws.node, target)
            if (paths == 0) throw new IllegalArgumentException("target not in workspace")
            if (paths > 1) throw new IllegalArgumentException("target in workspace multiple places")
            target = basicOperationsService.checkoutNode(ws.node, target)
            target = DomainUtils.refetchNode(target)
            instance = DomainUtils.refetchInstance(instance)
        }

        basicOperationsService.updateDraftNode(target, nslName: instance.name, nslInstance: instance)
        target = DomainUtils.refetchNode(target)

        return [
                target  : target,
                modified: [target]
        ]
    }

    def addNodeSubinstance(Arrangement ws, Node target, Instance instance) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!target) throw new IllegalArgumentException("null target")
        if (!instance) throw new IllegalArgumentException("null instance")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        if (DomainUtils.isCheckedIn(target)) {
            int paths = queryService.countPaths(ws.node, target)
            if (paths == 0) throw new IllegalArgumentException("target not in workspace")
            if (paths > 1) throw new IllegalArgumentException("target in workspace multiple places")
            target = basicOperationsService.checkoutNode(ws.node, target)
            target = DomainUtils.refetchNode(target)
            instance = DomainUtils.refetchInstance(instance)
        }

        Node n = basicOperationsService.createDraftNode(target, VersioningMethod.V, NodeInternalType.T,
                // TODO: NODE TYPE
                nslName: instance.name,
                nslInstance: instance,
                nodeType: DomainUtils.uri('apc-voc', 'ApcConcept')
        )

        target = DomainUtils.refetchNode(target)

        return [
                target  : target,
                modified: [n]
        ]
    }

    def setNodeType(Arrangement ws, Node target, Uri nodeType) {
        if (!ws) throw new IllegalArgumentException("null ws")
        if (!target) throw new IllegalArgumentException("null target")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        if (target.internalType != NodeInternalType.T) throw new IllegalArgumentException("not a taxonomic node")
        if (!nodeType) nodeType = DomainUtils.getDefaultNodeTypeUriFor(target.internalType)

        if (DomainUtils.isCheckedIn(target)) {
            int paths = queryService.countPaths(ws.node, target)
            if (paths == 0) throw new IllegalArgumentException("target not in workspace")
            if (paths > 1) throw new IllegalArgumentException("target in workspace multiple places")
            target = basicOperationsService.checkoutNode(ws.node, target)
            target = DomainUtils.refetchNode(target)
        }

        basicOperationsService.updateDraftNode(target, nodeType: nodeType)

        target = DomainUtils.refetchNode(target)

        return [
                target  : target,
                modified: [target]
        ]

    }

    def performCheckin(Node node) {
        if (!node) throw new IllegalArgumentException("null node")
        if (node.checkedInAt) throw new IllegalArgumentException("node not draft")
        if (!node.prev) throw new IllegalArgumentException("node not a checkout")
        if (node.prev.replacedAt) throw new IllegalArgumentException("target checkin is already replaced")

        performCheckinChecks(node)

        Event event = basicOperationsService.newEvent(node.namespace(), "checkin of ${node}")
        node = DomainUtils.refetchNode(node)
        basicOperationsService.createCopiesOfAllNonTreeNodes(event, node)
        node = DomainUtils.refetchNode(node)
        basicOperationsService.persistNode(event, node)
        node = DomainUtils.refetchNode(node)
        Map<Node, Node> v = versioningService.getCheckinVersioningMap(node.root, node.prev.root, node)
        versioningService.performVersioning(event, v, node.prev.root)
        node = DomainUtils.refetchNode(node)
        basicOperationsService.moveNodeSubtreeIntoArrangement(node.root, node.prev.root, node)
        node = DomainUtils.refetchNode(node)

        return [
                target  : node,
                modified: [node]
        ]
    }


    def performCheckinChecks(Node node) throws ServiceException {
        Message msg = Message.makeMsg(Msg.performCheckin, [node, node.name])
        msg.nested.addAll(getCheckinErrors(node))
        if (!msg.nested.empty) ServiceException.raise(msg)
    }

    //pmc TODO refactor: too long, repeated code - this is smelly as prawns in the sun
    Collection<Message> getCheckinErrors(Node node) {

        Node checkinTargetNode = DomainUtils.isCheckedIn(node) ? node : node.prev

        Collection<Message> messages = []

        sessionFactory_nsl.getCurrentSession().doWork(new Work() {

            void execute(Connection connection) throws SQLException {
                checkNameRankIssues(connection)
                checkForDiamonds(connection)
                // Check for duplicate names inside the checkin
                checkForDuplicateNamesInCheckin(connection)
                // Check for duplicate names between the checkin and the destination
                checkForDuplicateNamesOnDestination(connection)  //, node, checkin_target
            }

            private void checkForDuplicateNamesOnDestination(Connection connection) {
                String sql = '''
WITH RECURSIVE
nodes_being_checked_in AS (
    SELECT cast(NULL AS BIGINT) supernode_id, cast(? AS BIGINT) AS node_id
UNION ALL
    SELECT tree_link.supernode_id, tree_link.subnode_id AS node_id
    FROM nodes_being_checked_in
      JOIN tree_link ON nodes_being_checked_in.node_id = tree_link.supernode_id
        JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
),
links_being_replaced AS (
    SELECT tree_link.id AS link_id, tree_link.supernode_id, tree_link.subnode_id FROM
    tree_link
    WHERE
      tree_link.supernode_id = ?
      AND tree_link.subnode_id <> ? -- clip search
UNION ALL
    SELECT tree_link.id AS link_id, tree_link.supernode_id, tree_link.subnode_id FROM
    links_being_replaced JOIN tree_link ON links_being_replaced.subnode_id = tree_link.supernode_id
    JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE tree_link.subnode_id <> ? -- clip search
    AND subnode.internal_type <> 'V'
),
problems AS (
SELECT
  nodes_being_checked_in.supernode_id AS checkin_supernode_id,
  nodes_being_checked_in.node_id AS checkin_node_id,
  links_being_replaced.link_id AS being_enddated_link_id
FROM
nodes_being_checked_in
  JOIN tree_node checkin_node ON nodes_being_checked_in.node_id = checkin_node.id,
links_being_replaced
  JOIN tree_node replaced_node ON links_being_replaced.subnode_id = replaced_node.id
WHERE checkin_node.name_id = replaced_node.name_id
)
SELECT problems.* FROM problems
LEFT OUTER JOIN problems AS sup ON problems.checkin_supernode_id = sup.checkin_node_id
WHERE sup.checkin_node_id IS NULL
				'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                log.debug("nodes being checked in are the tree from ${node} ${node.root}")
                log.debug("nodes being replaced are the tree from ${checkinTargetNode.root.node}  ${checkinTargetNode.root}")
                log.debug("nodes being replaced will be clipped at  ${checkinTargetNode}")

                stmt.setLong(1, node.id)
                stmt.setLong(2, checkinTargetNode.root.node.id)
                stmt.setLong(3, checkinTargetNode.id)
                stmt.setLong(4, checkinTargetNode.id)


                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
                    Node checkin_supernode = Node.get(rs.getInt('checkin_supernode_id'))
                    Node n = Node.get(rs.getInt('checkin_node_id'))
                    Link l = Link.get(rs.getInt('being_enddated_link_id'))

                    Message submsg = Message.makeMsg(Msg.EMPTY, [
                            (node != checkinTargetNode) ?
                                    """
Checking in ${node.name.simpleName} from "${node.root.title}"
into ${checkinTargetNode.root.label}
will result in a duplicate placement of ${n.name.simpleName}, which is currently placed in ${
                                        checkinTargetNode.root.label
                                    }.
"""
                                    :
                                    """
${n.name.simpleName}
has a duplicate placement in ${node.root.label}.
""", checkin_supernode, n])

                    messages.add(submsg)

                    log.debug "CONFLICT - node ${n} with name ${n.name} and supernode ${checkin_supernode} has the same name as the subnode of link ${l} nodes ${l.supernode}->${l.subnode} names ${l.supernode.name}->${l.subnode.name} "
                }
            }

            private void checkForDuplicateNamesInCheckin(Connection connection) {
                String sql = '''
WITH RECURSIVE
nodes_being_checked_in AS (
    SELECT cast(NULL AS BIGINT) supernode_id, cast(? AS BIGINT) AS node_id
UNION ALL
    SELECT tree_link.supernode_id, tree_link.subnode_id AS node_id
    FROM nodes_being_checked_in
      JOIN tree_link ON nodes_being_checked_in.node_id = tree_link.supernode_id
        JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
),
problems AS (
SELECT
    name_id, count(*) AS ct
    FROM nodes_being_checked_in
    JOIN tree_node ON nodes_being_checked_in.node_id = tree_node.id
    GROUP BY name_id
    HAVING count(*) > 1
)
SELECT problems.* FROM problems
JOIN name ON problems.name_id = name.id
WHERE name.parent_id NOT IN (SELECT name_id FROM problems)

				'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                stmt.setLong(1, node.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
                    Name n = Name.get(rs.getInt('name_id'))
                    int ct = rs.getInt('ct')
                    Message submsg = Message.makeMsg(Msg.EMPTY, ["""
${node.name.simpleName} from "${node.root.title}"
contains ${n.simpleName} ${ct} times
""", n])
                    messages.add(submsg)
                }
            }

            private void checkForDiamonds(Connection connection) {
                String sql = '''
WITH RECURSIVE
nodes_being_checked_in AS (
    SELECT cast(NULL AS BIGINT) supernode_id, cast(? AS BIGINT) AS node_id
UNION ALL
    SELECT tree_link.supernode_id, tree_link.subnode_id AS node_id
    FROM nodes_being_checked_in
      JOIN tree_link ON nodes_being_checked_in.node_id = tree_link.supernode_id
        JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
),
problems AS (
SELECT
    node_id
FROM
nodes_being_checked_in
    GROUP BY node_id
    HAVING count(*) > 1
),
distinct_supers AS (
    SELECT DISTINCT *
    FROM nodes_being_checked_in
    WHERE node_id IN (SELECT node_id FROM problems)
),
one_distinct_super AS (
    SELECT node_id, max(supernode_id) AS supernode_id
    FROM distinct_supers
    GROUP BY node_id
    HAVING count(*) = 1
),
one_distinct_problem_super AS (
    SELECT * FROM one_distinct_super
    WHERE supernode_id IN (SELECT node_id FROM problems)
)
SELECT * FROM problems
WHERE node_id NOT IN (SELECT node_id FROM one_distinct_problem_super)

				'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                stmt.setLong(1, node.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
                    Node n = Node.get(rs.getInt('node_id'))
                    Message submsg = Message.makeMsg(Msg.EMPTY, ["""
${node.name.simpleName} from "${node.root.title}"
contains a node ${n.name.simpleName}
which is attached in multiple places (graph diamond).
""", n.name])

                    messages.add(submsg)
                }
            }

            private void checkNameRankIssues(Connection connection) {
                String rankIssuesSql = '''
WITH RECURSIVE
links_being_checked_in AS (
    SELECT tree_link.id link_id, tree_link.supernode_id, tree_link.subnode_id
    FROM tree_link
      JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE
      tree_link.supernode_id = ?
      AND subnode.internal_type <> 'V'
UNION ALL
    SELECT tree_link.id link_id, tree_link.supernode_id, tree_link.subnode_id
    FROM links_being_checked_in
      JOIN tree_link ON links_being_checked_in.subnode_id = tree_link.supernode_id
      JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
)SELECT
    l.link_id
FROM
  links_being_checked_in l
  JOIN tree_node supernode ON l.supernode_id = supernode.id
  JOIN name supername ON supernode.name_id = supername.id
  JOIN name_rank superrank ON supername.name_rank_id = superrank.id
  JOIN tree_node subnode ON l.subnode_id = subnode.id
  JOIN name subname ON subnode.name_id = subname.id
  JOIN name_rank subrank ON subname.name_rank_id = subrank.id
WHERE superrank.sort_order > subrank.sort_order
				'''

                PreparedStatement stmt = connection.prepareStatement(rankIssuesSql)

                stmt.setLong(1, node.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
                    Link l = Link.get(rs.getInt('link_id'))
                    Message submsg = Message.makeMsg(Msg.EMPTY, ["""
${l.subnode.name.simpleName} with rank ${l.subnode.name.nameRank.name}
is placed under
${l.supernode.name.simpleName} with rank ${l.supernode.name.nameRank.name}
.
""", l.subnode, l.supernode])
                    messages.add(submsg)
                }
            }
        })

        return messages
    }

    Collection<Message> getCheckinWarnings(Node node) {
        Node checkin_target = DomainUtils.isCheckedIn(node) ? node : node.prev

        Collection<Message> messages = []

        sessionFactory_nsl.getCurrentSession().doWork(new Work() {


            void execute(Connection connection) throws SQLException {

                checkNamePrefix(connection)

                checkForSynonymsOnTree(connection)

                checkAppearsElsewhere(connection) //, node, checkin_target

                checkReplacedSynonym(connection) //, node, checkin_target
            }

            private void checkReplacedSynonym(Connection connection) {
                String sql = '''
WITH RECURSIVE
nodes_being_checked_in AS (
    SELECT cast(NULL AS BIGINT) supernode_id, cast(? AS BIGINT) AS node_id
UNION ALL
    SELECT tree_link.supernode_id, tree_link.subnode_id AS node_id
    FROM nodes_being_checked_in
      JOIN tree_link ON nodes_being_checked_in.node_id = tree_link.supernode_id
        JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
),
links_being_replaced AS (
    SELECT tree_link.id AS link_id, tree_link.supernode_id, tree_link.subnode_id FROM
    tree_link
    WHERE
      tree_link.supernode_id = ?
      AND tree_link.subnode_id <> ? -- clip search
UNION ALL
    SELECT tree_link.id AS link_id, tree_link.supernode_id, tree_link.subnode_id FROM
    links_being_replaced JOIN tree_link ON links_being_replaced.subnode_id = tree_link.supernode_id
    JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE tree_link.subnode_id <> ? -- clip search
    AND subnode.internal_type <> 'V'
),
problems AS (
SELECT
  nodes_being_checked_in.supernode_id AS checkin_supernode_id,
  nodes_being_checked_in.node_id AS checkin_node_id,
  links_being_replaced.link_id AS being_enddated_link_id,
  replaced_synonym.id AS replaced_synonym_id
FROM
nodes_being_checked_in
  JOIN tree_node checkin_node ON nodes_being_checked_in.node_id = checkin_node.id
    ,
links_being_replaced
  JOIN tree_node replaced_node ON links_being_replaced.subnode_id = replaced_node.id
    JOIN instance replaced_synonym ON replaced_node.instance_id = replaced_synonym.cited_by_id
  JOIN instance_type ON replaced_synonym.instance_type_id = instance_type.id
WHERE replaced_synonym.name_id = checkin_node.name_id
    AND NOT instance_type.misapplied
    AND NOT instance_type.pro_parte
)
SELECT problems.* FROM problems
-- we do all synonymy issues, not just the top level
--LEFT OUTER JOIN problems AS sup ON problems.checkin_supernode_id = sup.checkin_node_id
--WHERE sup.checkin_node_id IS NULL
				'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                log.debug("nodes being checked in are the tree from ${node}")
                log.debug("nodes being replaced are the tree from ${checkin_target.root.node}")
                log.debug("nodes being replaced will be clipped at  ${checkin_target}")

                stmt.setLong(1, node.id)
                stmt.setLong(2, checkin_target.root.node.id)
                stmt.setLong(3, checkin_target.id)
                stmt.setLong(4, checkin_target.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
//                    Node checkin_supernode = Node.get(rs.getInt('checkin_supernode_id'))
                    Node checkin_node = Node.get(rs.getInt('checkin_node_id'))
                    Link replaced_link = Link.get(rs.getInt('being_enddated_link_id'))
                    Instance replaced_synonym = Instance.get(rs.getInt('replaced_synonym_id'))
                    Message submsg = Message.makeMsg(Msg.EMPTY, ["""
                    ${checkin_node.name.simpleName}
                    is ${replaced_synonym.instanceType.ofLabel} ${
                        replaced_link.subnode.name.simpleName
                    } in ${replaced_synonym.reference.citation},
                    which appears elsewhere in ${checkin_target.root.label}.
""", checkin_node, replaced_link.subnode])
                    messages.add(submsg)
                }
            }

            private void checkAppearsElsewhere(Connection connection) {
                String sql = '''
WITH RECURSIVE
nodes_being_checked_in AS (
    SELECT cast(NULL AS BIGINT) supernode_id, cast(? AS BIGINT) AS node_id
UNION ALL
    SELECT tree_link.supernode_id, tree_link.subnode_id AS node_id
    FROM nodes_being_checked_in
      JOIN tree_link ON nodes_being_checked_in.node_id = tree_link.supernode_id
        JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
),
links_being_replaced AS (
    SELECT tree_link.id AS link_id, tree_link.supernode_id, tree_link.subnode_id FROM
    tree_link
    WHERE
      tree_link.supernode_id = ?
      AND tree_link.subnode_id <> ? -- clip search
UNION ALL
    SELECT tree_link.id AS link_id, tree_link.supernode_id, tree_link.subnode_id FROM
    links_being_replaced JOIN tree_link ON links_being_replaced.subnode_id = tree_link.supernode_id
    JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE tree_link.subnode_id <> ? -- clip search
    AND subnode.internal_type <> 'V'
),
problems AS (
SELECT
  nodes_being_checked_in.supernode_id AS checkin_supernode_id,
  nodes_being_checked_in.node_id AS checkin_node_id,
  links_being_replaced.link_id AS being_enddated_link_id,
  checkin_synonym.id AS checkin_synonym_id
FROM
nodes_being_checked_in
  JOIN tree_node checkin_node ON nodes_being_checked_in.node_id = checkin_node.id
  --JOIN INSTANCE checkin_instance on checkin_node.instance_id = checkin_instance.id -- dont need this
  JOIN INSTANCE checkin_synonym ON checkin_node.instance_id = checkin_synonym.CITED_BY_ID
  JOIN instance_type ON checkin_synonym.instance_type_id = instance_type.id
    ,
links_being_replaced
  JOIN tree_node replaced_node ON links_being_replaced.subnode_id = replaced_node.id
WHERE checkin_synonym.name_id = replaced_node.name_id
    AND NOT instance_type.misapplied
    AND NOT instance_type.pro_parte
)
SELECT problems.* FROM problems
-- we do all synonymy issues, not just the top level
--LEFT OUTER JOIN problems AS sup ON problems.checkin_supernode_id = sup.checkin_node_id
--WHERE sup.checkin_node_id IS NULL
				'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                log.debug("nodes being checked in are the tree from ${node}")
                log.debug("nodes being replaced are the tree from ${checkin_target.root.node}")
                log.debug("nodes being replaced will be clipped at  ${checkin_target}")

                stmt.setLong(1, node.id)
                stmt.setLong(2, checkin_target.root.node.id)
                stmt.setLong(3, checkin_target.id)
                stmt.setLong(4, checkin_target.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
//                    Node checkin_supernode = Node.get(rs.getInt('checkin_supernode_id'))
                    Node checkin_node = Node.get(rs.getInt('checkin_node_id'))
//                    Link replaced_link = Link.get(rs.getInt('being_enddated_link_id'))
                    Instance checkin_synonym = Instance.get(rs.getInt('checkin_synonym_id'))
                    Message submsg = Message.makeMsg(Msg.EMPTY, ["""
                    ${checkin_node.name.simpleName} in ${checkin_node.instance.reference.citation}
                    has a ${checkin_synonym.instanceType.hasLabel} ${
                        checkin_synonym.name.simpleName
                    },
                    which appears elsewhere in ${checkin_target.root.label}.
""", checkin_node, checkin_synonym])
                    messages.add(submsg)
                }
            }

            private void checkForSynonymsOnTree(Connection connection) {
                String sql = '''
WITH RECURSIVE
nodes_being_checked_in AS (
    SELECT cast(NULL AS BIGINT) supernode_id, cast(? AS BIGINT) AS node_id
UNION ALL
    SELECT tree_link.supernode_id, tree_link.subnode_id AS node_id
    FROM nodes_being_checked_in
      JOIN tree_link ON nodes_being_checked_in.node_id = tree_link.supernode_id
        JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
),
problems AS (
SELECT
    a.node_id AS a_node_id, a_synonym.id AS a_synonym_id, b.node_id AS b_node_id
    FROM
    nodes_being_checked_in a
    JOIN tree_node a_node ON a.node_id = a_node.id
    JOIN instance a_synonym ON a_node.instance_id = a_synonym.cited_by_id
    JOIN instance_type ON a_synonym.instance_type_id = instance_type.id,
    nodes_being_checked_in b
    JOIN tree_node b_node ON b.node_id = b_node.id
    WHERE a_synonym.name_id = b_node.name_id
    AND a.node_id <> b.node_id
    AND NOT instance_type.misapplied
    AND NOT instance_type.pro_parte
)
SELECT * FROM problems
				'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                stmt.setLong(1, node.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
                    Node a_node = Node.get(rs.getInt('a_node_id'))
                    Instance a_synonym = Instance.get(rs.getInt('a_synonym_id'))
                    Node b_node = Node.get(rs.getInt('b_node_id'))
                    Message submsg = Message.makeMsg(Msg.EMPTY, ["""
${a_node.name.simpleName} in ${a_node.instance.reference.citation}
has a ${a_synonym.instanceType.hasLabel} ${a_synonym.name.simpleName}
which appears elsewhere in the check-in.
""", a_node, a_synonym, b_node])
                    messages.add(submsg)
                }
            }

            /**
             * produces warnings like "thing is placed under thingeae rather than thinga"
             * @param connection
             */
            private void checkNamePrefix(Connection connection) {
                String sql = '''
WITH RECURSIVE
    links_being_checked_in AS (
    SELECT
      tree_link.id link_id,
      tree_link.supernode_id,
      tree_link.subnode_id
    FROM tree_link
      JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE
      tree_link.supernode_id = ?
      AND subnode.internal_type <> 'V'
    UNION ALL
    SELECT
      tree_link.id link_id,
      tree_link.supernode_id,
      tree_link.subnode_id
    FROM links_being_checked_in
      JOIN tree_link ON links_being_checked_in.subnode_id = tree_link.supernode_id
      JOIN tree_node subnode ON tree_link.subnode_id = subnode.id
    WHERE subnode.internal_type <> 'V'
  ) SELECT l.link_id
    FROM
      name_rank genus,
      links_being_checked_in l
      JOIN tree_node supernode ON l.supernode_id = supernode.id
      JOIN tree_node subnode ON l.subnode_id = subnode.id
      JOIN name subname ON subnode.name_id = subname.id
      JOIN name_rank subname_rank ON subname.name_rank_id = subname_rank.id
      JOIN name_type subname_type ON subname.name_type_id = subname_type.id
      LEFT OUTER JOIN name subname_parent ON subname.parent_id = subname_parent.id
    WHERE
      genus.name = 'Genus'
      AND subnode.type_uri_id_part = 'ApcConcept'
      AND supernode.name_id IS NOT NULL
      AND (
        (
          NOT subname_type.hybrid
          AND subname.parent_id IS NOT NULL
          AND subname.parent_id <> supernode.name_id
        )
        OR
        (
          subname_type.hybrid
          AND subname_parent.parent_id IS NOT NULL
          AND subname_parent.parent_id <> supernode.name_id
        )
      )
      AND subname_rank.sort_order > genus.sort_order;
'''

                PreparedStatement stmt = connection.prepareStatement(sql)

                stmt.setLong(1, node.id)

                ResultSet rs = stmt.executeQuery()
                while (rs.next()) {
                    Link l = Link.get(rs.getInt('link_id'))

                    Name parentIs = l.supernode.name
                    Name parentShouldBe = l.subnode.name.nameType.hybrid ? l.subnode.name.parent.parent : l.subnode.name.parent

                    Closure display = (parentIs.simpleName == parentShouldBe.simpleName) ? { Name it -> it.fullName } : { Name it -> it.simpleName }

                    Message submsg = Message.makeMsg(Msg.EMPTY, [
                            """
${l.subnode.name.simpleName}
is placed under
${display(parentIs)}
rather than
${display(parentShouldBe)}
"""
                            , l.subnode, parentIs, parentShouldBe])
                    messages.add(submsg)
                }
            }
        })

        return messages
    }

    ////////////////////////////////////////////
    // these operations are the two operations required for the NSL-Editor. Yes, we are re-inventing the wheel here.

    String nodeSummary(Node n) {
        if (n == null) return 'null'
        else return "${n.id} ${n.name?.simpleName} ${n.checkedInAt ? "" : " (DRAFT)"}"
    }

    String linkSummary(Link l) {
        if (l == null) return 'null'
        else return "${nodeSummary(l.supernode)} -> [${l.id}] -> ${nodeSummary(l.subnode)}"
    }

    Message placeNameOnTree(Arrangement ws, Name name, Instance instance, Name parentName, Uri placementType) {
        mustHave(tree: ws, name: name, instance: instance, "Placement type": placementType) {
            if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

            try {

                Message error = Message.makeMsg(Msg.placeNameOnTree, [name, ws])

                /**
                 * To place a name on the tree, that name must not have any synonyms elsewhere on the tree,
                 * nor should it have synonyms that are elsewhere on the tree.
                 *
                 * If the name is placed under some other name, the the other name it is to be placed under
                 * must be of higher rank.
                 *
                 * If the name is being placed under a name that is is generic or below then,
                 * then the common part of the names must match unless the name being placed
                 * under it is an excluded name. NSL-464
                 *
                 * If the name is already on the tree as an accepted name, then this operation is a move of that node.
                 *
                 * If the name is already on the tree as an accepted name, and the parent name of that placement is the same
                 * as the required parent name, then this is simply an update of the node.
                 *
                 * https://www.anbg.gov.au/ibis25/display/NSL/Tree+Monitor+Functionality
                 *
                 */

                Link currentLink = queryService.findCurrentNslNameInTreeOrBaseTree(ws, name)

                log.debug("current link is ${linkSummary(currentLink)}")

                // CHECK FOR SYNONYMS
                // this query returns the relationship instance
                List<Instance> l = queryService.findSynonymsOfInstanceInTree(ws, instance)

                log.debug("findSynonymsOfInstanceInTree: ${l}")

                if (!l.isEmpty()) {
                    Message mm = Message.makeMsg(Msg.HAS_SYNONYM_ALREADY_IN_TREE)
                    error.nested.add(mm)
                    for (Instance i : l) {
                        if (!currentLink || i.cites.name != name)
                            mm.nested.add(Message.makeMsg(Msg.HAS_SYNONYM_ALREADY_IN_TREE_item, [i.cites, i.instanceType.ofLabel]))
                    }
                }

                // CHECK FOR SYNONYMS
                // this query returns the relationship instance
                l = queryService.findInstancesHavingSynonymInTree(ws, instance)

                log.debug("findInstancesHavingSynonymInTree: ${l}")

                if (!l.isEmpty()) {
                    Message mm = Message.makeMsg(Msg.IS_SYNONYM_OF_ALREADY_IN_TREE)
                    error.nested.add(mm)
                    for (Instance i : l) {
                        if (!currentLink || i.citedBy.name != name)
                            mm.nested.add(Message.makeMsg(Msg.IS_SYNONYM_OF_ALREADY_IN_TREE_item, [i.citedBy, i.instanceType.hasLabel]))
                    }
                }

                // CHECK FOR NAME COMPATIBILITY
                if (parentName) {
                    // If the name is placed under some other name, the the other name it is to be placed under
                    // must be of higher rank.
                    use(RankUtils) {
                        if (parentName.isRankedLowerThan(name)) {
                            error.nested.add(Message.makeMsg(Msg.CANNOT_PLACE_NAME_UNDER_HIGHER_RANK, [name.nameRank.abbrev, parentName.nameRank.abbrev]))
                        }
                    }

                    // If the name is being placed under a name that is is generic or below,
                    // then the common part of the names must match unless the name being placed under it is an excluded name.
                    if ("ApcConcept".equals(placementType.idPart) && !isNameCompatible(parentName, name)) {
                        error.nested.add(Message.makeMsg(Msg.NAME_CANNOT_BE_PLACED_UNDER_NAME, [parentName, name]))
                    }
                }

                Link newParentLink
                if (parentName != null) {
                    newParentLink = queryService.findCurrentNslNameInTreeOrBaseTree(ws, parentName)
                    if (newParentLink == null) {
                        error.nested.add(Message.makeMsg(Msg.THING_NOT_FOUND_IN_ARRANGEMENT, [ws, parentName, 'Name']))
                    }
                } else {
                    newParentLink = DomainUtils.getSingleSublink(ws.node)
                    if (newParentLink.typeUriIdPart != 'workspace-top-node') throw new IllegalStateException(newParentLink.typeUriIdPart)
                }

                if (!error.nested.isEmpty()) ServiceException.raise(error)

                // oh well. Let's write this dog. At ewqch step we may nned to re-search/re-fetch stuff

                // First, if the node needs to be updated, then check it out and update it.

                if (currentLink != null) {
                    log.debug("the name is currently in the tree at ${linkSummary(currentLink)}")
                    Node currentNode = currentLink.subnode
                    if (currentNode.name != name) throw new IllegalStateException()

                    if (currentNode.instance != instance || DomainUtils.getNodeTypeUri(currentNode) != placementType) {
                        log.debug("the node needs to be edited")
                        // needs to be possibly checked out and then saved.

                        if (DomainUtils.isCheckedIn(currentNode)) {
                            log.debug("checking out ${nodeSummary(currentNode)}")
                            currentNode = basicOperationsService.checkoutNode(ws.node, currentNode)
                            log.debug("checked out node is now ${nodeSummary(currentNode)}")
                            currentLink = DomainUtils.getDraftNodeSuperlink(currentNode)
                            log.debug("currentLink ${linkSummary(currentLink)}")
                        }

                        basicOperationsService.updateDraftNode(currentNode, nslInstance: instance, nodeType: placementType)
                    } else {
                        log.debug("the node does not need to be edited")
                    }
                }

                // at this point, the tree may have been disturbed and we need to re-fetch things - copy/paste the code.
                // Note that at this stage the current node may or may not be a draft node

                if (parentName != null) {
                    newParentLink = queryService.findCurrentNslNameInTreeOrBaseTree(ws, parentName)
                    if (newParentLink == null) {
                        error.nested.add(Message.makeMsg(Msg.THING_NOT_FOUND_IN_ARRANGEMENT, [ws, parentName, 'Name']))
                    }
                } else {
                    newParentLink = DomainUtils.getSingleSublink(ws.node)
                    if (newParentLink.typeUriIdPart != 'workspace-top-node') throw new IllegalStateException(newParentLink.typeUriIdPart)
                }


                log.debug("current link is now ${linkSummary(currentLink)}")
                log.debug("link to the new parent is now ${linkSummary(newParentLink)}")

                // next - the placement. If there is going to be a move, then the node's current parent must be checked out
                // and the destination parent must be checked out.

                // do we need to move at all?

                if (currentLink == null || currentLink.supernode != newParentLink.subnode) {
                    log.debug("the node needs to be moved")

                    // both the current and new parent need to be checked out, which they either or both may already be. If
                    // both of them need checking out, AND one of them is below the other, THEN the sequence becomes very critical.
                    // we check out the 'higher' one first and then the lower one, because checking out the lower one will
                    // also check out the higher one which will cause our reference to that higher one to get lost

                    if (currentLink != null && DomainUtils.isCheckedIn(currentLink.supernode) && DomainUtils.isCheckedIn(newParentLink.subnode)
                            && queryService.countPaths(newParentLink.subnode, currentLink.supernode) != 0) {
                        log.debug("both the current parent and the new parent may need to be checked out, in reverse order")
                        // the new parent is above the old parent, so we must check out the new parent first

                        if (DomainUtils.isCheckedIn(newParentLink.subnode)) {
                            log.debug("checking out new parent")
                            Node newNode = basicOperationsService.checkoutNode(ws.node, newParentLink.subnode)
                            newParentLink = DomainUtils.getDraftNodeSuperlink(newNode)
                        } else {
                            log.debug("new parent is already checked out")
                        }

                        if (currentLink != null && DomainUtils.isCheckedIn(currentLink.supernode)) {
                            log.debug("checking out old parent")
                            Node newNode = basicOperationsService.checkoutNode(ws.node, currentLink.supernode)
                            currentLink = Link.findBySupernodeAndLinkSeq(newNode, currentLink.linkSeq)
                        } else {
                            log.debug("no old parent, or old parent is already checked out")
                        }

                    } else {
                        if (currentLink != null && DomainUtils.isCheckedIn(currentLink.supernode)) {
                            Node newNode = basicOperationsService.checkoutNode(ws.node, currentLink.supernode)
                            currentLink = Link.findBySupernodeAndLinkSeq(newNode, currentLink.linkSeq)
                            log.debug("checking out old parent")
                        } else {
                            log.debug("no old parent, or old parent is already checked out")
                        }

                        if (DomainUtils.isCheckedIn(newParentLink.subnode)) {
                            log.debug("checking out new parent")
                            Node newNode = basicOperationsService.checkoutNode(ws.node, newParentLink.subnode)
                            newParentLink = DomainUtils.getDraftNodeSuperlink(newNode)
                        } else {
                            log.debug("new parent is already checked out")
                        }
                    }

                    // once the node's current parent and destination parent are both checked out, then the node is moved either as
                    // a draft node move or as an un-adopt/adopt sequence. Oh - or we have to create it, duh.

                    currentLink = DomainUtils.refetchLink(currentLink)
                    newParentLink = DomainUtils.refetchLink(newParentLink)

                    log.debug("currentLink ${linkSummary(currentLink)}")
                    log.debug("newParentLink ${linkSummary(newParentLink)}")

                    if (currentLink == null) {
                        log.debug("name is not in the tree. creating a new draft node")
                        basicOperationsService.createDraftNode(newParentLink.subnode, VersioningMethod.V, NodeInternalType.T,
                                nslName: name, nslInstance: instance, nodeType: placementType)
                    } else if (DomainUtils.isCheckedIn(currentLink.subnode)) {
                        log.debug("name is not checked out in the tree. Removing from old parent and adopting into the new one")
                        basicOperationsService.deleteLink(currentLink.supernode, currentLink.linkSeq)
                        basicOperationsService.adoptNode(newParentLink.subnode, currentLink.subnode, VersioningMethod.V)
                    } else {
                        log.debug("name checked out in the tree. Moving the draft node.")
                        basicOperationsService.simpleMoveDraftLink(currentLink, newParentLink.subnode)
                    }
                } else {
                    log.debug("node does not need to be moved")
                }

                return null

            }
            catch (Throwable t) {
                for (Throwable tt = t; tt != null; tt = tt.getCause()) {
                    log.error(tt.toString())
                    for (StackTraceElement e : tt.getStackTrace()) {
                        if (e.getClassName().startsWith("au.org.bio") && e.getLineNumber() >= 0) {
                            log.error("  " + e.toString())
                        }
                    }

                }

                throw t
            }
        } as Message
    }

    /**
     * If the name is being placed under a name that is is generic or below then,
     * then the common part of the names must match unless the name it is being placed under
     * it is an excluded name. NSL-464
     *
     * @param superName
     * @param subName
     * @return true if compatible
     */
    protected static Boolean isNameCompatible(Name superName, Name subName) {
        if (!superName || !subName) {
            throw new NullPointerException("Supername and subname cannot be null.")
        }

        use(RankUtils) {
            //sub name should always be below super name
            if (subName.isRankedHigherThan(superName)) {
                return false
            }
            //check only applies for sub genus sub names
            if (subName.nameAtRankOrHigher('Genus')) {
                return true
            }
            //can't place sub Genus name below name higher than genus
            if (superName.nameHigherThanRank('Genus')) {
                return false
            }
            // subspecies names should be placed below species
            if (subName.nameLowerThanRank('Species') &&
                    superName.nameHigherThanRank('Species')) {
                return false
            }

            //given the above the super name should be in the *name* parent path of the sub name
            //if the super name is a major rank
            Name majorSuperName = majorParentOf(superName)
            Name parent = subName.parent
            while (majorSuperName && majorSuperName != parent && !parent.isRankedHigherThan(majorSuperName)) {
                parent = parent.parent
            }
            return parent == majorSuperName //if equal we found it
        }
    }

    private static Name majorParentOf(Name name) {
        Name majorName = name
        while (majorName && !majorName.nameRank.major) {
            majorName = name.parent
        }
        return majorName.nameRank.major ? majorName : null
    }

    Message removeNameFromTree(Arrangement ws, Name name) {
        if (!ws) throw new IllegalArgumentException("null tree")
        if (!name) throw new IllegalArgumentException("null name")
        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")

        Message error = Message.makeMsg(Msg.removeNameFromTree, [name, ws])

        try {
            Link currentLink = queryService.findCurrentNslNameInTreeOrBaseTree(ws, name)
            if (!currentLink) {
                error.nested.add(Message.makeMsg(Msg.THING_NOT_FOUND_IN_ARRANGEMENT, [ws, name, "Name"]))
                ServiceException.raise(error)
            }

            if (currentLink.subnode.subLink.find { Link l -> l.subnode.internalType == NodeInternalType.T }) {
                error.nested.add(Message.makeMsg(Msg.NODE_HAS_SUBTAXA, [currentLink.subnode]))
                ServiceException.raise(error)
            }

            Node currentNode = currentLink.supernode

            if (DomainUtils.isCheckedIn(currentLink.supernode)) {
                currentNode = basicOperationsService.checkoutNode(ws.node, currentNode)
                currentLink = DomainUtils.refetchLink(currentLink)
            }

            // it's a little tricky, but this does cover all possibilities.
            // note that a non-draft node never has a draft node as a subnode

            if (DomainUtils.isCheckedIn(currentLink.subnode)) {
                basicOperationsService.deleteLink(currentNode, currentLink.linkSeq)
            } else {
                basicOperationsService.deleteDraftTree(currentLink.subnode)
            }
        }
        catch (ServiceException ex) {
            ex.printStackTrace()
            if (ex.msg == error)
                throw ex
            else {
                error.nested.add(ex.msg)
                ServiceException.raise(error)
            }
        }
        return null
    }

    void updateValue(Arrangement ws, Name name, ValueNodeUri valueUri, String value) {
        if (!ws) throw new IllegalArgumentException("null tree")
        if (!name) throw new IllegalArgumentException("null name")
        if (!valueUri) throw new IllegalArgumentException("null value uri")

        if (ws.arrangementType != ArrangementType.U) throw new IllegalArgumentException("ws is not a workspace")
        if (valueUri.isMultiValued) throw new IllegalArgumentException("${valueUri} is multivalued")

        Message error = Message.makeMsg(Msg.updateValue, [valueUri.title, name, ws])

        try {
            Link currentNameLink = queryService.findCurrentNslNameInTreeOrBaseTree(ws, name)
            if (!currentNameLink) {
                error.nested.add(Message.makeMsg(Msg.THING_NOT_FOUND_IN_ARRANGEMENT, [ws, name, "Name"]))
                ServiceException.raise(error)
            }

            Node currentNameNode = currentNameLink.subnode

            // find existing value node. There should only be one or none.

            Link currentValueLink = Link.where {
                supernode == currentNameNode &&
                        typeUriNsPart == valueUri.linkUriNsPart &&
                        typeUriIdPart == valueUri.linkUriIdPart &&
                        subnode.internalType == NodeInternalType.V
            }.find()

            if (!currentValueLink && !value) return
            if (currentValueLink
                    && currentValueLink.subnode.typeUriNsPart == valueUri.nodeUriNsPart
                    && currentValueLink.subnode.typeUriIdPart == valueUri.nodeUriIdPart
                    && currentValueLink.subnode.literal == value) {
                return
            }

            if (DomainUtils.isCheckedIn(currentNameNode)) {
                log.debug("checking out")
                currentNameNode = basicOperationsService.checkoutNode(ws.node, currentNameNode)
                currentNameNode = DomainUtils.refetchNode(currentNameNode)
                if (currentValueLink != null) {
                    currentValueLink = Link.findBySupernodeAndLinkSeq(currentNameNode, currentValueLink.linkSeq)
                }
            }

            // ok! now use the basic operations service to update/add values on the node

            if (currentValueLink && !DomainUtils.isCheckedIn(currentValueLink.subnode)) {
                // update the existing draft subnode
                if (value) {
                    basicOperationsService.updateDraftNode(currentValueLink.subnode,
                            nodeType: DomainUtils.getValueNodeTypeUri(valueUri),
                            literal: value
                    )
                    currentNameNode = DomainUtils.refetchNode(currentNameNode)
                    basicOperationsService.updateDraftNodeLink(currentNameNode, currentValueLink.linkSeq,
                            linkType: DomainUtils.getValueLinkTypeUri(valueUri))
                } else {
                    basicOperationsService.deleteDraftNode(currentValueLink.subnode)
                }
            } else {
                // unlink existing persistent subnode (if necessary),
                // crate new draft subnode (if necessary)
                if (currentValueLink) {
                    log.debug("deleting ")
                    basicOperationsService.deleteLink(currentNameNode, currentValueLink.linkSeq)
                    currentNameNode = DomainUtils.refetchNode(currentNameNode)
                }


                if (value) {
                    log.debug("creating new value ")
                    currentNameNode = DomainUtils.refetchNode(currentNameNode)
                    valueUri = DomainUtils.refetchValueNodeUri(valueUri)
                    basicOperationsService.createDraftNode(currentNameNode, VersioningMethod.F, NodeInternalType.V,
                            nodeType: DomainUtils.getValueNodeTypeUri(valueUri),
                            linkType: DomainUtils.getValueLinkTypeUri(valueUri),
                            literal: value
                    )
                }
            }
        }
        catch (ServiceException ex) {
            ex.printStackTrace()
            if (ex.msg == error)
                throw ex
            else {
                error.nested.add(ex.msg)
                ServiceException.raise(error)
            }
        }
    }

}
