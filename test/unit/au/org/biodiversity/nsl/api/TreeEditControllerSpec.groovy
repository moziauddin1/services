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
        TestUte.createUriNs()
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
