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

import grails.plugins.rest.client.RestBuilder
import grails.plugins.rest.client.RestResponse
import org.springframework.web.client.ResourceAccessException

/**
 * Handles making REST requests to a service.
 *
 * @author Peter McNeil
 */
class RestCallService {

    static transactional = false

    def grailsApplication

    private RestBuilder rest = new RestBuilder(proxy: Proxy.NO_PROXY)

    def get(String uri) throws RestCallException {
        log.debug "get json ${uri}"
        withRest {
            return rest.get(uri) {
                header 'Accept', "application/json"
            }
        }
    }

    RestResponse nakedGet(String uri) {
        log.debug "get json ${uri}"
        return rest.get(uri) {
            header 'Accept', "application/json"
        }
    }


    def post(String uri, List params) throws RestCallException {
        log.debug "call json ${uri} $params"
        withRest {
            return rest.post(uri) {
                contentType "application/json"
                json params
            }
        }
    }

    def withRest(Closure restCall) throws RestCallException {
        try {
            RestResponse resp = restCall()
            def data = resp.json
            if (resp.status != 200) {
                throw new RestCallException("Error talking to Service: $resp.status. ${data?.error ?: ''}")
            }
            if (data && data instanceof Map && data.error) {
                throw new RestCallException("Service error: ${data?.error}")
            }
            if (data.equals(null)) {
                data = null //turn it into a real null
            }
            return data
        }
        catch (ResourceAccessException e) {
            log.error e.message
            throw new RestCallException("Unable to connect to the service", e)
        }
    }

}

class RestCallException extends Throwable {
    RestCallException() {
        super()
    }

    RestCallException(String message) {
        super(message)
    }

    RestCallException(String message, Throwable cause) {
        super(message, cause)
    }

    RestCallException(Throwable cause) {
        super(cause)
    }
}
