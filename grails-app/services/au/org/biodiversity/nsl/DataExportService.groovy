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

import grails.transaction.Transactional
import groovy.sql.Sql
import groovy.transform.Synchronized
import groovy.xml.MarkupBuilder
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Transactional
class DataExportService {

    def grailsApplication
    def searchService

    /**
     * export Darwin Core Archive
     *
     * This uses the postgresql function to dump it to a file then returns the file
     */
    public File exportDarwinCoreArchiveFilesToCSV() {
        Date date = new Date()
        String tempFileDir = getBaseDir()
        String dateString = date.format('yyyy-MM-dd-mmss')

        File outputDirectory = new File(tempFileDir, "dca-$dateString")
        outputDirectory.mkdir()
        File taxaCsvFile = new File(outputDirectory, "taxa-${dateString}.csv")
        File relationshipCsvFile = new File(outputDirectory, "relationships-${dateString}.csv")
        File metadataFile = new File(outputDirectory, "metadata.xml")
        File outputFile = new File(tempFileDir, "darwin-core-archive-${dateString}.zip")

        withSql { sql ->
            metadataFile.write(dcaMetaDataXml(taxaCsvFile, relationshipCsvFile))
            makeDCAStandaloneInstanceExport(sql, taxaCsvFile)
            makeDCARelationshipInstanceExportTable(sql, relationshipCsvFile)
        }
        zip(outputDirectory, outputFile)
        outputDirectory.deleteDir() //clean up
        return outputFile
    }

    public File getBaseDir() {
        String tempFileDir = grailsApplication.config.shard.temp.file.directory
        File baseDir = new File(tempFileDir, 'nsl-tmp')
        if (baseDir.exists()) {
            return baseDir
        }
        if (baseDir.mkdirs()) {
            return baseDir
        }
        return null
    }

    private withSql(Closure work) {
        Sql sql = searchService.getNSL()
        try {
            work(sql)
        } finally {
            sql.close()
        }
    }

    private static zip(File dir, File outputFile) {
        ZipOutputStream zipFile = new ZipOutputStream(new FileOutputStream(outputFile))
        dir.eachFile { file ->
            println file.name
            zipFile.putNextEntry(new ZipEntry(file.getName()))
            file.withInputStream { i ->
                def buffer = new byte[1024]
                Integer length = 0
                while ((length = i.read(buffer)) > 0) {
                    zipFile.write(buffer, 0, length)
                }
            }
            zipFile.closeEntry()
        }
        zipFile.close()
    }

    /**
     * Wrap a select statement with COPY statement TO STDOUT and save in the local file passed in.
     * This streams the output from the copy to a local file over the JDBC connection using the postgresql CopyManager
     * which is part of the postgresql driver:
     * see https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/copy/CopyManager.html#copyOut%28java.lang.String,%20java.io.Writer%29
     * @param sql
     * @return the file you passed in
     */
    static File sqlCopyToCsvFile(String sqlStatement, File file, Sql sql) {
        String statement = "COPY ($sqlStatement) TO STDOUT WITH CSV HEADER"
        println statement
        CopyManager copyManager = new CopyManager(sql.connection as BaseConnection)
        file.withWriter { writer ->
            copyManager.copyOut(statement, writer)
        }
        return file
    }

    String dcaMetaDataXml(File standalone, File relationship) {
        String TERMS = "http://rs.tdwg.org/dwc/terms"
        String TDWG_TERMS = "http://rs.tdwg.org/ontology/voc"
        String OUR_TERMS = "http://www.biodiversity.org.au/voc/boa"

        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        builder.archive(
                xmlns: "http://rs.tdwg.org/dwc/text/",
                metadata: "description.xml",
                'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
                'xsi:schemaLocation': "http://rs.tdwg.org/dwc/text/ http://rs.tdwg.org/dwc/text/tdwg_dwc_text.xsd"
        ) {
            core(
                    ignoreHeaderLines: "1",
                    fieldsTerminatedBy: ",",
                    fieldsEnclosedBy: '"',
                    rowType: "$TERMS/Taxa"
            ) {
                files {
                    location standalone.name
                }
                id(index: 0)                                              // this instance id
                field(index: 1, term: "$TERMS/taxonID")                   // the name id
                field(index: 2, term: "$TERMS/acceptedNameUsageID")       // APC accepted name id
                field(index: 3, term: "$TERMS/parentNameUsageID")         // parent name id
                field(index: 4, term: "$TERMS/scientificName")            // the full name - if scientific Name
                field(index: 5, term: "$TERMS/vernacularName")            // common name - if common name instance
                field(index: 6, term: "$TDWG_TERMS/TaxonName#cultivarNameGroup") // cultivar name - if not scientific
                field(index: 7, term: "$TERMS/acceptedNameUsage")         // the APC accepted full name
                field(index: 8, term: "$TERMS/parentNameUsage")          // the parent full name
                field(index: 9, term: "$TERMS/namePublishedIn")          // reference citation
                field(index: 10, term: "$TERMS/namePublishedInYear")      // reference year
                //tree branch
                field(index: 11, term: "$TERMS/class")                    //
                field(index: 12, term: "$TERMS/family")                   //
                field(index: 13, term: "$TERMS/genus")                    //
                field(index: 14, term: "$TERMS/specificEpithet")          // species
                field(index: 15, term: "$TERMS/infraspecificEpithet")     //
                field(index: 16, term: "$TERMS/taxonRank")                // Rank name
                field(index: 17, term: "$TERMS/verbatimTaxonRank")        // verbatim rank
                field(index: 18, term: "$TERMS/scientificNameAuthorship") // name author
                field(index: 19, term: "$TERMS/nomenclaturalCode")        // "ICBN"
                field(index: 20, term: "$TERMS/taxonomicStatus")          // instance type
                field(index: 21, term: "$TERMS/nomenclaturalStatus")      // name_status
                field(index: 22, term: "$OUR_TERMS/Name.rdf#type")        // name_type
                field(index: 23, term: "$TERMS/taxonRemarks")       // instance notes - concatenate?

            }

            extension(
                    ignoreHeaderLines: "1",
                    fieldsTerminatedBy: ",",
                    fieldsEnclosedBy: '"',
                    rowType: "$TERMS/ResourceRelationship"
            ) {
                files {
                    location relationship.name
                }
                id(index: 0) // this instance id
                field(index: 0, term: "$TERMS/resourceRelationshipID ")       // this instance ID
                field(index: 1, term: "$TERMS/resourceID ")                   // cited by instance ID
                field(index: 2, term: "$TERMS/relatedResourceID ")            // cites instance ID
                field(index: 3, term: "$TERMS/relationshipOfResource ")       // instance type
                field(index: 4, term: "$TERMS/relationshipAccordingTo ")      // cited by reference citation
                field(index: 5, term: "$TERMS/relationshipEstablishedDate ")  // cited by reference year
                field(index: 6, term: "$TERMS/relationshipRemarks")           // instance notes - concatenate?
            }
        }
        return writer.toString()
    }

