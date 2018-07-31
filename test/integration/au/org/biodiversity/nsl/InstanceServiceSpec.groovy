package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * User: pmcneil
 * Date: 10/07/18
 *
 */

@TestFor(InstanceService)
class InstanceServiceSpec extends Specification {

    def setup() {

    }

    def "Test Sort Instances order"() {
        when: "CHAH 2011 instance of Hibbertia hirticalyx Toelken https://id.biodiversity.org.au/instance/apni/709624"
        Instance hibbertia = Instance.get(709624)
        List<Instance> sortedSynonyms = service.sortInstances(hibbertia.instancesForCitedBy as List)
        println sortedSynonyms.collect {
            "$it.instanceType.id, $it.instanceType.name $it.id: $it.name.fullName"
        }.join("\n")

        then: "instances will be in this order"
        sortedSynonyms.size() == 18
        sortedSynonyms[0].id == 85105
        sortedSynonyms[1].id == 85039
        sortedSynonyms[2].id == 233303
        sortedSynonyms[3].id == 93815

    }

}
