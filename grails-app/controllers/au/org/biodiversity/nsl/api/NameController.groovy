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
import grails.transaction.Transactional
import org.apache.shiro.SecurityUtils
import org.grails.plugins.metrics.groovy.Timed
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.*

@Transactional
class NameController implements UnauthenticatedHandler, WithTarget {

    def constructedNameService
    def classificationService
    def jsonRendererService
    def nameService
    def simpleNameService
    def apniFormatService
    def instanceService

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = ['json', 'xml', 'html']

    static allowedMethods = [
            nameStrings       : ["GET", "PUT"],
            apniFormat        : ["GET"],
            apcFormat         : ["GET"],
            delete            : ["GET", "DELETE"],
            family            : ["GET"],
            branch            : ["GET"],
            nameUpdateEventUri: ["PUT", "DELETE"],
            exportNslSimple   : ["GET"],
            apni              : ["GET"],
            apc               : ["GET"]
    ]
    static namespace = "api"

    @Timed()
    def index() {
        redirect(uri: '/docs/main.html')
    }

    @Timed()
    def apniFormat(Name name) {
        withTarget(name) {
            if (params.embed) {
                forward(controller: 'apniFormat', action: 'name', id: name.id)
            } else {
                forward(controller: 'apniFormat', action: 'display', id: name.id)
            }
        }
    }

    @Timed()
    def apcFormat(Name name) {
        withTarget(name) {
            if (params.embed) {
                forward(controller: 'apcFormat', action: 'name', id: name.id)
            } else {
                forward(controller: 'apcFormat', action: 'display', id: name.id)
            }
        }
    }

    @Timed()
    def simpleName(Name name) {
        withTarget(name) {
            forward(controller: 'restResource', action: 'nslSimpleName', params: [idNumber: name.id])
        }
    }

    @Timed()
    def nameStrings(Name name) {
        withTarget(name) { ResultObject result ->
            result.result = constructedNameService.constructName(name)
            result.result.fullName = constructedNameService.stripMarkUp(result.result.fullMarkedUpName as String)
            result.result.simpleName = constructedNameService.stripMarkUp(result.result.simpleMarkedUpName as String)
            if (request.method == 'PUT') {
                SecurityUtils.subject.checkRole('admin')
                name.fullNameHtml = result.result.fullMarkedUpName
                name.fullName = result.result.fullName
                name.simpleName = result.result.simpleName
                name.simpleNameHtml = result.result.simpleMarkedUpName
                name.save()
            }
        }
    }

    @Timed()
    def delete(Name name, String reason) {
        withTarget(name) { ResultObject result ->
            if (request.method == 'DELETE') {
                SecurityUtils.subject.checkRole('admin')
                result << nameService.deleteName(name, reason)
            } else if (request.method == 'GET') {
                result << nameService.canDelete(name, 'dummy reason')
            } else {
                result.status = METHOD_NOT_ALLOWED
            }
        }
    }

    @Timed()
    def family(Name name) {
        withTarget(name) { ResultObject result ->
            Name familyName = classificationService.getAPNIFamilyName(name)

            if (familyName) {
                result << [familyName: familyName]
            } else {
                result << [error: 'Family name not found']
                result.status = NOT_FOUND
            }
        }
    }

    @Timed()
    def branch(Name name) {
        withTarget(name) { ResultObject result ->
            List<Name> namesInBranch = classificationService.getPath(name)
            result << [branch: namesInBranch]
        }
    }

    @Timed()
    def apc(Name name) {
        withTarget(name) { ResultObject result ->
            Node node = classificationService.isNameInAPC(name)
            result << ["inAPC"   : node != null,
                       excluded  : node?.typeUriIdPart == 'ApcExcluded',
                       operation : params.action,
                       "nsl-name": name.id,
                       nameNs    : node?.nameUriNsPart?.label,
                       nameId    : node?.nameUriIdPart,
                       taxonNs   : node?.taxonUriNsPart?.label,
                       taxonId   : node?.taxonUriIdPart,
                       type      : node?.typeUriIdPart
            ]
        }
    }

