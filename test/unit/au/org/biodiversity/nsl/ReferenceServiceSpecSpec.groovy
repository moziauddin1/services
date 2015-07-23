package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(ReferenceService)
class ReferenceServiceSpecSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test reference string category remove full stop"() {
        when:
        String output = ReferenceStringCategory.removeFullStop(test)

        then:
        output == result

        where:
        test          | result
        "A string."   | "A string"
        "A string..." | "A string"
        "A string"    | "A string"

    }

    void "test reference string category full stop"() {
        when:
        String output = ReferenceStringCategory.fullStop(test)

        then:
        output == result

        where:
        test          | result
        "A string"    | "A string."
        "A string "   | "A string ."
        "A string..." | "A string..."
        "A string."   | "A string."
    }

    void "test reference string category comma"() {
        when:
        String output = ReferenceStringCategory.comma(test)

        then:
        output == result

        where:
        test        | result
        "A string"  | "A string,"
        "A string " | "A string ,"
        "A string," | "A string,"
    }

    void "test null string returns blank string"() {
        when:
        String result = ReferenceStringCategory.withString(null) {
            return 'result should not be this'
        }

        then:
        result == ''
    }

    void "test reference string category clean"() {
        when:
        String output = ReferenceStringCategory.clean(test)

        then:
        output == result

        where:
        test               | result
        "A (str)ing"       | "A string"
        "A st(ri)ng "      | "A string"
        "A (1984) string"  | "A 1984 string"
        "A string"         | "A string"
        "A string (1984 )" | "A string 1984"
    }
}
