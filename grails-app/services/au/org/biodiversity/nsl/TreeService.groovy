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
    def linkService
    def restCallService

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

    TreeElement placeTaxonUri(TreeElement parentElement, String taxonUri, Boolean excluded) {

        TaxonData taxonData = findInstanceByUri(taxonUri)
        if (!taxonData) {
            throw new ObjectNotFoundException("Taxon $taxonUri not found, trying to place it in $parentElement")
        }
        taxonData.excluded = excluded
        validateNewElementPlacement(parentElement, taxonData)
        null
    }

    protected List<String> validateNewElementPlacement(TreeElement parentElement, TaxonData taxonData) {
        List<String> warnings

        TreeVersion treeVersion = parentElement.treeVersion

        warnings = checkNameValidity(taxonData)
        checkInstanceOnTree(taxonData, treeVersion)
        checkNameAlreadyOnTree(taxonData, treeVersion)

        String[] parentNameElements = parentElement.namePath.split('/')
        NameRank taxonRank = rankOfElement(taxonData.rankPathPart as Map, taxonData.nameElement as String)
        NameRank parentRank = rankOfElement(parentElement.rankPath, parentNameElements.last())

        //is rank below parent
        if (!RankUtils.rankHigherThan(parentRank, taxonRank)) {
            throw new BadArgumentsException("Name $taxonData.simpleName of rank $taxonRank.name is not below rank $parentRank.name of $parentElement.simpleName.")
        }

        //polynomials must be placed under parent
        checkPolynomialsBelowNameParent(taxonData.simpleName, taxonData.excluded, taxonRank, parentNameElements)

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
        TreeElement existingElement = TreeElement.findByInstanceLinkAndTreeVersion(taxonData.instanceLink, treeVersion)
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
        TreeElement existingNameElement = TreeElement.findByNameLinkAndTreeVersion(taxonData.nameLink as String, treeVersion)
        if (existingNameElement) {
            throw new BadArgumentsException("${treeVersion.tree.name} version $treeVersion.id already contains name ${taxonData.nameLink}. See ${existingNameElement.elementLink}")
        }
    }

    protected List<Map> checkSynonyms(TaxonData taxonData, TreeVersion treeVersion, Sql sql = getSql()) {

        List<Map> synonymsFound = []
        String names = "('" + filterSynonyms(taxonData).join("','") + "')"

        sql.eachRow('''
SELECT
  el.name_id as name_id,
  el.simple_name as simple_name,
  tax_syn as synonym,
  synonyms -> tax_syn ->> 'type' as syn_type,
  synonyms -> tax_syn ->> 'name_id' as syn_id
FROM tree_element el
  JOIN name n ON el.name_id = n.id,
      jsonb_object_keys(synonyms) AS tax_syn
WHERE tree_version_id = :versionId
      AND synonyms -> tax_syn ->> 'type' !~ '.*(misapp|pro parte).*\'
  and tax_syn in ''' + names, [versionId: treeVersion.id]) { row ->
            synonymsFound << [nameId: row.name_id, simpleName: row.simple_name, synonym: row.synonym, type: row.syn_type, synonymId: row.syn_id]
        }
        return synonymsFound
    }

    protected static Set<String> filterSynonyms(TaxonData taxonData) {
        taxonData.synonyms.findAll { Map.Entry entry ->
            !entry.value.type.contains('pro parte') && !entry.value.type.contains('misapp')
        }.keySet()
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

        new TaxonData(
                nameId: instance.name.id,
                instanceId: instance.id,
                simpleName: instance.name.simpleName,
                nameElement: instance.name.nameElement,
                names: '|' + synonyms.keySet().join('|'),
                sourceShard: configService.nameSpaceName,
                synonyms: synonyms,
                rankPathPart: [(instance.name.nameRank.name): [id: instance.name.id, name: instance.name.nameElement]],
                nameLink: linkService.getPreferredLinkForObject(instance.name),
                instanceLink: linkService.getPreferredLinkForObject(instance),
                nomInval: instance.name.nameStatus.nomInval,
                nomIlleg: instance.name.nameStatus.nomIlleg,
        )
    }

    private static Map getSynonyms(Instance instance) {
        Map resultMap = [:]
        instance.instancesForCitedBy.each { Instance synonym ->
            resultMap.put((synonym.name.simpleName), [type: synonym.instanceType.name, name_id: synonym.name.id])
        }
        return resultMap
    }

    private TaxonData findInstanceByUri(String instanceUri) {
        Instance taxon = linkService.getObjectForLink(taxonUri) as Instance
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

    /**
     * Fetch instance data from another service
     * @param instanceUri
     * @return
     */
    private Map fetchInstanceData(String instanceUri) {
        Map result = [success: true]
        String uri = "$instanceUri/api/tree-api/elment-data-from-instance"
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
}

class TaxonData {

    Long nameId
    Long instanceId
    String simpleName
    String nameElement
    String names
    String sourceShard
    Map synonyms
    Map rankPathPart
    String nameLink
    String instanceLink
    Boolean nomInval
    Boolean nomIlleg
    Boolean excluded

}