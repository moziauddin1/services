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

package au.org.biodiversity.nsl

import groovy.sql.BatchingPreparedStatementWrapper
import groovy.sql.Sql
import groovy.transform.Synchronized

class SimpleNameService {

    def constructedNameService
    def nameTreePathService
    def searchService
    def classificationService
    def grailsApplication

    private List<InstanceType> protologues = []
    private List<Long> cultivarIds = []
    private Map<Long, Integer> nameYear = [:]

    List<InstanceType> getProtologues() {
        if (protologues.empty) {
            protologues = InstanceType.findAllByProtologue(true)
        }
        return protologues
    }

    Boolean isCultivar(Name name, Boolean cache = false) {
        if (cache) {
            if (cultivarIds.empty) {
                cultivarIds = (NameType.findAllByCultivar(true)).collect { it.id }
            }
            return cultivarIds.contains(name.nameType.id)
        } else {
            return name.nameType.cultivar
        }
    }

    Integer findProtologueYear(Name name, Boolean cache = false) {
        if (cache) {
            if (nameYear.isEmpty()) {
                log.debug "caching protologue years"
                List<List> nameYearList = Instance.executeQuery('select i.name.id, r.year, r.citation, i  from Instance i, Reference r where i.instanceType in (:protologues) and r = i.reference',
                        [protologues: getProtologues()]) as List<List>
                nameYearList.groupBy { it[0] }
                nameYearList.each { nameYear.put(it[0] as Long, it[1] as Integer) }
            }
            return nameYear[name.id]
        } else {
            List protoYear = Instance.executeQuery('select r.year from Instance i, Reference r where i.instanceType in (:protologues) and r = i.reference and i.name = :name',
                    [protologues: getProtologues(), name: name])
            if (protoYear && !protoYear.empty) {
                return protoYear[0] as Integer
            }
            return null
        }
    }

    private Name findNameByCitedInstance(Name name, String instanceTypeName) {
        List<Name> names = Instance.executeQuery("select i.name from Instance i where i.citedBy.name = :name and i.instanceType.name = :type", [name: name, type: instanceTypeName]) as List<Name>

        if (names && !names.empty) {
            return names.first()
        }
        return null
    }