    @Synchronized
    private static File makeDCAStandaloneInstanceExport(Sql sql, File file) {

        String sqlStatement = '''
SELECT
  'https://biodiversity.org.au/boa/instance/apni/' || sai.id AS id,
  'https://biodiversity.org.au/boa/name/apni/' || nsn.id     AS taxon_ID,
  CASE WHEN accepted_name.id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || accepted_name.id
  ELSE NULL END                                              AS accepted_Name_Usage_ID,
  CASE WHEN nsn.parent_nsl_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/name/apni/' || nsn.parent_nsl_id
  ELSE NULL END                                              AS parent_Name_Usage_ID,
  CASE WHEN nt.scientific = TRUE
    THEN n.full_name
  ELSE NULL END                                              AS scientific_Name,
  CASE WHEN nt.name = 'common'
    THEN n.full_name
  ELSE NULL END                                              AS vernacular_Name,
  CASE WHEN nt.cultivar = TRUE
    THEN n.full_name
  ELSE NULL END                                              AS cultivar_Name,
  accepted_name.full_name                                    AS accepted_name_usage,
  CASE WHEN parent_name IS NOT NULL
    THEN parent_name.full_name
  ELSE NULL END                                              AS parent_Name_Usage,
  ref.citation                                               AS name_Published_In,
  ref.year                                                   AS name_Published_In_Year,
  nsn.class                                                  AS class,
  nsn.family                                                 AS family,
  nsn.genus                                                  AS genus,
  nsn.species                                                AS specific_Epithet,
  nsn.infraspecies                                           AS infraspecific_Epithet,
  nsn.rank                                                   AS taxon_Rank,
  n.verbatim_rank                                            AS verbatim_Taxon_Rank,
  nsn.authority                                              AS scientific_Name_Authorship,
  CASE WHEN nt.scientific = TRUE
    THEN 'ICBN'
  ELSE NULL END                                              AS nomenclatural_Code,
  it.name                                                    AS taxonomic_Status,
  ns.name                                                    AS nomenclatural_Status,
  nt.name                                                    AS name_Type
FROM instance sai
  JOIN instance_type it ON sai.instance_type_id = it.id
  JOIN reference ref ON sai.reference_id = ref.id
  JOIN name n ON sai.name_id = n.id
  JOIN name_type nt ON n.name_type_id = nt.id
  JOIN name_status ns ON n.name_status_id = ns.id
  LEFT OUTER JOIN nsl_simple_name nsn ON sai.name_id = nsn.id
  LEFT OUTER JOIN name parent_name ON n.parent_id = parent_name.id
  LEFT OUTER JOIN instance apc_inst ON nsn.apc_instance_id = apc_inst.id
  LEFT OUTER JOIN name accepted_name ON apc_inst.name_id = accepted_name.id
WHERE sai.cites_id IS NULL'''

        sqlCopyToCsvFile(sqlStatement, file, sql)
    }

    @Synchronized
    private static File makeDCARelationshipInstanceExportTable(Sql sql, File file) {

        String sqlStatement = '''
SELECT
  'https://biodiversity.org.au/boa/instance/apni/' || i.id          AS resource_Relationship_ID,
  'https://biodiversity.org.au/boa/instance/apni/' || i.cited_by_id AS resource_ID,
  CASE WHEN i.cites_id IS NOT NULL
    THEN 'https://biodiversity.org.au/boa/instance/apni/' || i.cites_id
  ELSE NULL END                                                     AS related_Resource_ID,
  it.name                                                           AS relationship_Of_Resource,
  ref.citation                                                      AS relationship_According_To,
  ref.year                                                          AS relationship_Established_Date,
  'todo'                                                            AS relationship_Remarks
FROM instance i
  JOIN instance_type it ON i.instance_type_id = it.id
  JOIN instance cited_by ON cited_by.id = i.cited_by_id
  JOIN reference ref ON cited_by.reference_id = ref.id
WHERE i.cited_by_id IS NOT NULL'''

        sqlCopyToCsvFile(sqlStatement, file, sql)
    }
}
