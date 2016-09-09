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
import org.codehaus.groovy.grails.web.json.JSONArray
import org.grails.plugins.metrics.groovy.Timed
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

@Transactional
class LinkService {
    def restCallService
    def grailsApplication
    CacheManager grailsCacheManager

    @Timed()
    ArrayList<Map> getLinksForObject(target) {
        return doUsingCache(getLinksCache(), target?.id) {
            try {
                String url = getLinkServiceUrl(target, 'links', true)
                if (url) {
                    RestResponse response = restCallService.nakedGet(url)
                    if (response.status == 200) {
                        //copy the json response objects to a real ArrayList<Map> which is serializable
                        ArrayList<Map> links = []
                        ArrayList responseLinks = response.json as ArrayList
                        responseLinks.each {Map linkMap ->
                            links.add(new LinkedHashMap(linkMap))
                        }
                        return links
                    }
                    if (response.status == 404) {
                        String link = addTargetLink(target)
                        if (link) {
                            return [[link: link, resourceCount: 1, preferred: true]]  as ArrayList<Map>
                        } else {
                            log.error "Links not found for $target, and couldn't be added."
                            return []
                        }
                    }
                    log.error "Couldn't get links for $target. $response.status: $response.json"
                    return []
                }
            } catch (Exception e) {
                log.error(e.message)
            }
            return []
        } as ArrayList<Map>
    }

    @Timed()
    private String addTargetLink(target) {
        String params = targetParams(target) + "&" + mapperAuth()
        String mapper = mapper(true)
        try {
            RestResponse response = restCallService.nakedGet("$mapper/admin/addIdentifier?$params")
            if (response.status != 200) {
                log.error "Get $mapper/admin/addIdentifier?$params failed with status $response.status. ${response.json}"
                return null
            }
            if (response.json) {
                if (response.json.error) {
                    log.error("Get $mapper/admin/addIdentifier?$params returned errors: ${response.json.error} ${response.json.errors ?: ''}")
                    return null
                }
                log.debug "$response.json"
                return response.json.preferredURI as String
            }
        } catch (e) {
            log.error "Error $e.message adding link for $target"
        }
        return null
    }

