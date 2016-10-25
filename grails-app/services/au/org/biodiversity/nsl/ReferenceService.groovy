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
import org.springframework.transaction.TransactionStatus

import java.sql.Timestamp

class ReferenceService {

    def instanceService
    def linkService

    /**
     * Create a reference citation from reference
     *
     * For a Section or Paper:
     * author [in publication author] (publication date), [reference title.] [<i>parent title</i>.] [Edn. edition[, volume]] [Herbarium annotation][Personal Communication]
     *
     * For everything else e.g. Book:
     * author [in parent author] (publication date), [<i>reference title</i>.] [<i>parent title</i>.] [Edn. edition[, volume]] [Herbarium annotation][Personal Communication]
     *
     * @param reference
     * @param unknownAuthor
     * @param editor
     * @return
     */
    String generateReferenceCitation(Reference reference, Author unknownAuthor, RefAuthorRole editor) {
        use(ReferenceStringCategory) {

            List<Reference> parents = parents(reference)

            String authorName = (reference.author != unknownAuthor ?
                    "${reference.author.name.trim()}${reference.refAuthorRole == editor ? ' (ed.)' : ''}" : '')

            String parentAuthorName = (
                    (reference.parent &&
                            reference.parent.author != unknownAuthor &&
                            (
                                    reference.author != reference.parent.author ||
                                            reference.parent?.refAuthorRole == editor
                            )
                    ) ? "in ${reference.parent.author.name.trim()}" : '')

            String parentAuthorRole = ((reference.parent?.author != unknownAuthor && reference.parent?.refAuthorRole == editor) ? '(ed.)' : '')

            String pubDate = pubDate(reference)

            String referenceTitle = getReferenceTitle(reference)

            String superReferenceTitle = ((parents.size() > 1) ? getReferenceTitle(reference.parent) : '')

            String parentTitle = ultimateParentTitle(reference.parent)

            String volume = volume(reference)

            String edition = edition(reference, volume)

            List<String> bits = []
            //prefix
            switch (reference.refType.name) {
                case 'Section':
                case 'Paper':
                case 'Book':
                default:
                    bits << authorName
                    bits << parentAuthorName
                    bits << parentAuthorRole
                    bits << pubDate.comma()
                    if (superReferenceTitle) {
                        bits << referenceTitle.comma()
                        bits << "in"
                        bits << superReferenceTitle.fullStop()

                    } else {
                        bits << referenceTitle.fullStop()
                    }
            }

            //middle
            bits << (parentTitle ? "<i>$parentTitle</i>" : '')
            bits << edition
            bits << volume

            //postfix
            switch (reference.refType.name) {
                case 'Herbarium annotation':
                    bits << 'Herbarium annotation'
                    break
                case 'Personal Communication':
                    bits << 'Personal Communication'
                    break
            }

            String result = bits.findAll { it }.join(' ').removeFullStop()
            assert result != 'true'
            return result;
        }
    }

    private static List<Reference> parents(Reference reference) {
        List<Reference> parents = []
        Reference parent = reference.parent
        while (parent) {
            parents << parent
            parent = parent.parent
        }
        return parents
    }

    private static String getReferenceTitle(Reference reference) {
        use(ReferenceStringCategory) {
            if (reference.title && reference.title != 'Not set') {
                return reference.title.removeFullStop()
            }
            return ''
        }
    }

    private String edition(Reference reference, String volume) {
        if (reference.edition) {
            return "Edn. ${reference.edition.trim()}${volume ? ',' : ''}"
        }
        if (reference.parent) {
            return edition(reference.parent, volume)
        }
        return ''
    }

    private String ultimateParentTitle(Reference reference) {
        if (!reference) {
            return ''
        }

        use(ReferenceStringCategory) {
            if (reference.parent) {
                return ultimateParentTitle(reference.parent)
            }
            if (reference.title && reference.title != 'Not set') {
                return reference.title.removeFullStop()
            }
            return ''
        }
    }

    private String volume(Reference reference) {
        if (reference.volume) {
            return reference.volume.trim()
        }
        if (reference.parent) {
            return volume(reference.parent)
        }
        return ''
    }

