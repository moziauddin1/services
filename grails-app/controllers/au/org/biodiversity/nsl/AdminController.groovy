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

import au.org.biodiversity.nsl.api.ResultObject
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authz.annotation.RequiresRoles

import static org.springframework.http.HttpStatus.OK

@Transactional
class AdminController {

    def constructedNameService
    def simpleNameService
    def searchService
    def nameService
    def nameTreePathService
    def apcTreeService
    def referenceService
    def instanceService
    def authorService

    @RequiresRoles('admin') 
    def index() {
        Map stats = [:]
        stats.namesNeedingConstruction = nameService.countIncompleteNameStrings()
        stats.namesNotInApni = nameService.countNamesNotInApni()
        stats.namesNotInApniTreePath = nameTreePathService.treePathReport('APNI')
        stats.namesNotInApcTreePath = nameTreePathService.treePathReport('APC')
        stats.deletedNames = Name.executeQuery("select n from Name n where n.nameStatus.name = '[deleted]'")
        //todo iterate trees add back stats if they don't interrupt ops.
        [pollingNames: nameService.pollingStatus(), stats: stats]
    }

    @RequiresRoles('admin') 
    def checkNames() {
        Closure query = { Map params ->
            Name.listOrderById(params)
        }

        File tempFile = File.createTempFile('name-check', 'txt')

        SimpleNameService.chunkThis(1000, query) { List<Name> names, bottom, top ->
            long start = System.currentTimeMillis()
            names.each { Name name ->
                Map constructedNames = constructedNameService.constructName(name)

                if (name.fullNameHtml != constructedNames.fullMarkedUpName) {
                    String constStripped = constructedNameService.stripMarkUp(constructedNames.fullMarkedUpName)
                    String nameStripped = constructedNameService.stripMarkUp(name.fullNameHtml)
                    if (constStripped != nameStripped) {
                        String verbatim
                        List<Instance> primaryInstances = instanceService.findPrimaryInstance(name)
                        if(primaryInstances && primaryInstances.size() > 0) {
                            verbatim = primaryInstances.first().verbatimNameString
                        }
                        Boolean equalsVerbatim = constStripped.toLowerCase() == verbatim?.toLowerCase()
                        String msg = "$name.id, \"[${name.nameType.name}]\", \"${nameStripped}\", \"${constStripped}\", \"${equalsVerbatim}\", \"${verbatim}\""
                        log.info(msg)
                        tempFile.append("$msg\n")
                    } else {
                        log.info("$name.id [${name.nameType.name}]: ${name.fullNameHtml} != ${constructedNames.fullMarkedUpName}")
                    }
                }
                name.discard()
            }
            log.info "$top done. 1000 took ${System.currentTimeMillis() - start} ms"
        }
        render(file: tempFile, fileName: 'name-changes.csv', contentType: 'text/plain')
    }

    @RequiresRoles('admin')
    def reconstructNames() {
        nameService.reconstructAllNames()
        flash.message = "reconstructing all names where changed."
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def reconstructCitations() {
        referenceService.reconstructAllCitations()
        flash.message = "reconstructing all reference citations."
        redirect(action: 'index')
    }


    @RequiresRoles('admin') 
    def constructMissingNames() {
        nameService.constructMissingNames()
        flash.message = "constructing missing names."
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def allSimpleNames() {
        log.debug "making all simple names"
        flash.message = simpleNameService.backgroundMakeSimpleNames()
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def makeTreePaths() {
        log.debug "make all tree paths. ${request.getRemoteAddr()}"
        searchService.makeAllTreePathsSql()
        flash.message = "Making all tree paths"
        redirect(action: 'index')
    }

    @RequiresRoles('admin')
    def autoDedupeAuthors() {
        authorService.autoDeduplicate()
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def logs() {
        List<String> processLog = logSummary(300)
        render(template: 'log', model: [processLog: processLog])
    }

    @RequiresRoles('admin') 
    def startUpdater() {
        log.debug "starting updater"
        nameService.startUpdatePolling()
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def pauseUpdates() {
        log.debug "pausing updater"
        nameService.pauseUpdates()
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def resumeUpdates() {
        log.debug "un-pausing updater"
        nameService.resumeUpdates()
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def notifyMissingApniNames() {
        log.debug "adding notifications for names not in APNI"
        nameService.addNamesNotInApni()
        redirect(action: 'index')
    }

    @RequiresRoles('admin') 
    def transferApcProfileData() {
        log.debug "applying instance APC comments and distribution text to the APC tree"
        flash.message = apcTreeService.transferApcProfileData()
        redirect(action: 'index')
    }

    @RequiresRoles('admin')
    def deduplicateMarkedReferences() {
        String user = SecurityUtils.subject.principal.toString()
        ResultObject results = new ResultObject(referenceService.deduplicateMarked(user))
        //noinspection GroovyAssignabilityCheck
        respond(results, [status: OK, view: '/common/serviceResult', model: [data: results,]])
    }

    private static List<String> logSummary(Integer lineLength) {
        String logFileName = (System.getProperty('catalina.base') ?: 'target') + "/logs/nsl-services.log"
        List<String> logLines = new File(logFileName).readLines().reverse().take(50)
        List<String> processedLog = []
        logLines.each { String line ->
            line = line.replaceAll(/grails.app.services.au.org.biodiversity.nsl./, '')
            if (line.size() > lineLength) {
                line = line[0..lineLength] + '...'
            }
            processedLog.add(line)
        }
        return processedLog
    }


}
