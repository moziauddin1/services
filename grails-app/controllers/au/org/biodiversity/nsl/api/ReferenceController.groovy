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

package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Author
import au.org.biodiversity.nsl.ConstructedNameService
import au.org.biodiversity.nsl.RefAuthorRole
import au.org.biodiversity.nsl.Reference
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authz.annotation.RequiresRoles
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.*

@Transactional
class ReferenceController implements UnauthenticatedHandler, WithTarget {

    def referenceService
    def jsonRendererService

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = ['json', 'xml', 'html']
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

    @Timed()
    def delete(Reference reference, String reason) {
        withTarget(reference) { ResultObject result ->
            if (request.method == 'DELETE') {
                SecurityUtils.subject.checkRole('admin')
                result << referenceService.deleteReference(reference, reason)
                if (!result.ok) {
                    results.status = FORBIDDEN
                }
            } else if (request.method == 'GET') {
                result << referenceService.canDelete(reference, 'dummy reason')
            } else {
                result.status = METHOD_NOT_ALLOWED
            }
        }
    }

    @Timed()
    @RequiresRoles('admin')
    def move(Reference reference, Long target, String user) {
        Reference targetRef = null
        if (target) {
            targetRef = Reference.get(target)
        }
        withTarget(reference, "source Reference") { ResultObject result1 ->
            withTarget(targetRef, "target Reference ($target)") { ResultObject result ->
                if (!user) {
                    user = SecurityUtils.subject.principal.toString()
                }
                if(targetRef == reference) {
                    result.status = BAD_REQUEST
                    result.ok = false
                    result.error = "Source and target are the same. Here I am, brain the size of a planet...."
                    return
                }
                result << referenceService.moveReference(reference, targetRef, user)
                if (!result.ok) {
                    result.status = FORBIDDEN
                }
            }
        }
    }

    @Timed()
    @RequiresRoles('admin')
    def deduplicateMarked(String user){
        if (!user) {
            user = SecurityUtils.subject.principal.toString()
        }
        ResultObject results = new ResultObject(referenceService.deduplicateMarked(user))
        //noinspection GroovyAssignabilityCheck

        respond(results, [status: OK, view: '/common/serviceResult', model: [data: results,]])
    }
}
