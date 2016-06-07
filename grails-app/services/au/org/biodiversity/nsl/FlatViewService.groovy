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


@Transactional
class FlatViewService {

    def grailsApplication
    def searchService

    private static String TAXON_VIEW = 'apc_taxon_view'
    private static String NAME_VIEW = 'name_view'


    Closure nameView = { namespace ->
        return """
CREATE MATERIALIZED VIEW name_view AS

  SELECT
    'ICNAFP' :: TEXT                                         AS "nomenclaturalCode",
    'APNI' :: TEXT                                           AS "datasetName",
    nt.name                                                  AS "nameType",
    CASE WHEN apc_inst.id IS NULL
      THEN
        (SELECT '[unplaced ' || CASE WHEN i.cited_by_id IS NULL
          THEN 'name'
                                ELSE 'relationship' END || '?]'
         FROM INSTANCE i
           JOIN REFERENCE r ON r.id = i.reference_id
         WHERE i.name_id = n.id
         ORDER BY r.year DESC
         LIMIT 1
        )
    ELSE
      CASE WHEN apc_inst.id = apcn.instance_id
        THEN
          apcn.type_uri_id_part
      ELSE
        apc_inst_type.name
      END
    END                                                      AS "taxonomicStatus",
    'http://id.biodiversity.org.au/name/${namespace}/' || n.id       AS "scientificNameID",
    n.full_name                                              AS "scientificName",
    CASE WHEN ns.name NOT IN ('legitimate', '[default]')
      THEN ns.name
    ELSE NULL END                                            AS "nomenclaturalStatus",
    n.simple_name                                            AS "canonicalName",
    CASE WHEN nt.autonym
      THEN
        NULL
    ELSE
      regexp_replace(substring(n.full_name_html FROM '<authors>(.*)</authors>'), '<[^>]*>', '', 'g')
    END                                                      AS "scientificNameAuthorship",
    'http://creativecommons.org/licenses/by/3.0/' :: TEXT    AS "ccLicense",
    'http://id.biodiversity.org.au/name/${namespace}/' || n.id       AS "ccAttributionIRI",

    CASE WHEN nt.cultivar = TRUE
      THEN n.name_element
    ELSE NULL END                                            AS "cultivarEpithet",
    n.simple_name_html                                       AS "canonicalNameHTML",
    n.full_name_html                                         AS "scientificNameHTML",

    nt.autonym                                               AS "autonym",
    nt.hybrid                                                AS "hybrid",
    nt.cultivar                                              AS "cultivar",
    nt.formula                                               AS "formula",
    nt.scientific                                            AS "scientific",
    ns.nom_inval                                             AS "nomInval",
    ns.nom_illeg                                             AS "nomIlleg",

    pro_ref.citation                                         AS "namePublishedIn",
    pro_ref.year                                             AS "namePublishedInYear",
    pit.name                                                 AS "nameInstanceType",
    bnm.full_name                                            AS "originalNameUsage",
    CASE WHEN bin.id IS NOT NULL
      THEN 'http://id.biodiversity.org.au/instance/${namespace}/' || bin.id
    ELSE
      CASE WHEN pro.id IS NOT NULL
        THEN 'http://id.biodiversity.org.au/instance/${namespace}/' || pro.id
      ELSE NULL END
    END                                                      AS "originalNameUsageID",
    /*apc_comment.value */
    (SELECT string_agg(regexp_replace(VALUE, E'[\\\\n\\\\r\\\\u2028]+', ' ', 'g'), ' ')
     FROM instance_note nt
       JOIN instance_note_key key1
         ON key1.id = nt.instance_note_key_id
            AND key1.name = 'Type'
     WHERE nt.instance_id = apcn.instance_id)                AS "typeCitation",

    rank.name                                                AS "taxonRank",
    rank.sort_order                                          AS "taxonRankSortOrder",
    rank.abbrev                                              AS "taxonRankAbbreviation",

    substring(ntp.rank_path FROM 'Regnum:([^>]*)')           AS "kingdom",
    substring(ntp.rank_path FROM 'Classis:([^>]*)')          AS "class",
    substring(ntp.rank_path FROM 'Subclassis:([^>]*)')       AS "subclass",
    substring(ntp.rank_path FROM 'Familia:([^>]*)')          AS "family",
    substring(ntp.rank_path FROM 'Genus:([^>]*)')            AS "genericName",
    substring(ntp.rank_path FROM 'Species:([^>]*)')          AS "specificEpithet",
    substring(ntp.rank_path FROM 'Species:[^>]*>.*:(.*)\\\\\$') AS "infraspecificEpithet",

    n.created_at                                             AS "created",
    n.updated_at                                             AS "modified",

    n.name_element                                           AS "nameElement",

    CASE WHEN firstHybridParent.id IS NOT NULL
      THEN
        firstHybridParent.full_name
    ELSE NULL END                                            AS "firstHybridParentName",

    CASE WHEN firstHybridParent.id IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/name/${namespace}/' || firstHybridParent.id
    ELSE NULL END                                            AS "firstHybridParentNameID",

    CASE WHEN secondHybridParent.id IS NOT NULL
      THEN
        secondHybridParent.full_name
    ELSE NULL END                                            AS "secondHybridParentName",

    CASE WHEN secondHybridParent.id IS NOT NULL
      THEN
        'http://id.biodiversity.org.au/name/${namespace}/' || secondHybridParent.id
    ELSE NULL END                                            AS "secondHybridParentNameID"

  FROM NAME n
    JOIN name_type nt ON n.name_type_id = nt.id
    JOIN name_status ns ON n.name_status_id = ns.id
    JOIN name_rank rank ON n.name_rank_id = rank.id

    LEFT OUTER JOIN author combination_author ON combination_author.id = n.author_id
    LEFT OUTER JOIN author basionym_author ON n.base_author_id = basionym_author.id
    LEFT OUTER JOIN author ex_basionym_author ON n.ex_base_author_id = ex_basionym_author.id
    LEFT OUTER JOIN author ex_combination_author ON n.ex_author_id = ex_combination_author.id
    LEFT OUTER JOIN author sanctioning_work ON n.sanctioning_author_id = sanctioning_work.id

    LEFT OUTER JOIN INSTANCE pro /* ON pro.name_id = n.id */
    JOIN instance_type pit ON pit.id = pro.instance_type_id AND pit.primary_instance = TRUE
    JOIN REFERENCE pro_ref ON pro.reference_id = pro_ref.id
      ON pro.name_id = n.id

    LEFT OUTER JOIN INSTANCE bin
    JOIN instance_type bit ON bit.id = bin.instance_type_id AND bit.name = 'basionym'
    JOIN NAME bnm ON bnm.id = bin.name_id
      ON bin.id = pro.cites_id

    LEFT OUTER JOIN INSTANCE apc_inst
    JOIN instance_type apc_inst_type ON apc_inst.instance_type_id = apc_inst_type.id
    JOIN REFERENCE apc_ref ON apc_ref.id = apc_inst.reference_id
    JOIN tree_node apcn
    JOIN tree_arrangement tree ON tree.id = apcn.tree_arrangement_id AND tree.label = 'APC'
    JOIN name_tree_path ntp ON ntp.name_id = apcn.name_id and ntp.tree_id = tree.id
      ON (apcn.instance_id = apc_inst.id OR apcn.instance_id = apc_inst.cited_by_id)
         AND apcn.checked_in_at_id IS NOT NULL
         AND apcn.next_node_id IS NULL
         AND apcn.type_uri_id_part != 'DeclaredBt'
      ON apc_inst.name_id = n.id

    LEFT OUTER JOIN NAME firstHybridParent ON n.parent_id = firstHybridParent.id AND nt.hybrid
    LEFT OUTER JOIN NAME secondHybridParent ON n.second_parent_id = secondHybridParent.id AND nt.hybrid
  WHERE EXISTS(SELECT 1
               FROM INSTANCE
               WHERE name_id = n.id)
        AND n.duplicate_of_id IS NULL;
"""
    }

