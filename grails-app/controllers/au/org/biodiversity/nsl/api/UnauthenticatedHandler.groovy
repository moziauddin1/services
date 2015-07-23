package au.org.biodiversity.nsl.api

import org.apache.shiro.authz.UnauthenticatedException
import org.codehaus.groovy.grails.web.errors.ExceptionUtils
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.UNAUTHORIZED

/**
 * User: pmcneil
 * Date: 17/06/15
 *
 */
trait UnauthenticatedHandler {

    def unauthenticatedException(final UnauthenticatedException exception) {
        ResultObject error = errorResponse(exception, UNAUTHORIZED)
        withFormat {
            html {
                redirect(controller: 'auth',
                        action: 'login',
                        params: [targetUri: request.forwardURI - request.contextPath])
            }
            '*' {
                respond(error, status: UNAUTHORIZED)
            }
        }
    }

    private ResultObject errorResponse(Exception exception, HttpStatus status) {

        Map errorMap = [:]
        errorMap.status = "${status.value()}: ${status.reasonPhrase}"
        errorMap.uri = (WebUtils.getForwardURI(request) ?: request.getAttribute('javax.servlet.error.request_uri'))

        if (exception) {
            def root = ExceptionUtils.getRootCause(exception)
            errorMap.exception = root?.getClass()?.name ?: exception.getClass().name
            errorMap.reason = exception.message

            if (root != null && root != exception && root.message != exception.message) {
                errorMap.CausedBy = root.message
            }
        }
        return new ResultObject(errorMap)
    }

}
