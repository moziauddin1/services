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

    void "test find synonyms of this Instance in a tree"() {
        when: "I check the data exists"
        Arrangement apc = Arrangement.findByLabel('APC')

        println apc

        then: "I find it and can continue"
        apc

        when: "we try to place ficus virens Aiton sensu CHAH 2005"
        Instance ficusVirensCHAH2005 = Instance.get(598372)
        println ficusVirensCHAH2005
        List<Instance> synonyms = queryService.findSynonymsOfThisInstanceInATree(apc, ficusVirensCHAH2005)

        then: "we find Ficus virens var. sublanceolata"
        synonyms.size() == 1
        synonyms.first().cites.name.fullName == 'Ficus virens var. sublanceolata (Miq.) Corner'
        synonyms.first().citedBy.name.fullName == 'Ficus virens Aiton'
        synonyms.first().instanceType.name == 'taxonomic synonym'

        when: "we try to place ficus virens Aiton sensu CHAH 2014"
        //Ficus virens var. sublanceolata (Miq.) Corner tax synonym of Ficus virens Aiton
        // relationship instance = 835352, Tree instance = 488466
        Instance ficusVirensCHAH2014 = Instance.get(781547)
        synonyms = queryService.findSynonymsOfThisInstanceInATree(apc, ficusVirensCHAH2014)
        println synonyms

        then: "We find nothing"
        synonyms.size() == 0

        when: "I check for Woodwardia on a tree"
        //  Woodwardia Sm.
        // nomenclatural synonym: Doodia R.Br.
        Instance woodwardia = Instance.get(3749729)
        synonyms = queryService.findSynonymsOfThisInstanceInATree(apc, woodwardia)
        println synonyms

        then: "We should find Doodia aspera R.Br."
        synonyms.size() == 1
        synonyms.first().cites.name.fullName == 'Doodia R.Br.'
        synonyms.first().citedBy.name.fullName == 'Woodwardia Sm.'
        synonyms.first().instanceType.name == 'taxonomic synonym'
    }

    void "test find Instances in a Tree that say this is a synonym"() {
        when: "I check the data exists"
        Arrangement apc = Arrangement.findByLabel('APC')
        println apc

        then: "It does and I can continue this test"
        apc

        when: "I check for Woodwardia aspera on a tree"
        //  Woodwardia aspera (R.Br.) Mett.
        // nomenclatural synonym: Doodia aspera R.Br.
        Instance woodwardiaAspera = Instance.get(536794) //comb. nov.
        List<Instance> synonyms = queryService.findInstancesInATreeThatSayThisIsASynonym(apc, woodwardiaAspera)
        println synonyms

        then: "We should not find Doodia aspera R.Br. because it is a nomenclatural synonym"
        synonyms.size() == 0

        when: "I check for Woodwardia on a tree"
        //  Woodwardia Sm.
        // nomenclatural synonym: Doodia R.Br.
        Instance woodwardia = Instance.get(3749729)
        synonyms = queryService.findInstancesInATreeThatSayThisIsASynonym(apc, woodwardia)
        println synonyms

        then: "We should not find Doodia aspera R.Br. because it's instance (578615) doesn't think this is a synonym"
        synonyms.size() == 0

        when: "We try to place Blechnum neohollandicum Christenh instance/apni/751268"
        Instance blechnumNeohollandicum = Instance.get(751268)
        synonyms = queryService.findInstancesInATreeThatSayThisIsASynonym(apc, blechnumNeohollandicum)
        println synonyms

        then: "We should find the taxonomic synonym of Doodia aspera R.Br."
        synonyms.size() == 1
        synonyms.first().cites.name.fullName == 'Blechnum neohollandicum Christenh.'
        synonyms.first().citedBy.name.fullName == 'Doodia aspera R.Br.'
        synonyms.first().instanceType.name == 'taxonomic synonym'


    }

}