package au.org.biodiversity.nsl.tree

import au.org.biodiversity.nsl.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(UserWorkspaceManagerService)
@Mock([Name, Namespace, NameGroup, NameCategory, NameType, NameStatus, NameRank])
class UserWorkspaceManagerServiceSpec extends Specification {

    Namespace testNameSpace

    def setup() {
        TestUte.setUpNameInfrastructure()
        testNameSpace = new Namespace(name: 'TEST')
        testNameSpace.save()

    }

    def cleanup() {
    }

    // Requirement:
    // If the name is being placed under a name that is is generic or below then,
    // then the common part of the names must match unless the name it is being placed under it is an excluded name.
    /**
     * Binomial/tri-nomial/hybrid names must be a child of a name with the same parent part.
     *
     * More specifically.
     * If the name being placed is below genus, then it needs to be placed under a genus.
     * If the name is placed under a sub genetic name then the sub genetic name must have the same parent genetic name part
     * as the name being placed.
     *
     * If the name being placed is below species, then it needs to be placed under a species.
     * If the name is placed under a sub species name then the sub species name must have the same parent species name part
     * as the name being placed.
     */
    void "test checkNameCompatibility"() {
        given:
        Name family = TestUte.makeName('family', 'Familia', null, testNameSpace).save()
        Name genus1 = TestUte.makeName('genus1', 'Genus', family, testNameSpace).save()
        Name genus2 = TestUte.makeName('genus2', 'Genus', family, testNameSpace).save()
        Name species1 = TestUte.makeName('sp1', 'Species', genus1, testNameSpace).save()
        Name species2 = TestUte.makeName('sp2', 'Species', genus2, testNameSpace).save()
        Name subsp1 = TestUte.makeName('subsp1', 'Subspecies', species1, testNameSpace).save()
        Name subsp2 = TestUte.makeName('subsp2', 'Subspecies', species2, testNameSpace).save()
        Name var1 = TestUte.makeName('var1', 'Varietas', species1, testNameSpace).save()
        Name var2 = TestUte.makeName('var2', 'Varietas', species2, testNameSpace).save()

        println Name.list()

        expect:
        Name.count() == 9

        when: "$subName is placed under $superName"
        List errors = []
        Name sup = Name.findByNameElement(superName as String)
        Name sub = Name.findByNameElement(subName as String)
        println("$superName, $subName")
        Boolean yes = service.isNameCompatible(sup, sub)

        then:
        yes == isCompatible

        where:

        superName | subName | isCompatible
        "genus1"  | "sp1"   | true
        "sp1"     | "var1"  | true
        "genus2"  | "sp1"   | false //because the parent name part should be genus1
        "subsp1"  | "var1"  | true
        "subsp2"  | "var1"  | false //because subsp2 is under sp2 and var1 is under sp1
        "genus1"  | "var1"  | false //because var1 should be under a *species* or below
        "family"  | "genus1"| true  //we don't care about sub names at genus or above
        "family"  | "sp1"   | false //super name should be at or below genus
        "var1"    | "genus1"| false //super is below sub
    }
}
