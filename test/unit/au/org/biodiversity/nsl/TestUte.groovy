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

import java.sql.Timestamp

/**
 * User: pmcneil
 * Date: 15/01/15
 *
 */
class TestUte {

    /**
     * you need to mock
     * @mock ( [ NameGroup , NameCategory , NameType , NameStatus , NameRank] )
     *
     * to use in unit tests
     */
    static void setUpNameInfrastructure() {
        setUpNameGroups()
        setUpNameCategories()
        setUpNameTypes()
        setUpNameStatus()
        setUpNameRanks()
    }

    static String setUpNameGroups() {
        NameGroup.withTransaction { status ->
            [
                    [name: '[unknown]'],
                    [name: '[n/a]'],
                    [name: 'botanical'],
                    [name: 'zoological']
            ].each { data ->
                NameGroup nameGroup = new NameGroup(data)
                nameGroup.save()
            }
            NameGroup.withSession { s -> s.flush() }
        }
        return "Added name group records"
    }

    static String setUpNameCategories() {
        NameCategory.withTransaction { status ->
            [
                    [name                      : 'cultivar',
                     sortOrder                 : 50,
                     descriptionHtml           : 'names entered and edited as cultivar names',
                     rdfId                     : 'cultivar',
                     maxParentsAllowed         : 1,
                     minParentsRequired        : 1,
                     parent1HelpText           : "cultivar - genus and below, or unranked if unranked",
                     parent2HelpText           : null,
                     requiresFamily            : true,
                     requiresHigherRankedParent: false,
                     requiresNameElement       : true,
                     takesAuthorOnly           : false,
                     takesAuthors              : false,
                     takesCultivarScopedParent : true,
                     takesHybridScopedParent   : false,
                     takesNameElement          : true,
                     takesVerbatimRank         : true],

                    [name                      : 'scientific',
                     sortOrder                 : 10,
                     descriptionHtml           : 'names entered and edited as scientific names',
                     rdfId                     : 'scientific',
                     maxParentsAllowed         : 1,
                     minParentsRequired        : 1,
                     parent1HelpText           : "ordinary - restricted by rank, or unranked if unranked",
                     parent2HelpText           : null,
                     requiresFamily            : true,
                     requiresHigherRankedParent: true,
                     requiresNameElement       : true,
                     takesAuthorOnly           : false,
                     takesAuthors              : true,
                     takesCultivarScopedParent : false,
                     takesHybridScopedParent   : false,
                     takesNameElement          : true,
                     takesVerbatimRank         : true],

                    [name                      : 'cultivar hybrid',
                     sortOrder                 : 60,
                     descriptionHtml           : 'names entered and edited as cultivar hybrid names',
                     rdfId                     : 'NULL',
                     maxParentsAllowed         : 2,
                     minParentsRequired        : 2,
                     parent1HelpText           : "cultivar - genus and below, or unranked if unranked",
                     parent2HelpText           : "cultivar - genus and below, or unranked if unranked",
                     requiresFamily            : true,
                     requiresHigherRankedParent: false,
                     requiresNameElement       : true,
                     takesAuthorOnly           : false,
                     takesAuthors              : false,
                     takesCultivarScopedParent : true,
                     takesHybridScopedParent   : false,
                     takesNameElement          : true,
                     takesVerbatimRank         : true],

                    [name                      : 'other',
                     sortOrder                 : 70,
                     descriptionHtml           : 'names entered and edited as other names',
                     rdfId                     : 'NULL',
                     maxParentsAllowed         : 0,
                     minParentsRequired        : 0,
                     parent1HelpText           : "ordinary - restricted by rank, or unranked if unranked",
                     parent2HelpText           : null,
                     requiresFamily            : false,
                     requiresHigherRankedParent: false,
                     requiresNameElement       : true,
                     takesAuthorOnly           : false,
                     takesAuthors              : false,
                     takesCultivarScopedParent : true,
                     takesHybridScopedParent   : false,
                     takesNameElement          : true,
                     takesVerbatimRank         : true],

                    [name                      : 'phrase name',
                     sortOrder                 : 20,
                     descriptionHtml           : 'names entered and edited as scientific phrase names',
                     rdfId                     : 'NULL',
                     maxParentsAllowed         : 1,
                     minParentsRequired        : 1,
                     parent1HelpText           : "ordinary - restricted by rank, or unranked if unranked",
                     parent2HelpText           : null,
                     requiresFamily            : true,
                     requiresHigherRankedParent: false,
                     requiresNameElement       : false,
                     takesAuthorOnly           : true,
                     takesAuthors              : false,
                     takesCultivarScopedParent : false,
                     takesHybridScopedParent   : false,
                     takesNameElement          : true,
                     takesVerbatimRank         : false],

                    [name                      : 'scientific hybrid formula',
                     sortOrder                 : 30,
                     descriptionHtml           : 'names entered and edited as scientific hybrid formulae',
                     rdfId                     : 'NULL',
                     maxParentsAllowed         : 2,
                     minParentsRequired        : 2,
                     parent1HelpText           : 'hybrid - species and below or unranked if unranked',
                     parent2HelpText           : 'hybrid - species and below or unranked if unranked',
                     requiresFamily            : true,
                     requiresHigherRankedParent: false,
                     requiresNameElement       : false,
                     takesAuthorOnly           : false,
                     takesAuthors              : false,
                     takesCultivarScopedParent : false,
                     takesHybridScopedParent   : true,
                     takesNameElement          : false,
                     takesVerbatimRank         : true],

                    [name                      : 'scientific hybrid formula unknown 2nd parent',
                     sortOrder                 : 40,
                     descriptionHtml           : 'names entered and edited as scientific hybrid formulae with unknown 2nd parent',
                     rdfId                     : 'NULL',
                     maxParentsAllowed         : 1,
                     minParentsRequired        : 1,
                     parent1HelpText           : 'hybrid - species and below or unranked if unranked',
                     parent2HelpText           : null,
                     requiresFamily            : true,
                     requiresHigherRankedParent: false,
                     requiresNameElement       : false,
                     takesAuthorOnly           : false,
                     takesAuthors              : false,
                     takesCultivarScopedParent : true,
                     takesHybridScopedParent   : true,
                     takesNameElement          : false,
                     takesVerbatimRank         : true]
            ].each { data ->
                NameCategory nameCategory = new NameCategory(data)
                nameCategory.save()
            }
            NameCategory.withSession { s -> s.flush() }
        }
        return "Added name category records"

    }

