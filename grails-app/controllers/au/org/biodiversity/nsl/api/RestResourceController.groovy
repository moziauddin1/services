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

import au.org.biodiversity.nsl.*
import grails.converters.XML
import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

class RestResourceController {
    GrailsApplication grailsApplication
    def linkService
    def apniFormatService
    def queryService

//    @SuppressWarnings("GroovyUnusedDeclaration")
//    static responseFormats = ['json', 'xml', 'html']

    static allowedMethods = ['*': "GET"]

    @Timed()
    def name(String shard, Long idNumber) {
        Name name = Name.get(idNumber)
        if (name == null) {
            return notFound("No name in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(name)
        Map model = apniFormatService.getNameModel(name)
        model << [links: links]
        respond name, [model: model, status: OK]
    }

    @Timed()
    def instance(String shard, Long idNumber) {
        Instance instance = Instance.get(idNumber)
        if (instance == null) {
            return notFound("No instance in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(instance)
        respond instance, [model: [instance: instance, links: links], status: OK]
    }

    @Timed()
    def author(String shard, Long idNumber) {
        Author author = Author.get(idNumber)
        if (author == null) {
            return notFound("No author in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(author)
        respond author, [model: [author: author, links: links], status: OK]
    }

    @Timed()
    def reference(String shard, Long idNumber) {
        Reference reference = Reference.get(idNumber)
        if (reference == null) {
            return notFound("No reference in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(reference)
        respond reference, [model: [reference: reference, links: links], status: OK]
    }

    @Timed()
    def instanceNote(String shard, Long idNumber) {
        InstanceNote instanceNote = InstanceNote.get(idNumber)
        if (instanceNote == null) {
            return notFound("No instanceNote in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(instanceNote)
        respond instanceNote, [model: [instanceNote: instanceNote, links: links], status: OK]
    }

    @Timed()
    def tree(String shard, Long idNumber) {
        Arrangement tree = Arrangement.get(idNumber);
        if (tree == null) {
            return notFound("No tree in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(tree)
        respond tree, [model: [tree: tree, links: links], status: OK]
    }

    @Timed()
    def node(String shard, Long idNumber) {
        Node node = Node.get(idNumber)
        if (node == null) {
            return notFound("No node in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(node)
        respond node, [model: [node: node, links: links], status: OK]
    }

    @Timed()
    def event(String shard, Long idNumber) {
        Event event = Event.get(idNumber)
        if (event == null) {
            return notFound("No event in $shard with id $idNumber found")
        }
        def links = linkService.getLinksForObject(event)
        respond event, [model: [event: event, links: links], status: OK]
    }
    
    @Timed()
    def nslSimpleName(String shard, Long idNumber) {
        NslSimpleName simpleName = NslSimpleName.get(idNumber)
        if (simpleName == null) {
            return notFound("No name in $shard with id $idNumber found")
        }
        def links = []
        Map model = [name: simpleName, links: links]
        respond simpleName, [model: model, status: OK]
    }


    private notFound(String errorText) {
        response.status = NOT_FOUND.value()
        Map errorResponse = [error: errorText]
        withFormat {
            html {
                render(text: errorText)
            }
            json {
                render(contentType: 'application/json') { errorResponse }
            }
            xml {
                render errorResponse as XML
            }
        }
    }
}
