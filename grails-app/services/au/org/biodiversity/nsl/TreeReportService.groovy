package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.converters.JSON
import grails.transaction.Transactional
import groovy.sql.Sql

import javax.sql.DataSource

@Transactional
class TreeReportService implements ValidationUtils {

    DataSource dataSource_nsl

    def treeService

    /**
     * create a difference between the first and second version. Normally the first version would be the currently
     * published version and the second version would be the "newer" draft version.
     *
     * This report is with respect to the second version. i.e. it will report things added too or removed from the
     * second version. 
     *
     * @param first
     * @param second
     * @return
     */
    Map diffReport(TreeVersion first, TreeVersion second) {
        mustHave("version 1": first, "version 2": second)
        use(TreeReportUtils) {
            Sql sql = getSql()

            List<Long> treeElementsNotInSecond = first.notIn(second, sql)
            List<Long> treeElementsNotInFirst = second.notIn(first, sql)
            if (treeElementsNotInSecond.empty && treeElementsNotInFirst.empty) {
                return [v1: first, v2: second, added: [], removed: [], modified: [], changed: false, overflow: false]
            }

            if (treeElementsNotInSecond.size() > 1000 || treeElementsNotInFirst.size() > 1000) {
                return [v1: first, v2: second, added: [], removed: [], modified: [], changed: true, overflow: true]
            }

            List<List<TreeVersionElement>> modified = findModified(first, second, treeElementsNotInFirst)

            List<Long> treeElementsAddedToSecond = treeElementsNotInFirst - modified.collect { mod -> mod[0].treeElement.id }
            List<TreeVersionElement> added = getTvesInVersion(treeElementsAddedToSecond, second)

            List<Long> treeElementsRemovedFromSecond = treeElementsNotInSecond - modified.collect { mod -> mod[0].treeElement.previousElement.id }
            List<TreeVersionElement> removed = getTvesInVersion(treeElementsRemovedFromSecond, first)

            [v1: first, v2: second, added: added, removed: removed, modified: modified, changed: true, overflow: false]
        }
    }

    private
    static List<List<TreeVersionElement>> findModified(TreeVersion first, TreeVersion second, List<Long> treeElementsNotInFirst) {
        if (treeElementsNotInFirst.empty) {
            return []
        }
        (TreeVersionElement.executeQuery('''
select tve, ptve 
    from TreeVersionElement tve, TreeVersionElement ptve
where tve.treeVersion = :version
    and ptve.treeVersion =:previousVersion
    and ptve.treeElement = tve.treeElement.previousElement
    and tve.treeElement.id in :elementIds
''', [version: second, previousVersion: first, elementIds: treeElementsNotInFirst])) as List<List<TreeVersionElement>>
    }

    private static getTvesInVersion(List<Long> elementIds, TreeVersion version) {
        printf "querying ${elementIds.size()} elements"
        if (elementIds.empty) {
            return []
        }
        return TreeVersionElement.executeQuery(
                'select tve from TreeVersionElement tve where treeVersion = :version and treeElement.id in :elementIds',
                [version: version, elementIds: elementIds]
        )
    }

    Map validateTreeVersion(TreeVersion treeVersion) {
        if (!treeVersion) {
            throw new BadArgumentsException("Tree version needs to be set.")
        }
        Sql sql = getSql()

        Map problems = [:]

        problems.synonymsOfAcceptedNames = checkVersionSynonyms(sql, treeVersion)
        problems.commonSynonyms = checkVersionCommonSynonyms(sql, treeVersion)
        return problems
    }

