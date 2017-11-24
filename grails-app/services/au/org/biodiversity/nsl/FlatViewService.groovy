/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

/**
 * This service provides for a flat view of the NSL data for export of various outputs. This replaces the NslSimpleName
 * table.
 *
 * See JIRA NSL-1369
 *
 */
package au.org.biodiversity.nsl

import grails.transaction.Transactional
import groovy.sql.GroovyResultSet
import groovy.sql.Sql
import sun.reflect.generics.reflectiveObjects.NotImplementedException


@Transactional
class FlatViewService implements WithSql{

    def grailsApplication
    def configService

    private static String TAXON_VIEW = 'taxon_view'
    private static String NAME_VIEW = 'name_view'


    Closure nameView = { namespace ->
        String classificationTreeName = configService.classificationTreeName
        return """
CREATE MATERIALIZED VIEW ${NAME_VIEW} AS
WITH RECURSIVE

    kingdom AS (
      SELECT
        tree_node.id,
        name.id        AS name_id,
        name.simple_name,
        name_rank.name AS rank
      FROM tree_arrangement t
        -- get current top node
        JOIN tree_link AS top_link ON t.node_id = top_link.supernode_id
        -- get first layer of names
        JOIN tree_link AS top_names ON top_link.subnode_id = top_names.supernode_id
        -- get the name on that top  link
        JOIN tree_node ON top_names.subnode_id = tree_node.id
        JOIN name
        JOIN name_rank ON name.name_rank_id = name_rank.id
                          AND name_rank.name = 'Regnum'
          ON tree_node.name_id = name.id
      WHERE t.label = '${classificationTreeName}'
  ),
    tree AS (
    SELECT
      cast(NULL AS BIGINT)                      AS supernode_id,
      id                                        AS subnode_id,
      name_id,
      hstore(kingdom.rank, kingdom.simple_name) AS htree
    FROM kingdom
    UNION ALL
    SELECT
      l.supernode_id,
      l.subnode_id,
      name.id                                           AS name_id,
      htree || hstore(name_rank.name, name.simple_name) AS htree
    FROM tree
      JOIN tree_link l ON tree.subnode_id = l.supernode_id
      JOIN tree_node n ON l.subnode_id = n.id
      LEFT OUTER JOIN name
      JOIN name_rank ON name.name_rank_id = name_rank.id
        ON n.name_id = name.id
  ),
  /*all scientificNameIDs */
    names AS ( SELECT DISTINCT ON ("scientificNameID")

                 'ICNAFP' :: TEXT                                           AS "nomenclaturalCode",
                 'APNI' :: TEXT                                             AS "datasetName",
                 nt.name                                                    AS "nameType",
                 CASE WHEN apc_inst.id IS NULL
                   THEN
                     'unplaced' :: TEXT
                 ELSE
                   CASE WHEN apc_inst.id = apcn.instance_id
                     THEN
                       CASE apcn.type_uri_id_part
                       WHEN 'ApcConcept'
                         THEN 'accepted'
                       WHEN 'ApcExcluded'
                         THEN 'excluded'
                       ELSE apcn.type_uri_id_part
                       END
                   ELSE
                     'included' :: TEXT
                   END
                 END                                                        AS "taxonomicStatus",

                 'http://id.biodiversity.org.au/name/${namespace}/' || n.id :: TEXT AS "scientificNameID",
                 n.full_name                                                AS "scientificName",
                 CASE WHEN ns.name NOT IN ('legitimate', '[default]')
                   THEN ns.name
                 ELSE NULL END                                              AS "nomenclaturalStatus",
                 n.simple_name                                              AS "canonicalName",
                 CASE WHEN nt.autonym
                   THEN
                     NULL
                 ELSE
                   regexp_replace(substring(n.full_name_html FROM '<authors>(.*)</authors>'), '<[^>]*>', '', 'g')
                 END                                                        AS "scientificNameAuthorship",
                 'http://creativecommons.org/licenses/by/3.0/' :: TEXT      AS "ccLicense",
                 'http://id.biodiversity.org.au/name/${namespace}/' || n.id :: TEXT AS "ccAttributionIRI",
                 CASE WHEN nt.cultivar = TRUE
                   THEN n.name_element
                 ELSE NULL END                                              AS "cultivarEpithet",
                 n.simple_name_html                                         AS "canonicalNameHTML",
                 n.full_name_html                                           AS "scientificNameHTML",

                 nt.autonym                                                 AS "autonym",
                 nt.hybrid                                                  AS "hybrid",
                 nt.cultivar                                                AS "cultivar",
                 nt.formula                                                 AS "formula",
                 nt.scientific                                              AS "scientific",
                 ns.nom_inval                                               AS "nomInval",
                 ns.nom_illeg                                               AS "nomIlleg",
                 coalesce(pro_ref.citation, sec_ref.citation)               AS "namePublishedIn",
                 coalesce(pro_ref.year, sec_ref.year)                       AS "namePublishedInYear",
                 pit.name                                                   AS "nameInstanceType",
                 bnm.full_name                                              AS "originalNameUsage",
                 CASE WHEN bin.id IS NOT NULL
                   THEN 'http://id.biodiversity.org.au/instance/${namespace}/' || bin.cites_id :: TEXT
                 ELSE
                   CASE WHEN pro.id IS NOT NULL
                     THEN 'http://id.biodiversity.org.au/instance/${namespace}/' || pro.id :: TEXT
                   ELSE NULL END
                 END                                                        AS "originalNameUsageID",
                 /*apc_comment.value */
                 CASE WHEN nt.autonym = TRUE
                   THEN p.full_name
                 ELSE
                   (SELECT string_agg(regexp_replace(VALUE, E'[\\n\\r\\u2028]+', ' ', 'g'), ' ')
                    FROM instance_note nt
                      JOIN instance_note_key key1
                        ON key1.id = nt.instance_note_key_id
                           AND key1.name = 'Type'
                    WHERE nt.instance_id = coalesce(bin.cites_id, pro.id))
                 END                                                        AS "typeCitation",

                 coalesce(f.htree -> 'Regnum', 'Plantae')                   AS "kingdom",
                 f.htree -> 'Familia'                                       AS "family",

                 substring(ntp.rank_path FROM 'Genus:([^>]*)')              AS "genericName",
                 substring(ntp.rank_path FROM 'Species:([^>]*)')            AS "specificEpithet",
                 substring(ntp.rank_path FROM 'Species:[^>]*>.*:(.*)\$')     AS "infraspecificEpithet",

                 rank.name                                                  AS "taxonRank",
                 rank.sort_order                                            AS "taxonRankSortOrder",
                 rank.abbrev                                                AS "taxonRankAbbreviation",

                 n.created_at                                               AS "created",
                 n.updated_at                                               AS "modified",

                 n.name_element                                             AS "nameElement",

                 CASE WHEN firstHybridParent.id IS NOT NULL
                   THEN
                     firstHybridParent.full_name
                 ELSE NULL END                                              AS "firstHybridParentName",

                 CASE WHEN firstHybridParent.id IS NOT NULL
                   THEN
                     'http://id.biodiversity.org.au/name/${namespace}/' || firstHybridParent.id :: TEXT
                 ELSE NULL END                                              AS "firstHybridParentNameID",

                 CASE WHEN secondHybridParent.id IS NOT NULL
                   THEN
                     secondHybridParent.full_name
                 ELSE NULL END                                              AS "secondHybridParentName",

                 CASE WHEN secondHybridParent.id IS NOT NULL
                   THEN
                     'http://id.biodiversity.org.au/name/${namespace}/' || secondHybridParent.id :: TEXT
                 ELSE NULL END                                              AS "secondHybridParentNameID"

               FROM NAME n
                 JOIN name_type nt ON n.name_type_id = nt.id
                 JOIN name_status ns ON n.name_status_id = ns.id
                 JOIN name_rank rank ON n.name_rank_id = rank.id

                 LEFT OUTER JOIN name_tree_path ntp
                 JOIN tree_arrangement tr ON ntp.tree_id = tr.id AND tr.label = 'APNI'
                   ON ntp.name_id = n.id

                 /*LEFT OUTER JOIN name_walk ON name_walk.name_id = n.id*/

                 LEFT OUTER JOIN name_part np
                 JOIN name p ON p.id = np.preceding_name_id
                   ON np.name_id = n.id

                 LEFT OUTER JOIN INSTANCE pro
                 JOIN instance_type pit ON pit.id = pro.instance_type_id AND pit.primary_instance = TRUE
                 JOIN REFERENCE pro_ref ON pro.reference_id = pro_ref.id
                 LEFT OUTER JOIN INSTANCE bin
                 JOIN instance_type bit ON bit.id = bin.instance_type_id AND bit.name = 'basionym'
                 JOIN NAME bnm ON bnm.id = bin.name_id
                   ON bin.cited_by_id = pro.id
                   ON pro.name_id = n.id

                 LEFT OUTER JOIN INSTANCE sec
                 JOIN instance_type sit ON sit.id = sec.instance_type_id AND sit.secondary_instance = TRUE
                 JOIN
                 REFERENCE sec_ref ON sec.reference_id = sec_ref.id
                   ON sec.name_id = n.id

                 LEFT OUTER JOIN INSTANCE apc_inst
                 JOIN instance_type apc_inst_type ON apc_inst.instance_type_id = apc_inst_type.id
                 JOIN tree_node apcn
                 JOIN tree_arrangement t
                   ON t.id = apcn.tree_arrangement_id AND t.label = '${classificationTreeName}'
                   ON (apcn.instance_id = apc_inst.id OR apcn.instance_id = apc_inst.cited_by_id)
                      AND apcn.checked_in_at_id IS NOT NULL
                      AND apcn.next_node_id IS NULL
                      AND apcn.type_uri_id_part != 'DeclaredBt'
                 LEFT OUTER JOIN tree f ON f.subnode_id = apcn.id
                   ON apc_inst.name_id = n.id

                 LEFT OUTER JOIN NAME firstHybridParent ON n.parent_id = firstHybridParent.id AND nt.hybrid
                 LEFT OUTER JOIN NAME secondHybridParent ON n.second_parent_id = secondHybridParent.id AND nt.hybrid
               WHERE EXISTS(SELECT 1
                            FROM INSTANCE
                            WHERE name_id = n.id)
                     AND n.duplicate_of_id IS NULL
               ORDER BY "scientificNameID", "taxonomicStatus", "namePublishedInYear"
  ),
  /* Deduplicate 'unplaced' names */
    apni_names AS (
    SELECT *
    FROM names
    WHERE "taxonomicStatus" != 'unplaced'
    UNION
    SELECT DISTINCT ON ("scientificName") *
    FROM names
    WHERE "taxonomicStatus" = 'unplaced'
    ORDER BY "scientificName"
  )
SELECT *
FROM apni_names
"""
    }

