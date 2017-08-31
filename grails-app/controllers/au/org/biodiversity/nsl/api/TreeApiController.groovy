package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException

import static org.springframework.http.HttpStatus.*

class TreeApiController implements WithTarget {

    def treeService
    def jsonRendererService

    static responseFormats = [
            createTree                : ['JSON'],
            editTree                  : ['JSON'],
            copyTree                  : ['JSON'],
            deleteTree                : ['JSON'],
            createTreeVersion         : ['JSON'],
            setDefaultDraftTreeVersion: ['JSON'],
            editTreeVersion           : ['JSON'],
            validateTreeVersion       : ['JSON'],
            publishTreeVersion        : ['JSON'],
            placeTaxon                : ['JSON'],
            moveElement               : ['JSON'],
            removeElement             : ['JSON'],
            editElementProfile        : ['JSON'],
            editElementStatus         : ['JSON']
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

    def index() {}

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

    def deleteTree(Tree tree) {
        ResultObject results = requireTarget(tree, "Tree with id: $data.id")
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
    def createTreeVersion(Tree tree, TreeVersion fromVersion, String draftName, Boolean defaultVersion) {
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

    def setDefaultDraftTreeVersion(TreeVersion treeVersion) {
        ResultObject results = requireTarget(treeVersion, "Tree version $treeVersion")
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

    /* ******** Not implemented **** */

    def validateTreeVersion(TreeVersion treeVersion) {
        ResultObject results = requireTarget(treeVersion, "TreeVersion: $treeVersion")
        handleResults(results) {
            results.payload = treeService.validateTreeVersion(treeVersion)
        }
    }


    def placeTaxon(String treeElementUri, String taxonUri, Boolean excluded) {
        TreeElement parentElement = TreeElement.findByElementLink(treeElementUri)
        ResultObject results = requireTarget(treeElementUri, "Tree element with $treeElementUri")
        handleResults(results) {
            results.payload = treeService.placeTaxonUri(parentElement, taxonUri, excluded)
        }
    }

    def moveElement() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

    def removeElement() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

    def editElementProfile() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

    def editElementStatus() { respond(['Not implemented'], status: NOT_IMPLEMENTED) }

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
                results.fail(notImplementedException.message, NOT_FOUND)
                log.error("$notFound.message : $results")
            }
        }
        serviceRespond(results)
    }

}
