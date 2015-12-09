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
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * The Classification service is a high level service over the tree service plugin services. It's primary aim is to provide
 * very simple top level services that hide the tree mechanics.
 *
 * We'll try to abstract the classifications away from what the user wants.
 */
@Transactional
class ClassificationService {

    GrailsApplication grailsApplication
    def treeOperationsService
    def queryService

    private RestBuilder rest = new RestBuilder()

    /**
     * Get the list of names in the path
     * @param name
     * @return
     */
    @Deprecated
    List<Name> getPath(Name name) {
        getPath(name,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.nameTree as String)
    }

    @Deprecated
    List<Name> getPath(Name name, String classification) {
        log.warn 'deprecated'
        return getPath(name, Namespace.findByName(grailsApplication.config.services.classification.namespace as String), classification)
    }

    List<Name> getPath(Name name, Namespace namespace, String classification) {
        Arrangement arrangement = Arrangement.findByNamespaceAndLabel(
                namespace,
                classification)

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

    @Deprecated
    Node isNameInAPC(Name name) {
        isNameInClassification(name,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.classificationTree as String)
    }

    @Deprecated
    Node isNameInAPNI(Name name) {
        isNameInClassification(name,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.nameTree as String)
    }

    @Deprecated
    Node isInstanceInAPC(Instance instance) {
        isInstanceInClassification(instance,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.classificationTree as String)
    }

    @Deprecated
    Node isInstanceInAPNI(Instance instance) {
        isInstanceInClassification(instance,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.nameTree as String)
    }


    @Deprecated
    Node isNameInClassification(Name name, String classification) {
        log.warn('deprecated')
        return isNameInClassification(name, Namespace.findByName(grailsApplication.config.services.classification.namespace as String), classification)
    }

    Node isNameInClassification(Name name, Namespace namespace, String classification) {
        Arrangement arrangement = Arrangement.findByNamespaceAndLabel(
                namespace,
                classification)
        arrangement ? isNameInClassification(name, arrangement) : null
    }

    Node isNameInClassification(Name name, Arrangement arrangement) {
        if (arrangement) {
            List<Node> nodes = queryService.findCurrentNslName(arrangement, name)
            nodes ? nodes.first() : null
        } else {
            return null
        }
    }

    @Deprecated
    Node isInstanceInClassification(Instance instance, String classification) {
        return isInstanceInClassification(
                instance,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                classification)
    }

    Node isInstanceInClassification(Instance instance, Namespace namespace, String classification) {
        Arrangement arrangement = Arrangement.findByNamespaceAndLabel(namespace, classification)
        arrangement ? isInstanceInClassification(instance, arrangement) : null
    }

    Node isInstanceInClassification(Instance instance, Arrangement arrangement) {
        if (arrangement) {
            List<Node> nodes = queryService.findCurrentNslInstance(arrangement, instance)
            nodes ? nodes.first() : null
        } else {
            return null
        }
    }

    @Deprecated
    Name getAPNIFamilyName(Name name) {
        getFamilyName(
                name,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.nameTree as String
        )
    }

    @Deprecated
    Name getAPCFamilyName(Name name) {
        getFamilyName(
                name,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.classificationTree as String
        )
    }

    @Deprecated
    Name getFamilyName(Name name, String tree) {
        return getFamilyName(
                name,
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                tree
        )
    }

    Name getFamilyName(Name name, Namespace namespace, String tree) {
        NameRank familyRank = NameRank.findByName('Familia')
        List<Name> namesInPath = getPath(name, namespace, tree)
        return namesInPath.find { Name n ->
            return n?.nameRank == familyRank
        }
    }

    @Deprecated
    Instance getAcceptedInstance(Name name, String tree, String nodeTypeId = 'ApcConcept') {
      return getAcceptedInstance(name, Namespace.findByName(grailsApplication.config.services.classification.namespace as String), tree, nodeTypeId)
    }

    Instance getAcceptedInstance(Name name, Namespace namespace, String tree, String nodeTypeId) {
        List<Instance> instances = Instance.executeQuery('''select nd.instance
from Instance i,
 Node nd
 where i.name = :name
 and nd.instance = i.citedBy
 and nd.checkedInAt is not null
 and nd.next is null
 and nd.root.namespace = :namespace
 and nd.root.label = :tree
 and nd.typeUriIdPart = :nodeTypeId
''',
                [name: name, namespace: namespace, tree: tree, nodeTypeId: nodeTypeId])
        //there should only be one
        if (instances.size() == 1) {
            return instances.first()
        }
    }

    @Deprecated
    Node placeNameInAPNI(Name supername, Name name) {
        return placeNameInAPNI(
                Namespace.findByName(grailsApplication.config.services.classification.namespace as String),
                grailsApplication.config.services.classification.nameTree as String,
                supername, name
        )
    }

    Node placeNameInAPNI(Namespace namespace, String nameTreeLabel, Name supername, Name name) {
        Arrangement apni = Arrangement.findByNamespaceAndLabel(namespace, nameTreeLabel)

        Collection<Node> currentInApni = queryService.findCurrentNslName(apni, name)

        if (currentInApni.isEmpty()) {
            return treeOperationsService.addNslName(apni, name, supername, null, name.updatedBy)
        } else {
            // TODO: when we permit nulls to be used as taxon ids, then we will stop passing in the name as the taxon
            return treeOperationsService.updateNslName(apni, name, supername, null, name.updatedBy)
        }
    }

}
