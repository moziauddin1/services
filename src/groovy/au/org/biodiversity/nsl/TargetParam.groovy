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

    TargetParam(Name name, String nameSpace) {
        this.objectType = 'name'
        this.idNumber = name.id
        this.versionNumber = null
        this.nameSpace = nameSpace
    }

    TargetParam(Author author, String nameSpace) {
        this.objectType = 'author'
        this.idNumber = author.id
        this.versionNumber = null
        this.nameSpace = nameSpace
    }

    TargetParam(Instance instance, String nameSpace) {
        this.objectType = 'instance'
        this.idNumber = instance.id
        this.versionNumber = null
        this.nameSpace = nameSpace
    }

    TargetParam(Reference reference, String nameSpace) {
        this.objectType = 'reference'
        this.idNumber = reference.id
        this.versionNumber = null
        this.nameSpace = nameSpace
    }

    TargetParam(InstanceNote instanceNote, String nameSpace) {
        this.objectType = 'instanceNote'
        this.idNumber = instanceNote.id
        this.versionNumber = null
        this.nameSpace = nameSpace
    }

    TargetParam(TreeVersion treeVersion, String nameSpace) {
        this.objectType = 'tree'
        this.idNumber = 0
        this.versionNumber = treeVersion.id
        this.nameSpace = nameSpace
    }

    TargetParam(TreeVersionElement treeVersionElement, String nameSpace) {
        this.objectType = 'tree'
        this.idNumber = treeVersionElement.treeElement.id
        this.versionNumber = treeVersionElement.treeVersion.id
        this.nameSpace = nameSpace
    }

    Map paramMap() {
        [nameSpace: nameSpace, objectType: objectType, idNumber: idNumber, versionNumber: versionNumber]
    }

    Map paramMap(String nameSpaceKey, String objectTypeKey, String idNumberKey, String versionNumberKey) {
        [(nameSpaceKey): nameSpace, (objectTypeKey): objectType, (idNumberKey): idNumber, (versionNumberKey): versionNumber]
    }

    String paramString() {
        return "nameSpace=${nameSpace}&objectType=${objectType}&idNumber=${idNumber}&versionNumber=${versionNumber}"
    }

}
