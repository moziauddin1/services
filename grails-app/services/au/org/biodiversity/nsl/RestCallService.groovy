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

    private String serviceUri = null
    private RestBuilder rest = new RestBuilder()

    // todo make config for multiple services by name
    String getServiceUri(String name) {
        if (!serviceUri) {
            String uri = grailsApplication.config.nslServices.uri
            if (!uri) {
                throw new RestCallException("nslService.uri must be defined in grails-app/conf/Config.groovy")
            }
            serviceUri = (uri.endsWith('/') ? uri : "$uri/")
        }
        return serviceUri
    }

    def get(String uri) {
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


    def post(String uri, List params) {
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
                throw new RestCallException("Error talking to Service: $resp.status, error: ${data?.error}")
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
            throw new RestCallException("Unable to connect to the service at ${getServiceUri('mapper')}", e)
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
