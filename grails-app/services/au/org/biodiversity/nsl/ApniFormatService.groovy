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
import groovy.sql.GroovyResultSet
import groovy.sql.Sql

import javax.sql.DataSource

@Transactional
class ApniFormatService {

    def configService
    def linkService
    TreeService treeService
    DataSource dataSource_nsl

    List<ApniReferenceRecord> getApniNameModel(Name name) {
        Sql sql = getSql()
        List<ApniReferenceRecord> orderedApniRefs = getApniOrderedReferences(name.id, sql)
        orderedApniRefs.each { ApniReferenceRecord referenceRecord ->
            referenceRecord.synonymy = getOrderedSynonymy(referenceRecord.instanceId, sql)
            referenceRecord.synonymy.addAll(getSynonymRecord(referenceRecord.instanceId, sql))
            referenceRecord.typeNotes = getTypeNoteRecord(referenceRecord.instanceId, sql)
            referenceRecord.nonTypeNotes = getNonTypeNoteRecord(referenceRecord.instanceId, sql)
            referenceRecord.acceptedProfile = getAcceptedProfileRecord(referenceRecord.instanceId, sql)
        }
        return orderedApniRefs
    }

    protected List<ApniReferenceRecord> getApniOrderedReferences(Long nameId, Sql sql = getSql()) {
        List<ApniReferenceRecord> orderedApniRefs = []
        sql.eachRow('''select * from apni_ordered_refrences(:nameId);''', [nameId: nameId]) { GroovyResultSet row ->
            orderedApniRefs << new ApniReferenceRecord(row)
        }
        return orderedApniRefs
    }

    protected List<ApniSynonymRecord> getOrderedSynonymy(Long instanceId, Sql sql = getSql()) {
        List<ApniSynonymRecord> synonymRecords = []
        sql.eachRow('''select * from apni_ordered_synonymy(:instanceId);''', [instanceId: instanceId]) { GroovyResultSet row ->
            synonymRecords << new ApniSynonymRecord(row)
        }
        return synonymRecords
    }

    protected List<ApniSynonymRecord> getSynonymRecord(Long instanceId, Sql sql = getSql()) {
        List<ApniSynonymRecord> synonymRecords = []
        sql.eachRow('''select * from apni_synonym(:instanceId);''', [instanceId: instanceId]) { GroovyResultSet row ->
            synonymRecords << new ApniSynonymRecord(row)
        }
        return synonymRecords
    }

    protected List<InstanceNoteRecord> getTypeNoteRecord(Long instanceId, Sql sql = getSql()) {
        List<InstanceNoteRecord> records = []
        sql.eachRow('''select * from type_notes(:instanceId);''', [instanceId: instanceId]) { GroovyResultSet row ->
            records << new InstanceNoteRecord(row)
        }
        return records
    }

    protected List<InstanceNoteRecord> getNonTypeNoteRecord(Long instanceId, Sql sql = getSql()) {
        List<InstanceNoteRecord> records = []
        sql.eachRow('''select * from non_type_notes(:instanceId);''', [instanceId: instanceId]) { GroovyResultSet row ->
            records << new InstanceNoteRecord(row)
        }
        return records
    }

    protected AcceptedProfileRecord getAcceptedProfileRecord(Long instanceId, Sql sql = getSql()) {
        List<AcceptedProfileRecord> records = []
        sql.eachRow('''select * from latest_accepted_profile(:instanceId);''', [instanceId: instanceId]) { GroovyResultSet row ->
            records << new AcceptedProfileRecord(row)
        }
        return records.empty ? null : records.first()
    }

    Map getNameModel(Name name, TreeVersion version, Boolean draftInst) {
        if (!version) {
            version = treeService.getTree(configService.classificationTreeName)?.currentTreeVersion
        }
        Name familyName = name.family
        TreeVersionElement treeVersionElement = (version ? treeService.findElementForName(name, version) : null)
        String link = linkService.getPreferredLinkForObject(name)
        Map model = [name: name, treeVersionElement: treeVersionElement, familyName: familyName, preferredNameLink: link]
        model.putAll(nameReferenceInstanceMap(name, draftInst) as Map)
        return model
    }

