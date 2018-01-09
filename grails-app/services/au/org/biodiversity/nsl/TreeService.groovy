package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional
import groovy.sql.Sql
import org.apache.shiro.SecurityUtils

import javax.sql.DataSource
import java.sql.Timestamp
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * The 2.0 Tree service. This service is the central location for all interaction with the tree.
 */
@Transactional
class TreeService implements ValidationUtils {

    DataSource dataSource_nsl
    def configService
    def linkService
    def restCallService
    def treeReportService
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)

    /**
     * get the named tree. This is case insensitive
     * @param name
     * @return tree or null if not found
     */
    @Transactional(readOnly = true)
    Tree getTree(String name) {
        mustHave('Tree name': name)
        Tree.findByNameIlike(name)
    }

    @Transactional(readOnly = true)
    TreeVersionElement getTreeVersionElement(Long versionId, Long elementId) {
        TreeVersionElement.find('from TreeVersionElement where treeVersion.id = :versionId and treeElement.id = :elementId',
                [versionId: versionId, elementId: elementId])
    }

    @Transactional(readOnly = true)
    TreeVersionElement findElementBySimpleName(String simpleName, TreeVersion treeVersion) {
        TreeVersionElement.find("from TreeVersionElement tve where tve.treeElement.simpleName = :simpleName and treeVersion= :version", [simpleName: simpleName, version: treeVersion])
    }

    /**
     * get the current TreeElement for a name on the given tree
     * @param name
     * @param tree
     * @return treeElement or null if not on the tree
     */
    @Transactional(readOnly = true)
    TreeVersionElement findCurrentElementForName(Name name, Tree tree) {
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
    @Transactional(readOnly = true)
    TreeVersionElement findElementForName(Name name, TreeVersion treeVersion) {
        if (name && treeVersion) {
            return TreeVersionElement.find('from TreeVersionElement tve where tve.treeVersion = :treeVersion and tve.treeElement.nameId = :nameId',
                    [treeVersion: treeVersion, nameId: name.id])
        }
        return null
    }

    @Transactional(readOnly = true)
    TreeVersionElement findElementForNameLink(String nameLink, TreeVersion treeVersion) {
        if (nameLink && treeVersion) {
            return TreeVersionElement.find('from TreeVersionElement tve where tve.treeVersion = :treeVersion and tve.treeElement.nameLink = :nameLink',
                    [treeVersion: treeVersion, nameLink: nameLink])
        }
        return null
    }

    /**
     * get the TreeElement for an instance in the current version of a tree
     * @param instance
     * @param tree
     * @return treeElement or null if not on the tree
     */
    @Transactional(readOnly = true)
    TreeVersionElement findCurrentElementForInstance(Instance instance, Tree tree) {
        if (instance && tree) {
            return findElementForInstance(instance, tree.currentTreeVersion)
        }
        return null
    }

    /**
     * get the TreeElement for an instance in the given version of a tree
     * @param instance
     * @param treeVersion
     * @return treeElement or null if not on the tree
     */
    @Transactional(readOnly = true)
    TreeVersionElement findElementForInstance(Instance instance, TreeVersion treeVersion) {
        if (instance && treeVersion) {
            return TreeVersionElement.find('from TreeVersionElement tve where tve.treeVersion = :treeVersion and tve.treeElement.instanceId = :instanceId',
                    [treeVersion: treeVersion, instanceId: instance.id])
        }
        return null
    }

    @Transactional(readOnly = true)
    TreeVersionElement findElementForInstanceLink(String instanceLink, TreeVersion treeVersion) {
        if (instanceLink && treeVersion) {
            return TreeVersionElement.find('from TreeVersionElement tve where tve.treeVersion = :treeVersion and tve.treeElement.instanceLink = :instanceLink',
                    [treeVersion: treeVersion, instanceLink: instanceLink])
        }
        return null
    }

    /**
     * get the tree path as a list of TreeElements
     * @param treeElement
     * @return List of TreeElements
     */
    @Transactional(readOnly = true)
    List<TreeVersionElement> getElementPath(TreeVersionElement treeVersionElement) {
        mustHave(treeVersionElement: treeVersionElement)
        treeVersionElement.treePath.split('/').collect { String stringElementId ->
            if (stringElementId) {
                TreeVersionElement.find('from TreeVersionElement tve where tve.treeElement.id = :elementId and treeVersion = :version',
                        [elementId: stringElementId as Long, version: treeVersionElement.treeVersion])
            } else {
                null
            }
        }.findAll { it }
    }

    @Transactional(readOnly = true)
    List<TreeVersionElement> getChildElementsToDepth(TreeVersionElement parent, int depth) {
        mustHave(parent: parent, 'parent.treeElement': parent.treeElement, 'parent.treeVersion': parent.treeVersion)
        String pattern = "^${parent.treePath}(/[^/]*){1,$depth}\$"
        getElementsByPath(parent.treeVersion, pattern)
    }

    /**
     * Get child elements (not including this element)
     * @param parent
     * @return
     */
    @Transactional(readOnly = true)
    List<TreeVersionElement> getAllChildElements(TreeVersionElement parent) {
        mustHave(parent: parent, 'parent.treeElement': parent.treeElement, 'parent.treeVersion': parent.treeVersion)
        log.debug "getting children for $parent.treeElement.simpleName"
        String pattern = "^${parent.treePath}/.*"
        getElementsByPath(parent.treeVersion, pattern)
    }

    /**
     * Get just the display string and links for all the child tree elements.
     * @param treeElement
     * @return List of DisplayElements
     */
    @Transactional(readOnly = true)
    List<DisplayElement> childDisplayElements(TreeVersionElement treeVersionElement) {
        mustHave(TreeVersionElement: treeVersionElement)
        String pattern = "^${treeVersionElement.treePath}.*"
        fetchDisplayElements(pattern, treeVersionElement.treeVersion)
    }

    @Transactional(readOnly = true)
    int countAllChildElements(TreeVersionElement parent) {
        mustHave(parent: parent, 'parent.treeElement': parent.treeElement, 'parent.treeVersion': parent.treeVersion)
        String pattern = "^${parent.treePath}/.*"
        countElementsByPath(parent.treeVersion, pattern)
    }

    @Transactional(readOnly = true)
    int countElementsAtDepth(TreeVersion treeVersion, String prefix, int depth) {
        mustHave(treeVersion: treeVersion, prefix: prefix)
        String pattern = "$prefix(/[^/]*){0,$depth}\$"
        countElementsByPath(treeVersion, pattern)
    }

    /**
     * Get just the display string and link to the child tree elements to depth
     * @param treeVersionElement
     * @return List of DisplayElements
     */
    @Transactional(readOnly = true)
    List<DisplayElement> childDisplayElementsToDepth(TreeVersionElement treeVersionElement, int depth) {
        mustHave(treeVersionElement: treeVersionElement)
        String pattern = "^${treeVersionElement.treePath}(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersionElement.treeVersion)
    }

    /**
     * Get just the display string and link to the child tree elements to depth
     * @param treeElement
     * @return List of DisplayElement
     */
    @Transactional(readOnly = true)
    List<DisplayElement> displayElementsToDepth(TreeVersion treeVersion, int depth) {
        mustHave(treeElement: treeVersion)
        String pattern = "^[^/]*(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersion)
    }

    @Transactional(readOnly = true)
    List<DisplayElement> displayElementsToLimit(TreeVersionElement treeVersionElement, Integer limit) {
        displayElementsToLimit(treeVersionElement.treeVersion, "^${treeVersionElement.treePath}", limit)
    }

    @Transactional(readOnly = true)
    List<DisplayElement> displayElementsToLimit(TreeVersion treeVersion, Integer limit) {
        displayElementsToLimit(treeVersion, "^[^/]*", limit)
    }

    @Transactional(readOnly = true)
    List<DisplayElement> displayElementsToLimit(TreeVersion treeVersion, String prefix, Integer limit) {
        mustHave(treeVersion: treeVersion, limit: limit)
        int depth = 11 //pick a maximum depth - current APC has 10
        int count = countElementsAtDepth(treeVersion, prefix, depth)
        while (depth > 0 && count > limit) {
            depth--
            count = countElementsAtDepth(treeVersion, prefix, depth)
        }
        String pattern = "$prefix(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersion)
    }

    /**
     * get a list of DisplayElements
     * @param pattern
     * @param treeVersion
     * @return
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    @Transactional(readOnly = true)
    private List<DisplayElement> fetchDisplayElements(String pattern, TreeVersion treeVersion) {
        mustHave(treeVersion: treeVersion, pattern: pattern)
        log.debug("getting $pattern")

        TreeElement.executeQuery('''
select tve.treeElement.displayHtml, tve.elementLink, tve.treeElement.nameLink, tve.treeElement.instanceLink, 
 tve.treeElement.excluded, tve.depth, tve.treeElement.synonymsHtml 
    from TreeVersionElement tve 
    where tve.treeVersion = :version
    and regex(tve.treePath, :pattern) = true 
    order by tve.namePath
''', [version: treeVersion, pattern: pattern]).collect { data ->
            new DisplayElement(data as List)
        } as List<DisplayElement>
    }

    /**
     * Get tree version elements by treePath pattern ordered by treePath.
     * @param version
     * @param pattern
     * @return
     */
    @Transactional(readOnly = true)
    List<TreeVersionElement> getElementsByPath(TreeVersion version, String pattern) {
        mustHave(version: version, pattern: pattern)
        log.debug("getting $pattern")

        TreeVersionElement.executeQuery('''
select tve 
    from TreeVersionElement tve 
    where tve.treeVersion = :version
    and regex(tve.treePath, :pattern) = true 
    order by tve.treePath
''', [version: version, pattern: pattern])
    }

    @Transactional(readOnly = true)
    int countElementsByPath(TreeVersion parent, String pattern) {
        mustHave(parent: parent, pattern: pattern)
        log.debug("counting $pattern")

        int count = TreeElement.executeQuery('''
select count(tve) 
    from TreeVersionElement tve 
    where tve.treeVersion = :version
    and regex(tve.treePath, :pattern) = true  
''', [version: parent, pattern: pattern]).first() as int
        return count
    }

    /** Editing *****************************/

    Tree createNewTree(String treeName, String groupName, Long referenceId, String descriptionHtml,
                       String linkToHomePage, Boolean acceptedTree) {
        Tree tree = Tree.findByName(treeName)
        if (tree) {
            throw new ObjectExistsException("A Tree named $treeName already exists.")
        }
        tree = new Tree(
                name: treeName,
                groupName: groupName,
                referenceId: referenceId,
                descriptionHtml: descriptionHtml,
                linkToHomePage: linkToHomePage,
                acceptedTree: acceptedTree,
                config: [comment_key: "Comment", distribution_key: "Dist."]
        )
        tree.save()
        linkService.addTargetLink(tree)
        return tree
    }

    Tree editTree(Tree tree, String treeName, String groupName, Long referenceId, String descriptionHtml,
                  String linkToHomePage, Boolean acceptedTree) {
        if (!(treeName && groupName)) {
            throw new BadArgumentsException("Tree name ('$treeName') and Group name ('$groupName') must not be null.")
        }
        if (treeName != tree.name && Tree.findByName(treeName)) {
            throw new ObjectExistsException("A Tree named $treeName already exists.")
        }

        if (acceptedTree) {
            //there can only be one. Don't set the current tree to false as this will all be done within a session
            // and transaction.
            Tree.executeUpdate('update Tree set acceptedTree = false where id <> :treeId', [treeId: tree.id])
        }

        tree.name = treeName
        tree.groupName = groupName
        tree.referenceId = referenceId
        tree.descriptionHtml = descriptionHtml
        tree.linkToHomePage = linkToHomePage

        tree.acceptedTree = acceptedTree
        tree.save()

        return tree
    }

    /**
     * Delete a tree and all it's versions/elements
     *
     * WARNING: Because of the nature of a delete the session is flushed and cleared, so you need to refresh any objects
     * held prior to calling this method that you wish to re use. You should also discard anything you don't want persisted.
     *
     * @param tree
     */
    void deleteTree(Tree tree) {
        log.debug "Delete tree $tree"

        Sql sql = getSql()
        for (TreeVersion v in tree.treeVersions) {
            tree = deleteTreeVersion(v, sql)
        }
        tree.delete()
    }

    /**
     * This deletes a tree version and all it's elements.
     *
     * WARNING: Because of the nature of a delete the session is flushed and cleared, so you need to refresh any objects
     * held prior to calling this method that you wish to re use. You should also discard anything you don't want persisted.
     *
     * We return the re-loaded tree from this method as a nice way of reloading the object and helping the old object be
     * GC'd. So if you have a reference to tree call this like:
     *
     * tree = treeService.deleteTreeVersion(treeVersion)
     *
     * @param treeVersion
     * @param sql
     * @return reloaded Tree object of this version.
     */
    Tree deleteTreeVersion(TreeVersion treeVersion, Sql sql = getSql()) {
        notPublished(treeVersion)
        log.debug "deleting version $treeVersion"
        Long treeVersionId = treeVersion.id
        Long treeId = treeVersion.tree.id

        Map result = linkService.bulkRemoveTargets(treeVersion.treeVersionElements)
        log.info result
        if (!result.success) {
            throw new ServiceException("Error deleting tree links from the mapper: ${result.errors}")
        }

        TreeVersion.withSession { s ->
            s.flush()
            s.clear()
        }

        sql.execute('''
UPDATE tree SET default_draft_tree_version_id = NULL WHERE default_draft_tree_version_id = :treeVersionId;
UPDATE tree SET current_tree_version_id = NULL WHERE current_tree_version_id = :treeVersionId;
UPDATE tree_version SET previous_version_id = NULL WHERE previous_version_id = :treeVersionId;
DELETE FROM tree_version_element WHERE tree_version_id = :treeVersionId;
DELETE FROM tree_version WHERE id = :treeVersionId;
''', [treeVersionId: treeVersionId])
        deleteOrphanedTreeElements()
        return Tree.get(treeId)
    }

    private void deleteOrphanedTreeElements() {
        Closure work = {
            Sql sql = getSql()
            Integer count = sql.firstRow('SELECT count(*) FROM tree_element WHERE id NOT IN (SELECT DISTINCT(tree_element_id) FROM tree_version_element)')[0] as Integer
            if (count) {
                log.debug "deleting $count orphaned elements."

                sql.execute('''
SELECT id INTO TEMP orphans FROM tree_element WHERE id NOT IN (SELECT DISTINCT(tree_element_id) FROM tree_version_element); 
UPDATE tree_element SET previous_element_id = NULL FROM orphans o WHERE previous_element_id = o.id;
DELETE FROM tree_element e USING orphans o WHERE e.id = o.id;
DROP TABLE IF EXISTS orphans;
''')
            }
        }
        //We could make this a worker thread that does a GC every so often
        log.debug "Scheduling delete orphan elements"
        scheduler.schedule(work, 30, TimeUnit.SECONDS)
    }

    TreeVersion publishTreeVersion(TreeVersion treeVersion, String publishedBy, String logEntry) {
        log.debug "Publish tree version $treeVersion by $publishedBy, with log entry $logEntry"
        treeVersion.published = true
        treeVersion.logEntry = logEntry
        treeVersion.publishedAt = new Timestamp(System.currentTimeMillis())
        treeVersion.publishedBy = publishedBy
        treeVersion.save()
        if (treeVersion.tree.defaultDraftTreeVersion == treeVersion) {
            treeVersion.tree.defaultDraftTreeVersion = null
        }
        treeVersion.tree.currentTreeVersion = treeVersion
        treeVersion.tree.save()
        //clean up any draft tree elements left behind
        deleteOrphanedTreeElements()

        return treeVersion
    }

    TreeVersion createDefaultDraftVersion(Tree tree, TreeVersion treeVersion, String draftName) {
        log.debug "create default draft version $draftName on $tree using $treeVersion"
        tree.defaultDraftTreeVersion = createTreeVersion(tree, treeVersion, draftName)
        tree.save()
        return tree.defaultDraftTreeVersion
    }

    TreeVersion setDefaultDraftVersion(TreeVersion treeVersion) {
        log.debug "set default draft version $treeVersion"
        if (treeVersion.published) {
            throw new BadArgumentsException("TreeVersion must be draft to set as the default draft version. $treeVersion")
        }
        treeVersion.tree.defaultDraftTreeVersion = treeVersion
        treeVersion.tree.save()
        return treeVersion
    }

    TreeVersion createTreeVersion(Tree tree, TreeVersion treeVersion, String draftName) {
        log.debug "create tree version $draftName on $tree using $treeVersion"
        if (!draftName) {
            throw new BadArgumentsException("Draft name is required and can't be blank.")
        }
        TreeVersion fromVersion = (treeVersion ?: tree.currentTreeVersion)
        TreeVersion newVersion = new TreeVersion(
                tree: tree,
                previousVersion: fromVersion,
                draftName: draftName
        )
        tree.addToTreeVersions(newVersion)
        tree.save(flush: true)

        String link = linkService.addTargetLink(newVersion)
        log.debug "added TreeVersion link $link"

        if (fromVersion) {
            copyVersion(fromVersion, newVersion)
            newVersion.previousVersion = fromVersion
        }
        return newVersion
    }

    void copyVersion(TreeVersion fromVersion, TreeVersion toVersion) {
        if (!(fromVersion && toVersion)) {
            throw new BadArgumentsException("A from and to version are required to copy a version.")
        }
        log.debug "copying from $fromVersion to $toVersion"

        Sql sql = getSql()

        sql.execute('''
INSERT INTO tree_version_element (tree_version_id, 
                                  tree_element_id, 
                                  parent_id, 
                                  taxon_id, 
                                  element_link, 
                                  taxon_link, 
                                  tree_path,
                                  name_path,
                                  depth) 
  (SELECT :toVersionId, 
          tve.tree_element_id, 
          regexp_replace(tve.parent_id,  :fromVersionIdMatch, :toVersionIdMatch) ,
          tve.taxon_id, 
          regexp_replace(tve.element_link,  :fromVersionIdMatch, :toVersionIdMatch),
          tve.taxon_link,
          tve.tree_path,
          tve.name_path,
          tve.depth
   FROM tree_version_element tve WHERE tree_version_id = :fromVersionId)''',
                [fromVersionId     : fromVersion.id,
                 toVersionId       : toVersion.id,
                 fromVersionIdMatch: "/${fromVersion.id}/".toString(),
                 toVersionIdMatch  : "/${toVersion.id}/".toString()])

        toVersion.refresh()

        if (fromVersion.treeVersionElements.size() != toVersion.treeVersionElements.size()) {
            throw new ServiceException("Error copying tree version $fromVersion to $toVersion. They are not the same size. ${fromVersion.treeVersionElements.size()} != ${toVersion.treeVersionElements.size()}")
        }
        Map result = linkService.bulkAddTargets(toVersion.treeVersionElements)
        log.info result
        if (!result.success) {
            throw new ServiceException("Error adding new tree links to the mapper: ${result.errors}")
        }
    }

    String authorizeTreeOperation(Tree tree) {
        String groupName = tree.groupName
        log.debug("checking ${SecurityUtils.subject.principal} has role ${groupName}")
        SecurityUtils.subject.checkRole(groupName)
        return SecurityUtils.subject.principal as String
    }

    String authorizeTreeBuilder() {
        SecurityUtils.subject.checkRole('treeBuilder')
        return SecurityUtils.subject.principal as String
    }

    TreeVersion editTreeVersion(TreeVersion treeVersion, String draftName) {
        if (!draftName) {
            throw new BadArgumentsException('Draft name must be set when editing tree version.')
        }
        treeVersion.draftName = draftName
        treeVersion.save()
        return treeVersion
    }

    Map placeTaxonUri(TreeVersionElement parentElement, String instanceUri, Boolean excluded, String userName) {

        TaxonData taxonData = findInstanceByUri(instanceUri)
        if (!taxonData) {
            throw new ObjectNotFoundException("Taxon $instanceUri not found, trying to place it in $parentElement")
        }
        notPublished(parentElement)
        taxonData.excluded = excluded
        List<String> warnings = validateNewElementPlacement(parentElement, taxonData)
        //will throw exceptions for invalid placements, not warnings

        TreeElement treeElement = findTreeElement(taxonData) ?: makeTreeElementFromTaxonData(taxonData, null, userName)
        TreeVersionElement childElement = saveTreeVersionElement(treeElement, parentElement, nextSequenceId(), null)
        updateParentTaxaId(parentElement)
        return [childElement: childElement, warnings: warnings, message: "Placed ${childElement.treeElement.name.fullName}"]
    }

    /**
     * Place a taxon at the "top" of the tree, with no parent
     * @param treeVersion
     * @param instanceUri
     * @param excluded
     * @param userName
     * @return
     */
    Map placeTaxonUri(TreeVersion treeVersion, String instanceUri, Boolean excluded, String userName) {

        TaxonData taxonData = findInstanceByUri(instanceUri)
        if (!taxonData) {
            throw new ObjectNotFoundException("Taxon $instanceUri not found, trying to place it in $parentElement")
        }
        notPublished(treeVersion)
        taxonData.excluded = excluded

        TreeElement treeElement = findTreeElement(taxonData) ?: makeTreeElementFromTaxonData(taxonData, null, userName)
        TreeVersionElement childElement = saveTreeVersionElement(treeElement, null, treeVersion, nextSequenceId(), null)

        return [childElement: childElement, warnings: [], message: "Placed ${childElement.treeElement.name.fullName}"]
    }

    /**
     * Replace an existing Taxon Concept with another one and possibly move it's placement on the tree.
     * This moves the child taxa from the replaced taxon to this new one.
     *
     * A new taxon (tree element) is created with a new instance and placed on the tree under the desired
     * parent element. The child tree version elements tree paths are updated.
     *
     * This will copy the status from the replaced taxon. The profile will be copied from the replacedTaxon
     * only if the taxonData from the instance doesn't contain profile data.
     *
     * The old tree element will be removed from the current version of the tree.
     *
     * @param currentTve
     * @param parentTve
     * @param instanceUri
     * @param userName
     * @return
     */
    Map replaceTaxon(TreeVersionElement currentTve, TreeVersionElement parentTve, String instanceUri, String userName) {
        mustHave('Current Element': currentTve, 'Parent Element': parentTve, 'Instance Uri': instanceUri, userName: userName)
        notPublished(parentTve)

        if (currentTve.treeElement.instanceLink == instanceUri) {
            Map problems = treeReportService.validateTreeVersion(currentTve.treeVersion)
            return [replacementElement: currentTve, problems: problems]
        }

        TaxonData taxonData = findInstanceByUri(instanceUri)
        if (!taxonData) {
            throw new ObjectNotFoundException("Taxon $instanceUri not found, trying to place it in $parentTve")
        }

        taxonData.excluded = currentTve.treeElement.excluded
        if (taxonData.profile == null && currentTve.treeElement.profile != null) {
            taxonData.profile = currentTve.treeElement.profile
        }

        List<String> warnings = validateReplacementElement(parentTve, currentTve, taxonData)

        TreeElement treeElement = findTreeElement(taxonData) ?: makeTreeElementFromTaxonData(taxonData, currentTve.treeElement, userName)
        TreeVersionElement replacementTve = saveTreeVersionElement(treeElement, parentTve, nextSequenceId(), null)
        updateParentId(currentTve, replacementTve)
        updateChildTreePath(replacementTve, currentTve)

        updateParentTaxaId(parentTve)
        if (parentTve != currentTve.parent) {
            updateParentTaxaId(currentTve.parent)
        }

        deleteTreeVersionElement(currentTve)

        Map problems = treeReportService.validateTreeVersion(replacementTve.treeVersion)
        problems.put('warnings', warnings)
        return [replacementElement: replacementTve, problems: problems]
    }

    /**
     * for each treeVersionElement in the parent branch set the taxonId to a new, unique value. This can only happen in
     * a draft tree version, so first check the taxonId is not already unique (i.e. already been updated in this version)
     * before updating, to prevent wasting ID space and links.
     *
     * This does *not* check the draft status of the parent, so it needs to be checked before calling.
     *
     * @param parent
     */
    private void updateParentTaxaId(TreeVersionElement parent) {
        List<TreeVersionElement> branchElements = getParentTreeVersionElements(parent)
        Sql sql = getSql()
        branchElements.each { TreeVersionElement element ->
            if (!isUniqueTaxon(element)) {
                element.taxonId = nextSequenceId(sql)
                element.taxonLink = linkService.addTaxonIdentifier(element)
                element.save()
            }
        }
        sql.close()
    }

    private static boolean isUniqueTaxon(TreeVersionElement element) {
        TreeVersionElement.countByTaxonId(element.taxonId) == 1
    }

    /**
     * Fetch the tree version elements for each tree element in the tree path. This returns a List but in no guaranteed
     * order.
     * @param treeVersionElement
     * @return a set of TreeVersionElements
     */
    protected static List<TreeVersionElement> getParentTreeVersionElements(TreeVersionElement treeVersionElement) {
        List<Long> elementIds = treeVersionElement.treePath[1..-1].split('/').collect { it.toLong() }
        TreeVersionElement.findAll("from TreeVersionElement where treeVersion = :version and treeElement.id in :elements",
                [version: treeVersionElement.treeVersion, elements: elementIds])
    }

    /**
     * Remove this tree version element and all it's children.
     * @param treeVersionElement
     * @return count of treeVersionElements removed.
     */
    int removeTreeVersionElement(TreeVersionElement treeVersionElement) {
        notPublished(treeVersionElement)

        TreeVersionElement parent = treeVersionElement.parent

        List<TreeVersionElement> elements = getAllChildElements(treeVersionElement)
        elements.add(treeVersionElement)
        int count = elements.size()
        log.debug "Deleting ${count} tree version elements."
        Map result = linkService.bulkRemoveTargets(elements)
        log.info result
        if (!result.success) {
            throw new ServiceException("Error deleting tree links from the mapper: ${result.errors}")
        }

        for (TreeVersionElement kid in elements) {
            log.debug "Deleting $kid"
            kid.treeElement.removeFromTreeVersionElements(kid)
            kid.treeVersion.removeFromTreeVersionElements(kid)
            kid.delete()
        }

        updateParentTaxaId(parent)

        elements.clear()
        //if this is removing new elements in a draft we may orphan some tree elements so it pays to clean up
        //this may be moved to a background garbage collection task if it is too slow.
        deleteOrphanedTreeElements()
        return count
    }

    TreeVersionElement editProfile(TreeVersionElement treeVersionElement, Map profile, String userName) {
        mustHave(treeVersionElement: treeVersionElement, userName: userName)
        notPublished(treeVersionElement)
        treeVersionElement.treeElement.refresh() //fetch the element data including treeVersionElements

        log.debug treeVersionElement.treeElement.profile.toString()
        log.debug profile.toString()
        if (treeVersionElement.treeElement.profile == profile) {
            return treeVersionElement // data is equal, do nothing
        }

        //if there is an element that matches the new data use that element
        Map elementData = elementDataFromElement(treeVersionElement.treeElement)
        elementData.profile = profile
        TreeElement foundElement = findTreeElement(elementData)
        if (foundElement) {
            log.debug "Reusing $foundElement"
            return changeElement(treeVersionElement, foundElement)
        }

        //if this is not a draft only element clone it
        if (treeVersionElement.treeElement.treeVersionElements.size() > 1) {
            TreeElement copiedElement = copyTreeElement(treeVersionElement.treeElement, userName)
            treeVersionElement = changeElement(treeVersionElement, copiedElement)
            //don't update taxonId above as the taxon hasn't changed
        } else {
            treeVersionElement.treeElement.updatedBy = userName
            treeVersionElement.treeElement.updatedAt = new Timestamp(System.currentTimeMillis())
        }

        treeVersionElement.treeElement.profile = profile
        treeVersionElement.treeElement.updatedBy = userName
        treeVersionElement.treeElement.updatedAt = new Timestamp(System.currentTimeMillis())
        treeVersionElement.save()
        return treeVersionElement
    }

    //todo determine if this is needed
    TreeVersionElement minorEditProfile(TreeVersionElement treeVersionElement, Map profile, String userName) {
        mustHave(treeVersionElement: treeVersionElement, profile: profile, userName: userName)
        notPublished(treeVersionElement)
        treeVersionElement.treeElement.refresh() //fetch the element data including treeVersionElements

        log.debug treeVersionElement.treeElement.profile.toString()
        log.debug profile.toString()
        if (treeVersionElement.treeElement.profile == profile) {
            return treeVersionElement // data is equal, do nothing
        }

        treeVersionElement.treeElement.profile = profile
        treeVersionElement.treeElement.updatedBy = userName
        treeVersionElement.treeElement.updatedAt = new Timestamp(System.currentTimeMillis())
        treeVersionElement.save()
        return treeVersionElement
    }

    TreeVersionElement editExcluded(TreeVersionElement treeVersionElement, Boolean excluded, String userName) {
        mustHave(treeVersionElement: treeVersionElement, userName: userName)
        notPublished(treeVersionElement)
        treeVersionElement.treeElement.refresh() //fetch the element data including treeVersionElements

        if (treeVersionElement.treeElement.excluded == excluded) {
            return treeVersionElement // data equal, do nothing
        }

        //if there is an element that matches the new data use that element
        Map elementData = elementDataFromElement(treeVersionElement.treeElement)
        elementData.excluded = excluded
        TreeElement foundElement = findTreeElement(elementData)
        if (foundElement) {
            return changeElement(treeVersionElement, foundElement)
        }

        //if this is not a draft only element clone it
        if (treeVersionElement.treeElement.treeVersionElements.size() > 1) {
            TreeElement copiedElement = copyTreeElement(treeVersionElement.treeElement, userName)
            treeVersionElement = changeElement(treeVersionElement, copiedElement)
            //don't update taxonId above as the taxon hasn't changed
        } else {
            treeVersionElement.treeElement.updatedBy = userName
            treeVersionElement.treeElement.updatedAt = new Timestamp(System.currentTimeMillis())
        }

        treeVersionElement.treeElement.excluded = excluded
        treeVersionElement.save()
        return treeVersionElement
    }

    /**
     * Take replace and existing tree version element with a new one using a different tree element.
     *
     * This updates the tree version element tree path and deletes the existing treeVersionElement, so make sure it's
     * not published before calling this.
     *
     * @param treeVersionElement
     * @param newElement
     * @param userName
     * @return the replacement tree version element
     */
    private TreeVersionElement changeElement(TreeVersionElement treeVersionElement, TreeElement newElement) {
        TreeVersionElement replacementTve = treeVersionElement.parent ?
                saveTreeVersionElement(newElement, treeVersionElement.parent, treeVersionElement.taxonId, treeVersionElement.taxonLink) :
                saveTreeVersionElement(newElement, null, treeVersionElement.treeVersion, treeVersionElement.taxonId, treeVersionElement.taxonLink)
        updateChildTreePath(replacementTve, treeVersionElement)
        updateParentId(treeVersionElement, replacementTve)
        deleteTreeVersionElement(treeVersionElement)
        return replacementTve
    }

    /**
     * for tree version elements in this version update tree paths that contain the old element ids to contain the new
     * element id.
     *
     * Note that because this does an update, if you have any of the children loaded in the session you will need to
     * refresh them to see the change in the treePath.
     *
     * @param newTve
     * @param oldElementId
     * @return
     */
    protected TreeVersionElement updateChildTreePath(TreeVersionElement newTve, TreeVersionElement oldTve) {
        updateChildTreePath(newTve.treePath, oldTve.treePath, newTve.treeVersion)
        return newTve
    }

    protected void updateChildTreePath(String newPath, String oldPath, TreeVersion treeVersion) {
        log.debug "Replacing $oldPath with $newPath"
        TreeVersionElement.executeUpdate('''
update TreeVersionElement set treePath = regexp_replace(treePath, :oldId, :newId)
where treeVersion = :version
and regex(treePath, :oldId) = true
''',
                [oldId  : "^$oldPath/",
                 newId  : "$newPath/",
                 version: treeVersion])
    }

    private TreeVersionElement updateParentId(TreeVersionElement oldParent, TreeVersionElement newParent) {
        TreeVersionElement.executeUpdate('''
update TreeVersionElement set parent = :newParent
where parent = :oldParent''', [newParent: newParent, oldParent: oldParent])
        return newParent
    }


    private void removeLink(TreeVersionElement treeVersionElement) {
        Map result = linkService.bulkRemoveTargets([treeVersionElement])
        if (!result.success) {
            throw new ServiceException("Error deleting tree links from the mapper: ${result.errors}")
        }
    }

    private deleteTreeVersionElement(TreeVersionElement target) {
        removeLink(target)
        target.treeElement.removeFromTreeVersionElements(target)
        target.treeVersion.removeFromTreeVersionElements(target)
        target.delete()
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private TreeElement copyTreeElement(TreeElement source, String userName) {
        TreeElement treeElement = new TreeElement(instanceId: source.instanceId,
                nameId: source.nameId,
                excluded: source.excluded,
                displayHtml: source.displayHtml,
                synonymsHtml: source.synonymsHtml,
                simpleName: source.simpleName,
                nameElement: source.nameElement,
                rank: source.rank,
                sourceShard: source.sourceShard,
                synonyms: source.synonyms,
                profile: source.profile,
                sourceElementLink: source.sourceElementLink,
                nameLink: source.nameLink,
                instanceLink: source.instanceLink,
                updatedBy: userName,
                updatedAt: new Timestamp(System.currentTimeMillis()))
        treeElement.save()
        // setting these references here because of a bug? setting in the map above where the parentElement
        // changes to this new element.
        treeElement.previousElement = source
        treeElement.save()
        return treeElement
    }

    protected static notPublished(TreeVersionElement element) {
        notPublished(element.treeVersion)
    }

    protected static notPublished(TreeVersion version) {
        if (version.published) {
            throw new PublishedVersionException("You can't do this with a Published tree. $version.tree.name version $version.id is already published.")
        }
    }

    protected
    static TreeElement makeTreeElementFromTaxonData(TaxonData taxonData, TreeElement previousElement, String userName) {
        new TreeElement(
                previousElement: previousElement,
                instanceId: taxonData.instanceId,
                nameId: taxonData.nameId,
                excluded: taxonData.excluded,
                displayHtml: taxonData.displayHtml,
                synonymsHtml: taxonData.synonymsHtml,
                simpleName: taxonData.simpleName,
                nameElement: taxonData.nameElement,
                rank: taxonData.rank,
                sourceShard: taxonData.sourceShard,
                synonyms: taxonData.synonyms,
                profile: taxonData.profile,
                sourceElementLink: null,
                nameLink: taxonData.nameLink,
                instanceLink: taxonData.instanceLink,
                updatedBy: userName,
                updatedAt: new Timestamp(System.currentTimeMillis())
        ).save()
    }

    private static findTreeElement(Map treeElementData) {
        TreeElement.findWhere(treeElementData)
    }

    private static TreeElement findTreeElement(TaxonData taxonData) {
        findTreeElement(
                [instanceId : taxonData.instanceId,
                 nameId     : taxonData.nameId,
                 excluded   : taxonData.excluded,
                 simpleName : taxonData.simpleName,
                 nameElement: taxonData.nameElement,
                 sourceShard: taxonData.sourceShard,
                 synonyms   : taxonData.synonyms]
        )
    }

    protected static Map elementDataFromElement(TreeElement treeElement) {
        [
                instanceId : treeElement.instanceId,
                nameId     : treeElement.nameId,
                excluded   : treeElement.excluded,
                simpleName : treeElement.simpleName,
                nameElement: treeElement.nameElement,
                sourceShard: treeElement.sourceShard,
                synonyms   : treeElement.synonyms,
                profile    : treeElement.profile
        ]
    }

    private TreeVersionElement saveTreeVersionElement(TreeElement element, TreeVersionElement parentTve, Long taxonId, String taxonLink) {
        saveTreeVersionElement(element, parentTve, parentTve.treeVersion, taxonId, taxonLink)
    }

    private TreeVersionElement saveTreeVersionElement(TreeElement element, TreeVersionElement parentTve, TreeVersion version, Long taxonId, String taxonLink) {
        TreeVersionElement treeVersionElement = new TreeVersionElement(
                treeElement: element,
                treeVersion: version,
                parent: parentTve,
                taxonId: taxonId,
                treePath: makeTreePath(parentTve, element),
                namePath: makeNamePath(parentTve, element),
                depth: (parentTve?.depth ?: 0) + 1
        )

        treeVersionElement.elementLink = linkService.addTargetLink(treeVersionElement)
        treeVersionElement.taxonLink = taxonLink ?: linkService.addTaxonIdentifier(treeVersionElement)
        treeVersionElement.save()
        return treeVersionElement
    }

    private static String makeTreePath(TreeVersionElement parentTve, TreeElement element) {
        if (parentTve) {
            parentTve.treePath + "/${element.id}"
        } else {
            element.id.toString()
        }
    }

    private static String makeNamePath(TreeVersionElement parentTve, TreeElement element) {
        if (parentTve) {
            parentTve.namePath + "/${element.nameElement}"
        } else {
            element.nameElement
        }
    }

    private Long nextSequenceId(Sql sql = getSql()) {
        sql.firstRow("SELECT nextval('nsl_global_seq')")[0] as Long
    }

    protected List<String> validateNewElementPlacement(TreeVersionElement parentElement, TaxonData taxonData) {
        List<String> warnings

        TreeVersion treeVersion = parentElement.treeVersion

        warnings = checkNameValidity(taxonData)
        checkInstanceOnTree(taxonData, treeVersion)
        checkNameAlreadyOnTree(taxonData, treeVersion)

        NameRank taxonRank = NameRank.findByName(taxonData.rank)
        NameRank parentRank = NameRank.findByName(parentElement.treeElement.rank)

        //is rank below parent
        if (!RankUtils.rankHigherThan(parentRank, taxonRank)) {
            throw new BadArgumentsException("Name $taxonData.simpleName of rank $taxonRank.name is not below rank $parentRank.name of $parentElement.treeElement.simpleName.")
        }

        //polynomials must be placed under parent
        checkPolynomialsBelowNameParent(taxonData.simpleName, taxonData.excluded, taxonRank, parentElement.namePath.split('/'))

        checkForExistingSynonyms(taxonData, treeVersion, [])

        return warnings
    }

    protected List<String> validateReplacementElement(TreeVersionElement parentElement, TreeVersionElement currentTve, TaxonData taxonData) {
        List<String> warnings

        TreeVersion treeVersion = parentElement.treeVersion

        warnings = checkNameValidity(taxonData)
        checkInstanceOnTree(taxonData, treeVersion)

        NameRank taxonRank = NameRank.findByName(taxonData.rank)
        NameRank parentRank = NameRank.findByName(parentElement.treeElement.rank)

        //is rank below parent
        if (!RankUtils.rankHigherThan(parentRank, taxonRank)) {
            throw new BadArgumentsException("Name $taxonData.simpleName of rank $taxonRank.name is not below rank $parentRank.name of $parentElement.treeElement.simpleName.")
        }

        //polynomials must be placed under parent
        checkPolynomialsBelowNameParent(taxonData.simpleName, taxonData.excluded, taxonRank, parentElement.namePath.split('/'))

        checkForExistingSynonyms(taxonData, treeVersion, [currentTve])

        return warnings
    }

    private void checkForExistingSynonyms(TaxonData taxonData, TreeVersion treeVersion, List<TreeVersionElement> excluding) {
        //a name can't be already in the tree as a synonym
        List<Map> existingSynonyms = checkSynonyms(taxonData, treeVersion, excluding)
        if (!existingSynonyms.empty) {
            String synonyms = existingSynonyms.collect {
                "* ${it.synonym} (${it.synonymId}) is a ${it.type} of [${it.simpleName} (${it.nameId})](${it.existing} '${it.existing}')."
            }.join(',\n')
            throw new BadArgumentsException("${treeVersion.tree.name} version $treeVersion.id already contains name *${taxonData.simpleName}*:\n\n" +
                    synonyms +
                    "\n\naccording to the concepts involved.")
        }
    }

    private void checkInstanceOnTree(TaxonData taxonData, TreeVersion treeVersion) {
        //is instance already in the tree. We use instance link because that works across shards, there is a remote possibility instance id will clash.
        TreeVersionElement existingElement = findElementForInstanceLink(taxonData.instanceLink, treeVersion)
        if (existingElement) {
            throw new BadArgumentsException("${treeVersion.tree.name} version $treeVersion.id already contains taxon ${taxonData.instanceLink}. See ${existingElement.elementLink}")
        }
    }

    private static List<String> checkNameValidity(TaxonData taxonData) {
        List<String> warnings = []
        //name should not be invalid or illegal
        if (taxonData.nomIlleg) {
            warnings.add("$taxonData.simpleName is nomIlleg")
        }
        if (taxonData.nomInval) {
            warnings.add("$taxonData.simpleName is nomInval")
        }
        return warnings
    }

    private void checkNameAlreadyOnTree(TaxonData taxonData, TreeVersion treeVersion) {
        //a name can't be in the tree already
        TreeVersionElement existingNameElement = findElementForNameLink(taxonData.nameLink, treeVersion)
        if (existingNameElement) {
            throw new BadArgumentsException("${treeVersion.tree.name} version $treeVersion.id already contains name ${taxonData.nameLink}. See ${existingNameElement.elementLink}")
        }
    }

    protected List<Map> checkSynonyms(TaxonData taxonData, TreeVersion treeVersion, List<TreeVersionElement> excluding, Sql sql = getSql()) {

        List<Map> synonymsFound = []
        List nameIdList = [taxonData.nameId] + filterSynonyms(taxonData).collect { it.value.name_id }

        String nameIds = nameIdList.join(',')

        if (!nameIds) {
            return []
        }

        String excludedLinks = "'" + excluding.collect { it.elementLink }.join("','") + "'"

        sql.eachRow("""
SELECT
  el.name_id as name_id,
  el.simple_name as simple_name,
  tax_syn as synonym,
  synonyms -> tax_syn ->> 'type' as syn_type,
  synonyms -> tax_syn ->> 'name_id' as syn_id,
  tve.element_link as element_link
FROM tree_element el join tree_version_element tve on el.id = tve.tree_element_id 
  JOIN name n ON el.name_id = n.id,
      jsonb_object_keys(synonyms) AS tax_syn
WHERE tve.tree_version_id = :versionId                                                
and tve.element_link not in ($excludedLinks)
      AND synonyms -> tax_syn ->> 'type' !~ '.*(misapp|pro parte|common|vernacular).*'
      AND (synonyms -> tax_syn ->> 'name_id'):: NUMERIC :: BIGINT in ($nameIds)""", [versionId: treeVersion.id]) { row ->
            synonymsFound << [nameId: row.name_id, simpleName: row.simple_name, synonym: row.synonym, type: row.syn_type, synonymId: row.syn_id, existing: row.element_link]
        }
        return synonymsFound
    }

    protected static Map filterSynonyms(TaxonData taxonData) {
        taxonData.synonyms.findAll { Map.Entry entry ->
            !(entry.value.type ==~ '.*(misapp|pro parte|common|vernacular).*')
        }
    }

    protected static checkPolynomialsBelowNameParent(String simpleName, Boolean excluded, NameRank taxonRank,
                                                     String[] parentNameElements) {

        if (!excluded && RankUtils.rankLowerThan(taxonRank, 'Genus')) {
            //if this is a hybrid it takes the first part, if not it's just the name
            String firstNamePart = simpleName.split(' x ').first()
            String elementFound = parentNameElements.find { String nameElement ->
                firstNamePart.contains(nameElement)
            }
            if (!elementFound) {
                throw new BadArgumentsException("Polynomial name *$simpleName* is not under an appropriate parent name." +
                        "It should probably be under\n *${firstNamePart.split(' ').first()}* \n which isn't in this parents name path:\n\n" +
                        "* ${parentNameElements.join('\n* ')}")
            }
        }
    }

    protected static NameRank rankOfElement(Map rankPath, String elementName) {
        String rankName = rankPath.keySet().find { key ->
            (rankPath[key] as Map).name == elementName
        }
        NameRank rank = NameRank.findByName(rankName)
        return rank
    }

    protected TaxonData elementDataFromInstance(Instance instance) {

        //can't put relationship instances on a tree
        if (instance.instanceType.relationship) {
            return null
        }

        Map synonyms = getSynonyms(instance)
        String synonymsHtml = makeSynonymsHtml(synonyms)

        new TaxonData(
                nameId: instance.name.id,
                instanceId: instance.id,
                simpleName: instance.name.simpleName,
                nameElement: instance.name.nameElement,
                displayHtml: "<data>$instance.name.fullNameHtml <citation>$instance.reference.citationHtml</citation></data>",
                synonymsHtml: synonymsHtml,
                sourceShard: configService.nameSpaceName,
                synonyms: synonyms,
                rank: instance.name.nameRank.name,
                nameLink: linkService.getPreferredLinkForObject(instance.name),
                instanceLink: linkService.getPreferredLinkForObject(instance),
                nomInval: instance.name.nameStatus.nomInval,
                nomIlleg: instance.name.nameStatus.nomIlleg
        )
    }

    private static String makeSynonymsHtml(Map data) {
        '<synonyms>' +
                addSynType(data, 'nom') +
                addSynType(data, 'tax') +
                addSynType(data, 'mis') +
                '</synonyms>'
    }

    private static String addSynType(Map data, String type) {
        String synonymsHtml = ''
        data.findAll { Map.Entry entry -> entry.value[type] }.each { Map.Entry syn ->
            Map value = syn.value as Map
            if (type == 'mis') {
                synonymsHtml += "<$type>${value.full_name_html}<type>${value.type}</type> by <citation>${value.cites ?: ''}</citation></$type>"
            } else {
                synonymsHtml += "<$type>${value.full_name_html}<type>${value.type}</type></$type>"
            }
        }
        return synonymsHtml
    }

    private Map getSynonyms(Instance instance) {
        Map resultMap = [:]
        instance.instancesForCitedBy.each { Instance synonym ->
            String nameLink = linkService.getPreferredLinkForObject(synonym.name)
            resultMap.put((synonym.name.simpleName), [type          : synonym.instanceType.name,
                                                      name_id       : synonym.name.id,
                                                      name_link     : nameLink,
                                                      full_name_html: synonym.name.fullNameHtml,
                                                      nom           : synonym.instanceType.nomenclatural,
                                                      tax           : synonym.instanceType.taxonomic,
                                                      mis           : synonym.instanceType.misapplied,
                                                      cites         : synonym.cites ? synonym.cites.reference.citationHtml : ''
            ])
        }
        return resultMap
    }

    private TaxonData findInstanceByUri(String instanceUri) {
        Instance taxon = linkService.getObjectForLink(instanceUri) as Instance
        TaxonData instanceData
        if (taxon) {
            instanceData = elementDataFromInstance(taxon)
        } else {
            Map instanceDataMap = fetchInstanceData(instanceUri)
            if (instanceDataMap.success) {
                instanceData = new TaxonData(instanceDataMap.data as Map)
            } else {
                instanceData = null
            }
        }
        return instanceData
    }

    TaxonData getInstanceDataByUri(String instanceUri) {
        Instance taxon = linkService.getObjectForLink(instanceUri) as Instance
        if (taxon) {
            return elementDataFromInstance(taxon)
        } else {
            return null
        }
    }

    /**
     * Fetch instance data from another service
     * @param instanceUri
     * @return
     */
    private Map fetchInstanceData(String instanceUri) {
        Map result = [success: true]
        String uri = "$instanceUri/api/tree/element-data-from-instance"
        try {
            String failMessage = "Couldn't fetch $uri"
            restCallService.json('get', uri,
                    { Map data ->
                        log.debug "Fetched $uri. Response: $data"
                        result.data = data
                    },
                    { Map data, List errors ->
                        log.error "$failMessage. Errors: $errors"
                        result = [success: false, errors: errors]
                    },
                    { data ->
                        log.error "$failMessage. Not found response: $data"
                        result = [success: false, errors: ["$failMessage. Not found response: $data"]]
                    },
                    { data ->
                        log.error "$failMessage. Response: $data"
                        result = [success: false, errors: ["$failMessage. Response: $data"]]
                    }
            )
        } catch (RestCallException e) {
            log.error e.message
            result = [success: false, errors: "Communication error with mapper."]
        }
        return result
    }

    private Sql getSql() {
        //noinspection GroovyAssignabilityCheck
        return Sql.newInstance(dataSource_nsl)
    }

}

class TaxonData {

    Long nameId
    Long instanceId
    String simpleName
    String nameElement
    String displayHtml
    String synonymsHtml
    String sourceShard
    Map synonyms
    String rank
    Map profile
    String nameLink
    String instanceLink
    Boolean nomInval
    Boolean nomIlleg
    Boolean excluded

    Map asMap() {
        [
                nameId      : nameId,
                instanceId  : instanceId,
                simpleName  : simpleName,
                nameElement : nameElement,
                displayHtml : displayHtml,
                synonymsHtml: synonymsHtml,
                sourceShard : sourceShard,
                synonyms    : synonyms,
                rank        : rank,
                profile     : profile,
                nameLink    : nameLink,
                instanceLink: instanceLink,
                nomInval    : nomInval,
                nomIlleg    : nomIlleg,
                excluded    : excluded
        ]
    }
}

class DisplayElement {

    @SuppressWarnings("GrFinalVariableAccess")
    public final String displayHtml
    @SuppressWarnings("GrFinalVariableAccess")
    public final String elementLink
    @SuppressWarnings("GrFinalVariableAccess")
    public final String nameLink
    @SuppressWarnings("GrFinalVariableAccess")
    public final String instanceLink
    @SuppressWarnings("GrFinalVariableAccess")
    public final Boolean excluded
    @SuppressWarnings("GrFinalVariableAccess")
    public final Integer depth
    @SuppressWarnings("GrFinalVariableAccess")
    public final String synonymsHtml

    DisplayElement(List data) {
        assert data.size() == 7
        this.displayHtml = data[0] as String
        this.elementLink = data[1] as String
        this.nameLink = data[2] as String
        this.instanceLink = data[3] as String
        this.excluded = data[4] as Boolean
        this.depth = data[5] as Integer
        this.synonymsHtml = data[6] as String
    }

    Map asMap() {
        [
                displayHtml : displayHtml,
                elementLink : elementLink,
                nameLink    : nameLink,
                instanceLink: instanceLink,
                excluded    : excluded,
                depth       : depth,
                synonymsHtml: synonymsHtml
        ]
    }
}