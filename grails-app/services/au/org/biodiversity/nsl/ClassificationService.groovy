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

import grails.plugins.rest.client.RestBuilder
import grails.transaction.Transactional

/**
 * The Classification service is a high level service over the tree service plugin services. It's primary aim is to provide
 * very simple top level services that hide the tree mechanics.
 *
 * We'll try to abstract the classifications away from what the user wants.
 */
@Transactional
class ClassificationService {

    def treeOperationsService
    def queryService

    private RestBuilder rest = new RestBuilder()

    /**
     * Get the list of names in the path
     * @param name
     * @return
     */
    List<Name> getPath(Name name) {
        getPath(name, 'APNI')
    }

    List<Name> getPath(Name name, String classification) {
        Arrangement arrangement = Arrangement.findByLabel(classification)
        if (arrangement) {
            List<Node> nodes = queryService.findCurrentNslName(arrangement, name)
            if (nodes) {
                List<Link> links = queryService.getPathForNode(arrangement, nodes.first())
                return links.findAll { Link link -> link.subnode.internalType == NodeInternalType.T }
                            .collect { Link link -> link.subnode.name }
            } else {
                return []
            }
        } else {
            return []
        }
    }

    Node isNameInAPC(Name name) {
        isNameInClassification(name, 'APC')
    }

    Node isNameInAPNI(Name name) {
        isNameInClassification(name, 'APNI')
    }

    Node isInstanceInAPC(Instance instance) {
        isInstanceInClassification(instance, 'APC')
    }

    Node isInstanceInAPNI(Instance instance) {
        isInstanceInClassification(instance, 'APNI')
    }

    Node isNameInClassification(Name name, String classification) {
        Arrangement arrangement = Arrangement.findByLabel(classification)
        isNameInClassification(name, arrangement)
    }

    Node isNameInClassification(Name name, Arrangement arrangement) {
        if (arrangement) {
            List<Node> nodes = queryService.findCurrentNslName(arrangement, name)
            nodes ? nodes.first() : null
        } else {
            return null
        }
    }

    Node isInstanceInClassification(Instance instance, String classification) {
        Arrangement arrangement = Arrangement.findByLabel(classification)
        if (arrangement) {
            List<Node> nodes = queryService.findCurrentNslInstance(arrangement, instance)
            nodes ? nodes.first() : null
        } else {
            return null
        }
    }

    Name getAPNIFamilyName(Name name) {
        getFamilyName(name, 'APNI')
    }

    Name getAPCFamilyName(Name name) {
        getFamilyName(name, 'APC')
    }

    Name getFamilyName(Name name, String tree) {
        NameRank familyRank = NameRank.findByName('Familia')
        List<Name> namesInPath = getPath(name, tree)
        return namesInPath.find { Name n ->
            return n?.nameRank == familyRank
        }
    }

    Instance getAcceptedInstance(Name name, String tree, String nodeTypeId = 'ApcConcept') {
        List<Instance> instances = Instance.executeQuery('''select nd.instance
from Instance i,
 Node nd
 where i.name = :name
 and nd.instance = i.citedBy
 and nd.checkedInAt is not null
 and nd.next is null
 and nd.root.label = :tree
 and nd.typeUriIdPart = :nodeTypeId
''',
                [name: name, tree: tree, nodeTypeId: nodeTypeId])
        //there should only be one
        if (instances.size() == 1) {
            return instances.first()
        }
    }

    void placeNameInAPNI(Name supername, Name name) {
        Arrangement apni = Arrangement.findByLabel('APNI')

        Collection<Node> currentInApni = queryService.findCurrentNslName(apni, name)

        if (currentInApni.isEmpty()) {
            treeOperationsService.addNslName(apni, name, supername, null, name.updatedBy)
        } else {
            // TODO: when we permit nulls to be used as taxon ids, then we will stop passing in the name as the taxon
            treeOperationsService.updateNslName(apni, name, supername, null, name.updatedBy)
        }
    }

}
