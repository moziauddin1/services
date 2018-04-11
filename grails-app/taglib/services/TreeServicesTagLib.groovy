/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL tree services plugin project.

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

import au.org.biodiversity.nsl.Tree
import au.org.biodiversity.nsl.TreeService
import au.org.biodiversity.nsl.TreeVersion
import au.org.biodiversity.nsl.TreeVersionElement

class TreeServicesTagLib {
    @SuppressWarnings("GroovyUnusedDeclaration")
    static defaultEncodeAs = [taglib: 'raw']
    static namespace = "tree"

    TreeService treeService

    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    def elementPath = { attrs, body ->
        TreeVersionElement element = attrs.element
        Boolean excludeThis = attrs.excludeThis ?: false
        String var = attrs.var
        List<TreeVersionElement> path = treeService.getElementPath(element)
        log.debug "Path $path"
        if (excludeThis) {
            path.remove(element)
        }
        String separator = ''
        path.each { TreeVersionElement pathElement ->
            out << separator
            out << body("$var": pathElement)
            separator = attrs.separator
        }
    }

    def profile = { attrs ->
        Map profileData = attrs.profile
        if (profileData) {
            out << "<dl class='dl-horizontal'>"
            profileData.each { k, v ->
                if (k) {
                    out << "<dt>$k</dt><dd>${v.value}"
                }
                if (v.previous) {
                    out << '&nbsp;<span class="toggleNext"><i class="fa fa-clock-o"></i><i style="display: none" class="fa fa-circle"></i></span>' +
                            '<div style="display: none" class="previous"><ul>'
                    previous(v.previous).each {
                        out << "<li>${it.value ?: '(Blank)'} <span class='small text-muted'><date>${it.updated_at}</date></span></li>"
                    }
                    out << '</ul></div>'
                }
                out << "</dd>"
            }
            out << "</dl>"
        }
    }

    private List previous(Map data, List results = []) {
        results << data
        if (data.previous) {
            previous(data.previous, results)
        }
        results
    }

    def versionStatus = { attrs ->
        TreeVersion treeVersion = attrs.version
        if (treeVersion == treeVersion.tree.currentTreeVersion) {
            out << "current"
        } else if (!treeVersion.published) {
            out << "draft"
        } else {
            out << "old"
        }
    }

    def versionStats = { attrs, body ->
        TreeVersion treeVersion = attrs.version
        Integer count = TreeVersionElement.countByTreeVersion(treeVersion)
        out << body([elements: count])
    }

    def findCurrentVersion = { attrs, body ->
        TreeVersionElement tve = attrs.element
        TreeVersion currentVersion = tve.treeVersion.tree.currentTreeVersion
        if (currentVersion && currentVersion != tve.treeVersion) {
            TreeVersionElement currentElement = TreeVersionElement.
                    findByTreeVersionAndTreeElement(currentVersion, tve.treeElement)
            if (!currentElement) {
                currentElement = treeService.findElementBySimpleName(tve.treeElement.simpleName, currentVersion)
            }
            if (currentElement) {
                out << body(currentElement: currentElement)
            }
        }
    }

    def drafts = { attrs, body ->
        Tree tree = attrs.tree
        List<TreeVersion> drafts = TreeVersion.findAllWhere(tree: tree, published: false)
        drafts.each { TreeVersion draft ->
            out << body(draft: draft, defaultDraft: draft.id == tree.defaultDraftTreeVersion?.id)
        }
    }

}