    static String setUpNameTypes() {
        Map<String, NameCategory> nameCategories = [:]
        NameCategory.listOrderById().each { NameCategory nameCategory ->
            nameCategories.put(nameCategory.name, nameCategory)
        }

        Map<String, NameGroup> nameGroups = [:]
        NameGroup.listOrderById().each { NameGroup nameGroup ->
            nameGroups.put(nameGroup.name, nameGroup)
        }

        NameType.withTransaction { status ->
            [
                    [scientific: false, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 1, name: '[default]', nameGroup: '[n/a]', nameCategory: 'other'],
                    [scientific: false, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 2, name: '[unknown]', nameGroup: '[n/a]', nameCategory: 'other'],
                    [scientific: false, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 3, name: '[n/a]', nameGroup: '[n/a]', nameCategory: 'other'],
                    [scientific: true, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 4,  name: 'scientific', nameGroup: 'botanical', nameCategory: 'scientific'],
                    [scientific: true, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 5,  name: 'sanctioned', nameGroup: 'botanical', nameCategory: 'scientific'],
                    [scientific: true, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 6,  name: 'phrase name', nameGroup: 'botanical', nameCategory: 'phrase name'],
                    [scientific: true, cultivar: false, formula: true, hybrid: true, autonym: false, connector: 'x', sortOrder: 7,   name: 'hybrid formula parents known', nameGroup: 'botanical', nameCategory: 'scientific hybrid formula'],
                    [scientific: true, cultivar: false, formula: true, hybrid: true, autonym: false, connector: 'x', sortOrder: 8,   name: 'hybrid formula unknown 2nd parent', nameGroup: 'botanical', nameCategory: 'scientific hybrid formula unknown 2nd parent'],
                    [scientific: true, cultivar: false, formula: false, hybrid: true, autonym: false, connector: 'x', sortOrder: 9,  name: 'named hybrid', nameGroup: 'botanical', nameCategory: 'scientific'],
                    [scientific: true, cultivar: false, formula: false, hybrid: true, autonym: true, connector: 'x', sortOrder: 10,  name: 'named hybrid autonym', nameGroup: 'botanical', nameCategory: 'scientific'],
                    [scientific: true, cultivar: false, formula: false, hybrid: true, autonym: true, connector: 'x', sortOrder: 11,  name: 'hybrid autonym', nameGroup: 'botanical', nameCategory: 'scientific hybrid formula'],
                    [scientific: true, cultivar: false, formula: true, hybrid: true, autonym: false, connector: '-', sortOrder: 12,  name: 'intergrade', nameGroup: 'botanical', nameCategory: 'scientific hybrid formula'],
                    [scientific: true, cultivar: false, formula: false, hybrid: false, autonym: true, connector: '', sortOrder: 13,  name: 'autonym', nameGroup: 'botanical', nameCategory: 'scientific'],

                    [scientific: false, cultivar: true, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 16, name: 'cultivar', nameGroup: 'botanical', nameCategory: 'cultivar'],
                    [scientific: false, cultivar: true, formula: false, hybrid: true, autonym: false, connector: '', sortOrder: 17,  name: 'cultivar hybrid', nameGroup: 'botanical', nameCategory: 'cultivar hybrid'],
                    [scientific: false, cultivar: true, formula: true, hybrid: true, autonym: false, connector: '', sortOrder: 18,   name: 'cultivar hybrid formula', nameGroup: 'botanical', nameCategory: 'scientific hybrid formula'],
                    [scientific: false, cultivar: true, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 19, name: 'ACRA', nameGroup: 'botanical', nameCategory: 'cultivar'],
                    [scientific: false, cultivar: true, formula: false, hybrid: true, autonym: false, connector: '', sortOrder: 20,  name: 'ACRA hybrid', nameGroup: 'botanical', nameCategory: 'cultivar hybrid'],
                    [scientific: false, cultivar: true, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 21, name: 'PBR', nameGroup: 'botanical', nameCategory: 'cultivar'],
                    [scientific: false, cultivar: true, formula: false, hybrid: true, autonym: false, connector: '', sortOrder: 22,  name: 'PBR hybrid', nameGroup: 'botanical', nameCategory: 'cultivar hybrid'],
                    [scientific: false, cultivar: true, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 23, name: 'trade', nameGroup: 'botanical', nameCategory: 'cultivar'],
                    [scientific: false, cultivar: true, formula: false, hybrid: true, autonym: false, connector: '', sortOrder: 24,  name: 'trade hybrid', nameGroup: 'botanical', nameCategory: 'cultivar hybrid'],
                    [scientific: false, cultivar: true, formula: true, hybrid: false, autonym: false, connector: '+', sortOrder: 25, name: 'graft / chimera', nameGroup: 'botanical', nameCategory: 'scientific hybrid formula'],
                    [scientific: false, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 26, name: 'informal', nameGroup: 'botanical', nameCategory: 'other'],
                    [scientific: false, cultivar: false, formula: false, hybrid: false, autonym: false, connector: '', sortOrder: 15, name: 'common', nameGroup: 'botanical', nameCategory: 'other'],
            ].each { data ->
                data.nameCategory = nameCategories[data.nameCategory]
                data.nameGroup = nameGroups[data.nameGroup]
                NameType nameType = new NameType(data)
                nameType.save()
            }
            NameType.withSession { s -> s.flush() }
        }
        return "Added name type records"

    }

