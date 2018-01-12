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

class UrlMappings {

    static excludes = ["/metrics/*", "/monitoring", "/monitoring/*"]

    static mappings = {

        //TODO remove these
//        "/tree/$treeLabel"(controller: 'treeView', action: 'index')
//        "/tree/$treeLabel/name/$nameId"(controller: 'treeView', action: 'index')
//
//        "/api/tree/branch/$treeLabel"(controller: 'treeView', action: 'treeTopBranch')
//        "/api/tree/branch/$treeLabel/$nameId"(controller: 'treeView', action: 'namePlacementBranch')
//        "/api/tree/path/$treeLabel"(controller: 'treeView', action: 'treeTopPath')
//        "/api/tree/path/$treeLabel/$nameId"(controller: 'treeView', action: 'namePlacementPath')
//        "/api/tree/name/$treeLabel/$nameId"(controller: 'treeView', action: 'nameInTree')
//        "/api/tree/name-placement/$treeLabel/$nameId"(controller: 'treeView', action: 'namePlacementInTree')
//        "/api/tree/instance-placement/$treeLabel/$instanceId"(controller: 'treeView', action: 'instancePlacementInTree')
//        "/api/tree/place-apni-name"(controller: 'treeEdit', action: 'placeApniName')
//        "/api/tree/place-apc-instance"(controller: 'treeEdit', action: 'placeApcInstance')
//        "/api/tree/remove-apni-name"(controller: 'treeEdit', action: 'removeApniName')
//        "/api/tree/remove-apc-instance"(controller: 'treeEdit', action: 'removeApcInstance')

//        "/api/bulk-fetch"(controller: 'restResource', action: 'bulkFetch')

        "/search"(controller: 'search', action: 'search')
        "/search/form"(controller: 'search', action: 'searchForm')
        "/auth/$action"(controller: 'auth')

        "/$product"(controller: 'search', action: 'search')

        "/api/cgi-bin/apni"(controller: 'idMapper', action: 'apni')

        "/rest/$controller/$shard/$id/api/$action(.$format)?" {}

        "/rest/$action/$shard/$idNumber?(.$format)?"(controller: 'restResource', method: 'GET')

        "/$controller/$action/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/api/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/"(view: "/index")
        "500"(controller: 'error', action: 'index')
    }
}
