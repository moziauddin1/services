package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.ClassificationManagerService
import au.org.biodiversity.nsl.tree.ServiceException
import org.apache.shiro.authz.annotation.RequiresRoles

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

    def configService
    ClassificationManagerService classificationManagerService;

    private static String SELECTED_NODES_KEY = TreeFixupController.class.getName() + '#selectedNodes';

    @RequiresRoles('admin')
    def selectNameNode() {
        def state = []
        session[SELECTED_NODES_KEY] = state;

        Arrangement c = Arrangement.findByNamespaceAndLabelAndArrangementType(
                configService.nameSpace,
                params['classification'] as String, ArrangementType.P)
        Name n = Name.get(params['nameId'] as Long)

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
                    configService.nameSpace,
                    params['classification'] as String),
                    Name.get(params['nameId'] as Long),
                    Node.get(params['nodeId'] as Long));
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
                configService.nameSpace,
                params['classification'] as String, ArrangementType.P)

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
