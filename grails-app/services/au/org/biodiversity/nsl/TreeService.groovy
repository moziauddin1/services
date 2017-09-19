package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional
import groovy.sql.Sql
import org.apache.shiro.SecurityUtils

import javax.sql.DataSource
import java.sql.Timestamp

/**
 * The 2.0 Tree service. This service is the central location for all interaction with the tree.
 */
@Transactional
class TreeService implements ValidationUtils {

    DataSource dataSource_nsl
    def configService
    def linkService
    def restCallService

    /**
     * get the named tree. This is case insensitive
     * @param name
     * @return tree or null if not found
     */
    static Tree getTree(String name) {
        mustHave('Tree name': name)
        Tree.findByNameIlike(name)
    }

    static TreeVersionElement getTreeVersionElement(Long versionId, Long elementId) {
        TreeVersionElement.find('from TreeVersionElement where treeVersion.id = :versionId and treeElement.id = :elementId',
                [versionId: versionId, elementId: elementId])
    }

    static TreeVersionElement getParentTreeVersionElement(TreeVersionElement treeVersionElement) {
        if (treeVersionElement.treeElement.parentElement) {
            TreeVersionElement.find('from TreeVersionElement where treeVersion = :version and treeElement = :element',
                    [version: treeVersionElement.treeVersion, element: treeVersionElement.treeElement.parentElement])
        } else {
            null
        }
    }

    static TreeVersionElement findElementBySimpleName(String simpleName, TreeVersion treeVersion) {
        TreeVersionElement.find("from TreeVersionElement tve where tve.treeElement.simpleName = :simpleName and treeVersion= :version", [simpleName: simpleName, version: treeVersion])
    }

