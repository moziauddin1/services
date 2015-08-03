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
	
	def getBranchForName(Arrangement tree, Name name) {
		if(name) {
			Uri uri = DomainUtils.uri("nsl-name", name.id.toString())
			List<Node> nodes = queryService.findCurrentName(tree, uri) // Should return a unique current node for the name. Should.
			if (nodes) {
				return getBranchForNode(tree, nodes.first())
			}
		}
		
		return getBranchForTree(tree)
	}
	
	def getBranchForNode(Arrangement tree, Node node) {
		if(node) {
			List<Link> links = queryService.getPathForNode(tree, node).findAll { Link it -> it.subnode.internalType != NodeInternalType.V }
			if(links) {
				return buildPath(links)
			}
		}
		
		return getBranchForTree(tree)
	}
	
	def getBranchForTree(Arrangement tree) {
		return populateSubnodeFromLink(DomainUtils.getSingleSublink(tree.node), true)
	}
	
	def getNameInTree(Arrangement tree, Name name) {
		if(name) {
			Uri uri = DomainUtils.uri("nsl-name", name.id.toString())
			List<Node> nodes = queryService.findCurrentName(tree, uri) // Should return a unique current node for the name. Should.
			if (nodes) {
				return populateSubnodeFromNode(nodes.first(), true)
			}
		}
		else {
			
		}
	}
	
	def getNamePlacementInTree(Arrangement tree, Name name) {
		Map<?,?> o = [isPlaced: false];
		
		if(name) {
			Uri uri = DomainUtils.uri("nsl-name", name.id.toString())
			
			o['params']  = [:];
			o['params']['name'] = jsonRendererService.getBriefNameWithHtml(name);
			o['params']['nameUri'] = uri;
			
			Link link = null;
			List<Link> links = queryService.findCurrentNamePlacement(tree, uri) // Should return a unique current node for the name. Should.
			if (links) {
				link = links.first();
			}
			
			if(link) {
				o['isPlaced'] = true;
				populateSubnodeFromLink(link, false, o, true);
				o['supernode'] = populateSubnodeFromNode(link.supernode, false);
			}
		}
		
		return o;
	}
	
	def getInstancePlacementInTree(Arrangement tree, Instance instance) {
		Map<?,?> o = [isPlaced: false, isNamePlaced: false];
		
		if(instance) {
			Link link = null;
			Uri instanceUri = DomainUtils.uri("nsl-instance", instance.id.toString())
			Uri nameUri = DomainUtils.uri("nsl-name", instance.name.id.toString())
			
			o['params']  = [:];
			o['params']['instance'] = jsonRendererService.getBriefInstanceForNameWithHtml(instance);
			o['params']['instanceUri'] = instanceUri;
			o['params']['name'] = jsonRendererService.getBriefNameWithHtml(instance.name);
			o['params']['nameUri'] = nameUri;

			List<Link> links = queryService.findCurrentNamePlacement(tree, nameUri) // Should return a unique current node for the name. Should.
			if (links) {
				link = links.first();
			}
			
			if(link) {
				o['isNamePlaced'] = true;
				o['isPlaced'] = DomainUtils.getTaxonUri(link.subnode) == instanceUri;
				populateSubnodeFromLink(link, false, o, true);
				o['supernode'] = populateSubnodeFromNode(link.supernode, false);
				if(link.supernode.name) {
					o['params'].supername = link.supernode.name.id;
				}
			}
		}
		
		return o;
	}
	
	/* 
	 * recursively build the path. The return value is a list of elements,
	 * having a name, and one of them will have a non-null subname element
	 * containing the tail of the list.
	 */
	
	private Object buildPath(List<Link> links) {
		if(!links) return null;
		
		Link l = links.get(0)
		
		Object sublist = buildPath(links.subList(1, links.size()))
		
		Object o = populateSubnodeFromLink(l, true)
		
		if(sublist) {
			o.subnode = o.subnode.collect { it.node.id == sublist.node.id ? sublist : it }
		}
		
		return o;
	}
	
	private Map<?,?> populateSubnodeFromLink(Link link, boolean withKids, Map<?,?> o = [:], boolean withLiterals = false) {
		if(!link) return null
		
		populateSubnodeFromNode link.subnode, withKids, o, withLiterals
		
		o['link'] = [:]
		
		o['link']['type'] = jsonRendererService.getBriefTreeUri(DomainUtils.getLinkTypeUri(link))
		o['link']['seq'] = link.linkSeq
		o['link']['synthetic'] = link.synthetic
		o['link']['versioning'] = link.versioningMethod.name()

		return o;
	}
	
	private Map<?,?> populateSubnodeFromNode(Node node, boolean withKids, Map<?,?> o = [:], boolean withLiterals = false) {
		if(!node) return null
		
		o['node'] = [:]
		
		o['node']['id'] = node.id;
		o['node']['type'] = jsonRendererService.getBriefTreeUri(DomainUtils.getNodeTypeUri(node))
		o['node']['synthetic'] = node.synthetic

		Uri nameUri = DomainUtils.getNameUri(node)
		o['nameUri'] = nameUri
		if(nameUri?.nsPart?.label == 'nsl-name') {
			Name name = Name.get(nameUri.idPart as Long)
			if(name) {
				o['name'] = jsonRendererService.getBriefNameWithHtml(name);
				o['name']['id'] = name.id; // this is needed to make the APC 'select parent name' form work properly
			}
		}
		
		Uri taxonUri = DomainUtils.getTaxonUri(node)
		o['taxonUri'] = taxonUri
		if(taxonUri?.nsPart?.label == 'nsl-instance') {
			Instance inst = Instance.get(taxonUri.idPart as Long)
			if(inst) {
				o['instance'] = jsonRendererService.getBriefInstanceForNameWithHtml(inst);
			}
		}

		o['fetched'] = withKids;
		o['hasSubnodes'] = !! node.subLink.find { Link it -> it.subnode.internalType != NodeInternalType.V}
		
		if(withKids && o['hasSubnodes']) {
			o['subnode'] =
				node.subLink.findAll { Link it -> it.subnode.internalType != NodeInternalType.V}
				.sort { Link a, Link b -> sortLinks(a,b); }
				.collect { populateSubnodeFromLink(it, false) }
		}
		
		if(withLiterals) {
			node.subLink.findAll { Link it -> it.subnode.internalType == NodeInternalType.V}.each {Link it ->
				Uri linkType = DomainUtils.getLinkTypeUri(it);
				String k;
				if(linkType.isQNameOk()) {
					k = linkType.asQName();
				}
				else {
					k = linkType.asUri();
				}
				
				if(!o[k]) o[k] = [];

				Uri resourceUri = DomainUtils.getResourceUri(it.subnode)
				if(resourceUri) {
					o[k] << resourceUri;
				}
				else {
					// ok. we have a literal.
					
					Uri literalTypeUri = DomainUtils.getNodeTypeUri(it.subnode);
					String literal = it.subnode.literal;
					
					if(!literalTypeUri) {
						o[k] <<  it.subnode.literal;
					}
					else
					if(literalTypeUri.nsPart.label == 'xs') {
						try {
							if(!literal) {
								o[k] << null;
							}
							else
							if(literalTypeUri.idPart == 'boolean') {
								o[k] << Boolean.parseBoolean(literal)
							}
							else 
							if(literalTypeUri.idPart == 'float') {
								o[k] << Float.parseFloat(literal)
							}
							else 
							if(literalTypeUri.idPart == 'double') {
								o[k] << Double.parseDouble(literal)
							}
							else 
							if(false
								|| literalTypeUri.idPart == 'decimal'
								|| literalTypeUri.idPart == 'integer'
								|| literalTypeUri.idPart == 'nonPositiveInteger'
								|| literalTypeUri.idPart == 'negativeInteger'
								|| literalTypeUri.idPart == 'nonNegativeInteger'
								|| literalTypeUri.idPart == 'positiveInteger'
								|| literalTypeUri.idPart == 'int'
								|| literalTypeUri.idPart == 'unsignedInt'
								|| literalTypeUri.idPart == 'long'
								|| literalTypeUri.idPart == 'unsignedLong'
								|| literalTypeUri.idPart == 'short'
								|| literalTypeUri.idPart == 'unsignedShort'
								|| literalTypeUri.idPart == 'byte'
								|| literalTypeUri.idPart == 'unsignedByte'
								) {
								o[k] << Long.parseLong(literal)
							}
							else 
							{
								o[k] <<  literal;
							}
						}
						catch(NumberFormatException ex) {
							o[k] << it.subnode.literal;
						}
					}
					else {
						o[k] << [
							type: literalTypeUri,
							value: it.subnode.literal
						]
					}
					
				}
			}
		}
		
		return o;
	}

	private static int sortLinks(Link a, Link b) {
		Uri auri = DomainUtils.getNameUri(a.subnode);
		Uri buri = DomainUtils.getNameUri(b.subnode);
		if(!auri && !buri) return 0; 
		if(!auri) return -1;
		if(!buri) return 1;
		Name an, bn;
		an = auri?.nsPart?.label == 'nsl-name' ? Name.get(auri.idPart as Long) : null;
		bn = buri?.nsPart?.label == 'nsl-name' ? Name.get(buri.idPart as Long) : null;
		if(!an && !bn) return 0; 
		if(!an) return -1;
		if(!bn) return 1;
		return (an.simpleName ?: '') .compareTo(bn.simpleName ?: '')
		
	} 
}
