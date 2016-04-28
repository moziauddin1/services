package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.ClassificationManagerService
import au.org.biodiversity.nsl.tree.ServiceException
import org.apache.shiro.authz.annotation.RequiresRoles
import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * This controller allows the user to manually perform versioning on nodes.
 *
 * We store the state in a session variable, mainly because it's just plain easier.
 *
 * The entry points are methods that pre-load the session variable with a selection of nodes
 * chosen as a result of certain situations that may occur.
 *
 */
class TreeFixupController {
    private static String SELECTED_NODES_KEY = TreeFixupController.class.getName() + '#selectedNodes';
    GrailsApplication grailsApplication
    ClassificationManagerService classificationManagerService;


    @RequiresRoles('admin')
    def selectNameNode() {
        def state = []
        session[SELECTED_NODES_KEY] = state;

        Arrangement c = Arrangement.findByNamespaceAndLabelAndArrangementType(
                Namespace.findByName(grailsApplication.config.shard.classification.namespace as String),
                params['classification'], ArrangementType.P)
        Name n = Name.get(params['nameId'])

       [
               classification: c,
               name: n,
               nodes:  Node.findAllByRootAndNameAndReplacedAt(c, n, null).each {state << it.id}
       ]

    }

    @RequiresRoles('admin')
    def doUseNameNode() {
        log.debug 'about to call fixClassificationUseNodeForName'
        try {
            classificationManagerService.fixClassificationUseNodeForName(Arrangement.findByNamespaceAndLabel(
                    Namespace.findByName(grailsApplication.config.shard.classification.namespace as String),
                    params['classification']),
                    Name.get(params['nameId']),
                    Node.get(params['nodeId']));
            log.debug 'done call fixClassificationUseNodeForName'
            flash.message = "All placements of name ${params['nameId']} in ${params['classification']} merged into node  ${params['nodeId']}"
        }
        catch(ServiceException ex) {
            log.error ex, ex
            flash.error = "Failed to merge placements: ${ex}";
        }
        redirect action: 'selectNameNode', params: [classification: params['classification'], nameId: params['nameId']]
    }


    @RequiresRoles('admin')
    def enddateAndMakeCurrent() {
        Arrangement c = Arrangement.findByNamespaceAndLabelAndArrangementType(
                Namespace.findByName(grailsApplication.config.shard.classification.namespace as String),
                params['classification'], ArrangementType.P)

        def p = [
                classification: c
        ]

        p << classificationManagerService.fixClassificationEndDates(c, true)

        return p;
    }

    @RequiresRoles('admin')
    def doEnddateAndMakeCurrent() {
        Arrangement c = Arrangement.get(params['classificationId'] as Long)
        if(!c) throw new IllegalArgumentException(params as String)
        classificationManagerService.fixClassificationEndDates(c, false)
        flash.message = "Enddate performed."
        redirect action: 'enddateAndMakeCurrent', params: [classification: c.label ]
    }

}
