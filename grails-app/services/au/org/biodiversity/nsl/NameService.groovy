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

import grails.transaction.Transactional
import org.apache.shiro.grails.annotations.RoleRequired
import org.quartz.Scheduler
import org.springframework.transaction.TransactionStatus

@Transactional
class NameService {

    def configService
    def restCallService
    def nameConstructionService
    def linkService
    def treeService
    Scheduler quartzScheduler

    Set<String> restClients = []

    List<Long> seen = []
    //rather crude way of not repeating endlessly trying to act on notifications if something fails.

    //event types
    static String CREATED_EVENT = 'created'
    static String UPDATED_EVENT = 'updated'

    def nameUpdated(Name name, Notification note) {
        if (seen.contains(note.id)) {
            log.info "seen note, skipping $note"
            return
        }
        seen.add(note.id)

        notifyNameEvent(name, UPDATED_EVENT)
        name.discard() // make sure we don't update name in this TX
    }

    def nameCreated(Name name, Notification note) {
        if (seen.contains(note.id)) {
            log.info "seen note, skipping $note"
            return
        }
        seen.add(note.id)
        log.info "name $name created."
        notifyNameEvent(name, CREATED_EVENT)
    }

    /**
     * Delete a name
     * Check the name is ok to delete, then do all that is needed to clean it up.
     *
     * NSL-641
     * workflow:
     *
     * 1. User wants to delete name in editor - selects delete and adds a reason (if authorised)
     * 2. Editor calls delete on services (with apiKey and username of user doing the delete)
     * 3. Services does check to see if OK to delete, i.e. no instances/usages, if not respond to editor with error message
     * 4. Services calls the mapper and marks the identifier as deleted
     * 5. Services respond to editor that name has been deleted
     *

     *
     * @param name
     */
    @RoleRequired('admin')

    Map deleteName(Name name, String reason) {
        Map canWeDelete = canDelete(name, reason)
        if (canWeDelete.ok) {
            try {
                Name.withTransaction { TransactionStatus t ->
                    name.refresh()
                    Comment.findAllByName(name)*.delete()
                    notifyNameEvent(name, 'deleted')
                    NameTagName.findAllByName(name)*.delete()
                    name.delete()
                    Map response = linkService.deleteNameLinks(name, reason)
                    if (!response.success) {
                        List<String> errors = ["Error deleting link from the mapper"]
                        errors.addAll(response.errors)
                        t.setRollbackOnly()
                        return [ok: false, errors: errors]
                    }
                    t.flush()
                }
            } catch (e) {
                List<String> errors = [e.message]
                while (e.cause) {
                    e = e.cause
                    errors << e.message
                }
                return [ok: false, errors: errors]
            }
        }
        return canWeDelete
    }

    /**
     * Can we delete this name.
     * @param name
     * @return a map with ok and a list of error Strings
     */
    Map canDelete(Name name, String reason) {
        List<String> errors = []
        if (!reason) {
            errors << 'You need to supply a reason for deleting this name.'
        }
        if (treeService.isNameInAnyTree(name)) {
            errors << "This name is in a tree."
        }
        if (name.instances.size() > 0) {
            errors << 'There are instances that refer to this name'
        }
        Integer children = Name.countByParent(name)
        if (children > 0) {
            errors << "This name is a parent of $children names"
        }
        Integer stepChildren = Name.countBySecondParent(name)
        if (stepChildren > 0) {
            errors << "This name is a second parent of $stepChildren names"
        }
        Integer duplicates = Name.countByDuplicateOf(name)
        if (duplicates > 0) {
            errors << "This name $duplicates duplicates names. Delete them first?"
        }

        if (errors.size() > 0) {
            return [ok: false, errors: errors]
        }
        return [ok: true]
    }

    /**
     * IF an author is updated we need to check and update all names that author has written
     * @param author
     * @param note
     * @return
     */

    def authorUpdated(Author author, Notification note) {
        if (seen.contains(note.id)) {
            log.info "seen note, skipping $note"
            return
        }
        seen.add(note.id)

        author.namesForAuthor.each { Name name ->
            updateFullName(name)
        }
        author.namesForExAuthor.each { Name name ->
            updateFullName(name)
        }
        author.namesForBaseAuthor.each { Name name ->
            updateFullName(name)
        }
        author.namesForExBaseAuthor.each { Name name ->
            updateFullName(name)
        }
    }

    private void updateFullName(Name name) {
        Map fullNameMap = nameConstructionService.constructName(name)
        name.fullNameHtml = fullNameMap.fullMarkedUpName
        name.simpleNameHtml = fullNameMap.simpleMarkedUpName
        name.simpleName = nameConstructionService.stripMarkUp(fullNameMap.simpleMarkedUpName)
        name.fullName = nameConstructionService.stripMarkUp(fullNameMap.fullMarkedUpName)
        name.save()
    }

    private Boolean paused = false

    def startUpdatePolling() {
        quartzScheduler.start()
    }

    def pauseUpdates() {
        if (quartzScheduler.isStarted()) {
            quartzScheduler.pauseAll()
            paused = true
        }
    }

    def resumeUpdates() {
        if (quartzScheduler.isStarted()) {
            quartzScheduler.resumeAll()
        } else {
            quartzScheduler.start()
        }
        paused = false
    }

    String pollingStatus() {
        if (quartzScheduler.isStarted()) {
            return paused ? 'paused' : 'running'
        } else {
            return 'stopped'
        }
    }

    def nameEventRegister(String uri) {
        restClients.add(uri)
    }

    def nameEventUnregister(String uri) {
        restClients.remove(uri)
    }

    void notifyNameEvent(Name name, String type) {
        if (!restClients.empty) {
            runAsync {
                String link = linkService.getPreferredLinkForObject(name)
                restClients.each { String uri ->
                    restCallService.blindJsonGet("$uri/$type?id=${link}")
                }
            }
        }
    }


