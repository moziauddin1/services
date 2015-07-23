package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Author
import au.org.biodiversity.nsl.ConstructedNameService
import au.org.biodiversity.nsl.RefAuthorRole
import au.org.biodiversity.nsl.Reference
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

@Transactional(readOnly = true)
class ReferenceController implements UnauthenticatedHandler, WithTarget {

    def referenceService
    def jsonRendererService

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = ['json', 'xml', 'html']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]
    static namespace = "api"

    def list(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond Reference.list(params), [status: OK]
    }

    @Timed()
    def citationStrings(Reference reference) {
        withTarget(reference) { ResultObject result ->

            Author unknownAuthor = Author.findByName('-')
            RefAuthorRole editor = RefAuthorRole.findByName('Editor')

            String citationHtml = referenceService.generateReferenceCitation(reference, unknownAuthor, editor)
            result << [
                    result: [
                            citationHtml: citationHtml,
                            citation    : ConstructedNameService.stripMarkUp(citationHtml)
                    ]
            ]
            if (request.method == 'PUT') {
                SecurityUtils.subject.checkRole('admin')
                reference.citation = result.result.citation
                reference.citationHtml = result.result.citationHtml
                reference.save()
            }
            respond(result as Object, [status: OK])
        }
    }
}
