package services

import au.org.biodiversity.nsl.Name

class RefreshViewsJob {

    def configService
    def flatViewService

    static triggers = {
        //update at 7AM every day
        cron name: 'updateViews', startDelay: 10000, cronExpression: '0 0 7 * * ?'
    }

    def execute() {
        Name.withTransaction {
            String namespaceName = configService.nameSpace.name.toLowerCase()
            flatViewService.refreshNameView(namespaceName)
            flatViewService.refreshTaxonView(namespaceName)
        }
    }
}