    Map nameReferenceInstanceMap(Name name, Boolean draftInst) {
        Map<Reference, List<Instance>> refGroups = name.instances.findAll { instance ->
            (draftInst || !instance.draft)
        }.groupBy { instance ->
            instance.reference
        }
        List<Reference> references = new ArrayList(refGroups.keySet())
        references.sort { a, b ->
            //NSL-1119 protolog references must come first so if a reference contains a protologue instance make it win
            Integer aProto = refGroups[a].find { Instance i -> i.instanceType.protologue } ? 1 : 0
            Integer bProto = refGroups[b].find { Instance i -> i.instanceType.protologue } ? 1 : 0
            //NSL-1119 primary references must come next
            Integer aPrimary = refGroups[a].find { Instance i -> i.instanceType.primaryInstance } ? 1 : 0
            Integer bPrimary = refGroups[b].find { Instance i -> i.instanceType.primaryInstance } ? 1 : 0
            //NSL-1827 use parent details for sorting
            Integer aYear = ReferenceService.findReferenceYear(a)
            Integer bYear = ReferenceService.findReferenceYear(b)
            if (aYear == bYear) {
                if (aProto == bProto) {
                    if (aPrimary == bPrimary) {
                        if (a == b) {
                            if (a.pages == b.pages) {
                                return a.id <=> b.id  //highest Id first
                            }
                            return a.pages <=> b.pages //reverse string sort 1s, then 2s
                        }
                        return a.citation <=> b.citation //alpha sort by reference citation
                    }
                    return bPrimary <=> aPrimary // primary reference first (1)
                }
                return bProto <=> aProto // proto reference first (1)
            }
            return (aYear) <=> (bYear) //lowest year first
        }
        return [references: references, instancesByRef: refGroups]
    }

    static String transformXics(String text) {
        xicsMap.each { k, v ->
            text = text.replaceAll(k, v)
        }
        return HTMLSanitiser.encodeInvalidMarkup(text).trim()
    }

    private Sql getSql() {
        //noinspection GroovyAssignabilityCheck
        return Sql.newInstance(dataSource_nsl)
    }

    /**
     * This is a sequential list of regular expressions and replacements. The sequence is meaningful as replacements are
     * re-used to get some sequences to work. Yes it's ugly but works. The result needs to be parsed by a HTML parser to
     * make sure valid HTML exists since the input may contain invalid html, so there may not be an opening or closing tag.
     */
    static Map<String, String> xicsMap = ['~J'                : '&Aacute;',
                                          '~c'                : '&Agrave;',
                                          '~T'                : '&Acirc;',
                                          '~p'                : '&Auml;',
                                          '<AOU>'             : '&Aring;',
                                          '~K'                : '&Eacute;',
                                          '~d'                : '&Egrave;',
                                          '~U'                : '&Ecirc;',
                                          '~q'                : '&Euml;',
                                          '~L'                : '&Iacute;',
                                          '~3'                : '&Igrave;',
                                          '~V'                : '&Icirc;',
                                          '~r'                : '&Iuml;',
                                          '~M'                : '&Oacute;',
                                          '~e'                : '&Ograve;',
                                          '~W'                : '&Ocirc;',
                                          '~s'                : '&Ouml;',
                                          '~N'                : '&Uacute;',
                                          '~f'                : '&Ugrave;',
                                          '\\\\'              : '&Ucirc;',
                                          '~t'                : '&Uuml;',
                                          '~z'                : '&Ccedil;',
                                          '~1'                : '&Ntilde;',
                                          '~O'                : '&aacute;',
                                          '~g'                : '&agrave;',
                                          '~X'                : '&acirc;',
                                          '~u'                : '&auml;',
                                          '<AOL>'             : '&aring;',
                                          '<ATL>'             : '&atilde;',
                                          '~P'                : '&eacute;',
                                          '~h'                : '&egrave;',
                                          '~Y'                : '&ecirc;',
                                          '~v'                : '&euml;',
                                          '~Q'                : '&iacute;',
                                          '~i'                : '&igrave;',
                                          '~Z'                : '&icirc;',
                                          '~w'                : '&iuml;',
                                          '~R'                : '&oacute;',
                                          '~D'                : '&ograve;',
                                          '~a'                : '&ocirc;',
                                          '~x'                : '&ouml;',
                                          '<OTL>'             : '&otilde;',
                                          '~S'                : '&uacute;',
                                          '~E'                : '&ugrave;',
                                          '~b'                : '&ucirc;',
                                          '~y'                : '&uuml;',
                                          '~0'                : '&ccedil;',
                                          '~2'                : '&ntilde;',
                                          '<PM>'              : '&plusmn;',
                                          '<MR>'              : '&#151;',
                                          '<NR>'              : '&#150;',
                                          '<BY>'              : '&times;',
                                          '<DEG>'             : '&deg;',
                                          '<MIN>'             : '&prime;',
                                          '\\^I'              : '&#134;',
                                          '\\^K'              : '&#138;',
                                          '\\^k'              : '1/2',
                                          '\\^l'              : '<13>',
                                          '\\^m'              : '<23>',
                                          '\\^n'              : '1/4',
                                          '\\^o'              : '3/4',
                                          '\\^u'              : '&alpha;',
                                          '\\^v'              : '&beta;',
                                          '\\^w'              : '&gamma;',
                                          '\\^x'              : '&delta;',
                                          '\\^t'              : '=',
                                          '<13>'              : '1/3',
                                          '<23>'              : '2/3',
                                          '<IT>'              : '<i>',
                                          '<i>'               : '<i>',
                                          '</i>'              : '</i>',
                                          '<RO>'              : '</i>',
                                          '<RRO>'             : '</i>',
                                          '<MALE>'            : '&#9794;',
                                          '<M>'               : '&#9794;',
                                          '<FM>'              : '&#9792;',
                                          '<F,8>&uacute;<F,4>': '&Delta;',
                                          '<F,17>&auml;<F,4>' : '&#10218;',
                                          '<F,17>&auml; <F,4>': '&#10219;',
                                          '<F,17>&alpha;<F,4>': '&#10219;'
    ]