    def createNameView(String namespace, Sql sql) {
        throw new NotImplementedException()   //todo rewrite view
        createView(namespace, NAME_VIEW, sql, nameView)
    }

    def createNameView(String namespace) {
        withSql { Sql sql ->
            createNameView(namespace, sql)
        }
    }

    def refreshNameView(String namespace, Sql sql) {
        if (viewExists(sql, NAME_VIEW)) {
            String refresh = "REFRESH MATERIALIZED VIEW $NAME_VIEW"
            sql.execute(refresh)
        } else {
            createNameView(namespace, sql)
        }
    }

    def refreshNameView(String namespace) {
        withSql { Sql sql ->
            refreshNameView(namespace, sql)
        }
    }

    Closure taxonView = { namespace ->
        String classificationTreeName = configService.classificationTreeName
        return """
CREATE MATERIALIZED VIEW ${TAXON_VIEW} AS

  SELECT
    'ICNAFP' :: TEXT                                      AS "nomenclaturalCode",
    CASE WHEN apcn.id IS NOT NULL
      THEN
        CASE WHEN apc_cited_inst.id IS NOT NULL
          THEN
            'http://id.biodiversity.org.au/instance/${namespace}/' || apc_inst.id
        ELSE
          'http://id.biodiversity.org.au/node/${namespace}/' || apcn.id
        END
    ELSE NULL END                                         AS "taxonID",

    nt.name                                               AS "nameType",
    'http://id.biodiversity.org.au/name/${namespace}/' || n.id    AS "scientificNameID",

    n.full_name                                           AS "scientificName",

    CASE WHEN ns.name NOT IN ('legitimate', '[default]')
      THEN ns.name
    ELSE NULL END                                         AS "nomenclaturalStatus",

    CASE WHEN apc_inst.id = apcn.instance_id
      THEN
        apcn.type_uri_id_part
    ELSE
      apc_inst_type.name
    END                                                   AS "taxonomicStatus",
    apc_inst_type.pro_parte                               AS "proParte",

    CASE WHEN apc_inst.id != apcn.instance_id
      THEN
        accepted_name.full_name
    ELSE NULL END                                         AS "acceptedNameUsage",

    CASE WHEN apcn.instance_id IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/node/${namespace}/' || apcn.id
    ELSE NULL END                                         AS "acceptedNameUsageID",

    CASE WHEN apc_inst.id = apcn.instance_id AND apcp.id IS NOT NULL
      THEN
        CASE WHEN apcp.type_uri_id_part = 'classification-root'
          THEN '[${classificationTreeName}]'
        ELSE
          'http://id.biodiversity.org.au/node/${namespace}/' || apcp.id
        END
    ELSE NULL END                                         AS "parentNameUsageID",

    rank.name                                             AS "taxonRank",
    rank.sort_order                                       AS "taxonRankSortOrder",

    substring(ntp.rank_path FROM 'Regnum:([^>]*)')        AS "kingdom",
    substring(ntp.rank_path FROM 'Classis:([^>]*)')       AS "class",
    substring(ntp.rank_path FROM 'Subclassis:([^>]*)')    AS "subclass",
    substring(ntp.rank_path FROM 'Familia:([^>]*)')       AS "family",

    n.created_at                                          AS "created",
    n.updated_at                                          AS "modified",

    ARRAY(SELECT t2.label
          FROM name_tree_path ntp2 JOIN tree_arrangement t2 ON ntp2.tree_id = t2.id
          WHERE name_id = n.id
          ORDER BY t2.label)                              AS "datasetName",

    CASE WHEN apc_cited_inst.id IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/instance/${namespace}/' || apc_inst.cites_id
    ELSE
      'http://id.biodiversity.org.au/instance/${namespace}/' || apc_inst.id
    END                                                   AS "taxonConceptID",

    CASE WHEN apcr.citation IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/reference/${namespace}/' || apcr.id
    ELSE
      'http://id.biodiversity.org.au/reference/${namespace}/' || apc_inst.reference_id
    END                                                   AS "nameAccordingToID",

    CASE WHEN apcr.citation IS NOT NULL
      THEN
        apcr.citation
    ELSE
      apc_ref.citation
    END                                                   AS "nameAccordingTo",

    -- apc_comment.value
    (SELECT string_agg(regexp_replace(VALUE, E'[\\n\\r\\u2028]+', ' ', 'g'), ' ')
     FROM instance_note nt
       JOIN instance_note_key key1
         ON key1.id = nt.instance_note_key_id
            AND key1.name = 'APC Comment'
     WHERE nt.instance_id = apcn.instance_id)             AS "taxonRemarks",

    -- apc_dist.value
    (SELECT string_agg(regexp_replace(VALUE, E'[\\n\\r\\u2028]+', ' ', 'g'), ' ')
     FROM instance_note nt
       JOIN instance_note_key key1
         ON key1.id = nt.instance_note_key_id
            AND key1.name = 'APC Dist.'
     WHERE nt.instance_id = apcn.instance_id)             AS "taxonDistribution",

    CASE WHEN apc_inst.id = apcn.instance_id
      THEN regexp_replace(ntp.name_path, '\\.', '|',
                          'g')
    ELSE NULL END                                         AS "higherClassification",

    'http://creativecommons.org/licenses/by/3.0/' :: TEXT AS "ccLicense",

    CASE WHEN apcn.id IS NOT NULL
      THEN
        CASE WHEN apc_cited_inst.id IS NOT NULL
          THEN
            'http://id.biodiversity.org.au/instance/${namespace}/' || apc_inst.id
        ELSE
          'http://id.biodiversity.org.au/node/${namespace}/' || apcn.id
        END
    ELSE NULL END                                         AS "ccAttributionIRI ",

    n.simple_name                                         AS "canonicalName",
    CASE WHEN nt.autonym
      THEN NULL
    ELSE
      regexp_replace(substring(n.full_name_html FROM '<authors>(.*)</authors>'), '<[^>]*>', '', 'g')
    END                                                   AS "scientificNameAuthorship",
    CASE WHEN firstHybridParent.id IS NOT NULL
      THEN
        firstHybridParent.full_name
    ELSE NULL END                                         AS "firstHybridParentName",

    CASE WHEN firstHybridParent.id IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/name/${namespace}/' || firstHybridParent.id
    ELSE NULL END                                         AS "firstHybridParentNameID",

    CASE WHEN secondHybridParent.id IS NOT NULL
      THEN
        secondHybridParent.full_name
    ELSE NULL END                                         AS "secondHybridParentName",

    CASE WHEN secondHybridParent.id IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/name/${namespace}/' || secondHybridParent.id
    ELSE NULL END                                         AS "secondHybridParentNameID"

  FROM INSTANCE apc_inst
    JOIN instance_type apc_inst_type ON apc_inst.instance_type_id = apc_inst_type.id
    JOIN REFERENCE apc_ref ON apc_ref.id = apc_inst.reference_id
    JOIN tree_node apcn
    JOIN tree_arrangement tree ON tree.id = apcn.tree_arrangement_id AND tree.label = '${classificationTreeName}'
      ON (apcn.instance_id = apc_inst.id OR apcn.instance_id = apc_inst.cited_by_id)
         AND apcn.checked_in_at_id IS NOT NULL
         AND apcn.next_node_id IS NULL

    LEFT OUTER JOIN tree_link
    JOIN tree_node apcp
      ON apcp.id = tree_link.supernode_id
         AND apcp.checked_in_at_id IS NOT NULL
         AND apcp.next_node_id IS NULL
      ON apcn.id = tree_link.subnode_id

    LEFT OUTER JOIN INSTANCE apc_cited_inst ON apc_inst.cites_id = apc_cited_inst.id

    LEFT OUTER JOIN REFERENCE apcr ON apc_cited_inst.reference_id = apcr.id

    LEFT OUTER JOIN name_tree_path ntp ON ntp.name_id = apcn.name_id and ntp.tree_id = tree.id
    LEFT OUTER JOIN NAME accepted_name ON accepted_name.id = apcn.name_id

    JOIN NAME n ON n.id = apc_inst.name_id
    JOIN name_type nt ON n.name_type_id = nt.id
    JOIN name_status ns ON n.name_status_id = ns.id
    JOIN name_rank rank ON n.name_rank_id = rank.id

    LEFT OUTER JOIN NAME firstHybridParent ON n.parent_id = firstHybridParent.id AND nt.hybrid
    LEFT OUTER JOIN NAME secondHybridParent ON n.second_parent_id = secondHybridParent.id AND nt.hybrid;
"""
    }

