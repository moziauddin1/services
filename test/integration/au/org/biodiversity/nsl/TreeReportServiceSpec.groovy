package au.org.biodiversity.nsl

import grails.test.mixin.TestFor
import spock.lang.Specification

import javax.sql.DataSource

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(TreeReportService)
class TreeReportServiceSpec extends Specification {

    DataSource dataSource_nsl
    TreeService treeService

    def setup() {
        service.dataSource_nsl = dataSource_nsl
        treeService = new TreeService()
        treeService.dataSource_nsl = dataSource_nsl
        treeService.transactionManager = getTransactionManager()
        treeService.configService = new ConfigService(grailsApplication: grailsApplication)
        treeService.linkService = Mock(LinkService)

    }

    def cleanup() {
    }


}
