package au.org.biodiversity.nsl

/**
 * User: pmcneil
 * Date: 7/09/17
 *
 */
class TargetParam {

    private final String nameSpace
    private final String objectType
    private final Long idNumber
    private final Long versionNumber
    private final String uri

    TargetParam(Name name, String nameSpace) {
        this.objectType = 'name'
        this.idNumber = name.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = null
    }

    TargetParam(Author author, String nameSpace) {
        this.objectType = 'author'
        this.idNumber = author.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = null
    }

    TargetParam(Instance instance, String nameSpace) {
        this.objectType = 'instance'
        this.idNumber = instance.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = null
    }

    TargetParam(Reference reference, String nameSpace) {
        this.objectType = 'reference'
        this.idNumber = reference.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = null
    }

    TargetParam(InstanceNote instanceNote, String nameSpace) {
        this.objectType = 'instanceNote'
        this.idNumber = instanceNote.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = null
    }

    TargetParam(Tree tree, String nameSpace) {
        this.objectType = 'tree'
        this.idNumber = tree.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = "tree/namespace/$tree.name"
    }

    TargetParam(TreeVersion treeVersion, String nameSpace) {
        this.objectType = 'treeVersion'
        this.idNumber = treeVersion.id
        this.versionNumber = null
        this.nameSpace = nameSpace
        uri = "tree/$treeVersion.id"
    }

    TargetParam(TreeVersionElement treeVersionElement, String nameSpace) {
        this.objectType = 'treeElement'
        this.idNumber = treeVersionElement.treeElement.id
        this.versionNumber = treeVersionElement.treeVersion.id
        this.nameSpace = nameSpace
        uri = "tree/${this.versionNumber}/${this.idNumber}"
    }

    Map paramMap() {
        [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber, versionNumber: versionNumber, uri: uri]
    }

    Map paramMap(String nameSpaceKey, String objectTypeKey, String idNumberKey, String versionNumberKey) {
        [(nameSpaceKey): nameSpace, (objectTypeKey): objectType, (idNumberKey): idNumber, (versionNumberKey): versionNumber]
    }

    String identityParamString() {
        return "nameSpace=${nameSpace}&objectType=${objectType}&idNumber=${idNumber}&versionNumber=${versionNumber}"
    }

    String addIdentityParamString() {
        return "nameSpace=${nameSpace}&objectType=${objectType}&idNumber=${idNumber}&versionNumber=${versionNumber}&uri=${uri}"
    }

}
