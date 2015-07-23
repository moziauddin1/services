package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.ClassificationManagerService
import au.org.biodiversity.nsl.tree.ServiceException
import org.apache.shiro.authz.annotation.RequiresRoles

class ClassificationController {

    ClassificationManagerService classificationManagerService;

    @RequiresRoles('admin')
    def logs() {
        forward controller: "admin", action: "logs"
    }

    @RequiresRoles('admin')
    def index() {
        [list: Arrangement.findAll(sort: 'label') { arrangementType == ArrangementType.P }]
    }

    @RequiresRoles('admin')
    def createForm() {
        [list: Arrangement.findAll(sort: 'label') { arrangementType == ArrangementType.P }]
    }

    @RequiresRoles('admin')
    def editForm() {
        Arrangement classification = Arrangement.findByLabel(params['classification'] as String)
        [classification: classification, inputLabel: classification.label, inputDescription: classification.description]
    }

    @RequiresRoles('admin')
    def doCreate() {
        try {
            if (params['Create']) {
                if (!params['inputLabel'] || !params['inputDescription']) {
                    flash.validation = "Label and Description required";
                } else {
                    if (params['copyNameChk']) {
                        Arrangement copyNameIn = Arrangement.findByLabel(params['inputCopyNameIn'] as String)
                        classificationManagerService.createClassification(label: params.inputLabel, description: params.inputDescription, copyName: params['inputCopyName'], copyNameIn: copyNameIn);
                    } else {
                        classificationManagerService.createClassification(label: params.inputLabel, description: params.inputDescription);
                    }

                    flash.success = "Classification \"${params['inputLabel']}\" created."
                    return redirect(action: 'index')
                }
            } else {
                flash.validation = 'No action?'
            }
        }
        catch (ServiceException ex) {
            flash.error = ex.msg.toString();
            // need to roll back?
        }
        catch (Exception ex) {
            log.error(ex)
            flash.error = ex.toString();
        }
        return render(view: 'createForm', model: [
                inputLabel      : params['inputLabel'],
                inputDescription: params['inputDescription'],
                copyNameChk     : params['copyNameChk'],
                inputCopyName   : params['inputCopyName'],
                inputCopyNameIn : params['inputCopyNameIn'],
                list            : Arrangement.findAll(sort: 'label') { arrangementType == ArrangementType.P }
        ])
    }

    @RequiresRoles('admin')
    def doEdit() {
        Arrangement classification = Arrangement.findByLabel(params['classification'] as String)

        if (!classification) {
            flash.message = 'no classification specified'
            return redirect(action: 'index')
        }

        try {
            if (params['Cancel']) {
                return render(view: 'editForm', model: [
                        classification  : classification,
                        inputLabel      : params.inputLabel,
                        inputDescription: params.inputDescription
                ])
            } else if (params['Save'] && params.inputLabel != classification.label) {
                if (!params['inputLabel'] || !params['inputDescription']) {
                    flash.validation = 'Label and Description required'
                } else {
                    return render(view: 'editForm', model: [
                            classification     : classification,
                            inputLabel         : params.inputLabel,
                            inputDescription   : params.inputDescription,
                            needsConfirmation  : true,
                            confirmationMessage: "This action will change the persistent, publically-known name of \"${classification.label}\" to ${params.inputLabel}.",
                            confirmationAction : 'Save'
                    ])
                }
            } else if (params['Delete']) {
                return render(view: 'editForm', model: [
                        classification     : classification,
                        inputLabel         : params.inputLabel,
                        inputDescription   : params.inputDescription,
                        needsConfirmation  : true,
                        confirmationMessage: "This action will permanently delete classification \"${classification.label}\" and cannot be undone.",
                        confirmationAction : 'Delete'
                ])
            } else if (params['Ok'] && params['confirmationAction'] == 'Delete') {
                classificationManagerService.deleteClassification(classification);
                flash.success = "Classification \"${classification.label}\" permanently deleted."
                return redirect(action: 'index')
            } else if ((params['Save'] && params.inputLabel == classification.label) || (params['Ok'] && params['confirmationAction'] == 'Save')) {
                if (!params['inputLabel'] || !params['inputDescription']) {
                    flash.validation = 'Label and Description required'
                } else {
                    classificationManagerService.updateClassification(classification, label: params.inputLabel, description: params.inputDescription);
                    if (params.inputLabel == classification.label) {
                        flash.success = "Classification \"${classification.label}\" updated."
                    } else {
                        flash.success = "Classification \"${classification.label}\" changed to \"${params.inputLabel}\"."
                    }
                    return redirect(action: 'index')
                }
            } else {
                flash.validation = 'No action?'
            }
        }
        catch (ServiceException ex) {
            flash.error = ex.msg.toString();
            // need to roll back?
        }
        catch (Exception ex) {
            log.error(ex)
            flash.error = ex.toString();
            // need to roll back?
        }

        return render(view: 'editForm', model: [
                classification  : classification,
                inputLabel      : params['inputLabel'],
                inputDescription: params['inputDescription']])
    }
}
