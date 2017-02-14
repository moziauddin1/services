package au.org.biodiversity.nsl

import groovy.sql.GroovyResultSet
import groovy.sql.Sql

/**
 * User: pmcneil
 * Date: 25/01/17
 *
 */
trait WithSql {

    private withSql(Closure work) {
        Sql sql = configService.getSqlForNSLDB()
        try {
            work(sql)
        } finally {
            sql.close()
        }

    }


}
