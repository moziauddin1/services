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

import au.org.biodiversity.nsl.tree.DomainUtils
import au.org.biodiversity.nsl.tree.QueryService
import au.org.biodiversity.nsl.tree.Uri
import grails.transaction.Transactional

/**
 * The Classification service is a high level service over the tree service plugin services. It's primary aim is to provide
 * very simple top level services that hide the tree mechanics.
 *
 * We'll try to abstract the classifications away from what the user wants.
 */
@Transactional
class TreeViewService {
    JsonRendererService jsonRendererService
    QueryService queryService

    private Node getFirstCurrentNodeForName(Arrangement tree, Uri uri) {
        List<Node> nodes = queryService.findCurrentName(tree, uri)
        if (nodes.size() > 0) {
            return nodes.first()
        }
        return null
    }

    private Node nodeForName(Arrangement tree, Name name) {
        if (name) {
            Uri uri = DomainUtils.uri("nsl-name", name.id.toString())
            return getFirstCurrentNodeForName(tree, uri)
        }
        return null
    }

    Map getBranchForName(Arrangement tree, Name name) {
        Node node = nodeForName(tree, name)
        return (node ? getBranchForNode(tree, node) : getBranchForTree(tree))
    }

    Map getPathForName(Arrangement tree, Name name) {
        Node node = nodeForName(tree, name)
        return node ? getPathForNode(tree, node) : getPathForTree(tree)
    }

    Map getBranchForNode(Arrangement tree, Node node) {
        if (node) {
            List<Link> links = queryService.getPathForNode(tree, node).
                    findAll { Link it -> it.subnode.internalType != NodeInternalType.V }
            if (links) {
                return buildPath(links)
            }
        }

        return getBranchForTree(tree)
    }

    Map getBranchForTree(Arrangement tree) {
        return populateSubnodeFromLink(DomainUtils.getSingleSublink(tree.node), true)
    }


    def getPathForNode(Arrangement tree, Node node) {
        if (node) {
            List<Link> links = queryService.getPathForNode(tree, node).findAll { Link it -> it.subnode.internalType != NodeInternalType.V }
            if (links) {
                return [
                        path: links.drop(1).collect { populateSubnodeFromLink(it, false) },
                        tree: populateSubnodeFromLink(links.last(), true)
                ]
            }
        }

        return getPathForTree(tree)
    }

    Map getPathForTree(Arrangement tree) {
        return [
                path: [],
                tree: populateSubnodeFromLink(DomainUtils.getSingleSublink(tree.node), true)
        ]
    }

    Map getNameInTree(Arrangement tree, Name name) {
        Node node = nodeForName(tree, name)
        return (node ? populateSubnodeFromNode(node, true) : null)
    }

    private Link linkForNamePlacement(Arrangement tree, Uri uri) {
        List<Link> links = queryService.findCurrentNamePlacement(tree, uri)
        if (links.size() > 0) {
            return links.first()
        }
        return null
    }

    Map getNamePlacementInTree(Arrangement tree, Name name) {
        Map o = [isPlaced: false]

        if (name) {
            Uri uri = DomainUtils.uri("nsl-name", name.id.toString())

            o.params = [:]
            o.params.name = jsonRendererService.getBriefNameWithHtml(name)
            o.params.nameUri = uri

            Link link = linkForNamePlacement(tree, uri)

            if (link) {
                o.isPlaced = true
                populateSubnodeFromLink(link, false, o, true)
                o.supernode = populateSubnodeFromNode(link.supernode, false)
            }
        }

        return o
    }

    Map getInstancePlacementInTree(Arrangement tree, Instance instance) {
        Map o = [isPlaced: false, isNamePlaced: false]

        if (instance) {
            Uri instanceUri = DomainUtils.uri("nsl-instance", instance.id.toString())
            Uri nameUri = DomainUtils.uri("nsl-name", instance.name.id.toString())

            o.params = [:]
            o.params.instance = jsonRendererService.getBriefInstanceForNameWithHtml(instance)
            o.params.instanceUri = instanceUri
            o.params.name = jsonRendererService.getBriefNameWithHtml(instance.name)
            o.params.nameUri = nameUri

            Link link = linkForNamePlacement(tree, nameUri)

            if (link) {
                o.isNamePlaced = true
                o.isPlaced = DomainUtils.getTaxonUri(link.subnode) == instanceUri
                populateSubnodeFromLink(link, false, o, true)
                o.supernode = populateSubnodeFromNode(link.supernode, false)
                if (link.supernode.name) {
                    o.params.supername = link.supernode.name.id
                }
            }
        }

        return o
    }

    /*
     * recursively build the path. The return value is a list of elements,
     * having a name, and one of them will have a non-null subname element
     * containing the tail of the list.
     */

    private Map buildPath(List<Link> links) {
        if (!links) return null

        Link l = links.get(0)

        Object sublist = buildPath(links.subList(1, links.size()))

        Map o = populateSubnodeFromLink(l, true)

        if (sublist) {
            o.subnode = o.subnode.collect { it.node.id == sublist.node.id ? sublist : it }
        }

        return o
    }