    static String transformXicsToUTF8(String text) {
        xicsToUTF8Map.each { k, v ->
            text = text.replaceAll(k, v)
        }
        return text.trim()
    }

    static Map<String, String> xicsToUTF8Map = ['~J'          : 'Á', //\u00C1
                                                '~c'          : 'À', //\u00E0
                                                '~T'          : 'Â', //\u00E2
                                                '~p'          : 'Ä', //\u00E4
                                                '<AOU>'       : 'Å', //\u00E5
                                                '~K'          : 'É', //\u00E9
                                                '~d'          : 'È', //\u00C8
                                                '~U'          : 'Ê', //\u00CA
                                                '~q'          : 'Ë', //\u00CB
                                                '~L'          : 'Í', //\u00CD
                                                '~3'          : 'Ì', //\u00EC
                                                '~V'          : 'Î', //\u00EE
                                                '~r'          : 'Ï', //\u00EF
                                                '~M'          : 'Ó', //\u00F3
                                                '~e'          : 'Ò', //\u00D2
                                                '~W'          : 'Ô', //\u00D4
                                                '~s'          : 'Ö', //\u00D6
                                                '~N'          : 'Ú', //\u00DA
                                                '~f'          : 'Ù', //\u00F9
                                                '\\\\'        : 'Û', //\u00FB
                                                '~t'          : 'Ü', //\u00FC
                                                '~z'          : 'Ç', //\u00C7
                                                '~1'          : 'Ñ', //\u00D1
                                                '~O'          : 'á', //\u00E1
                                                '~g'          : 'à', //\u00C0
                                                '~X'          : 'â', //\u00C2
                                                '~u'          : 'ä', //\u00C4
                                                '<AOL>'       : 'å', //\u00C5
                                                '<ATL>'       : 'ã', //\u00E3
                                                '~P'          : 'é', //\u00E9
                                                '~h'          : 'è', //\u00C8
                                                '~Y'          : 'ê', //\u00CA
                                                '~v'          : 'ë', //\u00CB
                                                '~Q'          : 'í', //\u00CD
                                                '~i'          : 'ì', //\u00EC
                                                '~Z'          : 'î', //\u00EE
                                                '~w'          : 'ï', //\u00EF
                                                '~R'          : 'ó', //\u00F3
                                                '~D'          : 'ò', //\u00D2
                                                '~a'          : 'ô', //\u00D4
                                                '~x'          : 'ö', //\u00D6
                                                '<OTL>'       : 'õ', //\u00F5
                                                '~S'          : 'ú', //\u00FA
                                                '~E'          : 'ù', //\u00D9
                                                '~b'          : 'û', //\u00DB
                                                '~y'          : 'ü', //\u00DC
                                                '~0'          : 'ç', //\u00E7
                                                '~2'          : 'ñ', //\u00F1
                                                '<PM>'        : '±', //\u00B1
                                                '<MR>'        : '—', //\u2014
                                                '<NR>'        : '–', //\u2013
                                                '<BY>'        : '×', //\u00D7
                                                '<DEG>'       : '°', //\u00B0
                                                '<MIN>'       : '′', //\u2032
                                                '<SVL>'       : 'Š',
                                                '<CUL>'       : 'ç',
                                                '\\^I'        : '†', //\u2020
                                                '\\^K'        : 'Š', //\u0160
                                                '\\^k'        : '½', //\u00BD
                                                '\\^l'        : '⅓', //\u2153
                                                '\\^m'        : '⅔', //\u2154
                                                '\\^n'        : '¼', //\u00BC
                                                '\\^o'        : '¾', //\u00BE
                                                '\\^u'        : 'α', //\u03B1
                                                '\\^v'        : 'β', //\u03B2
                                                '\\^w'        : 'γ', //\u03B3
                                                '\\^x'        : 'δ', //\u03B4
                                                '\\^t'        : '=',
                                                '<13>'        : '⅓', //\u2153
                                                '<23>'        : '⅔', //\u2154
                                                '<IT>'        : '<i>',
                                                '<I>'         : '<i>',
                                                '</I>'        : '</i>',
                                                '<RO>'        : '</i>',
                                                '<RRO>'       : '</i>',
                                                '<MALE>'      : '♂', //\u2642
                                                '<M>'         : '♂', //\u2642
                                                '<FM>'        : '♀', //\u2640
                                                '<F,8>ú<F,4>' : 'Δ', //\u0394
                                                '<F,17>ä<F,4>': '⟪', //\u27EA
                                                '<F,17>α<F,4>': '⟫', //\u27EB
    ]

}
//TODO add reference id and perhaps link
class ApniReferenceRecord {
    final Long instanceId
    final String instanceType
    final String citation
    final String citationHtml
    final Integer year
    final String pages
    final String page
    List<ApniSynonymRecord> synonymy
    List<InstanceNoteRecord> typeNotes
    List<InstanceNoteRecord> nonTypeNotes
    AcceptedProfileRecord acceptedProfile