    Map makeSimpleNameMap(Name name, Boolean cache = false) {
        Name.withNewSession { t ->

            Instance protologue = Instance.findByNameAndInstanceTypeInList(name, getProtologues())
            Integer protologueYear = protologue?.reference?.year
            String protoCitation = protologue?.reference?.citation

            Name basionym = findNameByCitedInstance(name, 'basionym')
            Name replaced = findNameByCitedInstance(name, 'replaced synonym')

            NameTreePath treePath = nameTreePathService.findCurrentNameTreePath(name, 'APNI')
            if (!treePath) {
                return null
            }
            List<Long> ids = treePath.pathIds()
            treePath.discard()

            List<List> namesInBranch = (Name.executeQuery('select n.nameRank.name, n.id, n.nameElement from Name n where n.id in (:ids)', [ids: ids]) as List<List>)
            Map parentsByRank = [:]
            namesInBranch.each { n ->
                parentsByRank.put(n[0], [name: n[2], id: n[1]])
            }

            Name apcFamily = RankUtils.getParentOfRank(name, 'Familia', 'APC')

            Long parentId = name.parent?.id
            String authority = constructedNameService.getAuthorityFromFullNameHTML(name.fullNameHtml)
            String cultivarName = isCultivar(name, cache) ? name.nameElement : null
            Boolean apni = Instance.countByName(name) > 0
            Long secondParentId = name.secondParent?.id

            Node apcNode = classificationService.isNameInAPC(name)
            Instance apcInstance

            if (apcNode) {
                apcInstance = apcNode.instance
            } else {
                apcInstance = classificationService.getAcceptedInstance(name, 'APC', 'ApcConcept')
            }
            String infraSp = (RankUtils.rankLowerThan(name.nameRank, 'Species') ? name.nameElement : null)

            String classifications = NameTreePathService.findAllTreesForName(name).toString()

            try {
                Map sn = [
                        id                 : name.id,
                        name               : name.simpleName,
                        taxonName          : name.fullName,
                        nameElement        : name.nameElement,
                        simpleNameHtml     : name.simpleNameHtml,
                        fullNameHtml       : name.fullNameHtml,
                        cultivarName       : cultivarName,
                        basionym           : basionym?.fullName,
                        replacedSynonym    : replaced?.fullName,

                        nameTypeName       : name.nameType.name,
                        nameTypeId         : name.nameType.id,
                        homonym            : false,
                        autonym            : name.nameType.autonym,
                        hybrid             : name.nameType.hybrid,
                        cultivar           : name.nameType.cultivar,
                        formula            : name.nameType.formula,
                        scientific         : name.nameType.scientific,

                        authority          : authority,
                        baseNameAuthor     : name.baseAuthor?.abbrev,
                        exBaseNameAuthor   : name.exBaseAuthor?.abbrev,
                        author             : name.author?.abbrev,
                        exAuthor           : name.exAuthor?.abbrev,
                        sanctioningAuthor  : name.sanctioningAuthor?.abbrev,

                        rank               : name.nameRank.name,
                        nameRankId         : name.nameRank.id,
                        rankSortOrder      : name.nameRank.sortOrder,
                        rankAbbrev         : name.nameRank.abbrev,

                        classifications    : classifications,
                        apni               : apni,

                        protoCitation      : protoCitation,
                        protoInstanceId    : protologue?.id,
                        protoYear          : protologueYear,
                        nomStat            : name.nameStatus.name,
                        nameStatusId       : name.nameStatus.id,
                        nomIlleg           : name.nameStatus.nomIlleg,
                        nomInval           : name.nameStatus.nomInval,

                        dupOfId            : name.duplicateOf?.id,

                        updatedBy          : name.updatedBy,
                        updatedAt          : name.updatedAt,
                        createdBy          : name.createdBy,
                        createdAt          : name.createdAt,

                        parentNslId        : parentId,
                        secondParentNslId  : secondParentId,
                        familyNslId        : parentsByRank.Familia?.id,
                        genusNslId         : parentsByRank.Genus?.id,
                        speciesNslId       : parentsByRank.Species?.id,
                        classis            : parentsByRank.Classis?.name,
                        subclassis         : parentsByRank.Subclassis?.name,
                        familia            : parentsByRank.Familia?.name,
                        apcFamilia         : apcFamily?.nameElement,
                        genus              : parentsByRank.Genus?.name,
                        species            : parentsByRank.Species?.name,
                        infraspecies       : infraSp,

                        apcInstanceId      : apcInstance?.id,
                        apcName            : apcNode ? name.fullName : apcInstance?.citedBy?.name?.fullName,
                        apcRelationshipType: apcInstance?.instanceType?.name,
                        apcProparte        : apcInstance && apcInstance?.instanceType?.proParte,
                        apcComment         : apcInstance?.instanceNotes?.find {
                            it.instanceNoteKey.name == 'APC Comment'
                        }?.value, // note there may be more than one, we get the first
                        apcDistribution    : apcInstance?.instanceNotes?.find {
                            it.instanceNoteKey.name == 'APC Dist.'
                        }?.value, // note there may be more than one, we get the first
                        apcExcluded        : apcNode && apcNode?.typeUriIdPart == 'ApcExcluded'
                ]

                apcInstance?.discard()
                apcNode?.discard()
                apcFamily?.discard()
                basionym?.discard()
                replaced?.discard()
                protologue?.discard()

                return sn
            } catch (e) {
                log.error "got error $e.message making simple name map for $name"
                e.printStackTrace()
                throw e
            }
        }
    }

    String backgroundMakeSimpleNames() {
        log.debug "in makeAllSimpleNames"
        runAsync {
            try {
                long start = System.currentTimeMillis()
                makeSimpleNameTable()
                log.debug "making all simple names took ${System.currentTimeMillis() - start}ms"
            } catch (e) {
                log.error e.message
                e.printStackTrace()
            }
        }
        "Started making all Simple Name"
    }

    def makeSimpleNameTable() {
        Closure query = { Map params ->
            Name.executeQuery("select ntp.name from NameTreePath ntp, Arrangement a where a.label = 'APNI' and ntp.tree = a and ntp.next is null order by ntp.name.id asc", params)
            //this updates
//            Name.executeQuery('select ntp.name from NameTreePath ntp where not exists (select 1 from NslSimpleName sn where ntp.name.id = sn.id) order by ntp.name.id asc', params)
        }

        Sql sql = searchService.getNSL()
        sql.connection.autoCommit = false

        sql.execute('TRUNCATE TABLE ONLY nsl_simple_name')
        sql.commit()

        Name n = Name.list(max: 1).first()
        String valuesString = makeInsertValues(makeSimpleNameMap(n, true))

        try {
            sql.withBatch(100, "INSERT INTO nsl_simple_name $valuesString"
            ) { BatchingPreparedStatementWrapper ps ->

                chunkThis(1000, query) { List<Name> names, bottom, top ->
                    long start = System.currentTimeMillis()
                    names.each { Name name ->
                        if (!name.duplicateOf) {
                            ps.addBatch(makeSimpleNameMap(name, true))
                        }
                        name.discard()
                    }
                    log.info "$top done. 1000 took ${System.currentTimeMillis() - start} ms"
                }
            }
            sql.commit()
        } catch (e) {
            def orig = e
            while (e) {
                log.error(e.message)
                if (e.message.contains('getNextException')) {
                    e = e.getNextException()
                } else {
                    e = e.cause
                }
            }
            throw orig
        } finally {
            sql.close()
        }
    }

