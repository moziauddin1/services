package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.converters.JSON
import org.apache.shiro.SecurityUtils
import org.codehaus.groovy.grails.commons.GrailsApplication

import java.security.Principal

/**
 * Created by ibis on 14/01/2016.
 */
class TreeJsonViewController {
    GrailsApplication grailsApplication
    TreeViewService treeViewService
    JsonRendererService jsonRendererService
    LinkService linkService

    def test() {
        def result =  'TreeJsonEditController'

        render result as JSON
    }

    def listClassifications() {
        def result = Arrangement.findAll { arrangementType == ArrangementType.P }
                .sort { Arrangement a, Arrangement b -> a.label <=> b.label }
                .collect { linkService.getPreferredLinkForObject(it)}
        render result as JSON
    }

    def listNamespaces() {
        def result = Namespace.findAll() .sort { Namespace a, Namespace b -> a.name <=> b.name }
        render result as JSON
    }
}
