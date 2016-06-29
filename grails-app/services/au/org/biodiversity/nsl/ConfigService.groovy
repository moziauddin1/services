/*
    Copyright 2016 Australian National Botanic Gardens

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

import grails.transaction.Transactional
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * This is a helper service for abstracting, accessing and managing configuration of the services.
 *
 * Service configuration is held in these places
 * * database under the shard_config table
 * * ~/home/.nsl/services-config.groovy
 *
 * The services-config file is a standard grails config file that is slurped at startup and can be accessed via the
 * grailsApplication.config object.
 */

@Transactional
class ConfigService {

    GrailsApplication grailsApplication

    private Namespace nameSpaceName
    private String nameTreeName
    private String classificationTree

    public Namespace getNameSpace() {
        if (!nameSpaceName) {
            nameSpaceName = Namespace.findByName(grailsApplication.config.shard.classification.namespace as String)
        }
        return nameSpaceName
    }

    public String getNameTreeName() {
        if (!nameTreeName) {
            nameTreeName = grailsApplication.config.shard.classification.nameTree
        }
        return nameTreeName
    }

    public String getClassificationTreeName() {
        if (!classificationTree) {
            classificationTree = grailsApplication.config.shard.classification.classificationTree
        }
        return classificationTree
    }
}