    /**
     * get the current TreeElement for a name on the given tree
     * @param name
     * @param tree
     * @return treeElement or null if not on the tree
     */
    static TreeVersionElement findCurrentElementForName(Name name, Tree tree) {
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
    static TreeVersionElement findElementForName(Name name, TreeVersion treeVersion) {
        if (name && treeVersion) {
            return TreeVersionElement.find('from TreeVersionElement tve where tve.treeVersion = :treeVersion and tve.treeElement.nameId = :nameId',
                    [treeVersion: treeVersion, nameId: name.id])
        }
        return null
    }

    static TreeVersionElement findElementForNameLink(String nameLink, TreeVersion treeVersion) {
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
    static TreeVersionElement findCurrentElementForInstance(Instance instance, Tree tree) {
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
    static TreeVersionElement findElementForInstance(Instance instance, TreeVersion treeVersion) {
        if (instance && treeVersion) {
            return TreeVersionElement.find('from TreeVersionElement tve where tve.treeVersion = :treeVersion and tve.treeElement.instanceId = :instanceId',
                    [treeVersion: treeVersion, instanceId: instance.id])
        }
        return null
    }

    static TreeVersionElement findElementForInstanceLink(String instanceLink, TreeVersion treeVersion) {
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
    static List<TreeVersionElement> getElementPath(TreeVersionElement treeVersionElement) {
        mustHave(treeVersionElement: treeVersionElement)
        treeVersionElement.treeElement.treePath.split('/').collect { String stringElementId ->
            if (stringElementId) {
                TreeVersionElement.find('from TreeVersionElement tve where tve.treeElement.id = :elementId and treeVersion = :version',
                        [elementId: stringElementId as Long, version: treeVersionElement.treeVersion])
            } else {
                null
            }
        }.findAll { it }
    }

    List<TreeVersionElement> getChildElementsToDepth(TreeVersionElement parent, int depth) {
        mustHave(parent: parent, 'parent.treeElement': parent.treeElement, 'parent.treeVersion': parent.treeVersion)
        String pattern = "^${parent.treeElement.treePath}(/[^/]*){1,$depth}\$"
        getElementsByPath(parent.treeVersion, pattern)
    }

    List<TreeVersionElement> getAllChildElements(TreeVersionElement parent) {
        mustHave(parent: parent, 'parent.treeElement': parent.treeElement, 'parent.treeVersion': parent.treeVersion)
        String pattern = "^${parent.treeElement.treePath}/.*"
        getElementsByPath(parent.treeVersion, pattern)
    }

    int countAllChildElements(TreeVersionElement parent) {
        mustHave(parent: parent, 'parent.treeElement': parent.treeElement, 'parent.treeVersion': parent.treeVersion)
        String pattern = "^${parent.treeElement.treePath}/.*"
        countElementsByPath(parent.treeVersion, pattern)
    }

    int countElementsAtDepth(TreeVersion treeVersion, String prefix, int depth) {
        mustHave(treeVersion: treeVersion, prefix: prefix)
        String pattern = "$prefix(/[^/]*){0,$depth}\$"
        countElementsByPath(treeVersion, pattern)
    }

    /**
     * Get just the display string and links for all the child tree elements.
     * @param treeElement
     * @return List of DisplayElements
     */
    List<DisplayElement> childDisplayElements(TreeVersionElement treeVersionElement) {
        mustHave(TreeVersionElement: treeVersionElement)
        childDisplayElements(treeVersionElement.treeElement, treeVersionElement.treeVersion)
    }

    List<DisplayElement> childDisplayElements(TreeElement treeElement, TreeVersion treeVersion) {
        mustHave(treeElement: treeElement)
        String pattern = "^${treeElement.treePath}.*"
        fetchDisplayElements(pattern, treeVersion)
    }

    /**
     * Get just the display string and link to the child tree elements to depth
     * @param treeElement
     * @return List of DisplayElements
     */
    @SuppressWarnings("GroovyUnusedDeclaration")
    List<DisplayElement> childDisplayElementsToDepth(TreeElement treeElement, TreeVersion treeVersion, int depth) {
        mustHave(treeElement: treeElement)
        String pattern = "^${treeElement.treePath}(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersion)
    }

    /**
     * Get just the display string and link to the child tree elements to depth
     * @param treeElement
     * @return List of DisplayElement
     */
    @SuppressWarnings("GroovyUnusedDeclaration")
    List<DisplayElement> displayElementsToDepth(TreeVersion treeVersion, int depth) {
        mustHave(treeElement: treeVersion)
        String pattern = "^[^/]*(/[^/]*){0,$depth}\$"
        fetchDisplayElements(pattern, treeVersion)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    List<DisplayElement> displayElementsToLimit(TreeElement treeElement, TreeVersion treeVersion, Integer limit) {
        displayElementsToLimit(treeVersion, "^${treeElement.treePath}", limit)
    }

    List<DisplayElement> displayElementsToLimit(TreeVersion treeVersion, Integer limit) {
        displayElementsToLimit(treeVersion, "^[^/]*", limit)
    }

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
    private List<DisplayElement> fetchDisplayElements(String pattern, TreeVersion treeVersion) {
        mustHave(treeVersion: treeVersion, pattern: pattern)
        log.debug("getting $pattern")

        TreeElement.executeQuery('''
select tve.treeElement.displayHtml, tve.elementLink, tve.treeElement.nameLink, tve.treeElement.instanceLink, 
 tve.treeElement.excluded, tve.treeElement.depth, tve.treeElement.synonymsHtml 
    from TreeVersionElement tve 
    where tve.treeVersion = :version
    and regex(tve.treeElement.treePath, :pattern) = true 
    order by tve.treeElement.namePath
''', [version: treeVersion, pattern: pattern]).collect { data ->
            new DisplayElement(data as List)
        } as List<DisplayElement>
    }

    List<TreeVersionElement> getElementsByPath(TreeVersion parent, String pattern) {
        mustHave(parent: parent, pattern: pattern)
        log.debug("getting $pattern")

        TreeVersionElement.executeQuery('''
select tve 
    from TreeVersionElement tve 
    where tve.treeVersion = :version
    and regex(tve.treeElement.treePath, :pattern) = true 
    order by tve.treeElement.namePath
''', [version: parent, pattern: pattern])
    }

    int countElementsByPath(TreeVersion parent, String pattern) {
        mustHave(parent: parent, pattern: pattern)
        log.debug("counting $pattern")

        int count = TreeElement.executeQuery('''
select count(tve) 
    from TreeVersionElement tve 
    where tve.treeVersion = :version
    and regex(tve.treeElement.treePath, :pattern) = true  
''', [version: parent, pattern: pattern]).first() as int
        return count
    }

    /** Editing *****************************/

    Tree createNewTree(String treeName, String groupName, Long referenceId) {
        Tree tree = Tree.findByName(treeName)
        if (tree) {
            throw new ObjectExistsException("A Tree named $treeName already exists.")
        }
        tree = new Tree(name: treeName, groupName: groupName, referenceId: referenceId)
        tree.save()
        linkService.addTargetLink(tree)
        return tree
    }

    Tree editTree(Tree tree, String name, String groupName, Long referenceId) {
        if (!(name && groupName)) {
            throw new BadArgumentsException("Tree name ('$name') and Group name ('$groupName') must not be null.")
        }
        if (name != tree.name && Tree.findByName(name)) {
            throw new ObjectExistsException("A Tree named $name already exists.")
        }
        tree.name = name
        tree.groupName = groupName
        tree.referenceId = referenceId
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
        deleteOrphanedTreeElements(sql)
        return Tree.get(treeId)
    }

    private Integer deleteOrphanedTreeElements(Sql sql = getSql()) {
        log.debug "delete orphaned elements"
        Integer count = sql.firstRow('SELECT count(*) FROM tree_element WHERE id NOT IN (SELECT DISTINCT(tree_element_id) FROM tree_version_element)')[0] as Integer
        if (count) {
            log.debug "deleting $count orphaned elements."

            sql.execute('''
SELECT id INTO TEMP orphans FROM tree_element WHERE id NOT IN (SELECT DISTINCT(tree_element_id) FROM tree_version_element); 
UPDATE tree_element SET previous_element_id = NULL WHERE previous_element_id IN (SELECT id FROM orphans);
UPDATE tree_element SET parent_element_id = NULL WHERE parent_element_id IN  (SELECT id FROM orphans);
DELETE FROM tree_element WHERE id IN (SELECT id FROM orphans);
''')
        }
        return count
    }

    TreeVersion publishTreeVersion(TreeVersion treeVersion, String publishedBy, String logEntry) {
        log.debug "Publish tree version $treeVersion by $publishedBy, with log entry $logEntry"
        treeVersion.published = true
        treeVersion.logEntry = logEntry
        treeVersion.publishedAt = new Timestamp(System.currentTimeMillis())
        treeVersion.publishedBy = publishedBy
        treeVersion.save()
        treeVersion.tree.currentTreeVersion = treeVersion
        treeVersion.tree.save()
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

        sql.execute('''INSERT INTO tree_version_element (tree_version_id, tree_element_id, element_link) 
  (SELECT :toVersionId, tvte.tree_element_id, 
          substring(tvte.element_link, '^(.*)/[0-9]*/[0-9]+') || '/' || :toVersionId || '/' || tvte.tree_element_id
   FROM tree_version_element tvte WHERE tree_version_id = :fromVersionId)''',
                [fromVersionId: fromVersion.id, toVersionId: toVersion.id])

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
        SecurityUtils.subject.checkRole(groupName)
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

    Map validateTreeVersion(TreeVersion treeVersion) {
        Sql sql = getSql()
        if (!treeVersion) {
            throw new BadArgumentsException("Tree version needs to be set.")
        }

        Map problems = [:]

        problems.synonyms = checkVersionSynonyms(sql, treeVersion)
        return problems
    }

    private static List<String> checkVersionSynonyms(Sql sql, TreeVersion treeVersion) {
        List<String> problems = []
        sql.eachRow('''
SELECT
  e1.simple_name                    AS name1,
  tve1.element_link AS link1,
  e2.simple_name                    AS name2,
  tve2.element_link AS link2,
  tax_syn                           AS name2_synonym,
  e2.synonyms -> tax_syn ->> 'type' AS type
FROM tree_version_element tve1
  JOIN tree_element e1 ON tve1.tree_element_id = e1.id
  ,
  tree_version_element tve2
  JOIN tree_element e2 ON tve2.tree_element_id = e2.id
  ,
      jsonb_object_keys(e2.synonyms) AS tax_syn
WHERE tve1.tree_version_id = :treeVersionId
      AND tve2.tree_version_id = :treeVersionId
      AND tve2.tree_element_id <> tve1.tree_element_id
      AND e1.excluded = FALSE
      AND e2.excluded = FALSE
      AND e2.synonyms IS NOT NULL
      AND (e2.synonyms -> tax_syn ->> 'name_id') :: BIGINT = e1.name_id
      AND e2.synonyms -> tax_syn ->> 'type' !~ '.*(misapp|pro parte|common).*';
      ''', [treeVersionId: treeVersion.id]) { row ->
            problems.add("In taxon (${row['link2']}) ${row['name2']} considers ${row['name1']} (${row['link1']}) to be a ${row['type']}.")
        }
        return problems
    }

    Map placeTaxonUri(TreeVersionElement parentElement, String taxonUri, Boolean excluded, String userName) {

        TaxonData taxonData = findInstanceByUri(taxonUri)
        if (!taxonData) {
            throw new ObjectNotFoundException("Taxon $taxonUri not found, trying to place it in $parentElement")
        }
        notPublished(parentElement)
        taxonData.excluded = excluded
        List<String> warnings = validateNewElementPlacement(parentElement, taxonData)
        //note above will throw exceptions for invalid placements, not warnings
        TreeVersionElement childElement = makeVersionElementFromTaxonData(taxonData, parentElement, userName)
        return [childElement: childElement, warnings: warnings]
    }

    /**
     * Move a tree element placement from one parent to another. This requires the tree elements below the one being moved
     * to be copied and the name and tree paths updated. The elements being copied are removed from this version
     * of the tree.
     *
     * If this element is high up the tree this may result in thousands of new elements being created which may take
     * some time.
     *
     * @param child - the taxon you wish to move.
     * @param parent - the target parent that you want to move the child under.
     * @param userName - who is making the move.
     * @return The new child TreeVersionElement.
     */
    TreeVersionElement moveTaxon(TreeVersionElement child, TreeVersionElement parent, String userName) {
        notPublished(parent)

        TreeVersionElement childCopy = saveTreeVersionElement(copyTreeElement(child, parent.treeElement, userName), parent.treeVersion)

        List<TreeVersionElement> kids = getChildElementsToDepth(child, 1)

        copyChildElements(kids, childCopy, userName)

        removeTreeVersionElement(child)
        return childCopy
    }

    /**
     * Remove this tree version element and all it's children.
     * @param treeVersionElement
     * @return count of treeVersionElements removed.
     */
    int removeTreeVersionElement(TreeVersionElement treeVersionElement) {
        notPublished(treeVersionElement)
        TreeVersion treeVersion = treeVersionElement.treeVersion
        int count = countAllChildElements(treeVersionElement) + 1
        log.debug "Deleting ${count} tree version elements."

        List<TreeVersionElement> elements = getAllChildElements(treeVersionElement)
        elements.add(treeVersionElement)
        Map result = linkService.bulkRemoveTargets(elements)
        log.info result
        if (!result.success) {
            throw new ServiceException("Error deleting tree links from the mapper: ${result.errors}")
        }

        for (TreeVersionElement kid in elements) {
            log.debug "Deleting $kid"
            kid.treeElement.removeFromTreeVersionElements(kid)
            kid.treeVersion.removeFromTreeVersionElements(kid)
            kid.delete(flush: true)
        }
        elements.clear()
        //if this is removing new elements in a draft we may orphan some tree elements so it pays to clean up
        //this may be moved to a background garbage collection task if it is too slow.
        deleteOrphanedTreeElements()
        treeVersion.refresh()
        return count
    }

    TreeVersionElement editProfile(TreeVersionElement treeVersionElement, Map profile, String userName) {
        mustHave(treeVersionElement: treeVersionElement, profile: profile, userName: userName)
        notPublished(treeVersionElement)
        // if in any other versions we need to clone the treeElement into a new one.
        treeVersionElement.treeElement.refresh() //fetch the element data including treeVersionElements

        log.debug treeVersionElement.treeElement.profile.toString()
        log.debug profile.toString()
        if (treeVersionElement.treeElement.profile == profile) {
            return treeVersionElement // data is equal, do nothing
        }

        if (treeVersionElement.treeElement.treeVersionElements.size() > 1) {
            treeVersionElement.treeElement = copyTreeElement(treeVersionElement, treeVersionElement.treeElement.parentElement, userName)
        } else {
            treeVersionElement.treeElement.updatedBy = userName
            treeVersionElement.treeElement.updatedAt = new Timestamp(System.currentTimeMillis())
        }
        treeVersionElement.treeElement.profile = profile
        treeVersionElement.save()
        return treeVersionElement
    }

    TreeVersionElement editExcluded(TreeVersionElement treeVersionElement, Boolean excluded, String userName) {
        mustHave(treeVersionElement: treeVersionElement, userName: userName)
        notPublished(treeVersionElement)
        // if in any other versions we need to clone the treeElement into a new one.
        treeVersionElement.treeElement.refresh() //fetch the element data including treeVersionElements

        if (treeVersionElement.treeElement.excluded == excluded) {
            return treeVersionElement // data equal, do nothing
        }

        if (treeVersionElement.treeElement.treeVersionElements.size() > 1) {
            treeVersionElement.treeElement = copyTreeElement(treeVersionElement, treeVersionElement.treeElement.parentElement, userName)
        } else {
            treeVersionElement.treeElement.updatedBy = userName
            treeVersionElement.treeElement.updatedAt = new Timestamp(System.currentTimeMillis())
        }

        treeVersionElement.treeElement.excluded = excluded
        treeVersionElement.save()
        return treeVersionElement
    }

    private copyChildElements(List<TreeVersionElement> kids, TreeVersionElement parent, String userName) {
        log.debug "copying kids $kids"
        for (TreeVersionElement kid in kids) {
            TreeVersionElement kidCopy = saveTreeVersionElement(copyTreeElement(kid, parent.treeElement, userName), parent.treeVersion)
            copyChildElements(getChildElementsToDepth(kid, 1), kidCopy, userName)
        }
    }

    private TreeElement copyTreeElement(TreeVersionElement treeVersionElement, TreeElement parent, String userName) {
        Long treeElementId = generateNewElementId()
        TreeElement source = treeVersionElement.treeElement
        TreeElement treeElement = new TreeElement(
                treeElementId: treeElementId,
                treeVersion: treeVersionElement.treeVersion,
                previousElement: source,
                parentElement: parent,
                instanceId: source.instanceId,
                nameId: source.nameId,
                excluded: source.excluded,
                displayHtml: source.displayHtml,
                synonymsHtml: source.synonymsHtml,
                simpleName: source.simpleName,
                nameElement: source.nameElement,
                treePath: "${parent.treePath}/$treeElementId",
                namePath: "${parent.namePath}/$source.nameElement",
                rank: source.rank,
                depth: parent.depth + 1,
                sourceShard: source.sourceShard,
                synonyms: source.synonyms,
                rankPath: parent.rankPath << [(source.rank): [id: source.name.id, name: source.nameElement]],
                profile: source.profile,
                sourceElementLink: source.sourceElementLink,
                nameLink: source.nameLink,
                instanceLink: source.instanceLink,
                updatedBy: userName,
                updatedAt: new Timestamp(System.currentTimeMillis())
        )
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

    protected TreeVersionElement makeVersionElementFromTaxonData(TaxonData taxonData, TreeVersionElement parentElement, String userName) {
        Long treeElementId = generateNewElementId()
        TreeElement treeElement = new TreeElement(
                treeElementId: treeElementId,
                treeVersion: parentElement.treeVersion,
                previousElement: null,
                parentElement: parentElement.treeElement,
                instanceId: taxonData.instanceId,
                nameId: taxonData.nameId,
                excluded: taxonData.excluded,
                displayHtml: taxonData.displayHtml,
                synonymsHtml: taxonData.synonymsHtml,
                simpleName: taxonData.simpleName,
                nameElement: taxonData.nameElement,
                treePath: parentElement.treeElement.treePath + "/$treeElementId",
                namePath: parentElement.treeElement.namePath + "/$taxonData.nameElement",
                rank: taxonData.rank,
                depth: parentElement.treeElement.depth + 1,
                sourceShard: taxonData.sourceShard,
                synonyms: taxonData.synonyms,
                rankPath: parentElement.treeElement.rankPath << taxonData.rankPathPart,
                profile: taxonData.profile,
                sourceElementLink: null,
                nameLink: taxonData.nameLink,
                instanceLink: taxonData.instanceLink,
                updatedBy: userName,
                updatedAt: new Timestamp(System.currentTimeMillis())
        )
        treeElement.save()
        return saveTreeVersionElement(treeElement, parentElement.treeVersion)
    }

    private TreeVersionElement saveTreeVersionElement(TreeElement element, TreeVersion version) {
        TreeVersionElement treeVersionElement = new TreeVersionElement(treeElement: element, treeVersion: version)
        treeVersionElement.elementLink = linkService.addTargetLink(treeVersionElement)
        treeVersionElement.save()
        return treeVersionElement
    }

    private Long generateNewElementId(Sql sql = getSql()) {
        sql.firstRow("SELECT nextval('nsl_global_seq')")[0] as Long
    }

    protected List<String> validateNewElementPlacement(TreeVersionElement parentElement, TaxonData taxonData) {
        List<String> warnings

        TreeVersion treeVersion = parentElement.treeVersion

        warnings = checkNameValidity(taxonData)
        checkInstanceOnTree(taxonData, treeVersion)
        checkNameAlreadyOnTree(taxonData, treeVersion)

        NameRank taxonRank = rankOfElement(taxonData.rankPathPart, taxonData.nameElement)
        NameRank parentRank = rankOfElement(parentElement.treeElement.rankPath, parentElement.treeElement.nameElement)

        //is rank below parent
        if (!RankUtils.rankHigherThan(parentRank, taxonRank)) {
            throw new BadArgumentsException("Name $taxonData.simpleName of rank $taxonRank.name is not below rank $parentRank.name of $parentElement.treeElement.simpleName.")
        }

        //polynomials must be placed under parent
        checkPolynomialsBelowNameParent(taxonData.simpleName, taxonData.excluded, taxonRank, parentElement.treeElement.namePath.split('/'))

        checkForExistingSynonyms(taxonData, treeVersion)

        return warnings
    }

    private void checkForExistingSynonyms(TaxonData taxonData, TreeVersion treeVersion) {
        //a name can't be already in the tree as a synonym
        List<Map> existingSynonyms = checkSynonyms(taxonData, treeVersion)
        if (!existingSynonyms.empty) {
            String message = existingSynonyms.collect {
                "${it.simpleName} ($it.nameId) is a $it.type of $it.synonym ($it.synonymId)"
            }.join(',\n')
            throw new BadArgumentsException("${treeVersion.tree.name} version $treeVersion.id already contains name ${taxonData.simpleName}:\n" +
                    message +
                    " according to the concepts involved.")
        }
    }

    private static void checkInstanceOnTree(TaxonData taxonData, TreeVersion treeVersion) {
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

    private static void checkNameAlreadyOnTree(TaxonData taxonData, TreeVersion treeVersion) {
        //a name can't be in the tree already
        TreeVersionElement existingNameElement = findElementForNameLink(taxonData.nameLink, treeVersion)
        if (existingNameElement) {
            throw new BadArgumentsException("${treeVersion.tree.name} version $treeVersion.id already contains name ${taxonData.nameLink}. See ${existingNameElement.elementLink}")
        }
    }

    protected List<Map> checkSynonyms(TaxonData taxonData, TreeVersion treeVersion, Sql sql = getSql()) {

        List<Map> synonymsFound = []
        String nameIds = filterSynonyms(taxonData).collect { it.value.name_id }.join(',')

        if (!nameIds) {
            return []
        }

        sql.eachRow("""
SELECT
  el.name_id as name_id,
  el.simple_name as simple_name,
  tax_syn as synonym,
  synonyms -> tax_syn ->> 'type' as syn_type,
  synonyms -> tax_syn ->> 'name_id' as syn_id
FROM tree_element el join tree_version_element tve on el.id = tve.tree_element_id 
  JOIN name n ON el.name_id = n.id,
      jsonb_object_keys(synonyms) AS tax_syn
WHERE tve.tree_version_id = :versionId
      AND synonyms -> tax_syn ->> 'type' !~ '.*(misapp|pro parte|common).*'
      and (synonyms -> tax_syn ->> 'name_id') :: BIGINT in ($nameIds)""", [versionId: treeVersion.id]) { row ->
            synonymsFound << [nameId: row.name_id, simpleName: row.simple_name, synonym: row.synonym, type: row.syn_type, synonymId: row.syn_id]
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
                throw new BadArgumentsException("Polynomial name $simpleName is not under appropriate parent name. See parent name path $parentNameElements")
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
                displayHtml: "<data> $instance.name.fullNameHtml <citation>$instance.reference.citationHtml</citation></data>",
                synonymsHtml: synonymsHtml,
                sourceShard: configService.nameSpaceName,
                synonyms: synonyms,
                rank: instance.name.nameRank.name,
                rankPathPart: [(instance.name.nameRank.name): [id: instance.name.id, name: instance.name.nameElement]],
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

    private static Map getSynonyms(Instance instance) {
        Map resultMap = [:]
        instance.instancesForCitedBy.each { Instance synonym ->
            resultMap.put((synonym.name.simpleName), [type          : synonym.instanceType.name,
                                                      name_id       : synonym.name.id,
                                                      full_name_html: synonym.name.fullNameHtml,
                                                      nom           : synonym.instanceType.nomenclatural,
                                                      tax           : synonym.instanceType.taxonomic,
                                                      mis           : synonym.instanceType.misapplied,
                                                      cites         : synonym.instanceType.misapplied ? synonym.cites?.reference?.citationHtml : ''
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
    Map rankPathPart
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
                rankPathPart: rankPathPart,
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