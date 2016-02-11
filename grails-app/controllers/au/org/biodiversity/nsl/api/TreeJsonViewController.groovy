package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.converters.JSON
import grails.validation.Validateable
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.context.MessageSource
import org.springframework.validation.FieldError

import java.security.Principal

/**
 * Created by ibis on 14/01/2016.
 */
class TreeJsonViewController {
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

            msg += param.errors.globalErrors.collect { Error it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
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

            msg += param.errors.globalErrors.collect { Error it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
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

    def permissions(PermissionsParam param) {
        if (!param.validate()) {
            def msg = [];

            msg += param.errors.globalErrors.collect { Error it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, null)] }
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
class PermissionsParam {
    String uri
    static constraints = {
        uri nullable: false
    }
}

