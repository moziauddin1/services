package au.org.biodiversity.nsl

import groovy.sql.GroovyResultSet
import groovy.sql.Sql

import java.sql.Timestamp

class AuditService implements WithSql {

    def configService

    List<Audit> list(String userName, Timestamp from, Timestamp to) {
        List rows = []
        withSql { Sql sql ->
            sql.eachRow("""
SELECT event_id, action_tstamp_tx, table_name, action, hstore_to_json(row_data) AS rd, hstore_to_json(changed_fields) AS cf FROM audit.logged_actions 
WHERE action_tstamp_tx > :from 
 AND action_tstamp_tx < :to 
 AND (changed_fields -> 'updated_by' LIKE :user OR (row_data -> 'updated_by' LIKE :user AND changed_fields -> 'updated_by' IS NULL))
 ORDER BY event_id DESC LIMIT 500""", [from: from, to: to, user: userName]) { GroovyResultSet row ->
                log.debug row
                rows.add(new Audit(row))
            }
        }
        return rows
    }

    /**
     * Returns a Map report in the form:
     * [userName : [thing : [created : total, updated: total], thing2 : ...], userName: ...]
     * @param from Timestamp
     * @param to Timestamp
     * @param things (optional list of things to get totals on)
     * @return
     */
    Map report(Timestamp from, Timestamp to, List<String> things = ['name', 'author', 'reference', 'instance']) {
        Map userReport = [:]
        withSql { Sql sql ->
            things.each { String thing ->
                String query = "select count(t) as count, t.created_by as uname from $thing t where created_at > :from and created_at < :to group by created_by"
                sql.eachRow(query, [from: from, to: to]) { GroovyResultSet row ->
                    log.debug row
                    userReport.get(row.uname, defaultUserThingReport(things)).get(thing).created = row.count
                }
                String q2 = "select count(t) as count, t.updated_by as uname from $thing t where updated_at > :from and updated_at < :to group by updated_by"
                sql.eachRow(q2, [from: from, to: to]) { GroovyResultSet row ->
                    log.debug row
                    userReport.get(row.uname, defaultUserThingReport(things)).get(thing).updated = row.count
                }
            }
        }
        userReport
    }

    private static Map defaultUserThingReport(List<String> things) {
        Map defMap = [:]
        things.collect { String thing ->
            defMap.put(thing, [created: 0, updated: 0])
        }
        return defMap
    }
}
