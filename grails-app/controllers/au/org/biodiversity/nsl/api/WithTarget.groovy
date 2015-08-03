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

import static org.springframework.http.HttpStatus.NOT_FOUND

/**
 * User: pmcneil
 * Date: 18/06/15
 *
 * When using this trait you must have the jsonRendererService injected into your implementing code.
 */
trait WithTarget {

    public withTarget(Object target, Closure work) {
         assert jsonRendererService

        if (target) {
            ResultObject result
            switch (target.class.simpleName) {
                case 'Instance':
                    result = new ResultObject([instance: jsonRendererService.getBriefInstance(target)])
                    break
                case 'Name':
                    result = new ResultObject([name: jsonRendererService.getBriefName(target)])
                    break
                case 'Reference':
                    result = new ResultObject([reference: jsonRendererService.getBriefReference(target)])
                    break
                case 'Author':
                    result = new ResultObject([author: jsonRendererService.getBriefAuthor(target)])
                    break
                case 'InstanceNote':
                    result = new ResultObject([instanceNote: jsonRendererService.getBriefInstanceNote(target)])
                    break
                default:
                    result = new ResultObject([target: jsonRendererService.brief(target)])
            }
            result << [
                    action: params.action,
            ]
            work(result)
            //noinspection GroovyAssignabilityCheck
            respond(result, [view: '/common/serviceResult', model: [data: result], status: result.status])
        } else {
            ResultObject result = new ResultObject([
                    action: params.action,
                    error : "The Instance was not found."
            ])
            //noinspection GroovyAssignabilityCheck
            respond(result, [view: '/common/serviceResult', model: [data: result], status: NOT_FOUND])
        }
    }

}
