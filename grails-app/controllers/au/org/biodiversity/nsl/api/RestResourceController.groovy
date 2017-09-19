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
import grails.converters.JSON
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
    def treeService

//    static allowedMethods = [
//            '*'        : 'GET',
//            'bulkFetch': 'POST'
//    ]

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
        TreeVersion treeVersion
        treeVersion = Tree.get(idNumber)?.currentTreeVersion
        if (treeVersion == null) {
            return notFound("We couldn't find a tree version with id $shard")
        }
        List<TreeVersion> versions = TreeVersion.findAllByTree(treeVersion.tree, [sort: 'id', order: 'desc'])
        if (response.format == 'html') {
            List<DisplayElement> children = treeService.displayElementsToLimit(treeVersion, 2000)
            log.debug "Showing ${children.size()} child elements."
            respond treeVersion, [model: [treeVersion: treeVersion, versions: versions, children: children], status: OK]
        } else {
            List<DisplayElement> children = treeService.displayElementsToDepth(treeVersion, 1)
            log.debug "Showing ${children.size()} child elements."
            respond treeVersion, [model: [treeVersion: treeVersion, versions: versions], status: OK]
        }
    }

    @Timed()
    def treeVersion(String shard, Long idNumber) {
        TreeVersion treeVersion
        treeVersion = TreeVersion.get(idNumber)
        if (treeVersion == null) {
            return notFound("We couldn't find a tree version with id $idNumber")
        }
        List<TreeVersion> versions = TreeVersion.findAllByTree(treeVersion.tree, [sort: 'id', order: 'desc'])
        List<DisplayElement> children = treeService.displayElementsToLimit(treeVersion, 2000)
        log.debug "Showing ${children.size()} child elements."
        respond(treeVersion, [view: 'tree', model: [treeVersion: treeVersion, versions: versions, children: children], status: OK])
    }

    @Timed()
    def treeElement(Long shard, Long idNumber) {
        if (idNumber == 0) {
            forward(action: 'tree', model: [version: shard])
        }
        log.debug "Tree Element version $shard, element $idNumber"
        TreeVersionElement treeVersionElement = treeService.getTreeVersionElement(shard, idNumber)

        if (treeVersionElement) {
            List<DisplayElement> children = treeService.childDisplayElements(treeVersionElement)
            List<TreeVersionElement> path = treeService.getElementPath(treeVersionElement)
            respond(treeVersionElement, [model: [treeVersionElement: treeVersionElement, path: path, children: children, status: OK]])
            return
        }
        notFound("Couldn't find element $idNumber in tree version $shard.")
    }

    /**
     * This endpoint is a transation from node to tree_element. Node id's really only gave you access to the latest
     * version of the tree that the node participates in, not the tree from the point in time that it was referenced.
     * The tree below the node will look the same though the rest of the tree around the node may have changed.
     *
     * Get the latest version of the the tree element with the tree element id given
     * @param shard
     * @param idNumber
     * @return
     */
    @Timed()
    def node(String shard, Long idNumber) {
        notFound("Deprecated Node API. Use the Node URI to access the resource. No node in $shard with id $idNumber found")
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

    // not sure why this needs to be wrapped in a transaction

    @Timed()
    @Transactional
    def bulkFetch() {
        log.debug "Bulk Fetch request: $request.JSON"
        return render(request.JSON.collect { uri -> linkService.getObjectForLink(uri as String) } as JSON)
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