    private String pubDate(Reference reference) {
        use(ReferenceStringCategory) {
            if (reference.year) {
                return "(${reference.year})"
            }
            if (reference.publicationDate) {
                return "(${reference.publicationDate.clean()})"
            }
            if (reference.parent) {
                return pubDate(reference.parent)
            }
            return ''
        }
    }

    public static Integer findReferenceYear(Reference reference) {
        if(!reference) {
            return null
        }
        if(reference.year) {
            return reference.year
        }

        if (reference.refType.useParentDetails) {
            return reference.parent.year
        }
        return null
    }

    public void checkReferenceChanges(Reference reference) {
        reconstructChildCitations(reference)
    }

    @Transactional
    public void reconstructChildCitations(Reference parent){
        Author unknownAuthor = Author.findByName('-')
        RefAuthorRole editor = RefAuthorRole.findByName('Editor')

        Reference.findAllByParent(parent).each { Reference child ->
            String citationHtml = generateReferenceCitation(child, unknownAuthor, editor)
            if (child.citationHtml != citationHtml) {
                child.citationHtml = citationHtml
                child.citation = ConstructedNameService.stripMarkUp(citationHtml)
                child.save()
                log.debug "saved $child.citationHtml"
            } else {
                child.discard()
            }
        }
    }

    @Transactional
    def reconstructAllCitations() {
        runAsync {
            Closure query = { Map params ->
                Reference.listOrderById(params)
            }

            Author unknownAuthor = Author.findByName('-')
            RefAuthorRole editor = RefAuthorRole.findByName('Editor')

            NameService.chunkThis(1000, query) { List<Reference> references, bottom, top ->
                long start = System.currentTimeMillis()
                Name.withSession { session ->
                    references.each { Reference reference ->
                        String citationHtml = generateReferenceCitation(reference, unknownAuthor, editor)

                        if (reference.citationHtml != citationHtml) {
                            reference.citationHtml = citationHtml
                            reference.citation = ConstructedNameService.stripMarkUp(citationHtml)
                            reference.save()
                            log.debug "saved $reference.citationHtml"
                        } else {
                            reference.discard()
                        }
                    }
                    session.flush()
                }
                log.info "$top done. 1000 took ${System.currentTimeMillis() - start} ms"
            }

        }
    }