    static String setUpNameStatus() {

        Map<String, NameGroup> nameGroups = [:]
        NameGroup.listOrderById().each { NameGroup nameGroup ->
            nameGroups.put(nameGroup.name, nameGroup)
        }

        NameStatus.withTransaction { status ->
            [
                    [nomIlleg: false, nomInval: false, name: '[default]', nameGroup: '[n/a]'],
                    [nomIlleg: false, nomInval: false, name: '[unknown]', nameGroup: '[n/a]'],
                    [nomIlleg: false, nomInval: false, name: '[n/a]', nameGroup: '[n/a]'],
                    [nomIlleg: false, nomInval: false, name: '[deleted]', nameGroup: '[n/a]'],
                    [nomIlleg: false, nomInval: false, name: 'legitimate', nameGroup: 'botanical'],

                    [nomIlleg: false, nomInval: true, name: 'nom. inval.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., pro syn.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. nud.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. subnud.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., pro. syn.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. ambig.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. confus.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. prov.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. alt.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., nom. dub.', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., opera utique oppressa', nameGroup: 'botanical', parent: 'nom. inval.'],
                    [nomIlleg: false, nomInval: true, name: 'nom. inval., tautonym', nameGroup: 'botanical', parent: 'nom. inval.'],

                    [nomIlleg: true, nomInval: false, name: 'nom. illeg.', nameGroup: 'botanical'],
                    [nomIlleg: true, nomInval: false, name: 'nom. illeg., nom. superfl.', nameGroup: 'botanical', parent: 'nom. illeg.'],
                    [nomIlleg: true, nomInval: false, name: 'nom. illeg., nom. rej.', nameGroup: 'botanical', parent: 'nom. illeg.'],

                    [nomIlleg: false, nomInval: false, name: 'isonym', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. superfl.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. rej.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. alt.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. cult.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. cons.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. cons., orth. cons.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. cons., nom. alt.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. cult., nom. alt.', nameGroup: 'botanical', parent: 'nom. cult.'],
                    [nomIlleg: false, nomInval: false, name: 'nom. et typ. cons.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nom. et orth. cons.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'nomina utique rejicienda', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'typ. cons.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'orth. var.', nameGroup: 'botanical'],
                    [nomIlleg: false, nomInval: false, name: 'orth. cons.', nameGroup: 'botanical']
            ].each { data ->
                data.nameGroup = nameGroups[data.nameGroup]
                if (data.parent) {
                    data.parent = NameStatus.findByName(data.parent)
                }
                NameStatus nameStatus = new NameStatus(data)
                nameStatus.save()
            }
            NameStatus.withSession { s -> s.flush() }
        }
        return "Added name status records"
    }

    static String setUpNameRanks() {
        NameGroup botanicalNameGroup = NameGroup.findByName('botanical')

        NameRank.withTransaction { status ->
            [
                    [hasParent: false, major: true, sortOrder: 10, visibleInName: false, name: 'Regnum', displayName: 'Regnum', abbrev: 'reg.', parentRank: ''],
                    [hasParent: false, major: true, sortOrder: 20, visibleInName: false, name: 'Division', displayName: 'Division', abbrev: 'div.', parentRank: ''],
                    [hasParent: false, major: true, sortOrder: 30, visibleInName: false, name: 'Classis', displayName: 'Classis', abbrev: 'cl.', parentRank: ''],
                    [hasParent: false, major: false, sortOrder: 40, visibleInName: false, name: 'Subclassis', displayName: 'Subclassis', abbrev: 'subcl.', parentRank: ''],
                    [hasParent: false, major: false, sortOrder: 50, visibleInName: false, name: 'Superordo', displayName: 'Superordo', abbrev: 'superordo', parentRank: ''],
                    [hasParent: false, major: true, sortOrder: 60, visibleInName: false, name: 'Ordo', displayName: 'Ordo', abbrev: 'ordo', parentRank: ''],
                    [hasParent: false, major: false, sortOrder: 70, visibleInName: false, name: 'Subordo', displayName: 'Subordo', abbrev: 'subordo', parentRank: ''],
                    [hasParent: false, major: true, sortOrder: 80, visibleInName: false, name: 'Familia', displayName: 'Familia', abbrev: 'fam.', parentRank: ''],
                    [hasParent: true, major: false, sortOrder: 90, visibleInName: true, name: 'Subfamilia', displayName: 'Subfamilia', abbrev: 'subfam.', parentRank: 'Familia'],
                    [hasParent: true, major: true, sortOrder: 100, visibleInName: true, name: 'Tribus', displayName: 'Tribus', abbrev: 'trib.', parentRank: 'Familia'],
                    [hasParent: true, major: false, sortOrder: 110, visibleInName: true, name: 'Subtribus', displayName: 'Subtribus', abbrev: 'subtrib.', parentRank: 'Familia'],
                    [hasParent: false, major: true, sortOrder: 120, visibleInName: false, name: 'Genus', displayName: 'Genus', abbrev: 'gen.', parentRank: ''],
                    [hasParent: true, major: false, sortOrder: 130, visibleInName: true, name: 'Subgenus', displayName: 'Subgenus', abbrev: 'subg.', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 140, visibleInName: true, name: 'Sectio', displayName: 'Sectio', abbrev: 'sect.', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 150, visibleInName: true, name: 'Subsectio', displayName: 'Subsectio', abbrev: 'subsect.', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 160, visibleInName: true, name: 'Series', displayName: 'Series', abbrev: 'ser.', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 170, visibleInName: true, name: 'Subseries', displayName: 'Subseries', abbrev: 'subser.', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 180, visibleInName: true, name: 'Superspecies', displayName: 'Superspecies', abbrev: 'supersp.', parentRank: 'Genus'],
                    [hasParent: true, major: true, sortOrder: 190, visibleInName: false, name: 'Species', displayName: 'Species', abbrev: 'sp.', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 200, visibleInName: true, name: 'Subspecies', displayName: 'Subspecies', abbrev: 'subsp.', parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 210, visibleInName: true, name: 'Nothovarietas', displayName: 'Nothovarietas', abbrev: 'nothovar.', parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 210, visibleInName: true, name: 'Varietas', displayName: 'Varietas', abbrev: 'var.', parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 220, visibleInName: true, name: 'Subvarietas', displayName: 'Subvarietas', abbrev: 'subvar.', parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 230, visibleInName: true, name: 'Forma', displayName: 'Forma', abbrev: 'f.', parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 240, visibleInName: true, name: 'Subforma', displayName: 'Subforma', abbrev: 'subf.', parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 250, visibleInName: false, name: 'form taxon', displayName: 'form taxon', abbrev: 'form taxon', deprecated: true, parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 260, visibleInName: false, name: 'morphological var.', displayName: 'morphological var.', abbrev: 'morph.', deprecated: true, parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 270, visibleInName: false, name: 'nothomorph.', displayName: 'nothomorph.', abbrev: 'nothomorph', deprecated: true, parentRank: 'Species'],
                    [hasParent: true, major: false, sortOrder: 500, visibleInName: true, name: '[unranked]', displayName: '[unranked]', abbrev: '[unranked]', parentRank: ''],
                    [hasParent: true, major: false, sortOrder: 500, visibleInName: true, name: '[infrafamily]', displayName: '[infrafamily]', abbrev: '[infrafamily]', parentRank: 'Familia'],
                    [hasParent: true, major: false, sortOrder: 500, visibleInName: true, name: '[infragenus]', displayName: '[infragenus]', abbrev: '[infragenus]', parentRank: 'Genus'],
                    [hasParent: true, major: false, sortOrder: 500, visibleInName: true, name: '[infraspecies]', displayName: '[infraspecies]', abbrev: '[infrasp.]', parentRank: 'Species'],
                    [hasParent: false, major: false, sortOrder: 500, visibleInName: false, name: '[n/a]', displayName: '[n/a]', abbrev: '[n/a]', parentRank: ''],
                    [hasParent: false, major: false, sortOrder: 500, visibleInName: false, name: '[unknown]', displayName: '[unknown]', abbrev: '[unknown]', deprecated: true, parentRank: '']

            ].each { values ->
                if (values.parentRank) {
                    values.parentRank = NameRank.findByName(values.parentRank)
                }
                NameRank nameRank = new NameRank(values)
                nameRank.nameGroup = botanicalNameGroup
                nameRank.save()
            }
            NameRank.withSession { s -> s.flush() }
        }

        return "Added name rank records"
    }

    static Name makeName(String element, String rank, Name parent, Namespace namespace) {
        new Name(
                nameType: NameType.findByName('scientific'),
                nameStatus: NameStatus.findByName('legitimate'),
                nameRank: NameRank.findByName(rank),
                createdBy: 'tester',
                updatedBy: 'tester',
                createdAt: new Timestamp(System.currentTimeMillis()),
                updatedAt: new Timestamp(System.currentTimeMillis()),
                nameElement: element,
                parent: parent,
                namespace: namespace
        )
    }

}
