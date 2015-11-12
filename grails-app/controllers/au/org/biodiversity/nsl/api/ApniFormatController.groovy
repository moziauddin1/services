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

import au.org.biodiversity.nsl.Name
import grails.converters.JSON
import org.grails.plugins.metrics.groovy.Timed

/**
 * This controller replaces the cgi-bin/apni output. It takes a name and prints out informaiton in the APNI format as
 * it is.
 */
class ApniFormatController {

    def apniFormatService

    static responseFormats = [
            display: ['html'],
            name   : ['html']
    ]

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
            String inc = g.cookie(name: 'searchInclude')
            if (inc) {
                params.inc = JSON.parse(inc) as Map
            } else {
                params.inc = [scientific: 'on']
            }

            apniFormatService.getNameModel(name) << [query: [name: "$name.fullName", product: 'apni', inc: params.inc], stats: [:], names: [name], count: 1, max: 100]
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
            render(view: '_name', model: model)
        } else {
            render(status: 404, text: 'Name not found.')
        }
    }


}
