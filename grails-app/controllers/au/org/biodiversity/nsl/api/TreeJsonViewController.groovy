package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.tree.DomainUtils
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

        def result = Arrangement.findAll { arrangementType == ArrangementType.U && namespace.name == param.namespace }
                .sort { Arrangement a, Arrangement b -> a.title <=> b.title }
                .collect { linkService.getPreferredLinkForObject(it) }
        render result as JSON
    }

    def listNamespaces() {
        def result = Namespace.findAll().sort { Namespace a, Namespace b -> a.name <=> b.name }
        render result as JSON
    }

    def permissions(UriParam param) {
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

        def o = linkService.getObjectForLink(param.uri)

        def result = [:]
        render result as JSON

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

        def o = linkService.getObjectForLink(param.uri)

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

        def root = linkService.getObjectForLink(param.root)
        def focus = linkService.getObjectForLink(param.focus)

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

        List<Node> pathNodes = treeViewService.findPath(root, focus);

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
                if (param.namesOnly) {
                    PreparedStatement ps = connection.prepareStatement("""
                        SELECT id
                        FROM Name
                        WHERE full_name LIKE ?
                        ORDER BY full_name
                        LIMIT 200
                    """)
                    ps.setString(1, param.name);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Name.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();
                } else if (param.allReferences) {
                    PreparedStatement ps = connection.prepareStatement("""
                        select instance.id
                        from name
                        join instance on instance.name_id = name.id
                        where name.full_name like ?
                        ${param.primaryInstancesOnly ? ' and instance.cites_id is null ' : ''}
                        order by name.full_name
                        limit 200
                    """)
                    ps.setString(1, param.name);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Instance.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();
                } else if (!param.includeSubreferences) {
                    PreparedStatement ps = connection.prepareStatement("""
                    select instance.id
                    from name
                    join instance on instance.name_id = name.id
                    join reference on instance.reference_id = reference.id
                    where name.full_name like ?
                    and reference.citation like ?
                    ${param.primaryInstancesOnly ? ' and instance.cites_id is null ' : ''}
                    order by name.full_name, reference.citation
                    limit 200
                    """)
                    ps.setString(1, param.name);
                    ps.setString(2, param.reference);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Instance.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();
                } else {
                    PreparedStatement ps = connection.prepareStatement("""
                    select instance.id, name.full_name, reference.citation, reference.id, reference.parent_id
                    from name
                    join instance on instance.name_id = name.id
                    join reference on instance.reference_id = reference.id
                    join (
                        with recursive ref as (
                            select id from reference where citation like ?
                            union all
                            select reference.id from ref join reference on ref.id = reference.parent_id
                        )
                        select distinct id from ref
                    ) as distinctref on reference.id = distinctref.id
                    where name.full_name like ?
                    order by name.full_name, reference.citation
                    limit 200
                    """)
                    ps.setString(1, param.reference);
                    ps.setString(2, param.name);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result.add(Instance.get(rs.getLong(1)));
                    }
                    rs.close();
                    ps.close();

                }
            }
        }
        );

        return render(result.collect { linkService.getPreferredLinkForObject(it) } as JSON)
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
    String name
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
