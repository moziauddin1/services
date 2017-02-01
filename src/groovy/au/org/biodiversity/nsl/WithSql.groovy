package au.org.biodiversity.nsl

import groovy.sql.GroovyResultSet
import groovy.sql.Sql

/**
 * User: pmcneil
 * Date: 25/01/17
 *
 */
trait WithSql {

    def configService

    private withSql(Closure work) {
        Sql sql = configService.getSqlForNSLDB()
        try {
            work(sql)
        } finally {
            sql.close()
        }

    }

    private List<Map> executeQuery(String query, List params) {
        log.debug "executing query: $query, $params"
        List results = []
        withSql { Sql sql ->
            sql.eachRow(query, params) { GroovyResultSet row ->
                def res = row.toRowResult()
                Map d = new LinkedHashMap()
                res.keySet().each { key ->
                    d[key] = res[key] as String
                }
                results.add(d)
            }
        }
        return results
    }

}