    @Transactional
    Map deduplicateMarked(String user) {
        List<Map> refs = []
        //remove nested duplicates first
        Reference.findAllByDuplicateOfIsNotNull().each { Reference reference ->
            int depth = 0
            while (reference.duplicateOf.duplicateOf && depth++ < 6) {
                reference.duplicateOf = reference.duplicateOf.duplicateOf
                reference.save(flush: true)
            }
        }

        Reference.findAllByDuplicateOfIsNotNull().each { Reference reference ->
            Map result = [source: reference.id, target: reference.duplicateOf.id]
            //noinspection GroovyAssignabilityCheck
            result << moveReference(reference, reference.duplicateOf, user)
            refs << result
        }
        return [action: "deduplicate marked references", count: Reference.countByDuplicateOfIsNotNull(), references: refs]
    }

/**
 * Move all the
 * - instances
 * - externalRefs
 * - referencesForParent
 * - comments
 * - move reference note to the instances of the source.
 *
 * from the source reference to the target reference.
 * Also moves the URI of the source to the target.
 *
 * No references can have the source as their duplicate of id or this will fail.
 *
 * @param source
 * @param target
 * @return
 */
    @Transactional
    Map moveReference(Reference source, Reference target, String user) {
        if (target.duplicateOf) {
            throw new Exception("Target $target is a duplicate")
        }
        if (!user) {
            return [ok: false, errors: ['You must supply a user.']]
        }
        if (!source) {
            return [ok: false, errors: ['You must supply source.']]
        }
        if (source.referencesForDuplicateOf.size() > 0) {
            return [ok: false, errors: ['References say they are a duplicate of the source.']]
        }
        InstanceNoteKey refNote = instanceService.getInstanceNoteKey('Reference Note', true)
        try {
            Reference.withTransaction { t ->
                Reference.withSession { session ->
                    Timestamp now = new Timestamp(System.currentTimeMillis())
                    if (source.notes && (!source.notes.equals(target.notes))) {
                        //copy to the source instances as an instance note
                        if (source.instances.size() > 0) {
                            InstanceNote note = new InstanceNote(
                                    value: source.notes,
                                    instanceNoteKey: refNote,
                                    updatedBy: user,
                                    updatedAt: now,
                                    createdBy: user,
                                    createdAt: now
                            )
                            source.instances.each { instance ->
                                instance.addToInstanceNotes(note)
                                instance.save()
                                log.info "Added reference note $note to $instance"
                            }
                        } else {
                            //append to the reference notes.
                            if (target.notes) {
                                target.notes = "$target.notes (duplicate refererence Note: $source.notes)"
                                log.info "Appended notes to $target.notes"
                            } else {
                                target.notes = source.notes
                                log.info "Set target notes to $target.notes"
                            }
                        }
                    }

                    Instance.executeQuery('select i from Instance i where reference = :ref', [ref: source]).each { instance ->
                        log.info "Moving instance $instance to $target"
                        instance.reference = target
                        instance.updatedAt = now
                        instance.updatedBy = user
                        instance.save(flush: true)
                    }
                    source.externalRefs.each { extRef ->
                        log.info "Moving external reference $extRef to $target"
                        extRef.reference = target
                        extRef.save(flush: true)
                    }
                    source.referencesForParent.each { ref ->
                        log.info "Moving parent of $ref to $target"
                        ref.parent = target
                        ref.parent.updatedAt = now
                        ref.parent.updatedBy = user
                        ref.save(flush: true)
                    }
                    source.comments.each { comment ->
                        log.info "Moving comment $comment to $target"
                        comment.reference = target
                        comment.updatedAt = now
                        comment.updatedBy = user
                        comment.save(flush: true)
                    }

                    Map response = linkService.moveTargetLinks(source, target)

                    if (!response.success) {
                        List<String> errors = ["Error moving the link in the mapper."]
                        log.error "Setting rollback only: $response.errors"
                        t.setRollbackOnly()
                        return [ok: false, errors: errors]
                    }
                    target.updatedAt = now
                    target.updatedBy = user
                    target.save(flush: true)
                    source.save(flush: true)
                    source.delete(flush: true)
                    return [ok: true]
                }
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

    @RoleRequired('admin')
    @Transactional
    Map deleteReference(Reference reference, String reason) {
        Map canWeDelete = canDelete(reference, reason)
        if (canWeDelete.ok) {
            try {
                Reference.withTransaction { TransactionStatus t ->
                    Map response = linkService.deleteReferenceLinks(reference, reason)

                    reference.delete()
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

    public Map canDelete(Reference reference, String reason) {
        List<String> errors = []

        if (!reason) {
            errors << "You need to supply a reason for deleting."
        }

        if (reference.instances.size() > 0) {
            errors << "There are ${reference.instances.size()} instances for this reference."
        }

        if (reference.externalRefs.size() > 0) {
            errors << "There are ${reference.externalRefs.size()} external refs for this reference."
        }

        if (reference.referencesForParent.size() > 0) {
            errors << "There are ${reference.referencesForParent.size()} children of this reference."
        }

        if (reference.comments.size() > 0) {
            errors << "There are ${reference.comments.size()} comments for this reference."
        }

        if (reference.referencesForDuplicateOf.size() > 0) {
            errors << "There are ${reference.referencesForDuplicateOf.size()} references that are a duplicate of this reference."
        }

        if (errors.size() > 0) {
            return [ok: false, errors: errors]
        }
        return [ok: true]
    }
}

class ReferenceStringCategory {

    static String withString(String string, Closure work) {
        if (string) {
            return work()
        }
        return ''
    }

    static String clean(String string) {
        withString(string) {
            string.replaceAll(/[()]/, '').trim()
        }
    }

    static String removeFullStop(String string) {
        withString(string) {
            string.endsWith('.') ? string.replaceAll(/\.*$/, '') : string
        }
    }

    static String fullStop(String string) {
        withString(string) {
            string.endsWith('.') ? string : string + '.'
        }
    }

    static String comma(String string) {
        withString(string) {
            string.endsWith(',') ? string : string + ','
        }
    }
}