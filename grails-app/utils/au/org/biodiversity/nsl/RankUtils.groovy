package au.org.biodiversity.nsl
/**
 * User: pmcneil
 * Date: 3/12/14
 *
 */
class RankUtils {

    private static Map rankOrder = [Regnum              : 10,
                                    Division            : 20,
                                    Classis             : 30,
                                    Subclassis          : 40,
                                    Superordo           : 50,
                                    Ordo                : 60,
                                    Subordo             : 70,
                                    Familia             : 80,
                                    Subfamilia          : 90,
                                    Tribus              : 100,
                                    Subtribus           : 110,
                                    Genus               : 120,
                                    Subgenus            : 130,
                                    Sectio              : 140,
                                    Subsectio           : 150,
                                    Series              : 160,
                                    Subseries           : 170,
                                    Superspecies        : 180,
                                    Species             : 190,
                                    Subspecies          : 200,
                                    Nothovarietas       : 210,
                                    Varietas            : 210,
                                    Subvarietas         : 220,
                                    Forma               : 230,
                                    Subforma            : 240,
                                    'form taxon'        : 250,
                                    'morphological var.': 260,
                                    'nothomorph.'       : 270,
                                    '[unranked]'        : 500,
                                    '[n/a]'             : 500,
                                    '[unknown]'         : 500]

    public static Boolean rankHigherThan(NameRank rank, String rankName) {
        return rankOrder[rankName] > rank.sortOrder
    }

    public static Boolean rankLowerThan(NameRank rank, String rankName) {
        return  rank.sortOrder < 500 && rankOrder[rankName] < rank.sortOrder
    }

    public static Boolean nameAtRankOrHigher(Name name, String rankName) {
        return rankOrder[rankName] >= name.nameRank.sortOrder
    }

    public static Boolean nameAtRankOrLower(Name name, String rankName) {
        return rankOrder[rankName] <= name.nameRank.sortOrder
    }

    public static Integer getRankOrder(String rankName) {
        rankOrder[rankName]
    }

    public static Name getParentOfRank(Name name, String rank) {
        getParentOfRank(name, rank, 'APNI')
    }

    public static Name getParentOfRank(Name name, String rank, String treeName) {
        NameTreePath nameTreePath = NameTreePathService.findCurrentNameTreePath(name, treeName)
        if (nameTreePath) {
            List<Name> namesInBranch = nameTreePath.namesInBranch()
            return namesInBranch.reverse().find { (it && it.nameRank.name == rank) }
        }
    }
}