    @Timed()
    def apni(Name name) {
        withTarget(name) { ResultObject result ->
            Node node = classificationService.isNameInAPNI(name)
            result << ["inAPNI"  : node != null,
                       operation : params.action,
                       "nsl-name": name.id,
                       nameNs    : node?.nameUriNsPart?.label,
                       nameId    : node?.nameUriIdPart,
                       taxonNs   : node?.taxonUriNsPart?.label,
                       taxonId   : node?.taxonUriIdPart]
        }
    }

    @Timed()
    def nameUpdateEventUri(String uri) {
        if (request.method == 'PUT') {
            log.info "Adding $uri to event notification list"
            nameService.nameEventRegister(uri)
            respond(new ResultObject(text: "registered $uri"))
        } else if (request.method == 'DELETE') {
            log.info "Removing $uri from event notification list"
            nameService.nameEventUnregister(uri)
            respond(new ResultObject(text: "unregistered $uri"))
        }
    }

    @Timed()
    def exportNslSimple(String exportFormat) {
        File exportFile
        if (exportFormat == 'csv') {
            exportFile = simpleNameService.exportSimpleNameToCSV()
        } else if (exportFormat == 'text') {
            exportFile = simpleNameService.exportSimpleNameToText()
        } else {
            ResultObject result = new ResultObject([
                    action: params.action,
                    error : "Export format not supported"
            ])
            result.status = BAD_REQUEST
            //noinspection GroovyAssignabilityCheck
            return respond(result, [view: 'serviceResult', model: [data: result], status: result.status])
        }
        render(file: exportFile, fileName: exportFile.name, contentType: 'text/plain')
    }

    /**
     * NSL-1171 look for an 'Acceptable' name to be used in eFlora by simple name
     *
     * @param name
     */
    @Timed()
    def acceptableName(String name) {

        List<String> status = ['legitimate', 'manuscript', 'nom. alt.', 'nom. cons.', 'nom. cons., nom. alt.', 'nom. cons., orth. cons.', 'nom. et typ. cons.', 'orth. cons.', 'typ. cons.']
        List<Name> names = Name.executeQuery('''
select n
from Name n
where (lower(n.fullName) like :query or lower(n.simpleName) like :query)
and n.instances.size > 0
and n.nameStatus.name in (:ns)
order by n.simpleName asc''',
                [query: SearchService.tokenizeQueryString(name.toLowerCase()), ns: status], [max: 100])

        ResultObject result = new ResultObject([
                action: params.action,
                count : names.size(),
                names : names.collect { jsonRendererService.getBriefNameWithHtml(it) },
        ])

        //noinspection GroovyAssignabilityCheck
        return respond(result, [view: '/common/serviceResult', model: [names: names,]])
    }

    @Timed()
    def findConcept(Name name, String term) {
        log.debug "search concepts for $term"
        withTarget(name) { ResultObject result ->
            List<String> terms = term.replaceAll('(,|&)', '').split(' ')
            log.debug "terms are $terms"
            Integer highestRank = 0
            Instance match = null
            name.instances.each { Instance inst ->
                Integer rank = rank(inst.reference.citation, terms)
                if (rank > highestRank) {
                    highestRank = rank
                    match = inst
                }
            }
            result.matchedOn = term
            result.rank = highestRank
            result.instance = jsonRendererService.getBriefInstance(match)
        }
    }

    private static Integer rank(String target, List<String> terms) {
        terms.inject(0) { count, term ->
            target.contains(term) ? count + 1 : count
        } as Integer
    }

