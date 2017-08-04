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


import org.grails.plugins.metrics.groovy.Timed
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

class LinkService {
    def restCallService
    def grailsApplication
    def configService
    CacheManager grailsCacheManager

    private String lowerNameSpaceName

    String nameSpace() {
        if (!lowerNameSpaceName) {
            lowerNameSpaceName = configService.getNameSpaceName().toLowerCase()
        }
        return lowerNameSpaceName
    }


    @Timed()
    List<Map> getLinksForObject(target) {
        return doUsingCache(getLinksCache(), target?.id) {
            try {
                List<Map> results = []
                if (target) {
                    String url = getLinkServiceUrl(target, 'links', true)
                    restCallService.json('get', url,
                            { List links ->
                                results = links
                            },
                            { Map data, List errors ->
                                log.error "Couldn't get links for $target. Errors: $errors"
                            },
                            { data ->
                                String link = addTargetLink(target)
                                if (link) {
                                    results = [[link: link, resourceCount: 1, preferred: true]]
                                } else {
                                    log.error "Links not found for $target, and couldn't be added."
                                }
                            },
                            { data ->
                                log.error "Something went wrong getting $url. Response: $data"
                            }
                    )
                }
                return results
            } catch (RestCallException e) {
                log.error(e.message)
                return []
            }
        } as List<Map>
    }

    @Timed()
    String addTargetLink(target) {
        String identity = paramString(nameSpace(), targetParams(target)) + "&" + mapperAuth()
        String mapper = mapper(true)
        String newLink = null
        try {
            String uri = "$mapper/admin/addIdentifier?$identity"

            restCallService.json('put', uri,
                    { data ->
                        newLink = data.preferredURI
                    },
                    { Map data, List errors ->
                        log.error "Couldn't add link for $target. Errors: $errors"
                    },
                    { data ->
                        log.error "404 adding link: $uri. Response: $data"
                    },
                    { data ->
                        log.error "Something went wrong adding link: $uri. Response: $data"
                    }
            )
        } catch (RestCallException e) {
            log.error "Error $e.message adding link for $target"
        }
        return newLink
    }

    @Timed()
    String getPreferredLinkForObject(target) {
        doUsingCache(getLinkCache(), target?.id) {
            String link = null
            try {
                String url = getLinkServiceUrl(target, 'preferredLink', true)

                restCallService.json('get', url,
                        { Map data ->
                            link = data.link
                        },
                        { Map data, List errors ->
                            log.error "Couldn't get links for $target. Errors: $errors"
                        },
                        {
                            link = addTargetLink(target)
                            if (!link) {
                                log.error "Link not found for $target, and couldn't be added."
                            }
                        },
                        { data ->
                            log.error "Something went wrong getting $url. Response: $data"
                        }
                )
            } catch (RestCallException e) {
                log.error "Error $e.message getting preferred link for $target"
            }
            return link
        } as String
    }

    /**
     * Get the domain object matching a URI based identifier. This method asks
     * the mapper for the mapper identity for a URI, and recover the object.
     *
     * It returns null if:-
     * - the uri is not known to the mapper,
     * - there are multiple ids for the uri,
     * - no object matches the id, namespace, and object type
     *
     * @param uri a uri perhaps known to the mapper
     * @return a domain object matching the object type returned by the mapper identity
     */
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
        Long versionNumber = identity.versionNumber

