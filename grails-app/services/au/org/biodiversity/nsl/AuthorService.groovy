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

@Transactional
class AuthorService {

    def linkService

    def autoDeduplicate() {
        runAsync {
            List<Author> authorsMarkedAsDuplicates = Author.findAllByDuplicateOfIsNotNull()
            authorsMarkedAsDuplicates.each { Author author ->
                dedupe([author], author.duplicateOf)
            }

            List<String> namesWithDuplicates = Author.executeQuery('select distinct(a.name) from Author a where exists (select 1 from Author a2 where a2.id <> a.id and a2.name = a.name)') as List<String>
            namesWithDuplicates.each { String name ->
                List<Author> authors = Author.findAllByName(name)
                if (authors) {
                    Map abbrevs = authors.groupBy { it.abbrev }
                    if (abbrevs.size() > 2) {
                        log.debug "more than two abbrevs for $name: ${abbrevs.keySet()}"
                    } else {
                        abbrevs.remove(null)
                        Author targetAuthor
                        if (abbrevs.size() == 0) {
                            targetAuthor = authors.min { it.id }
                            dedupe(authors, targetAuthor)
                        } else if (abbrevs.size() == 1) {
                            targetAuthor = abbrevs.values().first().min { it.id }
                            dedupe(authors, targetAuthor)
                        } else {
                            log.debug "more than one remaining abbrev for $name: $abbrevs"
                        }
                    }
                }
            }
        }
    }

    private void dedupe(List<Author> authors, Author targetAuthor) {
        authors.each { Author dupeAuthor ->
            if (dupeAuthor != targetAuthor) {
                rewireDuplicateTo(targetAuthor, dupeAuthor)
                log.debug "move links to $targetAuthor from $dupeAuthor"
                linkService.moveTargetLinks(dupeAuthor, targetAuthor)
                log.info "About to delete $dupeAuthor"
                dupeAuthor.delete()
                targetAuthor.duplicateOf = null
                targetAuthor.save()
            }
        }
    }

    /**
     * rewire the duplicate author to the target author
     * @param target
     * @param duplicate
     * @return
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    private rewireDuplicateTo(Author target, Author duplicate) {
        log.debug "rewiring links for $duplicate to $target"
        duplicate.namesForAuthor.each { Name name ->
            log.debug "setting author on name $name to $target from $duplicate"
            name.author = target
            name.save()
        }
        duplicate.namesForBaseAuthor.each { Name name ->
            log.debug "setting base author on name $name to $target from $duplicate"
            name.baseAuthor = target
            name.save()
        }
        duplicate.namesForExAuthor.each { Name name ->
            log.debug "setting ex author on name $name to $target from $duplicate"
            name.exAuthor = target
            name.save()
        }
        duplicate.namesForExBaseAuthor.each { Name name ->
            log.debug "setting ex base author on name $name to $target from $duplicate"
            name.exBaseAuthor = target
            name.save()
        }
        duplicate.namesForSanctioningAuthor.each { Name name ->
            log.debug "setting sanctioning author on name $name to $target from $duplicate"
            name.sanctioningAuthor = target
            name.save()
        }
        duplicate.references.each { Reference reference ->
            log.debug "setting author on reference $reference to $target from $duplicate"
            reference.author = target
            reference.save()
        }
        duplicate.comments.each { Comment comment ->
            log.debug "setting author on comment $comment to $target from $duplicate"
            comment.author = target
            comment.save()
        }
        log.debug "setting duplicates for $duplicate to $target"
        Author.findAllByDuplicateOf(duplicate)*.duplicateOf = target
        duplicate.duplicateOf = target
        target.save()
        duplicate.save()
    }
}
