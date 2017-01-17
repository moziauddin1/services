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

@TestFor(SearchService)
class NameTreePathSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test ids are correctly extracted"() {
        when:
        NameTreePath nameTreePath = new NameTreePath(nameIdPath: '231182.54697.211565.208772.208959.208797.54408.74428.93305.93310')

        then:
        nameTreePath.namePathIds() == [231182,54697,211565,208772,208959,208797,54408,74428,93305,93310] as List<Long>
    }
}