        switch (identity.objectType) {
            case 'name':
                return Name.get(idNumber)
                break
            case 'author':
                return Author.get(idNumber)
                break
            case 'instance':
                return Instance.get(idNumber)
                break
            case 'reference':
                return Reference.get(idNumber)
                break
            case 'instanceNote':
                return InstanceNote.get(idNumber)
                break
            case 'tree':
                if (versionNumber) {
                    TreeVersion treeVersion = TreeVersion.get(versionNumber)
                    if (treeVersion) {
                        return TreeElement.get(new TreeElement(treeVersion: treeVersion, treeElementId: idNumber))
                    }
                }
                return null
                break
            default:
                return null
        }

    }

    /**
     * Ask the mapper for the single mapper identity for a URI. If the uri matches multiple identities,
     * this method returns null. This method also returns null if the uri is unknown to the mapper.
     * @param uri a uri known to the mapper
     * @return The Mapper Identity as a Map including nameSpace, objectType, and idNumber.
     */

    @Timed()
    Map getMapperIdentityForLink(String uri) {
        doUsingCache(getIdentityCache(), uri) {
            Map identity = null
            try {
                String url = "${mapper(true)}/broker/getCurrentIdentity?uri=${URLEncoder.encode(uri, "UTF-8")}"

                restCallService.json('get', url,
                        { List identities ->
                            if (identities.size() == 1) {
                                Map ident = identities[0] as Map
                                identity = [objectType: ident.objectType, nameSpace: ident.nameSpace, idNumber: ident.idNumber, versionNumber: ident.versionNumber]
                            } else {
                                log.error "expected only 1 identity for $uri"
                            }
                        },
                        { Map data, List errors ->
                            log.error "Couldn't get Identity for $uri. Errors: $errors"
                        },
                        { data ->
                            log.error "Identity not found for $uri. Response: $data"
                        },
                        { data ->
                            log.error "Something went wrong getting $url. Response: $data"
                        }
                )
            } catch (RestCallException e) {
                log.error "Error $e.message getting mapper id for $uri"
                return null
            }
            return identity
        } as Map
    }

    String getLinkServiceUrl(target, String endPoint = 'links', Boolean internal = false) {
        String identity = paramString(nameSpace(), targetParams(target))
        if (identity) {
            String mapper = mapper(internal)
            String url = "${mapper}/broker/${endPoint}?${identity}"
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

    private static Map targetParams(Name name) {
        [type: 'name', id: name.id, version: null]
    }

    private static Map targetParams(Author author) {
        [type: 'author', id: author.id, version: null]
    }

    private static Map targetParams(Instance instance) {
        [type: 'instance', id: instance.id, version: null]
    }

    private static Map targetParams(Reference reference) {
        [type: 'reference', id: reference.id, version: null]
    }

    private static Map targetParams(InstanceNote instanceNote) {
        [type: 'instanceNote', id: instanceNote.id, version: null]
    }

    private static Map targetParams(TreeElement treeElement) {
        [type: 'tree', id: treeElement.treeElementId, version: treeElement.treeVersion.id]
    }

    private static String paramString(String nameSpace, Map params) {
        return "nameSpace=${nameSpace}&objectType=${params.type}&idNumber=${params.id}&versionNumber=${params.version}"
    }


    Map deleteNameLinks(Name name, String reason) {
        String identity = paramString(nameSpace(), targetParams(name)) + "&reason=${reason.encodeAsURL()}" + "&" + mapperAuth()
        evictAllCache(name)
        deleteTargetLinks(identity)
    }

    Map deleteInstanceLinks(Instance instance, String reason) {
        String identity = paramString(nameSpace(), targetParams(instance)) + "&reason=${reason.encodeAsURL()}" + "&" + mapperAuth()
        evictAllCache(instance)
        deleteTargetLinks(identity)
    }

    Map deleteReferenceLinks(Reference reference, String reason) {
        String identity = paramString(nameSpace(), targetParams(reference)) + "&reason=${reason.encodeAsURL()}" + "&" + mapperAuth()
        evictAllCache(reference)
        deleteTargetLinks(identity)
    }

    private Map deleteTargetLinks(String identity) {
        String mapper = mapper(true)
        Map result = [success: true]
        try {
            String url = "$mapper/admin/deleteIdentifier?$identity"
            restCallService.json('delete', url,
                    { data ->
                        log.debug "Deleted links for $identity. Response: $data"
                    },
                    { Map data, List errors ->
                        log.error "Couldn't delete $identity links. Errors: $errors"
                        result = [success: false, errors: errors]
                    },
                    { data ->
                        log.error "Couldn't delete $identity links. Not found response: $data"
                        result = [success: false, errors: ["Couldn't delete $identity links. Not found response: $data"]]
                    },
                    { data ->
                        log.error "Couldn't delete $identity links. Response: $data"
                        result = [success: false, errors: ["Couldn't delete $identity links. Response: $data"]]
                    }
            )
        } catch (RestCallException e) {
            log.error e.message
            result = [success: false, errors: "Communication error with mapper."]
        }
        return result
    }

    private static Map fromParamMap(String nameSpace, Map params) {
        [fromNameSpace: nameSpace, fromObjectType: params.type, fromIdNumber: params.id, fromVersionNumber: params.version]
    }

    private static Map toParamMap(String nameSpace, Map params) {
        [toNameSpace: nameSpace, toObjectType: params.type, toIdNumber: params.id, toVersionNumber: params.version]
    }

    Map moveTargetLinks(Object from, Object to) {

        if (from && to) {
            Map paramsMap = fromParamMap(nameSpace(), targetParams(from)) + toParamMap(nameSpace(), targetParams(to))

            String mapper = mapper(true)
            Map result = [success: true]
            try {
                evictAllCache(from)
                evictAllCache(to)

                String url = "$mapper/admin/moveIdentity?${mapperAuth()}"

                restCallService.jsonPost(paramsMap, url,
                        { Map data ->
                            log.debug "Moved $from to $to. Response: $data"
                        },
                        { Map data, List errors ->
                            log.error "Couldn't move $from to $to. Errors: $errors"
                            result = [success: false, errors: errors]
                        },
                        { data ->
                            log.error "Couldn't move $from to $to. Not found response: $data"
                            result = [success: false, errors: ["Couldn't move $from to $to. Not found response: $data"]]
                        },
                        { data ->
                            log.error "Couldn't move $from to $to. Response: $data"
                            result = [success: false, errors: ["Couldn't move $from to $to. Response: $data"]]
                        }
                )

            } catch (RestCallException e) {
                log.error e.message
                result = [success: false, errors: "Communication error with mapper."]
            }
            return result
        } else {
            return [success: false, errors: "Invalid targets $from, $to."]
        }
    }

    Map removeNameLink(Name name, String uri) {
        String identity = paramString(nameSpace(), targetParams(name))
        evictAllCache(name)
        removeTargetLink(identity, uri)
    }

    Map removeInstanceLink(Instance instance, String uri) {
        String identity = paramString(nameSpace(), targetParams(instance))
        evictAllCache(instance)
        removeTargetLink(identity, uri)
    }

    Map removeReferenceLink(Reference reference, String uri) {
        String identity = paramString(nameSpace(), targetParams(reference))
        evictAllCache(reference)
        removeTargetLink(identity, uri)
    }

    private Map removeTargetLink(String identity, String targetUri) {
        String mapper = mapper(true)
        String url = "$mapper/admin/removeIdentityFromURI?$identity" + "&uri=${targetUri.encodeAsURL()}" + '&' + mapperAuth()
        Map result = [success: true]
        String whatImDoing = "Remove link $targetUri from $identity"
        try {
            restCallService.json('delete', url,
                    { Map data ->
                        log.debug "Removed link to $identity. Response: $data"
                    },
                    { Map data, List errors ->
                        log.error "Couldn't $whatImDoing. Errors: $errors"
                        result = [success: false, errors: errors]
                    },
                    { data ->
                        log.error "Couldn't $whatImDoing. Not found response: $data"
                        result = [success: false, errors: ["Couldn't $whatImDoing. Not found response: $data"]]
                    },
                    { data ->
                        log.error "Couldn't $whatImDoing. Response: $data"
                        result = [success: false, errors: ["Couldn't $whatImDoing. Response: $data"]]
                    }
            )
        } catch (RestCallException e) {
            log.error e.message
            result = [success: false, errors: "Communication error with mapper."]
        }
        return result
    }

    /** Cache stuff ******************************/
    private static doUsingCache(Cache cache, Object key, Closure c) {
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

    private void evictAllCache(Object target) {
        // the sequence here is important, as the identity cache uses getLinks
        evictIdentityCache(target)
        evictLinksCache(target)
        evictLinkCache(target)
    }

    private void evictLinksCache(target) {
        if (target) {
            getLinksCache().evict(target.id)
        }
    }

    private void evictIdentityCache(target) {
        if (target) {
            getLinksForObject(target).each { mapperIdentityCacheEvict(it.link as String) }
        }
    }

    private void mapperIdentityCacheEvict(String uri) {
        getIdentityCache().evict(uri)
    }
}
