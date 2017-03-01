package au.org.biodiversity.nsl

import groovy.sql.GroovyResultSet
import groovy.sql.Sql

import java.sql.Timestamp

class AuditService implements WithSql {

    def configService

    def list(String userName, Timestamp from, Timestamp to) {
        List rows = []
        withSql { Sql sql ->
            sql.eachRow("""
SELECT event_id, action_tstamp_tx, table_name, action, hstore_to_json(row_data) AS rd, hstore_to_json(changed_fields) AS cf FROM audit.logged_actions 
WHERE action_tstamp_tx > :from 
 AND action_tstamp_tx < :to 
 AND row_data -> 'updated_by' LIKE :user
 ORDER BY event_id DESC""", [from: from, to: to, user : userName]) { GroovyResultSet row ->
                println row
                rows.add(new Audit(row))
            }
        }
        return rows
    }

}
