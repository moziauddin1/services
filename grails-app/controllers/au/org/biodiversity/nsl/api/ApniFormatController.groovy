package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Name
import org.grails.plugins.metrics.groovy.Timed

/**
 * This controller replaces the cgi-bin/apni output. It takes a name and prints out informaiton in the APNI format as
 * it is.
 */
class ApniFormatController {

    def apniFormatService

    def index() {
        redirect(action: 'search')
    }

    /**
     * Display a name in APNI format
     * @param Name
     */
    @Timed()
    def display(Name name) {
        if (name) {
            params.product = 'apni'
            apniFormatService.getNameModel(name) << [query: [name: "\"$name.fullName\"", product: 'apni'], stats: [:], names: [name], count: 1, max: 100]
        } else {
            flash.message = "Name not found."
            redirect(action: 'search')
        }
    }

    @Timed()
    def name(Name name) {
        if (name) {
            log.info "getting apni name $name"
            ResultObject model = new ResultObject(apniFormatService.getNameModel(name))
            respond(model, [view: '_name', model: model])
        } else {
            render(text: "Name not found")
        }
    }


}
