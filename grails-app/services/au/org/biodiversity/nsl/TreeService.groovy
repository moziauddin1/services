package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional

/**
 * The 2.0 Tree service. This service is the central location for all interaction with the tree.
 */
@Transactional
class TreeService implements ValidationUtils {

    /**
     * get the named tree. This is case insensitive
     * @param name
     * @return tree or null if not found
     */
    Tree getTree(String name) {
        mustHave('Tree name': name)
        Tree.findByNameIlike(name)
    }

    /**
     * get the current TreeElement for a name on the given tree
     * @param name
     * @param tree
     * @return treeElement or null if not on the tree
     */
    TreeElement findCurrentElementForName(Name name, Tree tree) {
        if (name && tree) {
            return findElementForName(name, tree.currentTreeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for a name in the given version of a tree
     * @param name
     * @param treeVersion
     * @return treeElement or null if not on the tree
     */
    TreeElement findElementForName(Name name, TreeVersion treeVersion) {
        if (name && treeVersion) {
            return TreeElement.findByNameIdAndTreeVersion(name.id, treeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for an instance in the current version of a tree
     * @param instance
     * @param tree
     * @return treeElement or null if not on the tree
     */
    TreeElement findCurrentElementForInstance(Instance instance, Tree tree) {
        if (instance && tree) {
            return TreeElement.findByInstanceIdAndTreeVersion(instance.id, tree.currentTreeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for an instance in the given version of a tree
     * @param instance
     * @param treeVersion
     * @return treeElement or null if not on the tree
     */
    TreeElement findElementForInstance(Instance instance, TreeVersion treeVersion) {
        if (instance && treeVersion) {
            return TreeElement.findByInstanceIdAndTreeVersion(instance.id, treeVersion)
        }
        return null
    }

    /**
     * get the tree path as a list of TreeElements
     * @param treeElement
     * @return List of TreeElements
     */
    List<TreeElement> getElementPath(TreeElement treeElement) {
        mustHave(treeElement: treeElement)
        treeElement.treePath.split('/').collect { String stringElementId ->
            TreeElement key = new TreeElement(treeVersion: treeElement.treeVersion, treeElementId: stringElementId.toBigInteger())
            TreeElement.get(key)
        }.findAll { it }
    }

    /**
     * Get the child tree Elements of this treeElement
     * @param treeElement
     * @return
     */
    List<TreeElement> childElements(TreeElement treeElement) {
        mustHave(treeElement: treeElement)
        TreeElement.findAllByTreeVersionAndTreePathLike(treeElement.treeVersion, "$treeElement.treePath%", [sort: 'namePath', order: 'asc'])
    }

    /**
     * Get just the display string and links for all the child tree elements.
     * @param treeElement
     * @return [[displayString , link, name link, instance link], ...]
     */
    List<List> childDisplayElements(TreeElement treeElement) {
        mustHave(treeElement: treeElement)
        String pattern = "^${treeElement.treePath}.*"
        fetchDisplayElements(pattern, treeElement.treeVersion)
    }

    /**
     * Get just the display string and link to the child tree elements to depth
     * @param treeElement
     * @return [displayString , link, name link, instance link], ...]
     */
    List<List> childDisplayElementsToDepth(TreeElement treeElement, int depth) {
        mustHave(treeElement: treeElement)
        String pattern = "^${treeElement.treePath}(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeElement.treeVersion)
    }

    /**
     * Get just the display string and link to the child tree elements to depth
     * @param treeElement
     * @return [[displayString , link], ...]
     */
    List<List> displayElementsToDepth(TreeVersion treeVersion, int depth) {
        mustHave(treeElement: treeVersion)
        String pattern = "^[^/]*(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersion)
    }

    List<List> displayElementsToLimit(TreeElement treeElement, Integer limit) {
        displayElementsToLimit(treeElement.treeVersion, "^${treeElement.treePath}", limit)
    }

    List<List> displayElementsToLimit(TreeVersion treeVersion, Integer limit) {
        displayElementsToLimit(treeVersion, "^[^/]*", limit)
    }

    List<List> displayElementsToLimit(TreeVersion treeVersion, String prefix, Integer limit) {
        mustHave(treeElement: treeVersion, limit: limit)
        int depth = 11 //pick a maximum depth - current APC has 10
        int count = countElementsAtDepth(treeVersion, prefix, depth)
        while (depth > 0 && count > limit) {
            depth--
            count = countElementsAtDepth(treeVersion, prefix, depth)
        }
        String pattern = "$prefix(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersion)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private List<List> fetchDisplayElements(String pattern, TreeVersion treeVersion) {
        log.debug("getting $pattern")
        TreeElement.executeQuery('''
select displayString, elementLink, nameLink, instanceLink 
    from TreeElement 
    where treeVersion = :version 
        and regex(treePath, :pattern) = true 
    order by namePath
''', [version: treeVersion, pattern: pattern]) as List<List>
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    // can't be static because of log
    private int countElementsAtDepth(TreeVersion treeVersion, String prefix, int depth) {
        String pattern = "$prefix(/[^/]*){0,$depth}\$"
        int count = TreeElement.executeQuery('''
select count(*) 
    from TreeElement 
    where treeVersion = :version 
        and regex(treePath, :pattern) = true 
''', [version: treeVersion, pattern: pattern]).first() as int
        log.debug "Depth sounding $depth: $count"
        return count
    }

    /**
     * Return the depth of this treeElement down the tree , i.e. the number of levels down the tree.
     * @param treeElement
     * @return
     */
    Integer depth(TreeElement treeElement) {
        mustHave(treeElement: treeElement)
        treeElement.treePath.split('/').size()
    }

}
