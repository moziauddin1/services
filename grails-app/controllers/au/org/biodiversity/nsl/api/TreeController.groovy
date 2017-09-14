package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException

import static org.springframework.http.HttpStatus.*

class TreeController implements WithTarget {

    def treeService
    def jsonRendererService
    def linkService

    static responseFormats = [
            createTree                : ['json', 'html'],
            editTree                  : ['json', 'html'],
            copyTree                  : ['json', 'html'],
            deleteTree                : ['json', 'html'],
            createTreeVersion         : ['json', 'html'],
            setDefaultDraftTreeVersion: ['json', 'html'],
            editTreeVersion           : ['json', 'html'],
            validateTreeVersion       : ['json', 'html'],
            publishTreeVersion        : ['json', 'html'],
            placeTaxon                : ['json', 'html'],
            moveElement               : ['json', 'html'],
            removeElement             : ['json', 'html'],
            editElementProfile        : ['json', 'html'],
            editElementStatus         : ['json', 'html']
    ]

    static allowedMethods = [
            createTree                : ['PUT'],
            editTree                  : ['POST'],
            copyTree                  : ['PUT'],
            deleteTree                : ['DELETE'],
            createTreeVersion         : ['PUT'],
            setDefaultDraftTreeVersion: ['PUT'],
            editTreeVersion           : ['POST'],
            validateTreeVersion       : ['GET'],
            publishTreeVersion        : ['PUT'],
            placeTaxon                : ['PUT'],
            moveElement               : ['POST'],
            removeElement             : ['DELETE'],
            editElementProfile        : ['POST'],
            editElementStatus         : ['POST']
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
    def createTreeVersion(Long treeId, Long fromVersionId, String draftName, Boolean defaultVersion) {
        Tree tree = Tree.get(treeId)
        TreeVersion fromVersion = TreeVersion.get(fromVersionId)
        ResultObject results = requireTarget(tree, "Tree with id: $tree")
        handleResults(results) {
            treeService.authorizeTreeOperation(tree)
            if (defaultVersion) {
                results.payload = treeService.createDefaultDraftVersion(tree, fromVersion, draftName)
            } else {
                results.payload = treeService.createTreeVersion(tree, fromVersion, draftName)
            }
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
        ResultObject results = requireTarget(treeVersion, "TreeVersion with id: $data.id")
        handleResults(results) {
            treeService.authorizeTreeOperation(treeVersion.tree)
            results.payload = treeService.editTreeVersion(treeVersion, data.draftName as String)
        }
    }

    def validateTreeVersion(Long version) {
        log.debug "validate tree version $version"
        TreeVersion treeVersion = TreeVersion.get(version)
        ResultObject results = requireTarget(treeVersion, "TreeVersionId: $version")
        handleResults(results) {
            results.payload = treeService.validateTreeVersion(treeVersion)
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

        ResultObject results = requireTarget(parentTaxonUri, "Tree element with $parentTaxonUri")
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

    def removeTaxon() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

    def editTaxonProfile() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

    def editTaxonStatus() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

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
                results.fail(authException.message, FORBIDDEN)
                log.warn("You are not authorised to do this. $results")
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
