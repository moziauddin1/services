package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.transaction.Transactional

import static org.springframework.http.HttpStatus.*

@Transactional(readOnly = true)
class IdMapperController {

    static responseFormats = ['json', 'xml']
    static namespace = "api"

    def checkId(Long id) {
        Name name = Name.get(id)
        if (name) {
            respond name
            return
        }
        Instance instance = Instance.get(id)
        if (instance) {
            respond instance
            return
        }
        Author author = Author.get(id)
        if (author) {
            respond author
            return
        }
        Reference reference = Reference.get(id)
        if (reference) {
            respond reference
            return
        }
        render status: NOT_FOUND
    }

    def apni() {
        log.debug "cgi-bin/apni params: $params"
        if (params.taxon_id) {
            Long taxon_id = params.taxon_id as Long
            IdMapper idMapper = IdMapper.findByFromIdAndSystem(taxon_id, 'PLANT_NAME')
            if (!idMapper?.toId) {
                flash.message = "Old taxon ID ${taxon_id} was not found. Please try a search."
                redirect(controller: 'search')
                return
            }
            flash.message = "You have been redirected here from an old Link. Please use the APNI search directly for best results."
            return redirect(controller: 'apniFormat', action: 'display', id: idMapper.toId)
        }
        // OK look for any parameter with name in it and search it
        String name = params.taxon_name ?: (params['00taxon_name'] ?: params.TAXON_NAME)

        flash.message = "You have been redirected here from an old Link. We may have missed something in your search request. Please use the APNI search directly for best results."
        if(name) {
            return redirect(uri:'/apni', params: [name: name, max: 100, display: 'apni', search: true])
        } else {
            return redirect(uri:'/apni')
        }
    }

}