    private Map populateSubnodeFromLink(Link link, boolean withKids, Map o = [:], boolean withLiterals = false) {
        if (!link) {
            return null
        }

        populateSubnodeFromNode link.subnode, withKids, o, withLiterals

        o.link = [:]

        o.link.type = jsonRendererService.getBriefTreeUri(DomainUtils.getLinkTypeUri(link))
        o.link.seq = link.linkSeq
        o.link.synthetic = link.synthetic
        o.link.versioning = link.versioningMethod.name()

        return o
    }

    private Map populateSubnodeFromNode(Node node, boolean withKids, Map o = [:], boolean withLiterals = false) {
        if (!node) {
            return null
        }

        o.node = [:]

        o.node.id = node.id
        o.node.type = jsonRendererService.getBriefTreeUri(DomainUtils.getNodeTypeUri(node))
        o.node.synthetic = node.synthetic

        Uri nameUri = DomainUtils.getNameUri(node)
        o.nameUri = nameUri
        if (nameUri?.nsPart?.label == 'nsl-name') {
            Name name = Name.get(nameUri.idPart as Long)
            if (name) {
                o.name = jsonRendererService.getBriefNameWithHtml(name)
                o.name.id = name.id // this is needed to make the APC 'select parent name' form work properly
            }
        }

        Uri taxonUri = DomainUtils.getTaxonUri(node)
        o.taxonUri = taxonUri
        if (taxonUri?.nsPart?.label == 'nsl-instance') {
            Instance inst = Instance.get(taxonUri.idPart as Long)
            if (inst) {
                o.instance = jsonRendererService.getBriefInstanceForNameWithHtml(inst)
            }
        }

        o.fetched = withKids
        o.hasSubnodes = node.subLink.find { Link it -> it.subnode.internalType != NodeInternalType.V } != null

        if (withKids && o.hasSubnodes) {
            o.subnode =
                    node.subLink.findAll { Link it -> it.subnode.internalType != NodeInternalType.V }
                        .sort { Link a, Link b -> sortLinks(a, b) }
                        .collect { populateSubnodeFromLink(it, false) }
        }

        if (withLiterals) {
            node.subLink.findAll { Link it -> it.subnode.internalType == NodeInternalType.V }.each { Link subLink ->
                Uri linkType = DomainUtils.getLinkTypeUri(subLink)
                String k = linkType.asQNameIfOk()

                if (!o[k]) o[k] = []

                Uri resourceUri = DomainUtils.getResourceUri(subLink.subnode)
                if (resourceUri) {
                    o[k] << resourceUri
                } else {
                    // ok. we have a literal.

                    Uri literalTypeUri = DomainUtils.getNodeTypeUri(subLink.subnode)
                    String literal = subLink.subnode.literal

                    if (!literalTypeUri) {
                        o[k] << subLink.subnode.literal
                    } else if (literalTypeUri.nsPart.label == 'xs') {
                        try {
                            o[k] << convertXMLSchemaLiteral(literal, literalTypeUri.idPart)
                        }
                        catch (NumberFormatException ex) {
                            log.debug "$ex.message converting $literal of type $literalTypeUri.idPart to a number"
                            o[k] << subLink.subnode.literal
                        }
                    } else {
                        o[k] << [
                                type : literalTypeUri,
                                value: subLink.subnode.literal
                        ]
                    }

                }
            }
        }

        return o
    }

    private static convertXMLSchemaLiteral(String literal, String literalType) {
        def value
        if (!literal) {
            value = null
        } else {
            switch (literalType) {
                case 'boolean':
                    value = Boolean.parseBoolean(literal)
                    break
                case 'float':
                    value = Float.parseFloat(literal)
                    break
                case 'double':
                    value = Double.parseDouble(literal)
                    break
                case 'decimal':
                case 'integer':
                case 'nonPositiveInteger':
                case 'negativeInteger':
                case 'nonNegativeInteger':
                case 'positiveInteger':
                case 'int':
                case 'unsignedInt':
                case 'long':
                case 'unsignedLong':
                case 'short':
                case 'unsignedShort':
                case 'byte':
                case 'unsignedByte':
                    value = Long.parseLong(literal)
                    break
                default:
                    value = literal
            }
        }
        return value
    }

    private static int sortLinks(Link a, Link b) {
        Uri auri = DomainUtils.getNameUri(a.subnode)
        Uri buri = DomainUtils.getNameUri(b.subnode)
        if (!auri && !buri) return 0
        if (!auri) return -1
        if (!buri) return 1
        Name an, bn
        an = auri?.nsPart?.label == 'nsl-name' ? Name.get(auri.idPart as Long) : null
        bn = buri?.nsPart?.label == 'nsl-name' ? Name.get(buri.idPart as Long) : null
        if (!an && !bn) return 0
        if (!an) return -1
        if (!bn) return 1
        return (an.simpleName ?: '') <=> (bn.simpleName ?: '')

    }
}
