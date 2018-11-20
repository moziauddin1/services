package services

import au.org.biodiversity.nsl.Name

class RecheckSynonymyJob {

    def treeService

    static triggers = {
        //update at 6:30AM every day
        cron name: 'updateViews', startDelay: 10000, cronExpression: '0 30 6 * * ?'
    }

    def execute() {
        Name.withTransaction {
            treeService.refreshSynonymHtmlCache()
        }
    }
}