    @Timed()
    def apniConcepts(Name name, Boolean relationships) {
        if (relationships == null) {
            relationships = true
        }

        log.info "getting APNI concept for $name"
        withTarget(name) { ResultObject result ->
            Map nameModel = apniFormatService.getNameModel(name)
            result.name = jsonRendererService.getBriefNameWithHtml(name)
            if (nameModel.apc != null) {
                if (nameModel.apc.typeUriIdPart == 'ApcConcept') {
                    result.name.inAPC = true
                    result.name.APCExcluded = false
                } else {
                    result.name.inAPC = true
                    result.name.APCExcluded = true
                }
            } else {
                result.name.inAPC = false
            }
            result.name.family = jsonRendererService.getBriefNameWithHtml(nameModel.familyName)
            result.references = nameModel.references.collect { Reference reference ->
                Map refMap = jsonRendererService.getBriefReference(reference)
                refMap.citations = []
                //noinspection GroovyAssignabilityCheck
                List<Instance> sortedInstances = instanceService.sortInstances(nameModel.instancesByRef[reference] as List<Instance>)

                //noinspection GroovyAssignabilityCheck
                sortedInstances.eachWithIndex { Instance instance, Integer i ->

                    if (nameModel.apc?.taxonUriIdPart == instance.id.toString()) {
                        if (nameModel.apc.typeUriIdPart == 'ApcConcept') {
                            refMap.APCReference = true
                        } else {
                            refMap.APCExcludedReference = true
                        }
                    }

                    if (instance.instanceType.standalone) {
                        refMap.citations << [
                                instance    : jsonRendererService.brief(instance),
                                page        : instance.page,
                                relationship: instance.instanceType.name
                        ]
                    }
                    if (relationships) {
                        if (instance.instanceType.synonym || instance.instanceType.unsourced) {
                            refMap.citations << [
                                    instance    : jsonRendererService.brief(instance.citedBy),
                                    page        : instance.citedBy.page,
                                    relationship: "$instance.instanceType.name of $instance.citedBy.name.fullNameHtml",
                                    name        : jsonRendererService.getBriefNameWithHtml(instance.citedBy.name)
                            ]
                        }

                        if (instance.instancesForCitedBy) {
                            instanceService.sortInstances(instance.instancesForCitedBy.findAll {
                                it.instanceType.synonym
                            } as List<Instance>).each { Instance synonym ->
                                refMap.citations << [
                                        instance    : jsonRendererService.brief(synonym),
                                        page        : instancePage(synonym),
                                        relationship: "$synonym.instanceType.name: $synonym.name.fullNameHtml",
                                        name        : jsonRendererService.getBriefNameWithHtml(synonym.name)
                                ]
                            }

                            instanceService.sortInstances(instance.instancesForCitedBy.findAll {
                                it.instanceType.name.contains('misapplied')
                            } as List<Instance>).each { Instance missapp ->
                                String rel = "${missapp.instanceType.name.replaceAll('misapplied', 'misapplication')}" +
                                        " $missapp.cites.name.fullNameHtml" +
                                        " by ${missapp?.cites?.reference?.citationHtml}: ${missapp?.cites?.page ?: '-'}"

                                refMap.citations << [
                                        instance    : jsonRendererService.brief(missapp),
                                        page        : instancePage(missapp),
                                        relationship: rel,
                                        name        : jsonRendererService.getBriefNameWithHtml(missapp.cites.name)
                                ]
                            }

                            instanceService.sortInstances(instance.instancesForCitedBy.findAll {
                                (!it.instanceType.synonym && !it.instanceType.name.contains('misapplied'))
                            } as List<Instance>).each { Instance synonym ->
                                refMap.citations << [
                                        instance    : jsonRendererService.brief(synonym),
                                        page        : instancePage(synonym),
                                        relationship: "$synonym.instanceType.name: $synonym.name.fullNameHtml",
                                        name        : jsonRendererService.getBriefNameWithHtml(synonym.name)
                                ]
                            }
                        }

                        if (instance.instanceType.misapplied) {
                            refMap.citations << [
                                    instance    : jsonRendererService.brief(instance),
                                    page        : instancePage(instance),
                                    relationship: "$instance.instanceType.name to: $instance.citedBy.name.fullNameHtml" +
                                            " by  ${instance?.cites?.reference?.citationHtml}: ${instance?.cites?.page ?: '-'}",
                                    name        : jsonRendererService.getBriefNameWithHtml(instance.citedBy.name)
                            ]
                        }
                    }
                    refMap.notes = instance.instanceNotes.collect { InstanceNote instanceNote ->
                        [
                                instanceNoteKey : instanceNote.instanceNoteKey.name,
                                instanceNoteText: instanceNote.value
                        ]
                    }
                }

                return refMap
            }
        }
    }

    private static String instancePage(Instance instance) {
        if (instance.page) {
            return instance.page
        } else {
            if (instance.instanceType.citing && instance.citedBy.page) {
                if (instance.instanceType.name.contains('common')) {
                    return "~ $instance.citedBy.page"
                } else {
                    return instance.citedBy.page
                }
            } else {
                return '-'
            }
        }
    }


}

class ResultObject {
    @Delegate
    Map data

    HttpStatus status = OK

    ResultObject(Map data) {
        this.data = data
    }
}