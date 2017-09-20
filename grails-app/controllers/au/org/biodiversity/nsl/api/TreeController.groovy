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
            createTree                : ['json', 'html'],
            editTree                  : ['json', 'html'],
            copyTree                  : ['json', 'html'],
            deleteTree                : ['json', 'html'],
            createTreeVersion         : ['json', 'html'],
            placeTaxon                : ['json', 'html'],
            moveTaxon                 : ['json', 'html'],
            removeTaxon               : ['json', 'html'],
            editTaxonProfile          : ['json', 'html'],
            editTaxonStatus           : ['json', 'html']
    ]

    static allowedMethods = [
            createTree                : ['PUT'],
            editTree                  : ['POST'],
            copyTree                  : ['PUT'],
            deleteTree                : ['DELETE'],
            createTreeVersion         : ['PUT'],
            placeTaxon                : ['PUT'],
            moveTaxon                 : ['PUT'],
            removeTaxon               : ['DELETE'],
            editTaxonProfile          : ['POST'],
            editTaxonStatus           : ['POST']
    ]
    static namespace = "api"

    def index() {
        [trees: Tree.list()]
    }

    def createTree(String treeName, String groupName, Long refId) {
        ResultObject results = require(['Tree Name': treeName, 'Group Name': groupName])
        handleResults(results) {
            results.payload = treeService.createNewTree(treeName, groupName, refId)
        }
    }

    def editTree() {
        withTree { ResultObject results, Tree tree, Map data ->
            treeService.authorizeTreeOperation(tree)
            results.payload = treeService.editTree(tree, (String) data.name, (String) data.groupName, (Long) data.referenceId)
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
    def createTreeVersion(Long id, Long fromVersionId, String draftName, Boolean defaultVersion) {
        Tree tree = Tree.get(id)
        TreeVersion fromVersion = TreeVersion.get(fromVersionId)
        ResultObject results = requireTarget(tree, "Tree with id: $id")
        handleResults(results) {
            treeService.authorizeTreeOperation(tree)
            if (defaultVersion) {
                results.payload = treeService.createDefaultDraftVersion(tree, fromVersion, draftName)
            } else {
                results.payload = treeService.createTreeVersion(tree, fromVersion, draftName)
            }
        }
    }

    /**
     *
     * @param parentTaxonUri the URI or link of the parent treeVersionElement
     * @param instanceUri
     * @param excluded
     * @return
     */
    def placeTaxon(String parentTaxonUri, String instanceUri, Boolean excluded) {

        ResultObject results = requireTarget(parentTaxonUri, "Parent taxon $parentTaxonUri")
        TreeVersionElement treeVersionElement = TreeVersionElement.get(parentTaxonUri)

        handleResults(results) {
            String userName = treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
            results.payload = treeService.placeTaxonUri(treeVersionElement, instanceUri, excluded, userName)
        }
    }

    def moveTaxon(String taxonUri, String newParentTaxonUri) {
        TreeVersionElement childElement = TreeVersionElement.get(taxonUri)
        TreeVersionElement parentElement = TreeVersionElement.get(newParentTaxonUri)
        ResultObject results = require(['Child taxon': childElement, 'New parent taxon': parentElement])
        handleResults(results) {
            String userName = treeService.authorizeTreeOperation(parentElement.treeVersion.tree)
            results.payload = treeService.moveTaxon(childElement, parentElement, userName)
        }
    }

    def removeTaxon(String taxonUri) {
        TreeVersionElement treeVersionElement = TreeVersionElement.get(taxonUri)
        ResultObject results = requireTarget(treeVersionElement, "Taxon $taxonUri")
        handleResults(results) {
            treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
            int count = treeService.removeTreeVersionElement(treeVersionElement)
            results.payload = [count: count, message: "$count taxon removed, starting from $taxonUri"]
        }
    }

    def editTaxonProfile() {
        withJsonData(request.JSON, false, ['taxonUri', 'profile']) { ResultObject results, Map data ->
            TreeVersionElement treeVersionElement = TreeVersionElement.get(data.taxonUri as String)
            if (!treeVersionElement) {
                throw new ObjectNotFoundException("Can't find taxon with URI $data.taxonUri")
            }
            String userName = treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
            results.payload = treeService.editProfile(treeVersionElement, data.profile as Map, userName)
        }
    }

    def editTaxonStatus() {
        withJsonData(request.JSON, false, ['taxonUri', 'excluded']) { ResultObject results, Map data ->
            TreeVersionElement treeVersionElement = TreeVersionElement.get(data.taxonUri as String)
            if (!treeVersionElement) {
                throw new ObjectNotFoundException("Can't find taxon with URI $data.taxonUri")
            }
            String userName = treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
            results.payload = treeService.editExcluded(treeVersionElement, data.excluded as Map, userName)
        }
    }

    def elementDataFromInstance(String instanceUri) {
        ResultObject results = require('Instance URI': instanceUri)

        handleResults(results) {
            TaxonData taxonData = treeService.getInstanceDataByUri(instanceUri)
            if (taxonData) {
                results.payload = taxonData.asMap()
            } else {
                throw new ObjectNotFoundException("Instance with URI $instanceUri, is not in this shard.")
            }
        }
    }

    private withJsonData(Object json, Boolean list, List<String> requiredKeys, Closure work) {
        ResultObject results = new ResultObject([action: params.action], jsonRendererService as JsonRendererService)
        results.ok = true
        if (!json) {
            results.ok = false
            results.status = BAD_REQUEST
            results.error("JSON paramerters not supplied. You must supply JSON parameters ${list ? 'as a list' : required.keySet()}.")
        }
        if (list && !(json.class instanceof JSONArray)) {
            results.ok = false
            results.status = BAD_REQUEST
            results.error("JSON paramerters not supplied. You must supply JSON parameters as a list.")
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
                    results.error("$key not supplied. You must supply $key.")
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
