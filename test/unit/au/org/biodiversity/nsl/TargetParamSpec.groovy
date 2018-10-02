package au.org.biodiversity.nsl

import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([Name, NameGroup, NameCategory, NameStatus, NameRank, NameType, Namespace])
class TargetParamSpec extends Specification {

    Name doodia

    def setup() {
        Namespace namespace = new Namespace(name: 'test', rfId: 'blah', descriptionHtml: '<p>blah</p>')
        TestUte.setUpNameInfrastructure()
        doodia = TestUte.makeName('Doodia', 'Genus', null, namespace)
        doodia.save()
    }

    def cleanup() {
    }

    void "test target params for a Name"() {
        when: "I create a TargetParam"
        TargetParam targetParam = new TargetParam(doodia, 'blah')

        then: "I can get the right params back"
        targetParam.briefParamMap() == [s:'blah', o:'name', i:1, v:null, u:null]
        targetParam.identityParamString() == 'nameSpace=blah&objectType=name&idNumber=1'
        targetParam.addIdentityParamString() == 'nameSpace=blah&objectType=name&idNumber=1'
        targetParam.paramMap() == [nameSpace:'blah', objectType:'name', idNumber:1, versionNumber:null, uri:null]
        targetParam.paramMap('fromNameSpace',
                'fromObjectType',
                'fromIdNumber',
                'fromVersionNumber') == [fromNameSpace:'blah', fromObjectType:'name', fromIdNumber:1, fromVersionNumber:null]
    }
}
