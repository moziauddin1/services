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
    def eventService

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

            List<Long> treeElementsRemovedFromSecond = treeElementsNotInSecond - modified.collect { mod -> mod[1].treeElement.id }
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
    and ptve.treeElement.nameId = tve.treeElement.nameId
    and tve.treeElement.id in :elementIds
    order by tve.namePath
''', [version: second, previousVersion: first, elementIds: treeElementsNotInFirst])) as List<List<TreeVersionElement>>
    }

    private static getTvesInVersion(List<Long> elementIds, TreeVersion version) {
        printf "querying ${elementIds.size()} elements"
        if (elementIds.empty) {
            return []
        }
        return TreeVersionElement.executeQuery(
                'select tve from TreeVersionElement tve where treeVersion = :version and treeElement.id in :elementIds order by namePath',
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
        problems.commonSynonyms = sortCommonSynonyms(checkVersionCommonSynonyms(sql, treeVersion))
        return problems
    }

    List<EventRecord> currentSynonymyUpdatedEventRecords(Tree tree) {
        Sql sql = getSql()
        List<EventRecord> records = []
        sql.eachRow('''select id
from event_record
where type = :type
  and dealt_with = false
  and (data ->> 'treeId') :: NUMERIC :: BIGINT = :treeId
''', [treeId: tree.id, type: EventRecordTypes.SYNONYMY_UPDATED]) { row ->
            records.add(EventRecord.get(row.id as Long))
        }
        return records
    }


    private static List<Map> checkVersionSynonyms(Sql sql, TreeVersion treeVersion) {
        List<Map> problems = []
        sql.eachRow('''
SELECT
  e1.name_id                                                            AS accepted_name_id,
  e1.simple_name                                                        AS accepted_name,
  '<div class="tr">' || e1.display_html || e1.synonyms_html || '</div>' as accepted_html,
  tve1.element_link                                                     AS accepted_name_tve,
  tve1.name_path                                                        AS accepted_name_path,
  e2.simple_name                                                        AS synonym_accepted_name,
  '<div class="tr">' || e2.display_html || e2.synonyms_html || '</div>' as synonym_accepted_html,
  tve2.element_link                                                     AS synonym_tve,
  tax_syn                                                               AS synonym_record,
  tax_syn ->> 'type'                                                    AS synonym_type,
  tax_syn ->> 'name_id'                                                 as synonym_name_id,
  tax_syn ->> 'simple_name'                                             as synonym_name
FROM tree_version_element tve1
  JOIN tree_element e1 ON tve1.tree_element_id = e1.id
  ,
  tree_version_element tve2
  JOIN tree_element e2 ON tve2.tree_element_id = e2.id
  ,
      jsonb_array_elements(e2.synonyms -> 'list') AS tax_syn
WHERE tve1.tree_version_id = :treeVersionId
      AND tve2.tree_version_id = :treeVersionId
      AND tve2.tree_element_id <> tve1.tree_element_id
      AND e1.excluded = FALSE
      AND e2.excluded = FALSE
      AND e2.synonyms IS NOT NULL
      AND (tax_syn ->> 'name_id') :: NUMERIC :: BIGINT = e1.name_id
      AND tax_syn ->> 'type' !~ '.*(misapp|pro parte|common|vernacular).*'
order by accepted_name_path;
      ''', [treeVersionId: treeVersion.id]) { row ->
            Map record = [
                    accepted_name_id     : row.accepted_name_id,
                    accepted_name        : row.accepted_name,
                    accepted_html        : row.accepted_html,
                    accepted_name_tve    : row.accepted_name_tve,
                    accepted_name_path   : row.accepted_name_path,
                    synonym_accepted_name: row.synonym_accepted_name,
                    synonym_accepted_html: row.synonym_accepted_html,
                    synonym_tve          : row.synonym_tve,
                    synonym_record       : row.synonym_record,
                    synonym_type         : row.synonym_type,
                    synonym_name_id      : row.synonym_name_id,
                    synonym_name         : row.synonym_name
            ]
            record.description = "Accepted name ${record.accepted_name} is a ${record.synonym_type} of accepted name ${record.synonym_accepted_name}."
            problems.add(record)
        }
        return problems
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    private sortCommonSynonyms(List<Map> commonSynonyms) {
        for (Map result in commonSynonyms) {
            String nameId = result.keySet().first()
            result.elements = result.remove(nameId) as List<Map>
            Name synonym = Name.get(nameId as Long)
            result.commonSynonym = synonym
            if (!synonym) {
                log.error "No name found for ${result.keySet().first()}"
                log.debug result
            }
        }
        return commonSynonyms.sort { it.commonSynonym?.namePath }
    }

    private static List<Map> checkVersionCommonSynonyms(Sql sql, TreeVersion treeVersion) {
        List<Map> problems = []
        sql.eachRow('''
SELECT
  (tax_syn2 ->> 'name_id')       AS common_synonym,
  jsonb_build_object((tax_syn2 ->> 'name_id'),
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

            problems.add(JSON.parse(jsonString) as Map)
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
                eventService.dealWith(event) //synonymy is now the same so mark it as done
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
    def checkCurrentSynonymy(TreeVersion treeVersion, Integer limit = 100) {
        Sql sql = getSql()
        List<Map> results = []
        sql.eachRow('''select tve.element_link, te.instance_link, te.instance_id from tree_element te
          join tree_version_element tve on te.id = tve.tree_element_id
        where tve.tree_version_id = :versionId 
          and te.synonyms_html <> synonyms_as_html(te.instance_id)
          order by tve.name_path;''', [versionId: treeVersion.id], 0, limit) { row ->
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

