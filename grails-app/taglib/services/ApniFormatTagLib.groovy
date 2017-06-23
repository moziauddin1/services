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

package services

import au.org.biodiversity.nsl.*
import net.htmlparser.jericho.Source
import net.htmlparser.jericho.SourceFormatter
import org.apache.shiro.SecurityUtils

class ApniFormatTagLib {

    def nameConstructionService
    def nameTreePathService
    def treeService
    def instanceService
    LinkService linkService
    def configService

    static defaultEncodeAs = 'raw'
//    static encodeAsForTags = [replaceXics: 'raw']

    static namespace = "af"

    def getTypeNotes = { attrs, body ->
        List<String> types = ['Type', 'Lectotype', 'Neotype']
        filterNotes(attrs, types, body)
    }

    def getDisplayableNonTypeNotes = { attrs, body ->
        List<String> types = ['Type', 'Lectotype', 'Neotype', 'EPBC Advice', 'EPBC Impact', 'Synonym']
        filterNotes(attrs, types, body, true)
    }

    def getAPCNotes = { attrs, body ->
        List<String> types = ['APC Comment', 'APC Dist.']
        filterNotes(attrs, types, body)
    }

    private void filterNotes(Map attrs, List<String> types, body, boolean invertMatch = false) {
        Instance instance = attrs.instance
        String var = attrs.var ?: "note"
        def notes = instance.instanceNotes.findAll { InstanceNote note ->
            invertMatch ^ (types.contains(note.instanceNoteKey.name))
        }
        notes.sort { it.instanceNoteKey.sortOrder }.each { InstanceNote note ->
            out << body((var): note)
        }
    }

    def tidy = { attr ->
        Source source = new Source(attr.text as String)
        SourceFormatter sf = source.getSourceFormatter()
        out << sf.toString()
    }

    def sortedReferences = { attrs, body ->
        List<Instance> instances = new ArrayList<>(attrs.instances as Set)
        String var = attrs.var ?: "instance"
        String sortOn = attrs.sortOn
        instances.sort { a1, b1 ->
            InstanceService.compareReferences(a1, b1, sortOn)
        }.each { Instance instance ->
            out << body((var): instance)
        }
    }

    def sortedInstances = { attrs, body ->
        List<Instance> instances = new ArrayList<>(attrs.instances as Set)
        String var = attrs.var ?: "instance"
        String page = 'Not a page'
        Instance citedBy = null
        try {
            instances = instanceService.sortInstances(instances)
            instances.eachWithIndex { Instance instance, Integer i ->
                Boolean showRef = page != instance.page || instance.citedBy != citedBy
                out << body((var): instance, i: i, newPage: showRef)
                page = instance.page
                citedBy = instance.citedBy
            }
        } catch (e) {
            println e.message
        }
    }

    def replaceXics = { attrs ->
        out << ApniFormatService.transformXics(attrs.text)
    }

    def apcSortedInstances = { attrs, body ->
        List<Instance> instances = new ArrayList<>(attrs.instances as Set)
        String var = attrs.var ?: "instance"
        instances.sort { a, b ->
            if (a.name.simpleName == b.name.simpleName) {
                if (a.cites.reference.year == b.cites.reference.year) {
                    if (a.cites.reference == b.cites.reference) {
                        if (a.cites.page == b.cites.page) {
                            return b.cites.id <=> a.cites.id
                        }
                        return b.cites.page <=> a.cites.page
                    }
                    return a.cites.reference.citation <=> b.cites.reference.citation
                }
                return (a.cites.reference.year) <=> (b.cites.reference.year)
            }
            return a.name.simpleName <=> b.name.simpleName
        }.each { Instance instance ->
            out << body((var): instance)
        }
    }


    def author = { attrs ->
        Name name = attrs.name
        out << nameConstructionService.constructAuthor(name)
    }

    def harvard = { attrs ->
        Reference reference = attrs.reference
        out << "<span title=\"${reference.citation}\">${reference.author.name} ($reference.year)</span>"
    }

