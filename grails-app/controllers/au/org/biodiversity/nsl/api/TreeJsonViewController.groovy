package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.tree.DomainUtils
import au.org.biodiversity.nsl.tree.QueryService
import grails.converters.JSON
import grails.validation.Validateable
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.hibernate.SessionFactory
import org.hibernate.jdbc.Work
import org.springframework.context.MessageSource
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

import java.security.Principal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class TreeJsonViewController {
    static datasource = 'nsl'

    SessionFactory sessionFactory_nsl

    GrailsApplication grailsApplication
    MessageSource messageSource
    TreeViewService treeViewService
    JsonRendererService jsonRendererService
    LinkService linkService
    SearchService searchService
    QueryService queryService

    def getObjectForLink(String uri) {
        if (uri.contains('/api/')) {
            uri = uri.substring(0, uri.indexOf('/api/'))
        }

        return linkService.getObjectForLink(uri)
    }

    def test() {
        def result = 'TreeJsonEditController'

        render result as JSON
    }

    def listClassifications(NamespaceParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        def result = Arrangement.findAll { arrangementType == ArrangementType.P && namespace.name == param.namespace }
                .sort { Arrangement a, Arrangement b -> a.label <=> b.label }
                .collect { linkService.getPreferredLinkForObject(it) }
        render result as JSON
    }

    def listWorkspaces(NamespaceParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        def result = Arrangement.findAll {
            arrangementType == ArrangementType.U &&
                    namespace.name == param.namespace
        }
        .sort { Arrangement a, Arrangement b -> a.title <=> b.title }
                .findAll { Arrangement it -> it.shared || it.owner == SecurityUtils.subject.principal }
                .collect { linkService.getPreferredLinkForObject(it) }
        render result as JSON
    }

    def listNamespaces() {
        def result = Namespace.findAll().sort { Namespace a, Namespace b -> a.name <=> b.name }
        render result as JSON
    }

    def permissions(String uri) {

        String principal = SecurityUtils.subject.principal as String;

        def userPermissions = [canCreateWorkspaces: SecurityUtils.subject.hasRole('treebuilder')];
        def uriPermissions = [:];

        if (!uri) {
            uriPermissions.uriType = null;
        } else {
            def o = getObjectForLink(uri)

            if (!o) {
                return render([
                        success  : false,
                        uri      : uri,
                        principal: principal,
                        error    : 'uri not found'
                ] as JSON);
            }


            Arrangement a = null;
            Node n = null;

            if (o instanceof Arrangement) {
                a = (Arrangement) o;
                uriPermissions.uriType = 'tree';
            } else if (o instanceof Node) {
                uriPermissions.uriType = 'node';
                n = (Node) o;
                a = n.root;
            } else {
                return render([
                        success  : false,
                        uri      : uri,
                        principal: principal,
                        error    : 'unrecognised type'
                ] as JSON);
            }

            if (n) {
                uriPermissions.isDraftNode = n.checkedInAt == null;
            }

            uriPermissions.isClassification = a.arrangementType == ArrangementType.P;
            uriPermissions.isWorkspace = a.arrangementType == ArrangementType.U;
            uriPermissions.canEdit = (uriPermissions.isWorkspace && principal == a.owner) || (uriPermissions.isClassification && SecurityUtils.subject.hasRole(a.label));
        }


        def result = [
                success        : true,
                uri            : uri,
                principal      : principal,
                userPermissions: userPermissions,
                uriPermissions : uriPermissions
        ]

        return render(result as JSON);

    }

    def getTreeHistory(UriParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        def o = getObjectForLink(param.uri)

        if (!o || !(o instanceof Arrangement)) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += [msg: 'Not found', status: 'warning', body: "No tree named <tt>${param.uri}</tt>"]

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 404) { result as JSON }
        }

        Arrangement a = o as Arrangement

        def result = [];
        for (Node n = DomainUtils.getSingleSubnode(a.node); n; n = n.prev) {
            result.add([date: n.checkedInAt?.timeStamp, note: n.checkedInAt?.note, uri: linkService.getPreferredLinkForObject(n)])
        }

        return render(result as JSON)
    }


    def findPath(FindPathParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        def root = getObjectForLink(param.root)
        def focus = getObjectForLink(param.focus)

        if (!root || !(root instanceof Node) || !focus || !(focus instanceof Node)) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += [msg: 'Not found', status: 'warning', body: "Nodes not found"]

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 404) { result as JSON }
        }

        List<Node> pathNodes = queryService.findPath(root, focus);

        if (pathNodes.size() > 0 && DomainUtils.getBoatreeUri('classification-node').equals(DomainUtils.getNodeTypeUri(pathNodes.get(0)))) {
            pathNodes.remove(0);
        }

        def result = pathNodes.collect { linkService.getPreferredLinkForObject(it) }
        return render(result as JSON)
    }

    def searchNamesRefs(final SearchNamesRefsParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        // TODO this stuff shouldn't be being jammed here, but I;m not sure where to jam it

        final List<?> result = [];
        sessionFactory_nsl.currentSession.doWork(new Work() {
            void execute(Connection connection) throws SQLException {

                String names_subq;

                if (param.includeSubnames) {
                    names_subq = """
                        WITH RECURSIVE nn AS (
                                SELECT id
                                FROM Name
                                WHERE simple_name LIKE ?
                            UNION ALL
                                SELECT name.id FROM nn JOIN name ON nn.id = name.parent_id
                        )
                        SELECT DISTINCT id
                        FROM nn
                    """
                } else {
                    names_subq = """
                        SELECT name.id
                        FROM name
                        WHERE simple_name LIKE ?
                    """
                }


                if (param.namesOnly || (!param.reference && !param.allReferences)) {
                    PreparedStatement ps = connection.prepareStatement("""
                        select nn.id
                        from (${names_subq}) as nn
                        join name on nn.id = name.id
                        join namespace on name.namespace_id = namespace.id
                        where namespace.name = ?
                        order by name.simple_name
                    """
                    )
                    ps.setString(1, param.name);
                    ps.setString(2, param.namespace);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Name.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();
                } else if (param.allReferences) {
                    PreparedStatement ps = connection.prepareStatement("""
                        select instance.id
                        from (${names_subq}) as nn
                        join name on name.id = nn.id
                        join instance on instance.name_id = nn.id
                        join namespace on instance.namespace_id = namespace.id
                        join reference on instance.reference_id = reference.id
                        where namespace.name = ?
                        ${param.primaryInstancesOnly ? ' and instance.cites_id is null ' : ''}
                        order by name.full_name, reference.citation
                        limit 200
                    """)
                    ps.setString(1, param.name);
                    ps.setString(2, param.namespace);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Instance.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();
                } else {
                    String references_subq

                    if (!param.includeSubreferences) {
                        references_subq = """
                            SELECT id FROM reference
                            WHERE reference.citation LIKE ?
                        """
                    } else {
                        references_subq = """
                            WITH RECURSIVE rr AS (
                                SELECT reference.id FROM reference
                                WHERE reference.citation LIKE ?
                                UNION ALL
                                SELECT reference.id
                                FROM rr JOIN reference ON rr.id = reference.parent_id
                            )
                            SELECT DISTINCT id FROM rr
                        """
                    }


                    String sql = """
                        select instance.id
                        from
                            (${names_subq}) as nn join name on nn.id = name.id
                            join instance on instance.name_id = nn.id
                            join (${references_subq}) rr on instance.reference_id = rr.id
                            join reference on instance.reference_id = reference.id
                            join namespace on instance.namespace_id = namespace.id
                        where
                            namespace.name = ?
                            ${param.primaryInstancesOnly ? ' and instance.cites_id is null ' : ''}
                        order by name.full_name, reference.citation
                        limit 200
                    """

                    PreparedStatement ps = connection.prepareStatement(sql);
                    ps.setString(1, param.name);
                    ps.setString(2, param.reference);
                    ps.setString(3, param.namespace);

                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Instance.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();
                }
            } // execute
        } // Work
        ); // doWork

        return render(result.collect { linkService.getPreferredLinkForObject(it) } as JSON)
    }

    def searchNames() {
        Map results = searchService.searchForName(params, 200)
        return render(results.names.collect { linkService.getPreferredLinkForObject(it) } as JSON)
    }

    def searchNamesInTree() {
        log.fatal "searchNamesInTree params ${params}"
        log.fatal "searchNamesInTree params tree_uri ${params.tree_uri}"

        def pp = [:]
        pp << params;

        pp.tree = getObjectForLink(params.tree_uri) as Arrangement;

        log.fatal "searchNamesInTree params tree ${pp.tree}"

        pp.SELECT = 'Nodes'

        Map results = searchService.searchForName(pp, 200)
        return render(results.nodes.collect { linkService.getPreferredLinkForObject(it) } as JSON)
    }

    def searchNamesInSubtree(NamesInSubtreeParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        def searchSubtree = getObjectForLink(param.searchSubtree)

        if (!searchSubtree || !(searchSubtree instanceof Node)) {
            def result = [
                    success: false,
                    msg    : [msg: 'Not found', status: 'warning', body: "Can't find node ${param.searchSubtree}"],
            ];

            return render(status: 404) { result as JSON }
        }

        List results = queryService.findNamesInSubtree(searchSubtree, param.searchText)

        return render([
                success: true,
                msg    : results ? [msg: "Found", status: "success", body: "Found ${results.size()} matching placements"] : [msg: "Not found", status: "success", body: "Name not found in selected subtree."],
                results: results.sort { r1, r2 ->
                    // I use a tilde because it is the last printable in ascii.
                    "${(r1.matchedInstance as Instance)?.name?.simpleName}~${(r1.node as Node)?.instance?.name?.simpleName}" <=> "${(r2.matchedInstance as Instance)?.name?.simpleName}~${(r2.node as Node)?.instance?.name?.simpleName}"
                } .collect { result -> [ node: linkService.getPreferredLinkForObject(result.node), matched: linkService.getPreferredLinkForObject(result.matchedInstance)] }
        ] as JSON)

    }

    def searchNamesDirectlyInSubtree(NamesInSubtreeParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
            msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, null)] }

            def result = [
                    success: false,
                    msg    : msg,
                    errors : param.errors,
            ];

            return render(status: 400) { result as JSON }
        }

        def searchSubtree = getObjectForLink(param.searchSubtree)

        if (!searchSubtree || !(searchSubtree instanceof Node)) {
            def result = [
                    success: false,
                    msg    : [msg: 'Not found', status: 'warning', body: "Can't find node ${param.searchSubtree}"],
            ];

            return render(status: 404) { result as JSON }
        }

        List results = queryService.findNamesDirectlyInSubtree(searchSubtree, param.searchText)
        int sz = results.size()

        return render([
                success: true,
                msg    : results ? [msg: "Found", status: "success", body: "Found ${results.size()} matching placements"] : [msg: "Not found", status: "success", body: "Name not found in selected subtree."],
                total  : sz,
                results: results.sort { r1, r2 ->
                    // I use a tilde because it is the last printable in ascii.
                    "${(r1.matchedInstance as Instance)?.name?.simpleName}~${(r1.node as Node)?.instance?.name?.simpleName}" <=> "${(r2.matchedInstance as Instance)?.name?.simpleName}~${(r2.node as Node)?.instance?.name?.simpleName}"
                }
                .subList(0, sz > 10 ? 10 : sz)
                .collect { result -> [
                        node: linkService.getPreferredLinkForObject(result.node),
                        simpleName: result.matchedInstance.name.simpleName
                ] }
        ] as JSON)

    }
}

@Validateable
class NamespaceParam {
    String namespace

    String toString() {
        return [
                namespace: namespace,
        ].toString()
    }

    static constraints = {
        namespace nullable: false
    }
}

@Validateable
class UriParam {
    String uri
    static constraints = {
        uri nullable: false
    }
}

@Validateable
class FindPathParam {
    String root
    String focus
    static constraints = {
        root nullable: false
        focus nullable: false
    }
}

@Validateable
class SearchNamesRefsParam {
    String namespace
    String name
    boolean includeSubnames
    String reference
    boolean namesOnly
    boolean allReferences
    boolean includeSubreferences
    boolean primaryInstancesOnly
    static constraints = {
        name nullable: false
        reference nullable: true
    }
}


@Validateable
class NamesInSubtreeParam {
    String searchSubtree
    String searchText

    static constraints = {
        searchSubtree nullable: true
        searchText nullable: true
    }

}