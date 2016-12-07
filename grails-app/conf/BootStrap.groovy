import grails.util.Environment
import org.quartz.Scheduler

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

class BootStrap {
    def grailsApplication
    def jsonRendererService
    def searchService
    def nslDomainService
    def shiroSecurityManager
    def shiroSubjectDAO
    def configService
    Scheduler quartzScheduler

    def init = { servletContext ->
        if(!nslDomainService.checkUpToDate()) {
            Map scriptParams = configService.getUpdateScriptParams()
            if(!nslDomainService.updateToCurrentVersion(configService.sqlForNSLDB, scriptParams)) {
                log.error "Database is not up to date. Run update script on the DB before restarting."
                throw new Exception('Database not at expected version.')
            }
        }
        searchService.registerSuggestions()
        jsonRendererService.registerObjectMashallers()

        if(shiroSecurityManager) {
            shiroSecurityManager.setSubjectDAO(shiroSubjectDAO)
            println "Set subject DAO on security manager."
        }
//        if(Environment.current == Environment.PRODUCTION) {
//            quartzScheduler.start()
//        }
    }
    def destroy = {
    }
}