    ApniReferenceRecord(GroovyResultSet data) {
        instanceId = data.getAt('instance_id') as Long
        instanceType = data.getAt('instance_type')
        citation = data.getAt('citation')
        citationHtml = data.getAt('citation_html')
        year = data.getAt('year') as Integer
        pages = data.getAt('pages')
        page = data.getAt('page')
    }
}

//TODO add instance draft, bhl link
class ApniSynonymRecord {
    Long instanceId
    String instanceType
    Long instanceTypeId
    Long nameId
    String fullName
    String fullNameHtml
    String nameStatus
    String citation
    String citationHtml
    Integer year
    String page
    String sortName
    Boolean misapplied

    ApniSynonymRecord(GroovyResultSet data) {
        instanceId = data.getAt('instance_id') as Long
        instanceType = data.getAt('instance_type')
        instanceTypeId = data.getAt('instance_type_id') as Long
        nameId = data.getAt('name_id') as Long
        fullName = data.getAt('full_name')
        fullNameHtml = data.getAt('full_name_html')
        nameStatus = data.getAt('name_status')
        citation = data.getAt('citation')
        citationHtml = data.getAt('citation_html')
        year = data.getAt('year') as Integer
        page = data.getAt('page')
        sortName = data.getAt('sort_name')
        misapplied = data.getAt('misapplied')
    }
}

class InstanceNoteRecord {
    final String key
    final String value

    InstanceNoteRecord(GroovyResultSet data) {
        key = data.getAt('note_key')
        value = data.getAt('note')
    }
}

class AcceptedProfileRecord {
    String commentKey
    String commentValue
    String distKey
    String distValue

    AcceptedProfileRecord(GroovyResultSet data) {
        commentKey = data.getAt('comment_key')
        commentValue = data.getAt('comment_value')
        distKey = data.getAt('dist_key')
        distValue = data.getAt('dist_value')
    }
}