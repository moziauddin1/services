package au.org.biodiversity.nsl.api

import au.org.biodiversity.nsl.*
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.validation.ValidationException
import org.apache.commons.lang.NotImplementedException
import org.apache.shiro.authz.AuthorizationException
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(TreeApiController)
@TestMixin(DomainClassUnitTestMixin)
@Mock([Tree, TreeVersion, TreeElement])
class TreeApiControllerSpec extends Specification {

    def treeService = Mock(TreeService)

    def setup() {
        controller.jsonRendererService = new JsonRendererService()
        controller.jsonRendererService.registerObjectMashallers()
        /*
        Note treeService authorizeTreeOperation throws an unauthorized exception if not authorized. The mock returns
        null by default for methods that aren't mocked out, so you won't get an AuthorizationException unless you mock
        authorizeTreeOperation to throw one. e.g.

          1 * treeService.authorizeTreeOperation(tree) >> {Tree tree1 ->
            throw new AuthorizationException('Black Hatz')
          }

         */
        controller.treeService = treeService
    }

    def cleanup() {
    }

    void "test creating a tree"() {
        given:
        String treeName = 'aTree'
        String groupName = 'aGroup'
        Long refId = null
        request.method = 'PUT'

        when: 'I create a new tree'
        def data = jsonCall { controller.createTree(treeName, groupName, refId) }

        then: 'I get an OK response'
        1 * treeService.createNewTree(treeName, groupName, refId) >> new Tree(name: treeName, groupName: groupName, referenceId: refId)
        response.status == 200
        data.ok == true
        data.payload.tree.name == treeName
        data.payload.tree.groupName == groupName

        when: 'I create a new tree without a groupName'
        data = jsonCall { controller.createTree(treeName, null, refId) }

        then: 'I get a fail'
        response.status == 400
        data.ok == false
        data.error == 'Group Name not supplied. You must supply Group Name.'

        when: 'I create a new tree without a treeName'
        data = jsonCall { controller.createTree(null, groupName, refId) }

        then: 'I get a fail'
        response.status == 400
        data.ok == false
        data.error == 'Tree Name not supplied. You must supply Tree Name.'
    }

    void "test creating a tree that exists"() {
        given:
        String name = 'aTree'
        String group = 'aGroup'
        Long ref = null
        request.method = 'PUT'
        treeService.createNewTree(name, group, ref) >> { String treeName, String groupName, Long refId ->
            throw new ObjectExistsException('Object exists')
        }

        when: 'I create a new tree with a name that exists'
        def data = jsonCall { controller.createTree(name, group, ref) }

        then: 'I get a CONFLICT fail response with object exists'
        response.status == 409
        data.ok == false
        data.error == 'Object exists'
    }

    void "test creating a tree that clashes with db validation"() {
        given:
        String name = 'aTree'
        String group = 'aGroup'
        Long ref = null
        request.method = 'PUT'
        treeService.createNewTree(name, group, ref) >> { String treeName, String groupName, Long refId ->
            Errors errors = mockErrors()
            throw new ValidationException('validation error', errors)
        }

        when: 'I create a new tree with a name that exists'
        def data = jsonCall { controller.createTree(name, group, ref) }

        then: 'I get an Fail response with object exists'
        response.status == 500
        data.ok == false
        data.error == 'validation error:\n'
    }

    void "test editing a tree"() {
        given:
        Tree tree = new Tree(name: 'aTree', groupName: 'aGroup').save()
        Long treeId = tree.id
        request.method = 'POST'
        request.json = "{\"id\": $treeId, \"name\": \"A New Name\", \"referenceId\": 123456, \"groupName\": \"aGroup\"}".toString()

        expect:
        tree
        treeId
        tree.name == 'aTree'

        when: 'I change the name of a tree'
        def data = jsonCall { controller.editTree() }

        then: 'It works'
        1 * treeService.editTree(tree, 'A New Name', 'aGroup', 123456) >> { Tree tree2, String name, String group, Long refId ->
            tree2.name = name
            tree2.referenceId = refId
            tree2.groupName = group
            tree2.save()
            return tree2
        }
        response.status == 200
        data.ok
        data.payload.tree
        data.payload.tree.name == 'A New Name'
        data.payload.tree.groupName == 'aGroup'
        data.payload.tree.referenceId == 123456

        when: 'Im not authorized'
        data = jsonCall { controller.editTree() }

        then: 'I get a Authorization exception'
        1 * treeService.authorizeTreeOperation(tree) >> { Tree tree1 ->
            throw new AuthorizationException('Black Hatz')
        }
        response.status == 403
        data.ok == false
        data.error == 'Black Hatz'

    }

    void "test validation error editing a tree"() {
        given:
        Tree tree = new Tree(name: 'aTree', groupName: 'aGroup').save()
        Long treeId = tree.id
        request.method = 'POST'
        request.json = "{\"id\": $treeId, \"name\": \"A New Name\", \"referenceId\": 123456, \"groupName\": \"aGroup\"}".toString()
        treeService.editTree(_, _, _, _) >> { Tree tree2, String name, String group, Long refId ->
            Errors errors = mockErrors()
            throw new ValidationException('validation error', errors)
        }

        when: 'I do something gets a validation exception'
        def data = jsonCall { controller.editTree() }

        then: 'It gives a validation error'
        response.status == 500
        data.ok == false
        data.error == 'validation error:\n'
    }

