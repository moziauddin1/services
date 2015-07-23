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
        NameTreePath nameTreePath = new NameTreePath(path: '0.231182.54697.211565.208772.208959.208797.54408.74428.93305.93310')

        then:
        nameTreePath.pathIds() == [231182,54697,211565,208772,208959,208797,54408,74428,93305,93310]
    }
}
