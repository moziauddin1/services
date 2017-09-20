package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException

import static org.springframework.http.HttpStatus.*

class TreeVersionController implements WithTarget, ValidationUtils {

    def treeService
    def jsonRendererService
    def linkService

    static responseFormats = [
            deleteTreeVersion         : ['json', 'html'],
            setDefaultDraftTreeVersion: ['json', 'html'],
            editTreeVersion           : ['json', 'html'],
            validateTreeVersion       : ['json', 'html'],
            publishTreeVersion        : ['json', 'html'],
    ]

    static allowedMethods = [
            createTreeVersion         : ['PUT'],
            deleteTreeVersion         : ['DELETE'],
            setDefaultDraftTreeVersion: ['PUT'],
            editTreeVersion           : ['POST'],
            validateTreeVersion       : ['GET'],
            publishTreeVersion        : ['PUT'],
    ]
    static namespace = "api"


    def deleteTreeVersion(Long id) {
        TreeVersion treeVersion = TreeVersion.get(id)
        ResultObject results = requireTarget(treeVersion, "Tree version with id: $id")
        handleResults(results) {
            treeService.authorizeTreeOperation(treeVersion.tree)
            results.payload = treeService.deleteTreeVersion(treeVersion)
        }
    }

    def setDefaultDraftTreeVersion(Long version) {
        TreeVersion treeVersion = TreeVersion.get(version)
        ResultObject results = requireTarget(treeVersion, "Tree version $version")
        handleResults(results) {
            treeService.authorizeTreeOperation(treeVersion.tree)
            results.payload = treeService.setDefaultDraftVersion(treeVersion)
        }
    }

    def publishTreeVersion() {
        Map data = request.JSON as Map
        TreeVersion treeVersion = TreeVersion.get(data.id as Long)
        String logEntry = data.logEntry
        ResultObject results = requireTarget(treeVersion, "Tree version $treeVersion")
        handleResults(results) {
            String user = treeService.authorizeTreeOperation(treeVersion.tree)
            results.payload = treeService.publishTreeVersion(treeVersion, user.principal.toString(), logEntry)
        }
    }

    def editTreeVersion() {
        Map data = request.JSON as Map
        TreeVersion treeVersion = TreeVersion.get(data.id as Long)
        ResultObject results = requireTarget(treeVersion, "Tree version with id: $data.id")
        handleResults(results) {
            treeService.authorizeTreeOperation(treeVersion.tree)
            results.payload = treeService.editTreeVersion(treeVersion, data.draftName as String)
        }
    }

    def validateTreeVersion(Long version) {
        log.debug "validate tree version $version"
        TreeVersion treeVersion = TreeVersion.get(version)
        ResultObject results = requireTarget(treeVersion, "Tree version with id: $version")
        handleResults(results) {
            results.payload = treeService.validateTreeVersion(treeVersion)
        }
    }

    private handleResults(ResultObject results, Closure work) {
        if (results.ok) {
            try {
                work()
            } catch (ObjectExistsException exists) {
                results.ok = false
                results.fail(exists.message, CONFLICT)
            } catch (BadArgumentsException bad) {
                results.ok = false
                results.fail(bad.message, BAD_REQUEST)
            } catch (ValidationException invalid) {
                log.error("Validation failed ${params.action} : $invalid.message")
                results.ok = false
                results.fail(invalid.message, INTERNAL_SERVER_ERROR)
            } catch (AuthorizationException authException) {
                results.ok = false
                results.fail("You are not authorised to ${params.action}. ${authException.message}", FORBIDDEN)
                log.warn("You are not authorised to do this. $results.\n ${authException.message}")
            } catch (NotImplementedException notImplementedException) {
                results.ok = false
                results.fail(notImplementedException.message, NOT_IMPLEMENTED)
                log.error("$notImplementedException.message : $results")
            } catch (ObjectNotFoundException notFound) {
                results.ok = false
                results.fail(notFound.message, NOT_FOUND)
                log.error("$notFound.message : $results")
            } catch (PublishedVersionException published) {
                results.ok = false
                results.fail(published.message, BAD_REQUEST)
            }
        }
        serviceRespond(results)
    }

    private serviceRespond(ResultObject resultObject) {
        log.debug "result status is ${resultObject.status} $resultObject"
        //noinspection GroovyAssignabilityCheck
        respond(resultObject, [view: '/common/serviceResult', model: [data: resultObject], status: resultObject.remove('status')])
    }

}