    def createNameView(String namespace, Sql sql) {
        String drop = "DROP MATERIALIZED VIEW IF EXISTS $NAME_VIEW"
        sql.execute(drop)
        String query = nameView(namespace)
        sql.execute(query)
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

    Closure apcTaxonView = { namespace ->
        return """
CREATE MATERIALIZED VIEW apc_taxon_view AS

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
          THEN '[APC]'
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
    (SELECT string_agg(regexp_replace(VALUE, E'[\\\\n\\\\r\\\\u2028]+', ' ', 'g'), ' ')
     FROM instance_note nt
       JOIN instance_note_key key1
         ON key1.id = nt.instance_note_key_id
            AND key1.name = 'APC Comment'
     WHERE nt.instance_id = apcn.instance_id)             AS "taxonRemarks",

    -- apc_dist.value
    (SELECT string_agg(regexp_replace(VALUE, E'[\\\\n\\\\r\\\\u2028]+', ' ', 'g'), ' ')
     FROM instance_note nt
       JOIN instance_note_key key1
         ON key1.id = nt.instance_note_key_id
            AND key1.name = 'APC Dist.'
     WHERE nt.instance_id = apcn.instance_id)             AS "taxonDistribution",

    CASE WHEN apc_inst.id = apcn.instance_id
      THEN regexp_replace(ntp.name_path, '\\>', '|',
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
    JOIN tree_arrangement tree ON tree.id = apcn.tree_arrangement_id AND tree.label = 'APC'
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
        String drop = "DROP MATERIALIZED VIEW IF EXISTS ${TAXON_VIEW}"
        sql.execute(drop)
        String query = apcTaxonView(namespace)
        sql.execute(query)
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

    public File exportApcTaxonToCSV() {
        Date date = new Date()
        String tempFileDir = grailsApplication.config.shard.temp.file.directory
        String fileName = "apc-taxon-${date.format('yyyy-MM-dd-mmss')}.csv"
        File outputFile = new File(tempFileDir, fileName)
        withSql { Sql sql ->
            if (!viewExists(sql, TAXON_VIEW)) {
                log.debug "creating $TAXON_VIEW view for export."
                createTaxonView(grailsApplication.config.shard.classification.namespace.toLowerCase(), sql)
            }
            String query = "COPY (SELECT * FROM $TAXON_VIEW) TO '${outputFile.absolutePath}' WITH CSV HEADER"
            sql.execute(query)
        }
        return outputFile
    }

    public File exportNamesToCSV() {
        Date date = new Date()
        String tempFileDir = grailsApplication.config.shard.temp.file.directory
        String fileName = "names-${date.format('yyyy-MM-dd-mmss')}.csv"
        File outputFile = new File(tempFileDir, fileName)
        withSql { Sql sql ->
            if (!viewExists(sql, NAME_VIEW)) {
                log.debug "creating $NAME_VIEW view for export."
                createNameView(grailsApplication.config.shard.classification.namespace.toLowerCase(), sql)
            }
            String query = "COPY (SELECT * FROM $NAME_VIEW) TO '${outputFile.absolutePath}' WITH CSV HEADER"
            sql.execute(query)
        }
        return outputFile
    }

    public List<Map> getTaxonRecordFromNames(List<Name> names) {
        String nameIds = names.collect {Name name ->
            "'http://id.biodiversity.org.au/name/apni/$name.id'"
        }.join(',')
        List results = []
        withSql { Sql sql ->

            String query = "select * from $TAXON_VIEW where \"scientificNameID\" in ($nameIds)"
            println query
            sql.eachRow(query) { GroovyResultSet row ->
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

    private def withSql(Closure work) {
        Sql sql = searchService.getNSL()
        try {
            work(sql)
        } finally {
            sql.close()
        }

    }

}
