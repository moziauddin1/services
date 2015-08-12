package services

import au.org.biodiversity.nsl.Name
import au.org.biodiversity.nsl.SimpleNameService


class UpdateSimpleNameExportJob {

    def searchService

    static triggers = {
        //update at 7AM every day
        cron name: 'update simpleNameExport', cronExpression: '0 0 7 * * ?'
    }

    def execute() {
        Name.withTransaction {
             SimpleNameService.makeNslSimpleNameExportTable(searchService.getNSL())
        }
    }
}
