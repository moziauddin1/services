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

    def test() {
        def result =  [ ok: true, content: [msg: 'TreeJsonEditController'], messages: [] ]

        result << sessionData()

        render result as JSON
    }

    def listClassifications() {
        def result = [
            ok: true,
            content: Arrangement.findAll { arrangementType == ArrangementType.P } .sort { Arrangement a, Arrangement b -> a.label <=> b.label },
            messages : [],
        ]

        result << sessionData()

        render result as JSON
    }

    def sessionData() {
        return [
            session: [
                authenticated: SecurityUtils.subject.isAuthenticated(),
                remembered: SecurityUtils.subject.isRemembered(),
                principal: SecurityUtils.subject?.getPrincipal(),
            ]
        ]
    }
}
