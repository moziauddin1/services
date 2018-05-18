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

import org.apache.log4j.DailyRollingFileAppender
import org.apache.log4j.Level
import com.theconnman.slacklogger.SlackAppender

// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

grails.config.locations = ["file:${userHome}/.nsl/${appName}-config.groovy"]
updates.dir = "${userHome}/.nsl/updates"

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination

grails.mime.use.accept.header = true
// The ACCEPT header will not be used for content negotiation for user agents containing the following strings (defaults to the 4 major rendering engines)
grails.mime.disable.accept.header.userAgents = ['blah']
grails.mime.types = [ // the first one is the default format
                      all          : '*/*', // 'all' maps to '*' or the first available format in withFormat
                      atom         : 'application/atom+xml',
                      css          : 'text/css',
                      csv          : 'text/csv',
                      form         : 'application/x-www-form-urlencoded',
                      html         : ['text/html', 'application/xhtml+xml'],
                      js           : 'text/javascript',
                      json         : ['application/json', 'text/json'],
                      multipartForm: 'multipart/form-data',
                      rss          : 'application/rss+xml',
                      text         : 'text/plain',
                      hal          : ['application/hal+json', 'application/hal+xml'],
                      xml          : ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// Legacy setting for codec used to encode data with ${}
grails.views.default.codec = "html"

// The default scope for controllers. May be prototype, session or singleton.
// If unspecified, controllers are prototype scoped.
grails.controllers.defaultScope = 'singleton'

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        // filteringCodecForContentType.'text/html' = 'html'
    }
}


grails.converters.encoding = "UTF-8"
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart = false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false

// configure passing transaction's read-only attribute to Hibernate session, queries and criterias
// set "singleSession = false" OSIV mode in hibernate configuration after enabling
grails.hibernate.pass.readonly = false
// configure passing read-only to OSIV session by default, requires "singleSession = false" OSIV mode
grails.hibernate.osiv.readonly = false

grails.gorm.failOnError = true

grails.web.url.converter = 'hyphenated'

environments {
    development {
        grails.logging.jul.usebridge = true
        grails.serverURL = 'http://localhost:8080/services'
        nslServices.rulesEngine.uri = 'http://localhost:7070/rulesEngine/'
        ldap {
            domain = 'domain'
            server.url = 'ldap://localhost:10389'
            search.base = 'ou=users,dc=nsl,dc=bio,dc=org,dc=au'
            search.user = 'uid=admin,ou=system'
            search.pass = 'secret'
        }
    }
    test {
        grails.logging.jul.usebridge = true
        grails.serverURL = 'http://localhost:8080/services'
        nslServices.rulesEngine.uri = 'http://localhost:7070/rulesEngine/'
        ldap {
            domain = 'domain'
            server.url = 'ldap://localhost:10389'
            search.base = 'ou=users,dc=nsl,dc=bio,dc=org,dc=au'
            search.user = 'uid=admin,ou=system'
            search.pass = 'secret'
        }
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = 'http://155.187.10.62:1521/nsl/services'
        nslServices.rulesEngine.uri = 'http://localhost:1521/rulesEngine/'
        ldap {
            domain = 'domain'
            server.url = 'ldap://localhost:10389'
            search.base = 'ou=users,dc=nsl,dc=bio,dc=org,dc=au'
            search.user = 'uid=admin,ou=system'
            search.pass = 'secret'
        }
    }
}

// log4j configuration
// customise logging in ~/.nsl/services-config.groovy

log4j.main = {

    root {
        warn 'dailyFileAppender'
    }

    appenders {
        appender new DailyRollingFileAppender(
                name: 'dailyFileAppender',
                append: true,
                datePattern: "'.'yyyy-MM-dd",
                fileName: (System.getProperty('catalina.base') ?: 'target') + "/logs/nsl-services.log",
                layout: pattern(conversionPattern: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%5p] %c:%L %x - %m%n')
        )

        appender new SlackAppender(
                name: 'slackAppender',
                layout: pattern(conversionPattern: '%c{2} - %m%n'),
                threshold: Level.ERROR
        )

        console name: "stdout",
                layout: pattern(conversionPattern: '%d{yyyy-MM-dd HH:mm:ss.SSS} [%5p] %c:%L %x - %m%n')

    }

    environments {
        development {
            root {
                info 'stdout', 'dailyFileAppender'//, 'slackAppender'
            }
        }

        test {
            root {
                warn 'stdout'
            }
        }

        production {
            appenders {
                'null' name: "stacktrace"
            }

            root {
                info 'dailyFileAppender'
            }
        }
    }

    //uncomment to log sql with parameters
//    trace 'org.hibernate.type.descriptor.sql.BasicBinder'
//    debug 'org.hibernate.SQL'

    error 'org.codehaus.groovy.grails.web.servlet',        // controllers
            'org.codehaus.groovy.grails.web.pages',          // GSP
            'org.codehaus.groovy.grails.web.sitemesh',       // layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping',        // URL mapping
            'org.codehaus.groovy.grails.commons',            // core / classloading
            'org.codehaus.groovy.grails.plugins',            // plugins
            'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'
    debug 'grails.app'
}

