class UrlMappings {

    static mappings = {

        "/tree/$treeLabel"(controller: 'treeView', action: 'index')
        "/tree/$treeLabel/name/$nameId"(controller: 'treeView', action: 'index')

        "/api/tree/branch/$treeLabel"(controller: 'treeView', action: 'treeTopBranch')
        "/api/tree/branch/$treeLabel/$nameId"(controller: 'treeView', action: 'namePlacementBranch')
        "/api/tree/name/$treeLabel/$nameId"(controller: 'treeView', action: 'nameInTree')
        "/api/tree/name-placement/$treeLabel/$nameId"(controller: 'treeView', action: 'namePlacementInTree')
        "/api/tree/instance-placement/$treeLabel/$instanceId"(controller: 'treeView', action: 'instancePlacementInTree')
        "/api/tree/place-apni-name"(controller: 'treeEdit', action: 'placeApniName')
        "/api/tree/place-apc-instance"(controller: 'treeEdit', action: 'placeApcInstance')
        "/api/tree/remove-apni-name"(controller: 'treeEdit', action: 'removeApniName')
        "/api/tree/remove-apc-instance"(controller: 'treeEdit', action: 'removeApcInstance')

        "/search"(controller: 'search', action: 'search')
        "/auth/$action"(controller: 'auth')
        "/$product"(controller: 'search', action: 'search')

        "/$namespace/cgi-bin/apni"(controller: 'idMapper', action: 'apni')

        name restResource:
        "/$action/$shard/$idNumber(.$format)?"(controller: 'restResource')


        "/$controller/$shard/$id/$namespace/$action(.$format)?" { }


        "/$namespace/$controller/$action/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/$controller/$action/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        "/"(view: "/index")
        "500"(controller: 'error', action: 'index')
    }
}
