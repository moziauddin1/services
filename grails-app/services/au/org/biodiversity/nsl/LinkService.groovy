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

import com.google.gson.JsonElement
import grails.plugin.cache.Cacheable
import grails.plugins.rest.client.RestResponse
import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
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
    @Cacheable(value = 'linkcache', key = '#target.id')
    String getPreferredLinkForObject(target) {
        try {
            String url = getLinkServiceUrl(target, 'preferredLink', true)
            if (url) {
                RestResponse response = restCallService.nakedGet(url)
                if (response.status == 200) {
                    return response.json.link as String
                }
                if (response.status == 404) {
                    if (addTargetLink(target)) {
                        response = restCallService.nakedGet(url)
                        if (response.status == 200) {
                            return response.json.link as String
                        }
                        log.error "Link not found for $target, but should be there."
                        return null
                    } else {
                        log.error "Link not found for $target, and couldn't be added."
                        return null
                    }
                }
                log.debug "Couldn't get links, status $response.status, response headers are $response.headers\n response body is: $response.body"
            }
        } catch (Exception e) {
            log.error "Error $e.message getting preferred link for $target"
        }
        return null
    }

    /**
     * Ask the mapper for the single mapper id (triple) for a URI, and recover the object. Returns null
     * if the uri is not known to the mapper, if there are multiple ids for the uri, or if the objectType
     * is unknown, or if the namespaces of the mapper triple and the object do not match.
     *
     * The namespace check helps ward against the situation where a shard service is given a uri belonging
     * to some other shard, and there is an idNumber collision.
     *
     * @param uri a uri known to the mapper
     * @return The Mapper JSON. This includes nameSpace, objectType, and idNumber.
     */

    /*
     * This method is not marked cacheable because it returns hibernate objects. It calls getMapperIdForLink()
     * which *is* cacheable. It might be possible to make this method cacheable depending on how the cache plugin
     * manages transaction boundaries
     */
    Object getObjectForLink(String uri) {
        def id = getMapperIdForLink(uri)
        if(!id) {
            return null;
        }


        def obj = null
        Namespace ns = null

        if(id.objectType == 'name') {
            obj = Name.get(id.idNumber)
        }
        else if(id.objectType == 'author') {
            obj = Author.get(id.idNumber)
        }
        else if(id.objectType == 'instance') {
            obj = Instance.get(id.idNumber)
        }
        else if(id.objectType == 'reference') {
            obj = Reference.get(id.idNumber)
        }
        else if(id.objectType == 'instanceNote') {
            obj = InstanceNote.get(id.idNumber)
        }
        else if(id.objectType == 'event') {
            obj = Event.get(id.idNumber)
        }
        else if(id.objectType == 'tree') {
            obj = Arrangement.get(id.idNumber)
        }
        else if(id.objectType == 'node') {
            obj = Node.get(id.idNumber)
            // the node namespace is handled a little differently
            ns = ((Node)obj)?.root?.namespace
        }
        else {
            return null
        }

        if(!obj) {
            log.warn "Object for $id not found"
            return null;
        }

        if(!ns) ns = obj.namespace;

        if(ns.name.toLowerCase() != id.nameSpace) {
            log.warn "namespace mismatch on $id. Should be ${ns.name.toLowerCase()}."
            return null;
        }

        return obj
    }

    /**
     * Ask the mapper for the single mapper id (triple) for a URI. If the id matches multiple triples,
     * this method returns null. This method also returns null if the uri is unknown to the mapper.
     * @param uri a uri known to the mapper
     * @return The Mapper JSON. This includes nameSpace, objectType, and idNumber.
     */
    /* todo: add @Cacheable annotation.
     * In order to make this work properly, other methods here that as the mapper to create and delete ids need to
     * update the cache
     */
    @Timed()
    def getMapperIdForLink(String uri) {
        try {
            /*
              The usual kind of issue: sending this without encoding the uri is technically wrong, but if I
              ULREncode it then something along the way double-encodes it and I get a 404.

              String url = "${mapper(false)}/broker/getCurrentIdentity?uri=${URLEncoder.encode(uri, "UTF-8")}"
             */

            String url = "${mapper(false)}/broker/getCurrentIdentity?uri=${uri}"
            log.debug(url);
            RestResponse response = restCallService.nakedGet(url)
            if(response.status != 200) return null;
            if(!response.json) {
                log.warn "expected a json response for $uri"
                return null
            }
            if(!(response.json instanceof JSONArray)) {
                log.warn "expected a json array for $uri"
                return null
            }
            if(((JSONArray)response.json).size() != 1) {
                log.warn "expected 1 and only 1 identity for $uri"
                return null
            }
            return ((JSONArray)response.json)[0];
        } catch (Exception e) {
            log.error "Error $e.message getting mapper id for $uri"
        }
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
                target instanceof InstanceNote ||
                target instanceof Event
        ) {
            target = JsonRendererService.initializeAndUnproxy(target)
            String objectType = lowerFirst(target.class.simpleName)
            String nameSpace = target.namespace.name.toLowerCase()
            Long idNumber = target.id
            return [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber]
        }

        if (target instanceof Arrangement) {
            // override the default, because we use 'tree' rather than 'arrangement'
            target = JsonRendererService.initializeAndUnproxy(target)
            String objectType = 'tree'
            String nameSpace = target.namespace.name.toLowerCase()
            Long idNumber = target.id
            return [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber]
        }

        if (target instanceof Node) {
            // override the default, because the namespace id on the node root rather than the root itself
            target = JsonRendererService.initializeAndUnproxy(target)
            String objectType = lowerFirst(target.class.simpleName)
            String nameSpace = ((Node)target).root.namespace.name.toLowerCase()
            Long idNumber = target.id
            return [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber]
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
