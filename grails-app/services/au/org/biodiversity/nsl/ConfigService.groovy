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
import groovy.sql.Sql
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

    private Namespace nameSpace
    private String nameTreeName
    private String classificationTree

    public Namespace getNameSpace() {
        if (!nameSpace) {
            nameSpace = Namespace.findByName(grailsApplication.config.shard.classification.namespace as String)
        }
        return nameSpace
    }

    public String getNameTreeName() {
        if (!nameTreeName) {
            nameTreeName = ShardConfig.findByName('name tree label')?.value
        }
        return nameTreeName
    }

    public String getClassificationTreeName() {
        if (!classificationTree) {
            classificationTree = ShardConfig.findByName('classification tree label')?.value
        }
        return classificationTree
    }

    public Sql getSqlForNSLDB() {
        String dbUrl = grailsApplication.config.dataSource_nsl.url
        String username = grailsApplication.config.dataSource_nsl.username
        String password = grailsApplication.config.dataSource_nsl.password
        String driverClassName = grailsApplication.config.dataSource_nsl.driverClassName
        log.debug "Getting sql for $dbUrl, $username, $password, $driverClassName"
        Sql.newInstance(dbUrl, username, password, driverClassName)
    }

}
