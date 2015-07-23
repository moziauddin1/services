package au.org.biodiversity.nsl.api

import static org.springframework.http.HttpStatus.NOT_FOUND

/**
 * User: pmcneil
 * Date: 18/06/15
 *
 * When using this trait you must have the jsonRendererService injected into your implementing code.
 */
trait WithTarget {

    public withTarget(Object target, Closure work) {
         assert jsonRendererService

        if (target) {
            ResultObject result
            switch (target.class.simpleName) {
                case 'Instance':
                    result = new ResultObject([instance: jsonRendererService.getBriefInstance(target)])
                    break
                case 'Name':
                    result = new ResultObject([name: jsonRendererService.getBriefName(target)])
                    break
                case 'Reference':
                    result = new ResultObject([reference: jsonRendererService.getBriefReference(target)])
                    break
                case 'Author':
                    result = new ResultObject([author: jsonRendererService.getBriefAuthor(target)])
                    break
                case 'InstanceNote':
                    result = new ResultObject([instanceNote: jsonRendererService.getBriefInstanceNote(target)])
                    break
                default:
                    result = new ResultObject([target: jsonRendererService.brief(target)])
            }
            result << [
                    action: params.action,
            ]
            work(result)
            //noinspection GroovyAssignabilityCheck
            respond(result, [view: '/common/serviceResult', model: [data: result], status: result.status])
        } else {
            ResultObject result = new ResultObject([
                    action: params.action,
                    error : "The Instance was not found."
            ])
            //noinspection GroovyAssignabilityCheck
            respond(result, [view: '/common/serviceResult', model: [data: result], status: NOT_FOUND])
        }
    }

}