    def branch = { attrs, body ->
        Name name = attrs.name
        String treeName = attrs.tree ?: configService.classificationTreeName
        TreeElement treeElement = treeService.findTreeElementForName(name, treeService.getTree(treeName))
        if (treeElement) {
            out << "<branch title=\"click to see branch in $treeName.\">"
            out << body()

            List<TreeElement> elementPath = treeService.getElementPath(treeElement)

            out << '<ul>'
            out << "<li>$treeName</li>"
            elementPath.each { TreeElement element ->
                String link = element.elementLink
                if (link) {
                    out << "<li><a href='${link}'>${element.name.nameElement}</a> <span class=\"text-muted\">(${element.name.nameRank.abbrev})</span></li>"
                } else {
                    out << "<li>${element.name.nameElement} <span class=\"text-muted\">(${element.name.nameRank.abbrev})</span></li>"
                }
            }
            out << '</ul></branch>'
        }
    }

    def page = { attrs ->
        Instance instance = attrs.instance
        if (instance.page) {
            out << ApniFormatService.transformXics(instance.page)
        } else {
            if (instance.instanceType.citing && instance.citedBy.page) {
                if (instance.instanceType.name.contains('common')) {
                    out << "~ ${ApniFormatService.transformXics(instance.citedBy.page)}"
                } else {
                    out << "${ApniFormatService.transformXics(instance.citedBy.page)}"
                }
            } else {
                out << '-'
            }
        }
    }

    def bhlLink = { attrs ->
        Name name = attrs.name
        out << "<bhl>"
        out << "<a href='http://www.biodiversitylibrary.org/name/${name.simpleName.replaceAll(' ', '_')}'>BHL <span class='fa fa-external-link'></span></a>"
        out << " <bhl>"
    }

    def refNameTreeSearchLink = { attrs ->
        String citation = attrs.citation
        String product = attrs.product
        Map params = [publication: citation, search: true, advanced: true, display: 'apni']
        if (product) {
            params << [product: product]
        } else {
            //if not logged in force to name tree name
            if (!SecurityUtils.subject?.principal) {
                params << [product: ConfigService.nameTreeName]
            }
        }
        out << g.createLink(absolute: true, controller: 'search', action: 'search', params: params)
    }

    def refAPCSearchLink = { attrs ->
        String citation = attrs.citation
        String product = attrs.product
        Map params = [publication: citation, search: true, advanced: true, display: 'apc', 'tree.id': 1133571] //todo remove hard coded reference number
        if (product) {
            params << [product: product]
        } else {
            //if not logged in force to APC
            if (!SecurityUtils.subject?.principal) {
                params << [product: 'apc']
            }
        }
        out << g.createLink(controller: 'search', action: 'search', params: params)
    }

    def apniLink = { attrs ->
        String link = attrs.link
        if (!link) {
            Name name = attrs.name
            link = linkService.getPreferredLinkForObject(name) + '/api/apni-format'
        }
        if (link) {
            out << """<apnilink>
      <a class="vertbar" href="${link}">
        <i class="fa fa-list-alt see-through"></i>
        apni
      </a>
    </apnilink>"""
        }
    }

    def tick = { attrs ->
        if (attrs.val) {
            out << '<i class="fa fa-check-square-o"></i>'
        } else {
            out << '<i class="fa fa-square-o"></i>'
        }
    }

    def apc = { attrs ->
        Node apcNode = attrs.apc
        Instance instance = attrs.instance ?: apcNode.instance
        if (apcNode && instance && apcNode.instance.id == instance.id) {
            String link = g.createLink(absolute: true, controller: 'apcFormat', action: 'display', id: apcNode.name.id)
            String tree = ConfigService.classificationTreeName
            out << """<a href="${link}">"""
            switch (apcNode.typeUriIdPart) {
                case 'ApcConcept':
                    out << """<apc title="$tree concept"><i class="fa fa-check"></i>${tree}</apc>"""
                    break
                case 'ApcExcluded':
                    out << """<apc title="excluded from $tree"><i class="fa fa-ban"></i> ${tree}</apc>"""
                    break
                case 'DeclaredBt':
                    out << """<apc title="Declared broader term in $tree">BT ${tree}</apc>"""
                    break
                case 'ApcRecord':
                    break
            }
            out << "</a>"
        }
    }
}