    @Timed()
    String getPreferredLinkForObject(target) {
        doUsingCache(getLinkCache(), target?.id) {

            try {
                String url = getLinkServiceUrl(target, 'preferredLink', true)
                if (url) {
                    RestResponse response = restCallService.nakedGet(url)
                    if (response.status == 200) {
                        return response.json.link as String
                    }
                    if (response.status == 404) {
                        String link = addTargetLink(target)
                        if (link) {
                            return link as String
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
        } as String
    }

    /**
     * Get the domain object matching a URI based identifier. This method asks
     * the mapper for the mapper id (triple) for a URI, and recover the object.
     *
     * It returns null if:-
     * - the uri is not known to the mapper,
     * - there are multiple ids for the uri,
     * - no object matches the id, namespace, and object type
     *
     * @param uri a uri known to the mapper
     * @return a domain object matching the object type returned by the mapper identity
     */
    @SuppressWarnings("GroovyUnusedDeclaration")
    @Timed()
    Object getObjectForLink(String uri) {
        Map identity = getMapperIdentityForLink(uri)
        if (!identity) {
            return null
        }

        def domainObject = getDomainObjectFromIdentity(identity)

        if (!domainObject) {
            log.error "Object for $identity not found"
        }

        return domainObject

    }

    /**
     * Find the domain object for a given Identity Map (triple) of:
     *
     *  nameSpace, objectType, idNumber
     *
     * @param identity
     * @return Domain object or null if not found
     */
    private static Object getDomainObjectFromIdentity(Map identity) {

        Long idNumber = identity.idNumber
        Namespace ns = Namespace.findByNameIlike(identity.nameSpace as String)

        switch (identity.objectType) {
            case 'name':
                return Name.findByIdAndNamespace(idNumber, ns)
                break
            case 'author':
                return Author.findByIdAndNamespace(idNumber, ns)
                break
            case 'instance':
                return Instance.findByIdAndNamespace(idNumber, ns)
                break
            case 'reference':
                return Reference.findByIdAndNamespace(idNumber, ns)
                break
            case 'instanceNote':
                return InstanceNote.findByIdAndNamespace(idNumber, ns)
                break
            case 'event':
                return Event.findByIdAndNamespace(idNumber, ns)
                break
            case 'tree':
                return Arrangement.findByIdAndNamespace(idNumber, ns)
                break
            case 'node':
                Node n = Node.findById(idNumber)
                if (n && n.root.namespace == ns)
                    return n;
                else
                    return null;
                break
            default:
                return null
        }

    }

    /**
     * Ask the mapper for the single mapper id (triple) for a URI. If the id matches multiple triples,
     * this method returns null. This method also returns null if the uri is unknown to the mapper.
     * @param uri a uri known to the mapper
     * @return The Mapper Identity as a Map including nameSpace, objectType, and idNumber.
     */

    @Timed()
    Map getMapperIdentityForLink(String uri) {
        doUsingCache(getIdentityCache(), uri) {
            try {
                String url = "${mapper(true)}/broker/getCurrentIdentity?uri=${URLEncoder.encode(uri, "UTF-8")}"
                log.debug(url)
                JSONArray data = restCallService.get(url) as JSONArray
                if (data.size() != 1) {
                    log.error "expected only 1 identity for $uri"
                    return null
                }

                return data[0] as Map
            } catch (RestCallException e) {
                log.error "Error $e.message getting mapper id for $uri"
                return null
            } catch (Exception e) {
                log.error "Error $e.message getting mapper id for $uri"
                return null
            }
        } as Map
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
            def nameSpace = URLEncoder.encode(params.nameSpace as String, 'UTF-8')
            def objectType = URLEncoder.encode(params.objectType as String, 'UTF-8')
            def idNumber = URLEncoder.encode(params.idNumber as String, 'UTF-8')

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
            target = JsonRendererService.initializeAndUnproxy(target as Arrangement)
            String objectType = 'tree'
            String nameSpace = target.namespace.name.toLowerCase()
            Long idNumber = target.id
            return [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber]
        }

        if (target instanceof Node) {
            // override the default, because the namespace id on the node root rather than the root itself
            target = JsonRendererService.initializeAndUnproxy(target as Node)
            String objectType = 'node'
            String nameSpace = (target).root.namespace.name.toLowerCase()
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
            // the sequence here is important, as the identity cache uses getLinks
            evictIdentityCache(target);
            evictLinksCache(target);
            evictLinkCache(target);

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
                // the sequence here is important, as the identity cache uses getLinks
                evictIdentityCache(from);
                evictIdentityCache(to);
                evictLinksCache(from);
                evictLinksCache(to);
                evictLinkCache(from);
                evictLinkCache(to);

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
                errors.add(response.json.error as String)
            }
            if (response.json.errors) {
                errors.addAll(response.json.errors as List<String>)
            }
        }
        return errors
    }

    private def doUsingCache(Cache cache, Object key, Closure c) {
        if (key && cache.get(key)) {
            return ((Cache.ValueWrapper) cache.get(key)).get()
        }

        Object value = c()

        if (key && value) {
            cache.put(key, value)
        }

        return value
    }

    private Cache getLinkCache() {
        return grailsCacheManager.getCache('linkcache')
    }

    private Cache getLinksCache() {
        return grailsCacheManager.getCache('linkscache')
    }

    private Cache getIdentityCache() {
        return grailsCacheManager.getCache('identitycache')
    }

    private void evictLinkCache(target) {
        if (target) {
            getLinkCache().evict(target.id)
        }
    }

    private void evictLinksCache(target) {
        if (target) {
            getLinksCache().evict(target.id)
        }
    }

    void evictIdentityCache(target) {
        if (target) {
            getLinksForObject(target).each { mapperIdentityCacheEvict(it.link) };
        }
    }

    void mapperIdentityCacheEvict(String uri) {
        getIdentityCache().evict(uri)
    }
}