    void "test non existent tree editing a tree"() {
        given:
        request.method = 'POST'
        request.json = "{\"id\": 23, \"name\": \"A New Name\", \"referenceId\": 123456, \"groupName\": \"aGroup\"}".toString()
        treeService.editTree(_, _, _, _) >> { Tree tree2, String name, String group, Long refId ->
            fail('shouldn\'t call edit tree')
        }

        when: 'I edit a non existent tree'
        def data = jsonCall { controller.editTree() }

        then: 'It gives a not found error'
        response.status == 404
        data.ok == false
        data.error == 'Tree with id: 23 not found.'
    }

    void "test creating a new version"() {
        given:
        Tree tree = new Tree(name: 'aTree', groupName: 'aGroup').save()
        request.method = 'PUT'

        expect:
        tree

        when: 'I create a new version for a tree with no version'
        def data = jsonCall { controller.createTreeVersion(tree, null, 'my draft tree', false) }

        then: 'I should get an OK response'
        1 * treeService.createTreeVersion(_, _, _) >> { Tree tree1, TreeVersion version, String draftName ->
            TreeVersion v = new TreeVersion(tree: tree1, draftName: draftName)
            v.save()
            return v
        }
        response.status == 200
        data.ok == true
        data.payload.treeVersion

        when: 'I forget something like draftName'
        data = jsonCall { controller.createTreeVersion(tree, null, '', false) }

        then: 'I get a bad argument response'
        1 * treeService.createTreeVersion(_, _, _) >> { Tree tree1, TreeVersion version, String draftName ->
            throw new BadArgumentsException('naughty')
        }
        response.status == 400
        data.ok == false
        data.error == 'naughty'

        when: 'Im not authorized'
        data = jsonCall { controller.createTreeVersion(tree, null, '', false) }

        then: 'I get a Authorization exception'
        1 * treeService.authorizeTreeOperation(tree) >> { Tree tree1 ->
            throw new AuthorizationException('Black Hatz')
        }
        response.status == 403
        data.ok == false
        data.error == 'Black Hatz'
    }

    void "test validating a version"() {
        given:
        Tree tree = new Tree(name: 'aTree', groupName: 'aGroup').save()
        TreeVersion version = new TreeVersion(tree: tree, draftName: 'draft tree')
        request.method = 'GET'

        expect:
        tree
        version

        when: 'I validate this version'
        def data = jsonCall { controller.validateTreeVersion(version) }

        then: 'I should get an OK response'
        1 * treeService.validateTreeVersion(_) >> { TreeVersion version1 ->
            return version1
        }
        response.status == 200
        data.ok == true
        data.payload.treeVersion

        when: 'tree service hasnt implemented validation'
        data = jsonCall { controller.validateTreeVersion(version) }

        then: 'I get a not implemented response'
        1 * treeService.validateTreeVersion(_) >> { TreeVersion version1 ->
            throw new NotImplementedException('oops')
        }
        response.status == 501
        data.ok == false
        data.error == 'oops'
    }

    private def jsonCall(Closure action) {
        response.reset()
        response.format = 'json'
        action()
        println "response: ${response.text}"
        return response.json
    }

    private static Errors mockErrors() {
        new Errors() {
            @Override
            String getObjectName() {
                return null
            }

            @Override
            void setNestedPath(String nestedPath) {}

            @Override
            String getNestedPath() {}

            @Override
            void pushNestedPath(String subPath) {}

            @Override
            void popNestedPath() throws IllegalStateException {}

            @Override
            void reject(String errorCode) {}

            @Override
            void reject(String errorCode, String defaultMessage) {}

            @Override
            void reject(String errorCode, Object[] errorArgs, String defaultMessage) {}

            @Override
            void rejectValue(String field, String errorCode) {}

            @Override
            void rejectValue(String field, String errorCode, String defaultMessage) {}

            @Override
            void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {}

            @Override
            void addAllErrors(Errors errors) {}

            @Override
            boolean hasErrors() {
                return false
            }

            @Override
            int getErrorCount() {
                return 0
            }

            @Override
            List<ObjectError> getAllErrors() {
                return []
            }

            @Override
            boolean hasGlobalErrors() {
                return false
            }

            @Override
            int getGlobalErrorCount() {
                return 0
            }

            @Override
            List<ObjectError> getGlobalErrors() {
                return []
            }

            @Override
            ObjectError getGlobalError() {
                return null
            }

            @Override
            boolean hasFieldErrors() {
                return false
            }

            @Override
            int getFieldErrorCount() {
                return 0
            }

            @Override
            List<FieldError> getFieldErrors() {
                return []
            }

            @Override
            FieldError getFieldError() {
                return null
            }

            @Override
            boolean hasFieldErrors(String field) {
                return false
            }

            @Override
            int getFieldErrorCount(String field) {
                return 0
            }

            @Override
            List<FieldError> getFieldErrors(String field) {
                return []
            }

            @Override
            FieldError getFieldError(String field) {
                return null
            }

            @Override
            Object getFieldValue(String field) {
                return null
            }

            @Override
            Class<?> getFieldType(String field) {
                return null
            }
        }
    }
}
