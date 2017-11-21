package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

class TreeElementController implements WithTarget, ValidationUtils {

    def treeService
    def jsonRendererService
    def linkService

    static responseFormats = [
            placeTaxon      : ['json', 'html'],
            moveTaxon       : ['json', 'html'],
            removeTaxon     : ['json', 'html'],
            editTaxonProfile: ['json', 'html'],
            editTaxonStatus : ['json', 'html']
    ]

    static allowedMethods = [
            placeTaxon      : ['PUT'],
            moveTaxon       : ['PUT'],
            removeTaxon     : ['DELETE', 'POST'],
            editTaxonProfile: ['POST'],
            editTaxonStatus : ['POST']
    ]
    static namespace = "api"

    /**
     *
     * @param parentTaxonUri the URI or link of the parent treeVersionElement
     * @param instanceUri
     * @param excluded
     * @return
     */
    def placeElement() {
        withJsonData(request.JSON, false, ['parentElementUri', 'instanceUri', 'excluded']) { ResultObject results, Map data ->

            String parentTaxonUri = data.parentElementUri
            String instanceUri = data.instanceUri
            Boolean excluded = data.excluded
            TreeVersionElement treeVersionElement = TreeVersionElement.get(parentTaxonUri)
            if (treeVersionElement) {
                String userName = treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
                results.payload = treeService.placeTaxonUri(treeVersionElement, instanceUri, excluded, userName)
            } else {
                results.ok = false
                results.fail("Parent taxon with id $parentTaxonUri not found", NOT_FOUND)
            }
        }
    }

    def replaceElement() {
        withJsonData(request.JSON, false, ['currentElementUri', 'newParentElementUri', 'instanceUri']) { ResultObject results, Map data ->
            String instanceUri = data.instanceUri
            String currentElementUri = data.currentElementUri
            String newParentElementUri = data.newParentElementUri

            TreeVersionElement currentElement = TreeVersionElement.get(currentElementUri)
            TreeVersionElement newParentElement = TreeVersionElement.get(newParentElementUri)

            if (currentElement && newParentElement && instanceUri) {
                String userName = treeService.authorizeTreeOperation(newParentElement.treeVersion.tree)
                results.payload = treeService.replaceTaxon(currentElement, newParentElement, instanceUri, userName)
            } else {
                results.ok = false
                results.fail("Elements with ids $instanceUri, $currentElementUri, $newParentElementUri not found", NOT_FOUND)
            }
        }
    }

    def removeElement() {
        withJsonData(request.JSON, false, ['taxonUri']) { ResultObject results, Map data ->
            String taxonUri = data.taxonUri
            TreeVersionElement treeVersionElement = TreeVersionElement.get(taxonUri)

            if (treeVersionElement) {
                treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
                int count = treeService.removeTreeVersionElement(treeVersionElement)
                results.payload = [count: count, message: "$count taxon removed, starting from $taxonUri"]
            } else {
                results.ok = false
                results.fail("taxon with id $taxonUri not found", NOT_FOUND)
            }
        }
    }

    def editElementProfile() {
        withJsonData(request.JSON, false, ['taxonUri', 'profile']) { ResultObject results, Map data ->
            TreeVersionElement treeVersionElement = TreeVersionElement.get(data.taxonUri as String)
            if (!treeVersionElement) {
                throw new ObjectNotFoundException("Can't find taxon with URI $data.taxonUri")
            }
            String userName = treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
            results.payload = treeService.editProfile(treeVersionElement, data.profile as Map, userName)
        }
    }

    def editElementStatus() {
        withJsonData(request.JSON, false, ['taxonUri', 'excluded']) { ResultObject results, Map data ->
            TreeVersionElement treeVersionElement = TreeVersionElement.get(data.taxonUri as String)
            if (!treeVersionElement) {
                throw new ObjectNotFoundException("Can't find taxon with URI $data.taxonUri")
            }
            String userName = treeService.authorizeTreeOperation(treeVersionElement.treeVersion.tree)
            results.payload = treeService.editExcluded(treeVersionElement, data.excluded as Boolean, userName)
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
                if (data[key] == null) {
                    results.ok = false
                    results.fail("$key not supplied. You must supply $key.", BAD_REQUEST)
                }
            }
            handleResults(results) {
                work(results, data)
            }
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
