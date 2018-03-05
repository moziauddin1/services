package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException

import static org.springframework.http.HttpStatus.*

class TreeVersionController implements WithTarget {

    def treeService
    def treeReportService
    def jsonRendererService
    def linkService

    static responseFormats = [
            delete                    : ['json', 'html'],
            setDefaultDraftTreeVersion: ['json', 'html'],
            edit                      : ['json', 'html'],
            validate                  : ['json', 'html'],
            publish                   : ['json', 'html'],
            diff                      : ['json', 'html'],
    ]

    static allowedMethods = [
            createTreeVersion         : ['PUT'],
            delete                    : ['DELETE', 'POST'],
            setDefaultDraftTreeVersion: ['PUT'],
            editTreeVersion           : ['POST'],
            validate                  : ['GET'],
            publish                   : ['PUT'],
            diff                      : ['GET']
    ]

    def delete(Long id) {
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

    def publish() {
        Map data = request.JSON as Map
        Long versionId = (data.versionId ?: null) as Long
        String logEntry = data.logEntry

        TreeVersion treeVersion = TreeVersion.get(versionId)
        ResultObject results = requireTarget(treeVersion, "Tree version with id: $versionId")
        handleResults(results) {
            String userName = treeService.authorizeTreeOperation(treeVersion.tree)
            Boolean createNewDraft = treeVersion == treeVersion.tree.defaultDraftTreeVersion
            results.payload = treeService.publishTreeVersion(treeVersion, userName, logEntry)
            if (createNewDraft) {
                results.autocreate = true
                String draftName = treeVersion.draftName
                treeService.bgCreateDefaultDraftVersion(treeVersion.tree, treeVersion, draftName, userName, "Default draft.")
            }
        }
    }

    def edit() {
        Map data = request.JSON as Map
        Long versionId = (data.versionId ?: null) as Long
        String draftName = data.draftName
        Boolean defaultVersion = data.defaultDraft

        TreeVersion treeVersion = TreeVersion.get(versionId)
        ResultObject results = requireTarget(treeVersion, "Tree version with id: $versionId")
        handleResults(results) {
            treeService.authorizeTreeOperation(treeVersion.tree)
            results.payload = treeService.editTreeVersion(treeVersion, draftName)
            if (defaultVersion) {
                treeService.setDefaultDraftVersion(treeVersion)
            }
        }
    }

    def validate(Long version) {
        log.debug "validate tree version $version"
        TreeVersion treeVersion = TreeVersion.get(version)
        ResultObject results = requireTarget(treeVersion, "Tree version with id: $version")
        handleResults(results) {
            results.payload = treeReportService.validateTreeVersion(treeVersion)
        }
    }

    def diff(Long v1, Long v2, Boolean embed) {
        ResultObject results = require('Version 1 ID': v1, 'Version 2 ID': v2)

        handleResults(results, { diffRespond(results, embed) }) {
            TreeVersion first = TreeVersion.get(v1)
            if (!first) {
                throw new ObjectNotFoundException("Version $v1, not found.")
            }
            TreeVersion second = TreeVersion.get(v2)
            if (!second) {
                throw new ObjectNotFoundException("Version $v2, not found.")
            }
            results.payload = treeReportService.diffReport(first, second)
        }
    }

    private diffRespond(ResultObject resultObject, Boolean embed) {
        log.debug "result status is ${resultObject.status} $resultObject"
        if (embed) {
            //noinspection GroovyAssignabilityCheck
            respond(resultObject, [view: '_diffContent', model: [data: resultObject], status: resultObject.remove('status')])
        } else {
            //noinspection GroovyAssignabilityCheck
            respond(resultObject, [view: 'diff', model: [data: resultObject], status: resultObject.remove('status')])
        }
    }

    private handleResults(ResultObject results, Closure response, Closure work) {
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
                results.fail(published.message, CONFLICT)
                log.error("$published.message : $results")
            }
        }
        response()
    }

    private handleResults(ResultObject results, Closure work) {
        handleResults(results, { serviceRespond(results) }, work)
    }

    private serviceRespond(ResultObject resultObject) {
        log.debug "result status is ${resultObject.status} $resultObject"
        //noinspection GroovyAssignabilityCheck
        respond(resultObject, [view: '/common/serviceResult', model: [data: resultObject], status: resultObject.remove('status')])
    }
}