    /**
     * export NslSimpleName table to csv format. Only export what is produced by the JSON representation, including the
     * links in place of name IDs.
     *
     * This uses the postgresql function to dump it to a file then returns the file
     */
    public File exportSimpleNameToCSV() {
        Date date = new Date()
        String tempFileDir = grailsApplication.config.nslServices.temp.file.directory
        String fileName = "nslsimplename-${date.format('yyyy-MM-dd-mmss')}.csv"
        File outputFile = new File(tempFileDir, fileName)
        withSql { Sql sql ->
            ensureNslSimpleNameExportTable(sql)
            String copyToFile = "COPY nsl_simple_name_export TO '${outputFile.absolutePath}' WITH CSV HEADER"
            sql.execute(copyToFile)
        }
        return outputFile
    }

    /**
     * export NsLSimpleName table as a text file of data
     * @return
     */
    public File exportSimpleNameToText() {
        Date date = new Date()
        String tempFileDir = grailsApplication.config.nslServices.temp.file.directory
        String fileName = "nslsimplename-${date.format('yyyy-MM-dd-mmss')}.txt"
        File outputFile = new File(tempFileDir, fileName)
        withSql { Sql sql ->
            ensureNslSimpleNameExportTable(sql)
            String copyToFile = "COPY nsl_simple_name_export TO '${outputFile.absolutePath}'"
            sql.execute(copyToFile)
        }
        return outputFile
    }

    private static void ensureNslSimpleNameExportTable(Sql sql) {
        def rowResult = sql.firstRow('''
SELECT EXISTS (
    SELECT 1
    FROM   pg_catalog.pg_class c
      JOIN   pg_catalog.pg_namespace n ON n.oid = c.relnamespace
    WHERE  n.nspname = 'public\'
           AND    c.relname = 'nsl_simple_name_export\'
           AND    c.relkind = 'r'    -- only tables(?)
) AS exists''')
        if (!rowResult.exists) {
            makeNslSimpleNameExportTable(sql)
        }
    }


    private withSql(Closure work) {
        Sql sql = searchService.getNSL()
        try {
            work(sql)
        } finally {
            sql.close()
        }

    }

    @Synchronized
    private static void makeNslSimpleNameExportTable(Sql sql) {

        sql.execute('DROP TABLE IF EXISTS nsl_simple_name_export')
        sql.execute('''
CREATE TABLE nsl_simple_name_export AS
  (SELECT
  'https://biodiversity.org.au/boa/name/apni/' || id AS id,
  apc_comment,
  apc_distribution,
  apc_excluded,
  apc_familia,
  CASE WHEN apc_instance_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/instance/apni/' || apc_instance_id
  ELSE NULL END AS apc_instance_id,
  apc_name,
  apc_proparte,
  apc_relationship_type,
  apni,
  author,
  authority,
  autonym,
  basionym,
  base_name_author,
  classifications,
  created_at,
  created_by,
  cultivar,
  cultivar_name,
  ex_author,
  ex_base_name_author,
  familia,
  CASE WHEN family_nsl_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || family_nsl_id
  ELSE NULL END AS family_nsl_id,
  formula,
  full_name_html,
  genus,
  CASE WHEN genus_nsl_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || genus_nsl_id
  ELSE NULL END AS genus_nsl_id,
  homonym,
  hybrid,
  infraspecies,
  name,
  classis,
  name_element,
  subclassis,
  name_type_name,
  nom_illeg,
  nom_inval,
  nom_stat,
  CASE WHEN parent_nsl_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || parent_nsl_id
  ELSE NULL END AS parent_nsl_id,
  proto_citation,
  CASE WHEN proto_instance_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/instance/apni/' || proto_instance_id
  ELSE NULL END AS proto_instance_id,
  proto_year,
  rank,
  rank_abbrev,
  rank_sort_order,
  replaced_synonym,
  sanctioning_author,
  scientific,
  CASE WHEN second_parent_nsl_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || second_parent_nsl_id
  ELSE NULL END AS second_parent_nsl_id,
  simple_name_html,
  species,
  CASE WHEN species_nsl_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || species_nsl_id
  ELSE NULL END AS species_nsl_id,
  taxon_name,
  updated_at,
  updated_by
FROM nsl_simple_name)''')
    }


    public static chunkThis(Integer chunkSize, Closure query, Closure work) {

        Integer i = 0
        Integer size = chunkSize
        while (size == chunkSize) {
            Integer top = i + chunkSize
            //needs to be ordered or we might repeat items
            List items = query([offset: i, max: chunkSize])
            work(items, i, top)
            i = top
            size = items.size()
        }
    }

    private static makeInsertValues(Map data) {
        List<String> columns = data.keySet().collect { camelToSnake(it as String) }
        List<String> dataKeys = data.keySet().collect { ":$it" }
        "(${columns.join(',')}) values (${dataKeys.join(',')})"
    }


    private static camelToSnake(String camel) {
        if (camel) {
            return camel.replaceAll(/([a-z]+)([A-Z])/, '$1_$2').toLowerCase()
        }
    }

}

