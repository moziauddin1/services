/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl

import grails.converters.JSON
import grails.converters.XML
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.web.util.SavedRequest
import org.apache.shiro.web.util.WebUtils
import org.springframework.http.HttpStatus

class AuthController {
    def shiroSecurityManager
    def grailsApplication

    def index = { redirect(action: "login", params: params) }

    def login = {
        return [username: params.username, rememberMe: (params.rememberMe != null), targetUri: params.targetUri]
    }

    def signIn = {
        def authToken = new UsernamePasswordToken(params.username, params.password as String)

        // Support for "remember me"
        if (params.rememberMe) {
            authToken.rememberMe = true
        }

        // If a controller redirected to this page, redirect back
        // to it. Otherwise redirect to the root URI.
        String rootURI = grailsApplication.config.grails.serverURL
        def targetUri = params.targetUri ?: rootURI

        // Handle requests saved by Shiro filters.
        SavedRequest savedRequest = WebUtils.getSavedRequest(request)
        if (savedRequest) {
            targetUri = savedRequest.requestURI - request.contextPath
            if (savedRequest.queryString) targetUri = targetUri + '?' + savedRequest.queryString
        }

        try {
            // Perform the actual login. An AuthenticationException
            // will be thrown if the username is unrecognised or the
            // password is incorrect.
            SecurityUtils.subject.login(authToken)

            log.info "Redirecting to '${targetUri}'."
            redirect(uri: targetUri)
        }
        catch (AuthenticationException ex) {
            // Authentication failed, so display the appropriate message
            // on the login page.
            log.info "Authentication failure for user '${params.username}'."
            flash.message = message(code: "login.failed")

            // Keep the username and "remember me" setting so that the
            // user doesn't have to enter them again.
            def m = [username: params.username]
            if (params.rememberMe) {
                m["rememberMe"] = true
            }

            // Remember the target URI too.
            if (params.targetUri) {
                m["targetUri"] = params.targetUri
            }

            // Now redirect back to the login page.
            redirect(action: "login", params: m)
        }
    }

    def signInJson = {
        def authToken = new UsernamePasswordToken(params.username, params.password as String)
        try {
            SecurityUtils.subject.login(authToken)

            JsonToken jsonToken = new JsonToken( SecurityUtils.subject)

            def result = [ success: true, principal: SecurityUtils.subject?.principal, jwt: jsonToken.getCredentials() ]
            render result as JSON
        }
        catch (AuthenticationException ex) {
            response.setStatus(401)
            def result = [ success: false, principal: null ]
            render result as JSON
        }
    }

    def signOutJson = {
        SecurityUtils.subject?.logout()
        def result = [ success: true, principal: null ] as JSON
        render result
    }

    def getInfoJson = {
        def result = [ success: true, principal: SecurityUtils.subject?.principal, jwt: SecurityUtils.subject ? new JsonToken( SecurityUtils.subject).getCredentials() : null ] as JSON
        render result
    }


    def signOut = {
        // Log the user out of the application.
        SecurityUtils.subject?.logout()
        webRequest.getCurrentRequest().session = null

        // For now, redirect back to the home page.
        redirect(controller: 'search')
    }

    def unauthorized = {
        withFormat {
            html {
                render(status: 401, text: "You do not have permission to do that.")
            }
            json {
                def error = mapError()
                render(contentType: "application/json", status: 401){
                    error
                }
            }
            xml {
                def error = mapError()
                response.status = 401
                render error as XML
            }
        }
    }

    private Map mapError() {
        Map errorMap = [:]
        errorMap.status = prettyPrintStatus(401)
        errorMap.uri = (org.codehaus.groovy.grails.web.util.WebUtils.getForwardURI(request) ?: request.getAttribute('javax.servlet.error.request_uri'))
        errorMap.reason = "You do not have permission."
        return errorMap
    }

    private static String prettyPrintStatus(int statusCode) {
        String httpStatusReason = HttpStatus.valueOf(statusCode).getReasonPhrase()
        "$statusCode: ${httpStatusReason}"
    }

}