    def createTaxonView(String namespace, Sql sql) {
        throw new NotImplementedException() //todo rewrite view
        createView(namespace, TAXON_VIEW, sql, taxonView)
    }

    def createTaxonView(String namespace) {
        withSql { Sql sql ->
            createTaxonView(namespace, sql)
        }
    }

    def refreshTaxonView(String namespace, Sql sql) {
        if (viewExists(sql, TAXON_VIEW)) {
            String refresh = "REFRESH MATERIALIZED VIEW ${TAXON_VIEW}"
            sql.execute(refresh)
        } else {
            createTaxonView(namespace, sql)
        }
    }

    def refreshTaxonView(String namespace) {
        withSql { Sql sql ->
            refreshTaxonView(namespace, sql)
        }
    }

    def createView(String namespace, String viewName, Sql sql, Closure viewDefn) {
        String drop = "DROP MATERIALIZED VIEW IF EXISTS ${viewName}"
        sql.execute(drop)
        String query = viewDefn(namespace)
        sql.execute(query)
    }


    File exportTaxonToCSV() {
        exportToCSV(TAXON_VIEW, "${configService.classificationTreeName}-taxon", taxonView)
    }

    File exportNamesToCSV() {
        exportToCSV(NAME_VIEW, "${configService.nameTreeName}-names", nameView)
    }