    def reconstructAllNames() {
        runAsync {
            String updaterWas = pollingStatus()
            pauseUpdates()
            Closure query = { Map params ->
                Name.listOrderById(params)
            }

            chunkThis(1000, query) { List<Name> names, bottom, top ->
                long start = System.currentTimeMillis()
                Name.withSession { session ->
                    names.each { Name name ->
                        Map constructedNames = nameConstructionService.constructName(name)

                        if (!(name.fullNameHtml && name.simpleNameHtml && name.fullName && name.simpleName && name.sortName) ||
                                name.fullNameHtml != constructedNames.fullMarkedUpName) {
                            name.fullNameHtml = constructedNames.fullMarkedUpName
                            name.fullName = nameConstructionService.stripMarkUp(constructedNames.fullMarkedUpName)
                            name.simpleNameHtml = constructedNames.simpleMarkedUpName
                            name.simpleName = nameConstructionService.stripMarkUp(constructedNames.simpleMarkedUpName)
                            name.sortName = nameConstructionService.makeSortName(name, name.simpleName)
                            name.save()
//                            log.debug "saved $name.fullName"
                        } else {
                            name.discard()
                        }
                    }
                    session.flush()
                }
                log.info "$top done. 1000 took ${System.currentTimeMillis() - start} ms"
            }
            if (updaterWas == 'running') {
                resumeUpdates()
            }
        }
    }

    File checkAllNames() {
        File tempFile = File.createTempFile('name-check', 'txt')
        runAsync {
            Closure query = { Map params ->
                Name.listOrderById(params)
            }


            chunkThis(1000, query) { List<Name> names, bottom, top ->
                long start = System.currentTimeMillis()
                Name.withSession { session ->
                    names.each { Name name ->
                        try {
                            Map constructedNames = nameConstructionService.constructName(name)
                            String strippedName = nameConstructionService.stripMarkUp(constructedNames.fullMarkedUpName)
                            if (name.fullName != strippedName) {
                                String msg = "$name.id, \"${name.nameType.name}\", \"${name.nameRank.name}\", \"$name.fullName\", \"${strippedName}\""
                                log.info(msg)
                                tempFile.append("$msg\n")
                            }
                        } catch (e) {
                            String msg = "error constructing name $name : $e.message"
                            log.error(msg)
                            tempFile.append("$msg\n")
                            e.printStackTrace()
                        }
                        name.discard()
                    }
                    session.clear()
                }

                log.info "$top done. 1000 took ${System.currentTimeMillis() - start} ms"
            }
        }
        return tempFile
    }

    def reconstructSortNames() {
        runAsync {
            String updaterWas = pollingStatus()
            pauseUpdates()
            Closure query = { Map params ->
                Name.listOrderById(params)
            }

            chunkThis(1000, query) { List<Name> names, bottom, top ->
                long start = System.currentTimeMillis()
                Name.withSession { session ->
                    names.each { Name name ->
                        String sortName = nameConstructionService.makeSortName(name, name.simpleName)
                        if (!(name.sortName) || name.sortName != sortName) {
                            name.sortName = sortName
                            name.save()
                        } else {
                            name.discard()
                        }
                    }
                    session.flush()
                }
                log.info "$top done. 1000 took ${System.currentTimeMillis() - start} ms"
            }
            if (updaterWas == 'running') {
                resumeUpdates()
            }
        }
    }

    def constructMissingNames() {
        runAsync {
            String updaterWas = pollingStatus()
            pauseUpdates()
            Closure query = { Map params ->
                Name.executeQuery("""select n from Name n
where n.simpleName is null
or n.simpleNameHtml is null
or n.fullName is null
or n.fullNameHtml is null""", params)
            }

            chunkThis(1000, query) { List<Name> names, bottom, top ->
                long start = System.currentTimeMillis()
                Name.withSession { session ->
                    names.each { Name name ->
                        Map constructedNames = nameConstructionService.constructName(name)

                        name.fullNameHtml = constructedNames.fullMarkedUpName
                        name.fullName = nameConstructionService.stripMarkUp(constructedNames.fullMarkedUpName)
                        name.simpleNameHtml = constructedNames.simpleMarkedUpName
                        name.simpleName = nameConstructionService.stripMarkUp(constructedNames.simpleMarkedUpName)
                        name.save()
                        log.debug "saved $name.fullName"
                    }
                    session.flush()
                }
                log.info "${names.size()} done. 1000 took ${System.currentTimeMillis() - start} ms"
            }
            if (updaterWas == 'running') {
                resumeUpdates()
            }
        }
    }

    def addNamesNotInNameTree(String treeLabel) {
        List<Name> namesNotInApni = Name.executeQuery("""select n from Name n
where n.parent is not null
and n.nameType.name <> 'common'
and not exists (select t from Node t where cast(n.id as string) = t.nameUriIdPart and t.root.label = '${treeLabel}')""")
        namesNotInApni.each { Name name ->
            Notification notification = new Notification(objectId: name.id, message: 'name created')
            notification.save()
        }
    }

    Integer countIncompleteNameStrings() {
        Name.executeQuery("""select count(n) from Name n
where n.simpleName is null
or n.simpleNameHtml is null
or n.fullName is null
or n.fullNameHtml is null""")?.first() as Integer
    }

    static chunkThis(Integer chunkSize, Closure query, Closure work) {

        Integer i = 0
        Integer size = chunkSize
        while (size == chunkSize) {
            Integer top = i + chunkSize
            //needs to be ordered or we might repeat items
            List items = query([offset: i, max: chunkSize])
            work(items, i, top)
            i = top
            size = items.size()
        }
    }

}
