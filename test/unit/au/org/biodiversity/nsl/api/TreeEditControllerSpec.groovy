package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.tree.AsRdfRenderableService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TreeEditController)
@TestMixin(DomainClassUnitTestMixin)
@Mock([UriNs])
class TreeEditControllerSpec extends Specification {

    def setup() {
        controller.asRdfRenderableService = new AsRdfRenderableService()
        controller.asRdfRenderableService.transactionManager = getTransactionManager()
        [
                [
                        "id"                    : 1,
                        "lock_version"          : 1,
                        "description"           : "No namespace - the ID contains the full URI.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "none",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "no namespace",
                        "uri"                   : ""
                ],
                [
                        "id"                    : 2,
                        "lock_version"          : 1,
                        "description"           : "Top level BOATREE vocabulary.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "boatree-voc",
                        "owner_uri_id_part"     : "http://biodiversity.org.au/voc/boatree/BOATREE",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "BOATREE",
                        "uri"                   : "http://biodiversity.org.au/voc/boatree/BOATREE#"
                ],
                [
                        "id"                    : 1000,
                        "lock_version"          : 1,
                        "description"           : "Namespace of the vocabularies served by this instance.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "voc",
                        "owner_uri_id_part"     : "voc",
                        "owner_uri_ns_part_id"  : 1000,
                        "title"                 : "voc namespace",
                        "uri"                   : "http://biodiversity.org.au/boatree/voc/"
                ],
                [
                        "id"                    : 1001,
                        "lock_version"          : 1,
                        "description"           : "Namespace of the uri namespaces.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "ns",
                        "owner_uri_id_part"     : "ns",
                        "owner_uri_ns_part_id"  : 1000,
                        "title"                 : "uri_ns namespace",
                        "uri"                   : "http://biodiversity.org.au/boatree/voc/ns#"
                ],
                [
                        "id"                    : 1002,
                        "lock_version"          : 1,
                        "description"           : "Namespace for top-level public trees, by text identifier.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "clsf",
                        "owner_uri_id_part"     : "classification",
                        "owner_uri_ns_part_id"  : 1000,
                        "title"                 : "classification namespace",
                        "uri"                   : "http://biodiversity.org.au/boatree/classification/"
                ],
                [
                        "id"                    : 1003,
                        "lock_version"          : 1,
                        "description"           : "Namespace for all arrangemnts, by physical id.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "arr",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "arrangement namespace",
                        "uri"                   : "http://biodiversity.org.au/boatree/arrangement/"
                ],
                [
                        "id"                    : 1004,
                        "lock_version"          : 1,
                        "description"           : "Namespace for arrangement nodes.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "node",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "arrangement node namespace",
                        "uri"                   : "http://biodiversity.org.au/boatree/node/"
                ],
                [
                        "id"                    : 1005,
                        "lock_version"          : 1,
                        "description"           : "Base datatypes.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "xs",
                        "owner_uri_id_part"     : "http://www.w3.org/1999/02/22-rdf-syntax-ns",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "XML Schema",
                        "uri"                   : "http://www.w3.org/2001/XMLSchema#"
                ],
                [
                        "id"                    : 1006,
                        "lock_version"          : 1,
                        "description"           : "Namespace for rdf.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "rdf",
                        "owner_uri_id_part"     : "http://www.w3.org/1999/02/22-rdf-syntax-ns",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "rdf namespace",
                        "uri"                   : "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                ],
                [
                        "id"                    : 1007,
                        "lock_version"          : 1,
                        "description"           : "Namespace for rdf schema.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "rdfs",
                        "owner_uri_id_part"     : "http://www.w3.org/2000/01/rdf-schema",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "rdf schema namespace",
                        "uri"                   : "http://www.w3.org/2000/01/rdf-schema#"
                ],
                [
                        "id"                    : 1008,
                        "lock_version"          : 1,
                        "description"           : "Namespace for Dublin Core.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "dc",
                        "owner_uri_id_part"     : "http://purl.org/dc/elements/1.1",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "dublin core",
                        "uri"                   : "http://purl.org/dc/elements/1.1/"
                ],
                [
                        "id"                    : 1009,
                        "lock_version"          : 1,
                        "description"           : "Namespace for Dublin Core terms.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "dcterms",
                        "owner_uri_id_part"     : "http://purl.org/dc/terms",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "dublin core terms",
                        "uri"                   : "http://purl.org/dc/terms/"
                ],
                [
                        "id"                    : 1010,
                        "lock_version"          : 1,
                        "description"           : "Namespace for Web Ontology Language (OWL).",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "owl",
                        "owner_uri_id_part"     : "http://www.w3.org/2002/07/owl",
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "Web Ontology Language",
                        "uri"                   : "http://www.w3.org/2002/07/owl#"
                ],
                [
                        "id"                    : 1011,
                        "lock_version"          : 1,
                        "description"           : "An APNI name.",
                        "id_mapper_namespace_id": 1,
                        "id_mapper_system"      : "PLANT_NAME",
                        "label"                 : "apni-name",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "APNI name",
                        "uri"                   : "http://biodiversity.org.au/apni.name/"
                ],
                [
                        "id"                    : 1012,
                        "lock_version"          : 1,
                        "description"           : "An APNI taxon.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "apni-taxon",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "APNI taxon",
                        "uri"                   : "http://biodiversity.org.au/apni.taxon/"
                ],
                [
                        "id"                    : 1013,
                        "lock_version"          : 1,
                        "description"           : "An AFD name.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "afd-name",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "AFD name",
                        "uri"                   : "http://biodiversity.org.au/afd.name/"
                ],
                [
                        "id"                    : 1014,
                        "lock_version"          : 1,
                        "description"           : "An AFD taxon.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "afd-taxon",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "AFD taxon",
                        "uri"                   : "http://biodiversity.org.au/afd.taxon/"
                ],
                [
                        "id"                    : 1017,
                        "lock_version"          : 1,
                        "description"           : "Vocabulary terms relating specifically to the APC tree.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "apc-voc",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "APC vocabulary",
                        "uri"                   : "http://biodiversity.org.au/voc/apc/APC#"
                ],
                [
                        "id"                    : 1015,
                        "lock_version"          : 1,
                        "description"           : "An NSL name.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "nsl-name",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "NSL name",
                        "uri"                   : "http://id.biodiversity.org.au/name/apni/"
                ],
                [
                        "id"                    : 1016,
                        "lock_version"          : 1,
                        "description"           : "An NSL instance.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "nsl-instance",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "NSL instance",
                        "uri"                   : "http://id.biodiversity.org.au/instance/apni/"
                ],
                [
                        "id"                    : 1018,
                        "lock_version"          : 1,
                        "description"           : "APC_CONCEPT.APC_ID from APNI.",
                        "id_mapper_namespace_id": null,
                        "id_mapper_system"      : null,
                        "label"                 : "apc-concept",
                        "owner_uri_id_part"     : null,
                        "owner_uri_ns_part_id"  : 0,
                        "title"                 : "APC placement",
                        "uri"                   : "http://id.biodiversity.org.au/legacy-apc/"
                ]
        ].each { params ->
            UriNs uriNs = new UriNs(params)
            uriNs.id = params.id
            uriNs.save()
        }
    }

    def cleanup() {
    }

    void "test error response"() {
        when: "I pass in an invalid param object to placeApcInstance"
        PlaceApcInstanceParam params = new PlaceApcInstanceParam()
        controller.placeApcInstance(params)

        then: "we get something horrible as a response"
        controller.response.text == '{"success":false,"validationErrors":{"hasError":[{"rejectedValue":"","field":"instance","arguments":["instance","class au.org.biodiversity.nsl.api.PlaceApcInstanceParam"],"defaultMessage":"Property [{0}] of class [{1}] cannot be null","objectName":"au.org.biodiversity.nsl.api.PlaceApcInstanceParam","codes":["au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error.instance","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.Instance","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error","placeApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance","placeApcInstanceParam.instance.nullable.error.instance","placeApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.Instance","placeApcInstanceParam.instance.nullable.error","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.instance","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.Instance","au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable","placeApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance","placeApcInstanceParam.instance.nullable.instance","placeApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.Instance","placeApcInstanceParam.instance.nullable","nullable.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance","nullable.instance","nullable.au.org.biodiversity.nsl.Instance","nullable"],"isBindingFailure":false,"code":"nullable","rdf:type":[{"blank":false,"encoded":"FieldError","nsPart":{"class":"au.org.biodiversity.nsl.UriNs","id":1,"description":"No namespace - the ID contains the full URI.","idMapperNamespaceId":null,"idMapperSystem":null,"label":"none","ownerUriIdPart":null,"ownerUriNsPart":null,"title":"no namespace","uri":""},"idPart":"FieldError"},{"blank":false,"encoded":"ObjectError","nsPart":{"class":"au.org.biodiversity.nsl.UriNs","id":1,"description":"No namespace - the ID contains the full URI.","idMapperNamespaceId":null,"idMapperSystem":null,"label":"none","ownerUriIdPart":null,"ownerUriNsPart":null,"title":"no namespace","uri":""},"idPart":"ObjectError"}]}],"rdf:type":[{"blank":false,"encoded":"ValidationErrors","nsPart":{"class":"au.org.biodiversity.nsl.UriNs","id":1,"description":"No namespace - the ID contains the full URI.","idMapperNamespaceId":null,"idMapperSystem":null,"label":"none","ownerUriIdPart":null,"ownerUriNsPart":null,"title":"no namespace","uri":""},"idPart":"ValidationErrors"}]}}'
       /* so that error response looks something like this....
        [
                "success"         : false,
                "validationErrors": [
                        "hasError": [
                                [
                                        "rejectedValue"   : "",
                                        "field"           : "instance",
                                        "arguments"       : ["instance", "class au.org.biodiversity.nsl.api.PlaceApcInstanceParam"],
                                        "defaultMessage"  : "Property [[0]] of class [[1]] cannot be null",
                                        "objectName"      : "au.org.biodiversity.nsl.api.PlaceApcInstanceParam",
                                        "codes"           : ["au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error.instance",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.Instance",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.error",
                                                             "placeApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance",
                                                             "placeApcInstanceParam.instance.nullable.error.instance",
                                                             "placeApcInstanceParam.instance.nullable.error.au.org.biodiversity.nsl.Instance",
                                                             "placeApcInstanceParam.instance.nullable.error",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.instance",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.Instance",
                                                             "au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance.nullable",
                                                             "placeApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance",
                                                             "placeApcInstanceParam.instance.nullable.instance",
                                                             "placeApcInstanceParam.instance.nullable.au.org.biodiversity.nsl.Instance",
                                                             "placeApcInstanceParam.instance.nullable",
                                                             "nullable.au.org.biodiversity.nsl.api.PlaceApcInstanceParam.instance",
                                                             "nullable.instance",
                                                             "nullable.au.org.biodiversity.nsl.Instance",
                                                             "nullable"],
                                        "isBindingFailure": false,
                                        "code"            : "nullable",
                                        "rdf:type"        : [
                                                [
                                                        "blank"  : false,
                                                        "encoded": "FieldError",
                                                        "nsPart" : [
                                                                "class"              : "au.org.biodiversity.nsl.UriNs",
                                                                "id"                 : 1,
                                                                "description"        : "No namespace - the ID contains the full URI.",
                                                                "idMapperNamespaceId": null,
                                                                "idMapperSystem"     : null,
                                                                "label"              : "none",
                                                                "ownerUriIdPart"     : null,
                                                                "ownerUriNsPart"     : null,
                                                                "title"              : "no namespace",
                                                                "uri"                : ""
                                                        ],
                                                        "idPart" : "FieldError"
                                                ],
                                                [
                                                        "blank"  : false,
                                                        "encoded":
                                                                "ObjectError",
                                                        "nsPart" : [
                                                                "class"              : "au.org.biodiversity.nsl.UriNs",
                                                                "id"                 : 1,
                                                                "description"        : "No namespace - the ID contains the full URI.",
                                                                "idMapperNamespaceId": null,
                                                                "idMapperSystem"     : null,
                                                                "label"              : "none",
                                                                "ownerUriIdPart"     : null,
                                                                "ownerUriNsPart"     : null,
                                                                "title"              : "no namespace",
                                                                "uri"                : ""
                                                        ],
                                                        "idPart" : "ObjectError"
                                                ]
                                        ]
                                ]
                        ],
                        "rdf:type": [
                                [
                                        "blank"  : false,
                                        "encoded": "ValidationErrors",
                                        "nsPart" : [
                                                "class"              : "au.org.biodiversity.nsl.UriNs",
                                                "id"                 : 1,
                                                "description"        : "No namespace - the ID contains the full URI.",
                                                "idMapperNamespaceId": null,
                                                "idMapperSystem"     : null,
                                                "label"              : "none",
                                                "ownerUriIdPart"     : null,
                                                "ownerUriNsPart"     : null,
                                                "title"              : "no namespace",
                                                "uri"                : ""
                                        ],
                                        "idPart" : "ValidationErrors"
                                ]
                        ]
                ]
        ]   */
    }
}
