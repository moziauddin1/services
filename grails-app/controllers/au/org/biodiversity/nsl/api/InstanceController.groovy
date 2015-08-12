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
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.FORBIDDEN
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED

@Transactional
class InstanceController implements UnauthenticatedHandler, WithTarget {

    def jsonRendererService
    def instanceService

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = ['json', 'xml', 'html']

    static allowedMethods = [
            delete: ["GET", "DELETE"]
    ]
    static namespace = "api"

    def index() {}

    @Timed()
    def delete(Instance instance, String reason) {
        withTarget(instance) { ResultObject result ->
            if (request.method == 'DELETE') {
                SecurityUtils.subject.checkRole('admin')
                result << instanceService.deleteInstance(instance, reason)
                if(!result.ok) {
                    results.status = FORBIDDEN
                }
            } else if (request.method == 'GET') {
                result << instanceService.canDelete(instance, 'dummy reason')
            } else {
                result.status = METHOD_NOT_ALLOWED
            }
        }
    }

}
