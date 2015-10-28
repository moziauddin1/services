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

package services

import au.org.biodiversity.nsl.HibernateDomainUtils
import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.Name

class ServiceTagLib {

    def grailsApplication
    def linkService
    def instanceService

//    static defaultEncodeAs = 'html'
    static encodeAsForTags = [tagName: 'raw']
    static namespace = "st"

    def displayMap = { attrs ->

        def map = attrs.map

        out << '<ul>'
        map.each { k, v ->
            out << "<li>"
            out << "<b>${k.encodeAsHTML()}:</b>&nbsp;"
            if (v instanceof Map) {
                out << displayMap(map: v)
            } else {
                if (v.toString().startsWith('http://')) {
                    out << "<a href='$v'>${v.encodeAsHTML()}</a>"
                } else {
                    out << v.encodeAsHTML()
                }
            }
            out << '</li>'
        }
        out << '</ul>'
    }

    def systemNotification = { attrs ->
        String messageFileName = grailsApplication?.config?.nslServices?.system?.message?.file
        if (messageFileName) {
            File message = new File(messageFileName)
            if (message.exists()) {
                String text = message.text
                if (text) {
                    out << """<div class="alert alert-danger" role="alert">
  <span class="fa fa-warning" aria-hidden="true"></span>
  $text</div>"""
                }
            }
        } else {
            out << 'configure message filename.'
        }
    }

    def scheme = {attrs ->
        def colourScheme = grailsApplication.config.nslServices.colourScheme
        if (colourScheme) {
            out << colourScheme
        }
    }

    def preferedLink = { attrs, body ->
        def target = attrs.target
        if (target) {
            target = HibernateDomainUtils.initializeAndUnproxy(target)
            try {
                def link = linkService.getPreferredLinkForObject(target)
                if (link) {
                    out << "<a href='${link.link}'>"
                    out << body(link: link.link)
                    out << "</a>"
                } else {
                    out << body(link: '/')
                }
            } catch (e) {
                log.debug e.message
            }
        } else {
            out << '<span class="text-danger">Link not available.</span>'
        }
    }

    def editorLink = {attrs, body ->
        def nameId = attrs.nameId
        if (nameId) {
            try {
                String link = (grailsApplication.config.services.link.editor ?: 'https://biodiversity.org.au/nsl-editor') +
                        "/search?query=id:${nameId}&query_field=name-instances&query_on=instance"
                if (link) {
                    out << "<a href='${link}'>"
                    out << body(link: link)
                    out << "</a>"
                }
            } catch (e) {
                log.debug e.message
            }
        } else {
            out << '<span class="text-danger">Link not available.</span>'
        }
    }

    def productName = { attrs ->
        String product = grailsApplication?.config?.nslServices?.product
        if (product) {
            out << product.encodeAsHTML()
        } else {
            out << 'APNI'
        }
    }

    def mapperUrl = { attrs ->
        String url = grailsApplication?.config?.services?.link?.mapperURL
        out << url
    }

    def linkedData = { attr ->
        if (attr.val != null) {
            String description = ''
            def data = null
            if (attr.val instanceof Map) {
                description = attr.val.desc
                data = attr.val.data
            } else {
                data = attr.val
            }

            if (data instanceof String) {
                if (description) {
                    out << "&nbsp;<strong>${data}</strong>"
                    out << "&nbsp;<span class='text text-muted'>$description</span>"
                } else {
                    out << "<strong>${attr.val}</strong>"
                }
                return
            }

            if (data instanceof Collection) {
                out << "<a href='#' data-toggle='collapse' data-target='#${data.hashCode()}'>(${data.size()})</a>"
                out << "&nbsp;<span class='text text-muted'>$description</span>"
                out << "<ol id='${data.hashCode()}' class='collapse'>"
                Integer top = Math.min(data.size(), 99)
                (0..top).each { idx ->
                    Object obj = data[idx]
                    if (obj) {
                        if (obj instanceof Collection) {
                            out << "<li>&nbsp;"
                            out << '['
                            obj.eachWithIndex { Object subObj, i ->
                                if (i) {
                                    out << ', '
                                }
                                printObject(subObj)
                            }
                            out << ']</li>'
                        } else {
                            out << '<li>'
                            printObject(obj)
                            out << '</li>'
                        }
                    }
                }
                if(top < data.size()) {
                    out << '<li>...</li>'
                }
                out << '</ol>'

            } else {
                out << "<strong>${attr.val}</strong>"
            }

        }
    }

    private void printObject(obj) {
        if (obj.properties.containsKey('id')) {
            def target = HibernateDomainUtils.initializeAndUnproxy(obj)
            try {
                def link = linkService.getPreferredLinkForObject(target)
                if (link) {
                    out << "<a href='${link.link}'>${obj}</a>"
                } else {
                    out << "<strong>$obj</strong>"
                }
            } catch (e) {
                out << "<strong>$obj</strong>"
                log.debug e.message
            }
        } else {
            out << "<strong>$obj</strong>"
        }
    }

    def camelToLabel = { attrs ->
        String label = attrs.camel
        if (label) {
            label = label.replaceAll(/([a-z]+)([A-Z])/, '$1 $2').toLowerCase()
            out << label.capitalize()
        }
    }

    private static String toCamelCase(String text) {
        return text.toLowerCase().replaceAll("(_)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() })
    }

    def primaryInstance = { attrs, body ->
        Name name = attrs.name
        String var = attrs.var ?: 'primaryInstance'
        if(name) {
            List<Instance> primaryInstances = instanceService.findPrimaryInstance(name)
            if(primaryInstances && primaryInstances.size() > 0) {
                out << body((var): primaryInstances.first())
            } else {
                out << body((var): null)
            }
        }
    }

    def documentationLink = { attrs ->
        String serverURL = grailsApplication?.config?.grails?.serverURL
        if(serverURL) {
            serverURL -= '/services'
            out << "<a class=\"doco\" href=\"$serverURL/docs/main.html\">"
            out << '<i class="fa fa-book"></i> docs </a>'
        }
    }
}
