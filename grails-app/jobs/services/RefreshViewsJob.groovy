package services

import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.SimpleNameService


class RefreshViewsJob {

    def flatViewService
    def grailsApplication

    static triggers = {
        //update at 7AM every day
        cron name: 'update views', cronExpression: '0 0 7 * * ?'
    }

    def execute() {
        Name.withTransaction {
            String namespace = grailsApplication.config.shard.classification.namespace.toLowerCase()
            flatViewService.refreshNameView(namespace)
            flatViewService.refreshTaxonView(namespace)
        }
    }
}
