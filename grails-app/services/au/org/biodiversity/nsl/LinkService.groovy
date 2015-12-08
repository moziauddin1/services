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

package au.org.biodiversity.nsl

import grails.plugins.rest.client.RestResponse
import grails.transaction.Transactional
import org.grails.plugins.metrics.groovy.Timed

@Transactional
class LinkService {
    def restCallService
    def grailsApplication

    @Timed()
    ArrayList getLinksForObject(target) {
        try {
            String url = getLinkServiceUrl(target, 'links', true)
            if (url) {
                RestResponse response = restCallService.nakedGet(url)
                if (response.status == 404) {
                    if (addTargetLink(target)) {
                        response = restCallService.nakedGet(url)
                        if (response.status != 200) {
                            log.error "Links not found for $target, but should be there."
                        }
                    } else {
                        log.error "Links not found for $target, and couldn't be added."
                    }
                }
                //if 404 json.links will be empty
                return response.json as ArrayList
            }
        } catch (Exception e) {
            log.error(e.message)
        }
        return []
    }

    @Timed()
    Boolean addTargetLink(target) {
        String params = targetParams(target) + "&" + mapperAuth()
        String mapper = mapper(true)
        try {
            RestResponse response = restCallService.nakedGet("$mapper/admin/addIdentifier?$params")
            if (response.status != 200) {
                log.error "Get $mapper/admin/addIdentifier?$params failed with status $response.status"
                return false
            }
            if (response.json) {
                if (response.json.error) {
                    log.error("Get $mapper/admin/addIdentifier?$params returned errors: ${response.json.error} ${response.json.errors ?: ''}")
                    return false
                }
                return true
            }
        } catch (e){
            log.error "Error $e.message adding link for $target"
        }
        return false
    }

    @Timed()
    Map getPreferredLinkForObject(target) {
        try {
            String url = getLinkServiceUrl(target, 'preferredLink', true)
            if (url) {
                RestResponse response = restCallService.nakedGet(url)
                if (response.status == 200) {
                    return response.json as Map
                }
                if (response.status == 404) {
                    if (addTargetLink(target)) {
                        response = restCallService.nakedGet(url)
                        if (response.status != 200) {
                            log.error "Link not found for $target, but should be there."
                        }
                    } else {
                        log.error "Link not found for $target, and couldn't be added."
                    }
                }
                log.debug "Couldn't get links, response headers are $response.headers\n response body is: $response.body"
            }
        } catch (Exception e) {
            log.error "Error $e.message getting preferred link for $target"
        }
        return [:]
    }

    String getLinkServiceUrl(target, String endPoint = 'links', Boolean internal = false) {
        String params = targetParams(target)
        if (params) {
            String mapper = mapper(internal)
            String url = "${mapper}/broker/${endPoint}?${params}"
            return url
        }
        return null
    }

    private String mapper(Boolean internal) {
        return (internal ? grailsApplication.config.services.link.internalMapperURL : grailsApplication.config.services.link.mapperURL)
    }

