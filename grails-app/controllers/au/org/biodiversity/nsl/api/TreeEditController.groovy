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

import org.codehaus.groovy.grails.commons.GrailsApplication

import grails.converters.JSON
import grails.transaction.Transactional
import grails.validation.Validateable
import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.tree.*
import static au.org.biodiversity.nsl.tree.DomainUtils.*

@Transactional
class TreeEditController {
    def configService
    AsRdfRenderableService asRdfRenderableService
    TreeViewService treeViewService
    TreeOperationsService treeOperationsService
    QueryService queryService
    ClassificationService classificationService
    NameTreePathService nameTreePathService

    /** @deprecated */

    @Deprecated
    def placeApniName(PlaceApniNameParam p) {
        // this should invoke the classification service

        return render([success         : false,
                       validationErrors: TMP_RDF_TO_MAP.asMap(asRdfRenderableService.springErrorsAsRenderable(p.errors))]) as JSON
    }

    def placeApcInstance(PlaceApcInstanceParam p) {
        // most of this code belongs in the classification service

        log.debug "placeApcInstance(${p})"
        if (!p.validate()) {
            log.debug "!p.validate()"
            RdfRenderable err = asRdfRenderableService.springErrorsAsRenderable(p.errors)
            Map<?, ?> result = [success: false, validationErrors: TMP_RDF_TO_MAP.asMap(err)]
            return render(result as JSON)
        }

        Arrangement apc = Arrangement.findByNamespaceAndLabel(
                configService.nameSpace,
                configService.classificationTreeName)

        Uri nodeTypeUri
        Uri linkTypeUri

        if (p.placementType == 'DeclaredBt') {
            nodeTypeUri = uri('apc-voc', 'DeclaredBt')
        } else if (p.placementType == 'ApcExcluded') {
            nodeTypeUri = uri('apc-voc', 'ApcExcluded')
        } else {
            nodeTypeUri = uri('apc-voc', 'ApcConcept')
        }

        if (p.supername == null) {
            linkTypeUri = uri('apc-voc', 'topNode')
        } else {
            linkTypeUri = extractLinkTypeUri(apc, p.instance.name)
        }

        Boolean nameExists = !queryService.findCurrentNslNamePlacement(apc, p.instance.name).isEmpty()

        try {
            log.debug "perform update/add"

            def profileData = [:]

            if (nameExists)
                treeOperationsService.updateNslName(apc, p.instance.name, p.supername, p.instance,
                        nodeTypeUri: nodeTypeUri, linkTypeUri: linkTypeUri, profileData)
            else
                treeOperationsService.addNslName(apc, p.instance.name, p.supername, p.instance,
                        nodeTypeUri: nodeTypeUri, linkTypeUri: linkTypeUri, profileData)

            apc = refetchArrangement(apc)
            refetch(p)
        }
        catch (ServiceException ex) {
            RdfRenderable err = asRdfRenderableService.serviceExceptionAsRenderable(ex)
            Map result = [success: false, serviceException: TMP_RDF_TO_MAP.asMap(err)]
            log.debug "ServiceException"
            log.warn ex
            return render(result as JSON)
        }

        def result = [success: true]
        log.debug "treeViewService.getInstancePlacementInTree"
        Node currentNode = classificationService.isNameInClassification(p.instance.name, apc)

        nameTreePathService.updateNameTreePathFromNode(currentNode)

        Map npt = treeViewService.getInstancePlacementInTree(apc, p.instance)
        result << npt

        log.debug "render(result as JSON)"
        return render(result as JSON)
    }

    private Uri extractLinkTypeUri(Arrangement apc, Name name) {
        Uri linkTypeUri = null
        List<Link> supernameLinks = queryService.findCurrentNslNamePlacement(apc, name)
        if (supernameLinks.size() > 0) {
            Link supernameLink = supernameLinks.first()
            if (supernameLink.supernode.typeUriIdPart == 'DeclaredBt') {
                linkTypeUri = uri('apc-voc', 'declaredBtOf')
            }
        }
        return linkTypeUri
    }

