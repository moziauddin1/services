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

import au.org.biodiversity.nsl.JsonRendererService

import static org.springframework.http.HttpStatus.NOT_FOUND

/**
 * User: pmcneil
 * Date: 18/06/15
 *
 * When using this trait you must have the jsonRendererService injected into your implementing code.
 */
trait WithTarget {

    def withTarget(Object target, Closure work) {
        withTarget(target, 'Object', work)
    }

    def withTarget(Object target, String targetInfo, Closure work) {
        assert jsonRendererService

        ResultObject result = new ResultObject([action: params.action], jsonRendererService as JsonRendererService)

        if (target) {
            result.briefObject(target)
            work(result)
        } else {
            result.error("$targetInfo not found.")
            result.status = NOT_FOUND
        }
        respond(result, [view: '/common/serviceResult', model: [data: result], status: result.remove('status')])
    }

    def withTargets(Map targets, Closure work) {
        assert jsonRendererService
        ResultObject result = new ResultObject([action: params.action], jsonRendererService as JsonRendererService)
        boolean ok = true
        for (key in targets.keySet()) {
            if (!targets[key]) {
                result.status = NOT_FOUND
                result.error("$key not found.")
                ok = false
            } else {
                result.briefObject(targets[key], key as String)
            }
        }
        if(ok) {
            work(result)
        }
        log.debug "result status is ${result.status}"
        respond(result, [view: '/common/serviceResult', model: [data: result], status: result.remove('status')])
    }

}
