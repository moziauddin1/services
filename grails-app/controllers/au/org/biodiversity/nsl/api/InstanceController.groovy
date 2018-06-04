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

import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.ObjectNotFoundException
import au.org.biodiversity.nsl.TaxonData
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.FORBIDDEN
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED

@Transactional
class InstanceController extends BaseApiController {

    def jsonRendererService
    def instanceService
    def treeService

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = ['json', 'xml', 'html']

    static allowedMethods = [
            delete: ["GET", "DELETE"]
    ]

    def index() {}

    @Timed()
    delete(Instance instance, String reason) {
        withTarget(instance) { ResultObject result, target ->
            if (request.method == 'DELETE') {
                SecurityUtils.subject.checkRole('admin')
                result << instanceService.deleteInstance(instance, reason)
                if (!result.ok) {
                    result.status = FORBIDDEN
                }
            } else if (request.method == 'GET') {
                result << instanceService.canDelete(instance, 'dummy reason')
            } else {
                result.status = METHOD_NOT_ALLOWED
            }
        }
    }

    def elementDataFromInstance(Long id) {
        ResultObject results = require('Instance id': id)

        handleResults(results) {
            Instance instance = got({ Instance.get(id) }, "Instance with id $id not found.") as Instance
            TaxonData taxonData = treeService.elementDataFromInstance(instance)
            if (taxonData) {
                results.payload = taxonData.asMap()
            } else {
                throw new ObjectNotFoundException("Couldn't get data for $instance.")
            }
        }
    }
}
