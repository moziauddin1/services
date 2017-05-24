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

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authz.annotation.RequiresRoles
import grails.converters.JSON
import grails.transaction.Transactional
import grails.validation.Validateable
import au.org.biodiversity.nsl.*
import au.org.biodiversity.nsl.tree.*
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

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
    MessageSource messageSource
    UserWorkspaceManagerService userWorkspaceManagerService
    LinkService linkService

    ///////////////////////////////////////////
    //
    // These methods support the new old APC
    // edit component in the NSL editor
    // and are to be deleted

    /** @deprecated */

    @Deprecated
    placeApniName(PlaceApniNameParam p) {
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

        // if the instance is draft currentNode will be null and updateNameTreePathFromNode will do nothing and return null
        nameTreePathService.updateNameTreePathFromNode(currentNode)

        Map nodePlacementInTree = treeViewService.getInstancePlacementInTree(apc, p.instance)
        result << nodePlacementInTree

        log.debug "render(result as JSON)"
        return render(result as JSON)
    }

    @Deprecated
    // removing support for the APC editor pane
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

    @Deprecated
    // removing support for the APC editor pane
    removeApcInstance(RemoveApcInstanceParam p) {
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

    @Deprecated
    // removing support for the APC editor pane
    private static void refetch(PlaceApcInstanceParam p) {
        p.instance = refetchInstance(p.instance)
        p.supername = refetchName(p.supername)
    }

    @Deprecated
    // removing support for the APC editor pane
    private static void refetch(RemoveApcInstanceParam p) {
        p.instance = refetchInstance(p.instance)
        p.replacementName = refetchName(p.replacementName)
        p.replacementInstance = refetchInstance(p.replacementInstance)
    }

    ///////////////////////////////////////////
    //
    // These methods support the new edit classification
    // component in the NSL editor

    @RequiresRoles('treebuilder')
    placeNameOnTree(PlaceNameOnTreeParam param) {
        validateAndAuthorize(param) {
            try {
                Uri placementType = makePlacementType(param.placementType)

                userWorkspaceManagerService.placeNameOnTree(param.tree, param.name, param.instance, param.parentName, placementType)

                return render([
                        success: true,
                        msg    : [[msg: "Placed."]]
                ] as JSON)
            } catch (PlacementException ex) {

                log.debug ex.message

                response.status = ex.status?.value()
                return render([
                        success: false,
                        msg    : [[msg: ex.message]]
                ] as JSON)
            } catch (IllegalStateException ex) {

                ex.printStackTrace()

                response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
                return render([
                        success: false,
                        msg    : [[msg: "An error happened, sorry: $ex.message"]]
                ] as JSON)
            }
        }
    }

    private Uri makePlacementType(String placementTypeName) {
        Uri placementType = null

        if ("accepted" == placementTypeName)
            placementType = uri('apc-voc', 'ApcConcept')
        else if ("excluded" == placementTypeName)
            placementType = uri('apc-voc', 'ApcExcluded')
        else if ("untreated" == placementTypeName)
            placementType = uri('apc-voc', 'DeclaredBt')
        return placementType
    }

    private validateAndAuthorize(param, Closure work) {
        if (!param.validate()) {
            return renderValidationErrors(param)
        }

        if (!canEdit(param.tree as Arrangement)) {
            response.status = 403
            return render([
                    success: false,
                    msg    : [
                            [msg   : "403 - Forbidden",
                             body  : "You do not have permission to edit this tree",
                             status: 'danger'
                            ]
                    ]
            ] as JSON)
        }
        return work()
    }


    @RequiresRoles('treebuilder')
    removeNameFromTree(RemoveNameFromTreeParam param) {
        if (!param.validate()) return renderValidationErrors(param)
        if (!canEdit(param.tree)) {
            response.status = 403
            return render([
                    success: false,
                    msg    : [
                            [msg   : "403 - Forbidden",
                             body  : "You do not have permission to edit this tree",
                             status: 'danger',
                            ]
                    ]
            ] as JSON)
        }

        handleException { handleExceptionIgnore ->
            Message msg = userWorkspaceManagerService.removeNameFromTree(param.tree, param.name)

            return render([
                    success: true,
                    msg    : new TreeServiceMessageUtil(linkService).unpackMessage(msg, 'success')
            ] as JSON)
        }
    }

    @RequiresRoles('treebuilder')
    updateValue(UpdateValueParam param) {
        if (!param.validate()) return renderValidationErrors(param)
        if (!canEdit(param.tree)) {
            response.status = 403
            return render([
                    success: false,
                    msg    : [
                            [msg   : "403 - Forbidden",
                             body  : "You do not have permission to edit this tree",
                             status: 'danger',
                            ]
                    ]
            ] as JSON)
        }

        handleException { handleExceptionIgnore ->

            ValueNodeUri uri = ValueNodeUri.findByRootAndLabel(param.tree.baseArrangement ?: param.tree, param.valueUriLabel)

            if (uri == null) {
                // TODO: this should be a validation exception
                ServiceException.raise(Message.makeMsg(Msg.SIMPLE_2, ["Unknown value uri", param.valueUriLabel]))
            }

            userWorkspaceManagerService.updateValue(param.tree, param.name, uri, param.value)

            return render([
                    success: true,
                    msg    : null
            ] as JSON)
        }
    }

    boolean canEdit(Arrangement a) {
        return a.arrangementType == ArrangementType.U && SecurityUtils.subject.hasRole(a.baseArrangement.label)
    }

    // ==============================

    private renderValidationErrors(param) {
        def msg = []
        msg += param.errors.globalErrors.collect { ObjectError it -> [msg: 'Validation', status: 'warning', body: messageSource.getMessage(it, (java.util.Locale) null)] }
        msg += param.errors.fieldErrors.collect { FieldError it -> [msg: it.field, status: 'warning', body: messageSource.getMessage(it, (java.util.Locale) null)] }
        response.status = 400

        log.debug msg

        return render([
                success: false,
                msg    : msg
        ] as JSON)
    }

    private handleException(Closure doIt) {
        try {
            return doIt()
        }
        catch (Exception ex) {
            log.debug ex

            doIt.delegate.response.status = ex instanceof ServiceException ? 400 : 500

            TreeServiceMessageUtil tsmu = new TreeServiceMessageUtil(linkService)

            return render([
                    success   : false,
                    msg       : tsmu.unpackThrowable(ex),
                    stackTrace: tsmu.unpackStacktrace(ex)
            ] as JSON)
        }
    }
}

