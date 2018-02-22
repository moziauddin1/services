package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.api.ValidationUtils
import grails.transaction.Transactional
import groovy.sql.Sql

import javax.sql.DataSource

@Transactional
class TreeReportService implements ValidationUtils {

    DataSource dataSource_nsl

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
      AND (e2.synonyms -> tax_syn ->> 'name_id') :: NUMERIC :: BIGINT = e1.name_id
      AND e2.synonyms -> tax_syn ->> 'type' !~ '.*(misapp|pro parte|common).*';
      ''', [treeVersionId: treeVersion.id]) { row ->
            problems.add("Taxon concept <a href=\"${row['link2']}\" title=\"tree link\">${row['name2']}</a> " +
                    "considers <a href=\"${row['link1']}\" title=\"tree link\">${row['name1']}</a> to be a ${row['type']}.")
        }
        return problems
    }

    private Sql getSql() {
        //noinspection GroovyAssignabilityCheck
        return Sql.newInstance(dataSource_nsl)
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