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

package services

import au.org.biodiversity.nsl.*
import spock.lang.Specification

import static org.springframework.http.HttpStatus.OK

/**
 *
 */
class ClassificationServiceSpec extends Specification {

    def classificationService

    def setup() {
    }

    def cleanup() {
    }

    void "Find non draft Node in name tree"() {
        when:
        Arrangement tree = Arrangement.findByLabel('APNI')
        Name name = Name.get(8020746)
        Node node = classificationService.findCurrentNonDraftNslName(tree, name)

        then: "A node with id 8020783 is returned"
        node != null
        node.id == 8020783l
        node.instance == null
    }


}
