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

import au.org.biodiversity.nsl.LinkService
import au.org.biodiversity.nsl.Name
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(NameController)
class NameControllerSpec extends Specification {
    @SuppressWarnings("GroovyUnusedDeclaration")
    static transactional = true

    def setup() {
        def linkServiceMock = mockFor(LinkService)
        linkServiceMock.demand.getLinksForObject(0..1) { Object thing -> ["first $thing link", "second $thing link"] }
        linkServiceMock.demand.getPreferredLinkForObject(0..1) { Object thing -> "Link for $thing" }
        controller.jsonRendererService.linkService = linkServiceMock.createMock()
    }

    def cleanup() {
    }

    void "Test nameStrings accepts get and put only"() {
        when: "no parameters are passed"
        controller.request.method = method
        controller.nameStrings(null)

        then: "? is returned"
        controller.response.status == status

        where:
        method   | status
        'GET'    | 404
        'PUT'    | 404
        'POST'   | 405
        'DELETE' | 405
        'PATCH'  | 405
    }

    void "Test nameStrings"() {
        when: "no parameters are passed"
        controller.request.contentType = "application/json"
        controller.request.format = 'json'
        controller.response.format = 'json'
        controller.request.method = 'GET'
        controller.nameStrings(null)

        then: "not found is returned"
        controller.response.status == 404
        controller.response.text == '{"action":null,"error":"Object not found."}'

        when: "Doodia aspera is provided"
        controller.response.reset()
        controller.response.format = 'json'
        Name doodiaAspera = Name.get(70944)
        controller.nameStrings(doodiaAspera)

        then: "expected name strings are returned"
        controller.response.status == 200
        controller.response.text == '{"action":null,"name":{"class":"au.org.biodiversity.nsl.Name","_links":{"permalink":{"link":"Link for Name 70944: Doodia aspera R.Br.","preferred":true,"resources":1}},"nameElement":"aspera","fullNameHtml":"<scientific><name id=\'70944\'><scientific><name id=\'70914\'><element class=\'Doodia\'>Doodia<\\u002felement><\\u002fname><\\u002fscientific> <element class=\'aspera\'>aspera<\\u002felement> <authors><author id=\'1441\' title=\'Brown, R.\'>R.Br.<\\u002fauthor><\\u002fauthors><\\u002fname><\\u002fscientific>"},"result":{"fullMarkedUpName":"<scientific><name id=\'70944\'><scientific><name id=\'70914\'><element class=\'Doodia\'>Doodia<\\u002felement><\\u002fname><\\u002fscientific> <element class=\'aspera\'>aspera<\\u002felement> <authors><author id=\'1441\' title=\'Brown, R.\'>R.Br.<\\u002fauthor><\\u002fauthors><\\u002fname><\\u002fscientific>","simpleMarkedUpName":"<scientific><name id=\'70944\'><scientific><name id=\'70914\'><element class=\'Doodia\'>Doodia<\\u002felement><\\u002fname><\\u002fscientific> <element class=\'aspera\'>aspera<\\u002felement><\\u002fname><\\u002fscientific>","fullName":"Doodia aspera R.Br.","simpleName":"Doodia aspera","sortName":"doodia aspera"}}'
        controller.response.json.result.fullName == 'Doodia aspera R.Br.'
    }

}
