package au.org.biodiversity.nsl.api

import grails.converters.JSON
import grails.converters.XML
import org.apache.shiro.authz.AuthorizationException
import org.apache.shiro.authz.UnauthenticatedException
import org.codehaus.groovy.grails.web.errors.ExceptionUtils
import org.codehaus.groovy.grails.web.util.WebUtils
import org.springframework.http.HttpStatus

class ErrorController {

    def grailsApplication
//    static responseFormats = ['html', 'json', 'xml']

    def index() {
        def e = request.exception.cause
        def err = e
        def status = request.getAttribute('javax.servlet.error.status_code') as int

        log.debug "Error controller: error is $err"

        while (e && !(e instanceof AuthorizationException)) {
            e = e.cause
        }

        // Redirect to the 'unauthorized' page if the cause was an
        // AuthorizationException.
        if (e instanceof AuthorizationException) {
            if (e instanceof UnauthenticatedException) {
                status = HttpStatus.UNAUTHORIZED.value()
            } else {
                status = HttpStatus.FORBIDDEN.value()
            }
        }

        request.setAttribute('javax.servlet.error.status_code', status)
        response.status = status

        withFormat {
            html {
                if (status == HttpStatus.UNAUTHORIZED.value()) {
                    redirect(controller: 'auth',
                            action: 'login',
                            params: [targetUri: request.forwardURI - request.contextPath])
                } else {
                    render(view: "/error", model: [exception: err])
                }
            }
            json {
                def response = jsonError(e, status)
                render response as JSON
            }
            xml {
                def response = jsonError(e, status)
                render response as XML
            }
        }
    }

    private Map jsonError(Exception exception, int status) {

        Map errorMap = [:]
        int statusCode = status
        errorMap.status = prettyPrintStatus(statusCode)
        errorMap.uri = (WebUtils.getForwardURI(request) ?: request.getAttribute('javax.servlet.error.request_uri'))

        if (exception) {
            def root = ExceptionUtils.getRootCause(exception)
            errorMap.exception = root?.getClass()?.name ?: exception.getClass().name
            errorMap.reason = exception.message

            if (root != null && root != exception && root.message != exception.message) {
                errorMap.CausedBy = root.message
            }
        }
        return errorMap
    }

    private static String prettyPrintStatus(int statusCode) {
        String httpStatusReason = HttpStatus.valueOf(statusCode).getReasonPhrase()
        "$statusCode: ${httpStatusReason}"
    }

}