    private File exportToCSV(String viewName, String namePrefix, Closure viewDefn) {
        Date date = new Date()
        String tempFileDir = grailsApplication.config.shard.temp.file.directory
        String fileName = "$namePrefix-${date.format('yyyy-MM-dd-mmss')}.csv"
        File outputFile = new File(tempFileDir, fileName)
        withSql { Sql sql ->
            if (!viewExists(sql, viewName)) {
                log.debug "creating $viewName view for export."
                createView(configService.nameSpace.name.toLowerCase(), viewName, sql, viewDefn)
            }
            DataExportService.sqlCopyToCsvFile("SELECT * FROM $viewName", outputFile, sql)
        }
        return outputFile
    }

    Map findNameRow(Name name, String namespace = configService.nameSpace.name.toLowerCase()) {
        String query = "select * from $NAME_VIEW where \"scientificNameID\" = 'http://id.biodiversity.org.au/name/$namespace/$name.id'"
        List<Map> results = executeQuery(query, [])
        if (results.size()) {
            return results.first()
        }
        return null
    }

    /**
     * Search the Taxon view for an accepted name tree (currently just APC) giving an APC format data output
     * as a list of taxon records.
     * See NSL-1805
     * @param nameQuery - the query name
     * @return a Map of synonyms and accepted names that match the query
     */
    Map taxonSearch(String name) {
        String nameQuery = name.toLowerCase()
        Map results = [:]
        String query = "select * from $TAXON_VIEW where lower(\"canonicalName\") like ? or lower(\"scientificName\") like ? limit 100"
        List<Map> allResults = executeQuery(query, [nameQuery, nameQuery])
        List<Map> acceptedResults = allResults.findAll { Map result ->
            result.acceptedNameUsage == null
        }
        allResults.removeAll(acceptedResults)
        if (!allResults.empty) {
            results.synonyms = allResults
        }
        results.acceptedNames = [:]
        acceptedResults.each { Map result ->
            results.acceptedNames[result.scientificNameID as String] = result
            List<Map> synonyms = executeQuery("select * from $TAXON_VIEW where \"acceptedNameUsage\" = ? limit 100", [result.scientificName])
            results.acceptedNames[result.scientificNameID as String].synonyms = synonyms
        }
        return results

    }

    private static Boolean viewExists(Sql sql, String tableName) {
        String query = """
SELECT EXISTS
( SELECT 1 FROM   pg_catalog.pg_class c
  JOIN   pg_catalog.pg_namespace n ON n.oid = c.relnamespace
  WHERE  n.nspname = 'public'
       AND    c.relname = '$tableName'
       AND    c.relkind = 'm')
AS exists"""
        def rowResult = sql.firstRow(query)
        return rowResult.exists
    }

    private List<Map> executeQuery(String query, List params) {
        log.debug "executing query: $query, $params"
        List results = []
        withSql { Sql sql ->
            sql.eachRow(query, params) { GroovyResultSet row ->
                def res = row.toRowResult()
                Map d = new LinkedHashMap()
                res.keySet().each { key ->
                    d[key] = res[key] as String
                }
                results.add(d)
            }
        }
        return results
    }

}