    List<EventRecord> treeEventRecords(Tree tree) {
        Sql sql = getSql()
        List<EventRecord> records = []
        sql.eachRow('''select id
from event_record
where type = 'Synonymy Updated\'
  and dealt_with = false
  and (data ->> 'treeId') :: NUMERIC :: BIGINT = :treeId
''', [treeId: tree.id]) { row ->
            records.add(EventRecord.get(row.id))
        }
        return records
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
      AND (e2.synonyms -> tax_syn ->> 'name_id') :: NUMERIC :: BIGINT = e1.name_id
      AND e2.synonyms -> tax_syn ->> 'type' !~ '.*(misapp|pro parte|common|vernacular).*';
      ''', [treeVersionId: treeVersion.id]) { row ->
            problems.add("Taxon concept <a href=\"${row['link2']}\" title=\"tree link\">${row['name2']}</a> " +
                    "considers <a href=\"${row['link1']}\" title=\"tree link\">${row['name1']}</a> to be a ${row['type']}.")
        }
        return problems
    }

    private static List<Map> checkVersionCommonSynonyms(Sql sql, TreeVersion treeVersion) {
        List<Map> problems = []
        sql.eachRow('''
SELECT
  tax_syn2 ->> 'simple_name'       AS common_synonym,
  jsonb_build_object(tax_syn2 ->> 'simple_name',
                     jsonb_agg(jsonb_build_object('html',
                                                  '<div class="tr">' || e1.display_html || e1.synonyms_html || '</div>',
                                                  'name_link', e1.name_link,
                                                  'tree_link', tree.host_name || tve1.element_link,
                                                  'type', tax_syn1 ->> 'type',
                                                  'syn_name_id', tax_syn2 ->> 'name_id\'
                               ))) as names
FROM tree_version_element tve1
  JOIN tree_element e1 ON tve1.tree_element_id = e1.id
  ,
  tree_version_element tve2
  JOIN tree_element e2 ON tve2.tree_element_id = e2.id
  ,
      jsonb_array_elements(e1.synonyms -> 'list') AS tax_syn1,
      jsonb_array_elements(e2.synonyms -> 'list') AS tax_syn2,
  tree_version tv
  join tree on tv.tree_id = tree.id
WHERE tv.id = :treeVersionId
      AND tve1.tree_version_id = tv.id
      AND tve2.tree_version_id = tv.id
      AND tve2.tree_element_id <> tve1.tree_element_id
      AND e1.excluded = FALSE
      AND e2.excluded = FALSE
      AND e1.synonyms ->> 'list' is not null
      AND tax_syn1 ->> 'type' !~ '.*(misapp|pro parte|common|vernacular).*\'
      AND e2.synonyms ->> 'list' is not null
      AND tax_syn2 ->> 'type' !~ '.*(misapp|pro parte|common|vernacular).*\'
      AND (tax_syn1 ->> 'name_id') = (tax_syn2 ->> 'name_id')
group by common_synonym
order by common_synonym;
      ''', [treeVersionId: treeVersion.id]) { row ->
            String jsonString = row['names']

            problems.add(JSON.parse(jsonString))
        }
        return problems
    }

    private Sql getSql() {
        //noinspection GroovyAssignabilityCheck
        return Sql.newInstance(dataSource_nsl)
    }

    Map synonymyUpdatedReport(EventRecord event, Tree tree) {
        assert event.type == EventRecordTypes.SYNONYMY_UPDATED
        TreeVersionElement tve
        if (tree.defaultDraftTreeVersion) {
            tve = treeService.findElementForInstanceId(event.data.instanceId as Long, tree.defaultDraftTreeVersion)
        } else if (tree.currentTreeVersion) {
            tve = treeService.findElementForInstanceId(event.data.instanceId as Long, tree.currentTreeVersion)
        }
        if (tve) {
            tve.treeElement.refresh()
            TaxonData taxonData = treeService.findInstanceByUri(event.data.instanceLink as String)
            if (tve.treeElement.synonymsHtml != taxonData.synonymsHtml) {
                return [
                        taxonData         : taxonData,
                        treeVersionElement: tve,
                        instanceLink      : event.data.instanceLink,
                        instanceId        : event.data.instanceId,
                        eventId           : event.id,
                        updatedBy         : event.updatedBy,
                        updatedAt         : event.updatedAt
                ]
            } else {
                event.dealtWith = true //synonymy is now the same so mark it as done
            }
        }
        return null
    }

    /**
     * Check the synonymy of all instances on the version to see if they match the current synonymy of the instance
     * on the tree. This is a much longer process than the synonymyUpdatedReport.
     * @param treeVersion
     * @return
     */
    def checkCurrentSynonymy(TreeVersion treeVersion) {
        Sql sql = getSql()
        List<Map> results = []
        sql.eachRow('''select tve.element_link, te.instance_link, te.instance_id from tree_element te
          join tree_version_element tve on te.id = tve.tree_element_id
        where tve.tree_version_id = :versionId 
          and te.synonyms_html <> synonyms_as_html(te.instance_id);''', [versionId: treeVersion.id]) { row ->
            TaxonData taxonData = treeService.findInstanceByUri(row.instance_link as String)
            Map d = [
                    taxonData         : taxonData,
                    treeVersionElement: TreeVersionElement.get(row.element_link as String),
                    instanceLink      : row.instance_link,
                    instanceId        : row.instance_id
            ]
            results.add(d)
        }
        return results
    }
}

class TreeReportUtils {
    /**
     * returns a list of element ids that are in version 1 but not version 2
     * @param version1
     * @param version2
     * @param sql
     * @return element ids not in version 2
     */
    static List<Long> notIn(TreeVersion version1, TreeVersion version2, Sql sql) {
        List<Long> treeElementIdsNotInVersion2 = []
        sql.eachRow('''    
  SELECT tree_element_id
   FROM tree_version_element
   WHERE tree_version_id = :v1
  EXCEPT
  SELECT tree_element_id
   FROM tree_version_element
   WHERE tree_version_id = :v2''', [v1: version1.id, v2: version2.id]) { row ->
            treeElementIdsNotInVersion2.add(row.tree_element_id as Long)
        }
        return treeElementIdsNotInVersion2
    }

}