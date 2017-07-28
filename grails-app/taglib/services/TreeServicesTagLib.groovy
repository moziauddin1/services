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

import au.org.biodiversity.nsl.Node
import au.org.biodiversity.nsl.TreeElement
import au.org.biodiversity.nsl.TreeService
import au.org.biodiversity.nsl.TreeVersion
import au.org.biodiversity.nsl.tree.QueryService

class TreeServicesTagLib {
    @SuppressWarnings("GroovyUnusedDeclaration")
    static defaultEncodeAs = [taglib: 'raw']
    static namespace = "tree"

    TreeService treeService
    QueryService queryService

    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    def getNodeNameAndInstance = { attrs, body ->
        Node node = attrs.node
        out << body(name: queryService.resolveName(node), instance: queryService.resolveInstance(node))
    }

    def collapsedIndent = { attrs ->
        TreeElement element = attrs.element
        int depth = treeService.depth(element)
        out << "indent$depth"
    }

    def elementPath = { attrs, body ->
        TreeElement element = attrs.element
        String var = attrs.var
        List<TreeElement> path = treeService.getElementPath(element)
        String separator = ''
        path.each { TreeElement pathElement ->
            out << separator
            out << body("$var": pathElement)
            separator = attrs.separator
        }
    }

    def profile = { attrs ->
        Map profileData = attrs.profile
        out << "<dl class='dl-horizontal'>"
        profileData.each { k, v ->
            out << "<dt>$k</dt><dd>${v.value}</dd>"
        }
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
        Integer count = TreeElement.countByTreeVersion(treeVersion)
        out << body([elements: count])
    }

}
