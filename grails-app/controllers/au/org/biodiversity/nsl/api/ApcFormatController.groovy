package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Node
import org.grails.plugins.metrics.groovy.Timed

class ApcFormatController {

    def classificationService

    def index() {
        redirect(action: 'search')
    }

    @Timed()
    def display(Name name) {
        if (name) {
            params.product = 'apc'
            getNameModel(name) << [query: [name: "\"$name.fullName\"", product: 'apc'], stats: [:], names: [name], count: 1, max: 100]
        } else {
            flash.message = "Name not found."
            redirect(action: 'search')
        }
    }

    @Timed()
    def name(Name name) {
        log.info "getting apc name $name"
        ResultObject model = new ResultObject(getNameModel(name))
        respond(model, [view: '_name', model: model])
    }

    private Map getNameModel(Name name) {
        Node apc = classificationService.isNameInAPC(name)
        Instance apcInstance = null
        Set<Instance> synonymOf = null
        Set<Instance> misapplied = null
        Set<Instance> instances = []
        Boolean excluded = false
        if (!apc) {
            synonymOf = name.instances.findAll { Instance i ->
                i.citedBy && classificationService.isInstanceInAPC(i.citedBy)
            }
        } else {
            excluded = apc.typeUriIdPart != 'ApcConcept'
            apcInstance = Instance.get(apc.taxonUriIdPart as Long)
            instances = apcInstance?.instancesForCitedBy ?: []
            misapplied = name.instances.findAll { Instance i ->
                i.cites && classificationService.isInstanceInAPC(i.citedBy)
            }
        }
        [name: name, synonymOf: synonymOf, misapplied: misapplied, apcInstance: apcInstance, instances: instances, excluded: excluded]
    }

}