    private String mapperAuth() {
        return "apiKey=${grailsApplication.config.services.mapper.apikey}"
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private String targetParams(target) {
        Map params = getParamMap(target)
        if (params) {
            // if this is the point where we are putting ampersands in, then this is the point where
            // the paramters need to be encoded.
            def nameSpace = URLEncoder.encode(params.nameSpace as String,'UTF-8')
            def objectType = URLEncoder.encode(params.objectType as String,'UTF-8')
            def idNumber = URLEncoder.encode(params.idNumber as String,'UTF-8')

            return "nameSpace=${nameSpace}&objectType=${objectType}&idNumber=${idNumber}"
        }

        log.error "Target ${target?.class?.simpleName} ${target ?: 'null'} is not a mapped type."
        return null
    }

    private static Map getParamMap(Object target) {
        if (target instanceof Name ||
                target instanceof Author ||
                target instanceof Instance ||
                target instanceof Reference ||
                target instanceof InstanceNote
        ) {
            target = JsonRendererService.initializeAndUnproxy(target)
            String objectType = lowerFirst(target.class.simpleName)
            String nameSpace = target.namespace.name.toLowerCase()
            Long idNumber = target.id
            return [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber]
        }


        /*
         For tree and tree-related entities, we use the tree label as the namespace.
         For tree events and for objects attached to trees that don't have labels, we just
         use 'tree as the namespace'.

         This admittedly makes the rule for decoding a little tricky.

         If its type is tree and its id is zero, then the namespace is the label of the tree.
         In all other cases, we can ignore the namespace and use just the type and id.


         tree labels are uppercase at present. Perhaps we need an rdfId column on them,
         as we have with the tree_ns_uri table.
         */

        if (target instanceof Event) {
            return [nameSpace: 'tree', objectType: lowerFirst(target.class.simpleName), idNumber: target.id]
        }

        if (target instanceof Arrangement) {
            if (target.label) {
                // a tree with a label is object 0 of its namespace
                return [nameSpace: target.label, objectType: lowerFirst(target.class.simpleName), idNumber: 0]
            } else {
                // a tree without a label is simply an arrangement of nodes
                return [nameSpace: 'tree', objectType: lowerFirst(target.class.simpleName), idNumber: target.id]
            }
        }

        if (target instanceof Node) {
            return [nameSpace: target.root.label ?: 'tree', objectType: lowerFirst(target.class.simpleName), idNumber: target.id]
        }

        return null
    }

    private static lowerFirst(String string) {
        if (string) {
            String first = Character.toLowerCase(string.charAt(0))
            return first + string.substring(1)
        }
        return string
    }

    Map deleteNameLinks(Name name, String reason) {
        deleteTargetLinks(name, reason)
    }

    Map deleteInstanceLinks(Instance instance, String reason) {
        deleteTargetLinks(instance, reason)
    }

    Map deleteReferenceLinks(Reference reference, String reason) {
        deleteTargetLinks(reference, reason)
    }

    Map deleteTargetLinks(Object target, String reason) {
        String params = targetParams(target) + "&reason=${reason.encodeAsURL()}" + "&" + mapperAuth()
        String mapper = mapper(true)
        try {
            RestResponse response = restCallService.nakedGet("$mapper/admin/deleteIdentifier?$params")
            if (response.status != 200) {
                List<String> errors = getResopnseErrorMessages(response)
                errors << "response was: $response.statusCode.reasonPhrase"
                log.error "Get $mapper/admin/addIdentifier?$params failed with $errors"
                return [success: false, errors: errors]
            }
            if (response.json) {
                List<String> errors = getResopnseErrorMessages(response)
                if (errors) {
                    log.error("Deleting links $mapper/admin/deleteIdentifier?$params ${errors}")
                    return [success: false, errors: errors]
                }
                log.debug(response.json)
                return [success: true]
            }
        } catch (e) {
            log.error e.message
            return [success: false, errors: "Communication error with mapper."]
        }
        return [success: false, errors: "No json response, unknown outcome."]
    }

    Map moveTargetLinks(Object from, Object to) {
        Map fromParams = getParamMap(from)
        Map toParams = getParamMap(to)

        if (fromParams && toParams) {
            String params = "fromNameSpace=${fromParams.nameSpace}&fromObjectType=${fromParams.objectType}&fromIdNumber=${fromParams.idNumber}"
            params += "&toNameSpace=${toParams.nameSpace}&toObjectType=${toParams.objectType}&toIdNumber=${toParams.idNumber}"
            params += "&" + mapperAuth()

            String mapper = mapper(true)
            try {
                RestResponse response = restCallService.nakedGet("$mapper/admin/moveIdentity?$params")
                if (response.status != 200) {
                    List<String> errors = getResopnseErrorMessages(response)
                    errors << "response was: $response.statusCode.reasonPhrase"
                    log.error "Get $mapper/admin/moveIdentity?$params failed with $errors"
                    return [success: false, errors: errors]
                }
                if (response.json) {
                    List<String> errors = getResopnseErrorMessages(response)
                    if (errors) {
                        log.error("Moving links $mapper/admin/moveIdentity?$params ${errors}")
                        return [success: false, errors: errors]
                    }
                    log.debug(response.json)
                    return [success: true]
                }
                return [success: false, errors: "No json response, unknown outcome."]
            } catch (e) {
                log.error e.message
                return [success: false, errors: "Communication error with mapper."]
            }
        }
        return [success: false, errors: "Invalid targets $from, $to."]
    }

    private static List<String> getResopnseErrorMessages(RestResponse response) {
        List<String> errors = []
        if (response?.json) {
            if (response.json.error) {
                errors << response.json.error
            }
            if (response.json.errors) {
                errors.addAll(response.json.errors)
            }
        }
        return errors
    }

}