    def removeApcInstance(RemoveApcInstanceParam p) {
        // most of this code belongs in the classification service

        log.debug "removeAPCInstance(${p})"
        if (!p.validate()) {
            log.debug "!p.validate()"
            RdfRenderable err = asRdfRenderableService.springErrorsAsRenderable(p.errors)
            Map result = [success: false, validationErrors: TMP_RDF_TO_MAP.asMap(err)]
            return render(result as JSON)
        }

        Arrangement apc = Arrangement.findByNamespaceAndLabel(
                configService.nameSpace,
                configService.classificationTreeName)

        try {
            log.debug "perform remove"
            treeOperationsService.deleteNslInstance(apc, p.instance, p.replacementInstance)
            apc = refetchArrangement(apc)
            refetch(p)
        }
        catch (ServiceException ex) {
            RdfRenderable err = asRdfRenderableService.serviceExceptionAsRenderable(ex)
            Map result = [success: false, serviceException: TMP_RDF_TO_MAP.asMap(err)]
            log.debug "ServiceException"
            log.warn ex
            return render(result as JSON)
        }

        //		sessionFactory_nsl.getCurrentSession().clear()

        def result = [success: true]
        log.debug "treeViewService.getInstancePlacementInTree"
        nameTreePathService.removeNameTreePath(p.instance.name, apc)
        Map npt = treeViewService.getInstancePlacementInTree(apc, p.instance)
        result << npt

        log.debug "render(result as JSON)"
        return render(result as JSON)
    }

    private static void refetch(PlaceApcInstanceParam p) {
        p.instance = refetchInstance(p.instance);
        p.supername = refetchName(p.supername);
    }

    private static void refetch(RemoveApcInstanceParam p) {
        p.instance = refetchInstance(p.instance);
        p.replacementName = refetchName(p.replacementName);
        p.replacementInstance = refetchInstance(p.replacementInstance);
    }
}

/** This class does not belong here. */
class TMP_RDF_TO_MAP {
    static Object asMap(RdfRenderable r) {
        if (r == null) return null
        if (r instanceof RdfRenderable.Obj) return objAsMap((RdfRenderable.Obj) r)
        if (r instanceof RdfRenderable.Literal) return literalAsMap((RdfRenderable.Literal) r)
        if (r instanceof RdfRenderable.Coll) return collAsMap((RdfRenderable.Coll) r)
        if (r instanceof RdfRenderable.Primitive) return ((RdfRenderable.Primitive) r).o
        if (r instanceof RdfRenderable.Resource) return resourceAsMap((RdfRenderable.Resource) r)
        return r.getClass().getName()
    }

    static Object objAsMap(RdfRenderable.Obj r) {
        def o = [:]
        for (Map.Entry<Uri, RdfRenderable.Obj.Value> e : r.entrySet()) {
            String k = e.getKey().asQNameDIfOk()

            if (e.getValue().isSingleValue()) {
                o.put(k, asMap(e.getValue().asSingleValue().v))
            } else {
                def oo = []
                o.put(k, oo)
                for (RdfRenderable rr : e.getValue().asMultiValue()) {
                    oo << asMap(rr)
                }
            }
        }
        return o
    }

    static Object collAsMap(RdfRenderable.Coll r) {
        def o = []
        boolean ignoreFirst = true
        for (RdfRenderable i : r) {
            if (ignoreFirst)
                ignoreFirst = false
            else
                o << asMap(i)
        }
        return o
    }

    static Object literalAsMap(RdfRenderable.Literal r) {
        return [
                type : r.uri.asQNameDIfOk(),
                value: r.literal
        ]
    }

    static Object resourceAsMap(RdfRenderable.Resource r) {
        return r.uri
    }
}

@Validateable
class PlaceApniNameParam {
    long nameId
    long supernameId

    String toString() {
        return [nameId: nameId, superNameId: supernameId].toString()
    }

    Name getName() {
        return nameId ? Name.get(nameId) : null
    }

    Name getSupername() {
        return supernameId ? Name.get(supernameId) : null
    }

    static constraints = {
        nameId nullable: false
        supernameId nullable: true
    }
}

@Validateable
class PlaceApcInstanceParam {
    Instance instance
    Name supername
    String placementType

    String toString() {
        return [instance: instance, supername: supername, placementType: placementType].toString()
    }

    static constraints = {
        instance nullable: false
        supername nullable: true
        placementType nullable: true
    }
}

@Validateable
class RemoveApcInstanceParam {
    Instance instance
    Name replacementName
    Instance replacementInstance

    String toString() {
        return [instance: instance, replacementName: replacementName].toString()
    }

    static constraints = {
        instance nullable: false
        replacementName nullable: true
        replacementInstance nullable: true
    }
}
