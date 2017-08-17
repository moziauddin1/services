package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional
import groovy.sql.Sql
import org.apache.commons.lang.NotImplementedException
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
    /**
     * get [displayString , link, name link, instance link], ...]
     * @param pattern
     * @param treeVersion
     * @return
     */
    @SuppressWarnings("GrMethodMayBeStatic")
    private List<List> fetchDisplayElements(String pattern, TreeVersion treeVersion) {
        log.debug("getting $pattern")
        TreeElement.executeQuery('''
select displayString, elementLink, nameLink, instanceLink, excluded 
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

    /** Editing *****************************/


    Tree createNewTree(String treeName, String groupName, Long referenceId) {
        Tree tree = Tree.findByName(treeName)
        if (tree) {
            throw new ObjectExistsException("A Tree named $treeName already exists.")
        }
        tree = new Tree(name: treeName, groupName: groupName, referenceId: referenceId)
        tree.save()
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
     * Because of the nature of a delete the session is flushed and cleared, meaning any previously held domain objects
     * need to be refreshed before use.
     *
     * @param tree
     */
    void deleteTree(Tree tree) {
        log.debug "Delete tree $tree"
        /*
        Note a simple tree.delete() will work here, but hibernate looks at the ownership and will delete objects one at
        a time, so if you have 2 versions in the tree and they have 35k elements it will issue 70k+ delete element statements.

        Since that will take a long time, we'll do it using sql directly.
         */
        Sql sql = getSql()
        for (TreeVersion v in tree.treeVersions) {
            tree = deleteTreeVersion(v, sql)
        }
        tree.delete(flush: true)
    }

    /**
     * This deletes a tree version and all it's elements. Because of the nature of a delete the session is flushed and
     * cleared, so you need to refresh any objects held prior to calling this method.
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
        log.debug "deleting version $treeVersion"
        Long treeVersionId = treeVersion.id
        Long treeId = treeVersion.tree.id
        TreeVersion.withSession { s ->
            s.flush()
            s.clear()
        }

        sql.execute('''
UPDATE tree_element SET parent_element_id = NULL, parent_version_id = NULL, previous_element_id = NULL, previous_version_id = NULL
WHERE tree_version_id = :treeVersionId;

UPDATE tree_element SET previous_element_id = NULL, previous_version_id = NULL
WHERE previous_version_id = :treeVersionId;

DELETE FROM tree_element
WHERE tree_version_id = :treeVersionId;

UPDATE tree_version SET previous_version_id = NULL WHERE previous_version_id = :treeVersionId;

UPDATE tree SET default_draft_tree_version_id = NULL WHERE default_draft_tree_version_id = :treeVersionId;

UPDATE tree SET current_tree_version_id = NULL WHERE current_tree_version_id = :treeVersionId;

DELETE FROM tree_version WHERE id = :treeVersionId;
''', [treeVersionId: treeVersionId])
        return Tree.get(treeId)
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
        log.debug "copying ${fromVersion.treeElements.size()} elements from $fromVersion to $toVersion"

        Sql sql = getSql()

        sql.execute('''INSERT INTO tree_element
(tree_version_id, tree_element_id, lock_version, excluded, display_string, element_link, instance_id, instance_link,
 name_id, name_link, parent_version_id, parent_element_id, previous_version_id, previous_element_id, profile, rank_path,
 simple_name, tree_path, name_path, updated_at, updated_by)
  (SELECT
     :toVersionId,
     tree_element_id,
     lock_version,
     excluded,
     display_string,
     'http://' || :hostname || '/tree/' || :toVersionId || '/' || tree_element_id,
     instance_id,
     instance_link,
     name_id,
     name_link,
     :toVersionId,
     parent_element_id,
     tree_version_id, -- previous version
     tree_element_id,
     profile,
     rank_path,
     simple_name,
     tree_path,
     name_path,
     updated_at,
     updated_by
   FROM tree_element fromElement WHERE fromElement.tree_version_id = :fromVersionId
  )
''', [fromVersionId: fromVersion.id, toVersionId: toVersion.id, hostname: 'id.biodiversity.org.au'])

        toVersion.refresh()
        log.debug "inserted ${toVersion.treeElements.size()} elements"
        assert fromVersion.treeElements.size() == toVersion.treeElements.size()
    }


    String authorizeTreeOperation(Tree tree) {
        String groupName = tree.groupName
        SecurityUtils.subject.checkRole(groupName)
        return SecurityUtils.subject.principal as String
    }

    private Sql getSql() {
        //noinspection GroovyAssignabilityCheck
        return Sql.newInstance(dataSource_nsl)
    }

    TreeVersion editTreeVersion(TreeVersion treeVersion, String draftName) {
        if (!draftName) {
            throw new BadArgumentsException('Draft name must be set when editing tree version.')
        }
        treeVersion.draftName = draftName
        treeVersion.save()
        return treeVersion
    }

    TreeVersion validateTreeVersion(TreeVersion treeVersion) {
        throw new NotImplementedException('Validate Tree Version is not implemented')
    }
}