grails.cache.config = {
    provider {
        name "ehcache-${appName}-${appVersion}"
    }
    cache {
        name "linkcache"
        maxElementsInMemory 10000
        eternal false
        timeToIdleSeconds 600
        timeToLiveSeconds 3600
        overflowToDisk true
        maxElementsOnDisk 10000000
        diskPersistent false
        diskExpiryThreadIntervalSeconds 600
        memoryStoreEvictionPolicy 'LRU'
    }


    cache {
        name "identitycache"
        maxElementsInMemory 10000
        eternal false
        timeToIdleSeconds 600
        timeToLiveSeconds 3600
        overflowToDisk true
        maxElementsOnDisk 10000000
        diskPersistent false
        diskExpiryThreadIntervalSeconds 600
        memoryStoreEvictionPolicy 'LRU'
    }

    cache {
        name "linkscache"
        maxElementsInMemory 10000
        eternal false
        timeToIdleSeconds 600
        timeToLiveSeconds 3600
        overflowToDisk true
        maxElementsOnDisk 10000000
        diskPersistent false
        diskExpiryThreadIntervalSeconds 600
        memoryStoreEvictionPolicy 'LRU'
    }

//currently not used while we work out eviction policy
//    cache {
//        name 'apniblockcache'
//        maxElementsInMemory 10000
//        eternal false
//        timeToIdleSeconds 120
//        timeToLiveSeconds 120
//        overflowToDisk true
//        maxElementsOnDisk 10000000
//        diskPersistent false
//        diskExpiryThreadIntervalSeconds 120
//        memoryStoreEvictionPolicy 'LRU'
//    }
//
//    cache {
//        name 'apcblockcache'
//        maxElementsInMemory 10000
//        eternal false
//        timeToIdleSeconds 120
//        timeToLiveSeconds 120
//        overflowToDisk true
//        maxElementsOnDisk 10000000
//        diskPersistent false
//        diskExpiryThreadIntervalSeconds 120
//        memoryStoreEvictionPolicy 'LRU'
//    }

    defaultCache {
        maxElementsInMemory 10000
        eternal false
        timeToIdleSeconds 120
        timeToLiveSeconds 120
        overflowToDisk true
        maxElementsOnDisk 10000000
        diskPersistent false
        diskExpiryThreadIntervalSeconds 120
        memoryStoreEvictionPolicy 'LRU'
    }

    defaults {
        maxElementsInMemory 10000
        eternal false
        overflowToDisk false
        maxElementsOnDisk 0
        timeToLiveSeconds 120
        memoryStoreEvictionPolicy 'LRU'
    }
}

grails.assets.minifyCss = false
grails.assets.minifyJs = false

//Note all these config options can be overridden in the ~/.nsl/services-config.groovy

cors {
    url.pattern = '/*'
    headers = ['Access-Control-Allow-Origin' : '*',
               'Access-Control-Allow-Headers': 'authorization, content-type, Content-Type'
    ]
}

shard {
    system.message.file = "${userHome}/.nsl/broadcast.txt"
    temp.file.directory = "/tmp"
}

services {
    mapper.apikey = 'not set'
    link {
        mapperURL = 'http://localhost:7070/nsl-mapper'
        internalMapperURL = 'http://localhost:7070/nsl-mapper'
        editor = 'https://biodiversity.org.au/test-nsl-editor'
    }
}

grails.plugin.slacklogger.webhook = "https://hooks.slack.com/services/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"
grails.plugin.slacklogger.channel = "errors"
grails.plugin.slacklogger.botName = "LocalNSLBot"