/** This class does not belong here. */
@Deprecated
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
@Deprecated
// removing support for the APC editor pane
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
class PlaceNameOnTreeParam {
    Arrangement tree
    Name name
    Instance instance
    Name parentName
    String placementType

    String toString() {
        return [tree: tree, name: name, instance: instance, parentName: parentName, placementType: placementType].toString()
    }

    static constraints = {
        tree nullable: false
        name nullable: false
        instance nullable: false
        parentName nullable: true
        placementType nullable: false
    }
}

@Validateable
class RemoveNameFromTreeParam {
    Arrangement tree
    Name name

    String toString() {
        return [tree: tree, name: name].toString()
    }

    static constraints = {
        tree nullable: false
        name nullable: false
    }
}

@Validateable
class UpdateValueParam {
    Arrangement tree
    Name name
    String valueUriLabel
    String value

    String toString() {
        return [tree: tree, name: name, valueUriLabel: valueUriLabel, value: value].toString()
    }

    static constraints = {
        tree nullable: false
        valueUriLabel nullable: false
        value nullable: true
    }
}

@Validateable
@Deprecated
// removing support for the APC editor pane
class RemoveApcInstanceParam {
    Instance instance
    Name replacementName
    Instance replacementInstance

    String toString() {
        return [instance           : instance,
                replacementName    : replacementName,
                replacementInstance: replacementInstance].toString()
    }

    static constraints = {
        instance nullable: false
        replacementName nullable: true
        replacementInstance nullable: true
    }

}