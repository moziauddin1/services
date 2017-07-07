package au.org.biodiversity.nsl

import grails.transaction.Transactional

/**
 * The 2.0 Tree service. This service is the central location for all interaction with the tree.
 */
@Transactional
class TreeService {

    /**
     * get the named tree. This is case insensitive
     * @param name
     * @return tree or null if not found
     */
    Tree getTree(String name) {
        Tree.findByNameIlike(name)
    }

    /**
     * get the current TreeElement for a name on the given tree
     * @param name
     * @param tree
     * @return treeElement or null if not on the tree
     */
    TreeElement findTreeElementForName(Name name, Tree tree) {
        findTreeElementForName(name, tree.currentTreeVersion)
    }

    /**
     * get the TreeElement for a name in the given version of a tree
     * @param name
     * @param treeVersion
     * @return treeElement or null if not on the tree
     */
    TreeElement findTreeElementForName(Name name, TreeVersion treeVersion) {
        TreeElement.findByNameIdAndTreeVersion(name.id, treeVersion)
    }

    /**
     * get the tree path as a list of TreeElements
     * @param treeElement
     * @return List of TreeElements
     */
    List<TreeElement> getElementPath(TreeElement treeElement) {
        treeElement.treePath.split('/').collect { String stringElementId ->
            TreeElement key = new TreeElement(treeVersion: treeElement.treeVersion, treeElementId: stringElementId.toBigInteger())
            TreeElement.get(key)
        }
    }


}
