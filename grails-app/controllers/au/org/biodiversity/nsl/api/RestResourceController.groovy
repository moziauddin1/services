package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.converters.XML
import grails.transaction.Transactional
import org.grails.plugins.metrics.groovy.Timed

import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK

class RestResourceController {

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
        Arrangement tree;
        if (shard == "tree") {
            tree = Arrangement.get(idNumber);
            if (tree == null) {
                return notFound("No tree with id $idNumber found")
            }
        } else {
            tree = Arrangement.findByLabel(shard)
            if (tree == null) {
                return notFound("No tree with name $shard found")
            }
        }

        def links = linkService.getLinksForObject(tree)
        respond tree, [model: [shard: shard, tree: tree, links: links], status: OK]
    }

    @Timed()
    def node(String shard, Long idNumber) {
        Node node = Node.get(idNumber)
        if (node == null) {
            return notFound("No node with id $idNumber found")
        }
        def links = linkService.getLinksForObject(node)
        respond node, [model: [shard: shard, node: node, links: links], status: OK]
    }

    @Timed()
    def event(String shard, Long idNumber) {
        Event event = Event.get(idNumber)
        if (event == null) {
            return notFound("No event with id $idNumber found")
        }
        def links = linkService.getLinksForObject(event)
        respond event, [model: [shard: shard, event: event, links: links], status: OK]
    }

    @Transactional
    @Timed()
    def branch(String shard, Long idNumber) {
        Node node = Node.get(idNumber)
        if (node == null) {
            return notFound("No tree node in $shard with id $idNumber found")
        }

        def links = linkService.getLinksForObject(node)

        Object tree = queryService.getTree(node);

        respond tree as Object, [
                model : [
                        shard : shard,
                        node  : node,
                        branch: tree,
                        tree  : tree,
                        links : links
                ],
                status: OK
        ]
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
