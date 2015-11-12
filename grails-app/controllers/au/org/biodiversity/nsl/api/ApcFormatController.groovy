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
import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.Node
import grails.converters.JSON
import org.grails.plugins.metrics.groovy.Timed

class ApcFormatController {

    def classificationService

    static responseFormats = [
            display: ['html'],
            name   : ['html']
    ]
    def index() {
        redirect(action: 'search')
    }

    @Timed()
    def display(Name name) {
        if (name) {
            params.product = 'apc'
            String inc = g.cookie(name: 'searchInclude')
            if (inc) {
                params.inc = JSON.parse(inc) as Map
            } else {
                params.inc = [scientific: 'on']
            }

            getNameModel(name) << [query: [name: "$name.fullName", product: 'apc', inc: params.inc], stats: [:], names: [name], count: 1, max: 100]
        } else {
            flash.message = "Name not found."
            redirect(action: 'search')
        }
    }

    @Timed()
    def name(Name name) {
        if (name) {
            log.info "getting apc name $name"
            ResultObject model = new ResultObject(getNameModel(name))
            render(view: '_name', model: model)
        } else {
            render(status: 404, text: 'Name not found.')
        }
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
