package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Instance
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED

@Transactional
class InstanceController implements UnauthenticatedHandler, WithTarget {

    def jsonRendererService
    def instanceService

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = ['json', 'xml', 'html']

    static allowedMethods = [
            delete: ["GET", "DELETE"]
    ]
    static namespace = "api"

    def index() {}

    @Timed()
    def delete(Instance instance, String reason) {
        withTarget(instance) { ResultObject result ->
            if (request.method == 'DELETE') {
                SecurityUtils.subject.checkRole('admin')
                result << instanceService.deleteInstance(instance, reason)
            } else if (request.method == 'GET') {
                result << instanceService.canDelete(instance, 'dummy reason')
            } else {
                result.status = METHOD_NOT_ALLOWED
            }
        }
    }

}
