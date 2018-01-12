package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

class TreeController implements WithTarget, ValidationUtils {

    def treeService
    def jsonRendererService
    def linkService

    static responseFormats = [
            createTree   : ['json', 'html'],
            editTree     : ['json', 'html'],
            copyTree     : ['json', 'html'],
            deleteTree   : ['json', 'html'],
            createVersion: ['json', 'html']
    ]

    static allowedMethods = [
            createTree   : ['PUT'],
            editTree     : ['POST'],
            copyTree     : ['PUT'],
            deleteTree   : ['DELETE', 'GET'],
            createVersion: ['PUT']
    ]

    def index() {
        [trees: Tree.list()]
    }

    def createTree() {
        withJsonData(request.JSON, false, ['treeName', 'descriptionHtml']) { ResultObject results, Map data ->
            String treeName = data.treeName
            String groupName = data.groupName
            Long referenceId = data.referenceId
            String descriptionHtml = data.descriptionHtml
            String linkToHomePage = data.linkToHomePage
            Boolean acceptedTree = data.acceptedTree

            String userName = treeService.authorizeTreeBuilder()
            results.payload = treeService.createNewTree(treeName, groupName ?: userName, referenceId, descriptionHtml, linkToHomePage, acceptedTree)
        }
    }

    def editTree() {
        withTree { ResultObject results, Tree tree, Map data ->
            treeService.authorizeTreeOperation(tree)
            results.payload = treeService.editTree(tree,
                    (String) data.treeName,
                    (String) data.groupName,
                    (Long) data.referenceId,
                    (String) data.descriptionHtml,
                    (String) data.linkToHomePage,
                    (Boolean) data.acceptedTree
            )
        }
    }

    def deleteTree(Long treeId) {
        Tree tree = Tree.get(treeId)
        ResultObject results = requireTarget(tree, "Tree with id: $treeId")
        handleResults(results) {
            treeService.authorizeTreeOperation(tree)
            treeService.deleteTree(tree)
        }
    }

    def copyTree() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

    /**
     * Creates a new draft tree version. If there is a currently published version it copies that versions elements to
     * this new version. If defaultVersion is set to true then the new version becomes the default draft version.
     * @return
     */
    def createVersion() {
        withJsonData(request.JSON, false, ['treeId', 'draftName']) { ResultObject results, Map data ->
            Long treeId = data.treeId
            Long fromVersionId = data.fromVersionId
            String draftName = data.draftName
            Boolean defaultVersion = data.defaultDraft

            Tree tree = Tree.get(treeId)
            if (tree) {
                treeService.authorizeTreeOperation(tree)

                TreeVersion fromVersion = TreeVersion.get(fromVersionId)
                if (defaultVersion) {
                    results.payload = treeService.createDefaultDraftVersion(tree, fromVersion, draftName)
                } else {
                    results.payload = treeService.createTreeVersion(tree, fromVersion, draftName)
                }
            } else {
                results.ok = false
                results.fail("Tree with id $treeId not found", NOT_FOUND)
            }
        }
    }

    private withJsonData(Object json, Boolean list, List<String> requiredKeys, Closure work) {
        ResultObject results = new ResultObject([action: params.action], jsonRendererService as JsonRendererService)
        results.ok = true
        if (!json) {
            results.ok = false
            results.fail("JSON paramerters not supplied. You must supply JSON parameters ${list ? 'as a list' : requiredKeys}.",
                    BAD_REQUEST)
            return serviceRespond(results)
        }
        if (list && !(json.class instanceof JSONArray)) {
            results.ok = false
            results.fail("JSON paramerters not supplied. You must supply JSON parameters as a list.", BAD_REQUEST)
            return serviceRespond(results)
        }
        if (list) {
            List data = RestCallService.convertJsonList(json as JSONArray)
            handleResults(results) {
                work(results, data)
            }
        } else {
            Map data = RestCallService.jsonObjectToMap(json as JSONObject)
            for (String key in requiredKeys) {
                if (!data[key]) {
                    results.ok = false
                    results.fail("$key not supplied. You must supply $key.", BAD_REQUEST)
                }
            }
            handleResults(results) {
                work(results, data)
            }
        }
    }

    private withTree(Closure work) {
        Map data = request.JSON as Map
        Tree tree = Tree.get(data.id as Long)
        ResultObject results = requireTarget(tree, "Tree with id: $data.id")
        handleResults(results) {
            work(results, tree, data)
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
                results.fail(published.message, CONFLICT)
                log.error("$published.message : $results")
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
