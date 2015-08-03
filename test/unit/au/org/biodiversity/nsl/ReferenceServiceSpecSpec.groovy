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
