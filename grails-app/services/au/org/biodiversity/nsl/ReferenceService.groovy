package au.org.biodiversity.nsl

import grails.transaction.Transactional

class ReferenceService {

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

            String authorName = (reference.author != unknownAuthor ?
                    "${reference.author.name.trim()}${reference.refAuthorRole == editor ? ' (ed.)' : ''}" : '')

            String parentAuthorName = (
                    (reference.parent && reference.parent.author != unknownAuthor && reference.author != reference.parent.author) ?
                            "in ${reference.parent.author.name.trim()}" : '')

            String parentAuthorRole = ((reference.parent?.author != unknownAuthor && reference.parent?.refAuthorRole == editor) ? '(ed.)' : '')

            String pubDate = (reference.year ?
                    "(${reference.year})" : (reference.publicationDate ? "(${reference.publicationDate.clean()})" : ''))

            String referenceTitle = (reference.title && reference.title != 'Not set') ?
                    reference.title.removeFullStop() : ''

            String parentTitle = (reference.parent ? reference.parent.title.trim().removeFullStop() : '')

            String volume = (reference.volume ? reference.volume.trim() : '')

            String edition = (reference.edition ? "Edn. ${reference.edition.trim()}${volume ? ',' : ''}" : '')

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
                    bits << referenceTitle.fullStop()
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

    @Transactional
    def reconstructAllCitations() {
        runAsync {
            Closure query = { Map params ->
                Reference.listOrderById(params)
            }

            Author unknownAuthor = Author.findByName('-')
            RefAuthorRole editor = RefAuthorRole.findByName('Editor')

            SimpleNameService.chunkThis(1000, query) { List<Reference> references, bottom, top ->
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
            string.endsWith('.') ? string.replaceAll(/\.*$/,'') : string
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