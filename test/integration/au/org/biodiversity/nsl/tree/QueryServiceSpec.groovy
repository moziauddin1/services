/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL tree services plugin project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl.tree

import au.org.biodiversity.nsl.Arrangement
import au.org.biodiversity.nsl.Instance
import au.org.biodiversity.nsl.Name

import javax.sql.DataSource

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hibernate.SessionFactory
import spock.lang.*

import java.sql.Timestamp

@Mixin(BuildSampleTreeMixin)
class QueryServiceSpec extends Specification {
    DataSource dataSource_nsl
    SessionFactory sessionFactory_nsl
    static final Log log = LogFactory.getLog(QueryServiceSpec.class)

    // fields
    BasicOperationsService basicOperationsService
    QueryService queryService

    void "test getStatistics simple"() {
        when: "we get statistics"
        basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(),
                new Timestamp(System.currentTimeMillis()),
                'TEST', 'test getStatistics simple')

        SomeStuff s1 = makeSampleTree()

        QueryService.Statistics s = queryService.getStatistics(s1.tree)
        println s.dump()

        then: "some statics are returned"
        s
        s.nodesCt == 7
        s.currentNodesCt == 7
        s.typesCt == 2
        s.currentTypesCt == 2
        s.namesCt == 7
        s.currentNamesCt == 7
        s.taxaCt == 7
        s.currentTaxaCt == 7
    }

    void "test getDependencies simple"() {
        when:
        basicOperationsService.newEventTs(TreeTestUtil.getTestNamespace(),
                new Timestamp(System.currentTimeMillis()),
                'TEST', 'test getDependencies simple')

        SomeStuff s1 = makeSampleTree()

        QueryService.Statistics s = queryService.getDependencies(s1.tree)
        println s.dump()

        then:
        s
    }

    void "test find Synonyms of instance in tree"() {
        when: "I look for Doodia"
        List<Name> doodiaAspera = Name.findAllBySimpleName('Doodia aspera')

        println doodiaAspera

        then:
        doodiaAspera.size() == 1

        when: "I check for synonyms on a tree"
        Arrangement apc = Arrangement.findByLabel('APC')
        //  Woodwardia aspera (R.Br.) Mett.
        // nomenclatural synonym: Doodia aspera R.Br.
        Instance woodwardiaAspera = Instance.get(536794) //comb. nov.
        List<Instance> synonyms = queryService.findInstancesHavingSynonymInTree(apc, woodwardiaAspera)
        println synonyms

        then: "We should find Doodia aspera R.Br."
        woodwardiaAspera
        woodwardiaAspera.name.simpleName == 'Woodwardia aspera'
        synonyms.size() == 1
        synonyms.first().name.fullName == 'Woodwardia aspera (R.Br.) Mett.'
        synonyms.first().citedBy.name.fullName == 'Doodia aspera R.Br.'


    }

}