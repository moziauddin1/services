package services

import au.org.biodiversity.nsl.Author
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Notification


class CheckNamesJob {

    def nameService

    def concurrent = false
    def sessionRequired = true

    static triggers = {
        simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute() {
        Name.withTransaction {
            List<Notification> notifications = Notification.list()
            notifications.each { Notification note ->
                switch (note.message) {
                    case 'name updated':
                        log.debug "Name $note.objectId updated"
                        Name name = Name.get(note.objectId)
                        if (name) {
                            nameService.nameUpdated(name, note)
                        } else {
                            log.debug "Name $note.objectId  doesn't exist "
                        }
                        break
                    case 'name created':
                        log.debug "Name $note.objectId created"
                        Name name = Name.get(note.objectId)
                        if (name) {
                            nameService.nameCreated(name, note)
                        } else {
                            log.debug "Name $note.objectId doesn't exist"
                        }
                        break
                    case 'author updated':
                        log.debug "Author $note.objectId updated"
                        Author author = Author.get(note.objectId)
                        if (author) {
                            nameService.authorUpdated(author, note)
                        } else {
                            log.debug "Author $note.objectId  doesn't exist "
                        }
                        break
                    case 'author created':
                    case 'author deleted':
                            //NSL-1032 ignore for now, deleted authors can't have names
                        break
                    default:
                        //probably caused by previous error. This note will be deleted
                        log.error "unhandled note $note.message"
                }
                note.delete()
            }
        }
    }
}
