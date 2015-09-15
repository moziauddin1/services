package au.org.biodiversity.nsl

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

    def index() {
        def keys = session[SELECTED_NODES_KEY] ?: [:];
        def nodes = [:];

        keys.each { k, v -> nodes[Node.get(k)] = Node.get(v) }

        return [
                keys : keys,
                nodes: nodes
        ]
    }

    def clearList() {
        session[SELECTED_NODES_KEY] = [:];
        redirect action: 'index';
    }

    def duplicateNameInClassification() {
        session[SELECTED_NODES_KEY] = [:];
        redirect action: 'index';
    }

    def duplicateInstanceInClassification() {
        session[SELECTED_NODES_KEY] = [:];
        redirect action: 'index';
    }
}
