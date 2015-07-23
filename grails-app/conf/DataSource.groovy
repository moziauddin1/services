import java.sql.Connection

dataSource {
    pooled = true
    jmxExport = true
}
hibernate {
    cache.use_second_level_cache = false
    cache.use_query_cache = false
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
    flush.mode = 'manual' // OSIV session flush mode outside of transactional context
}

// environment specific settings
// Note these *should* be overridden in the external config file services-config.groovy, esp the DB passwords :-)
environments {
    development {
        dataSource_nsl {
            pooled = true
            driverClassName = "org.postgresql.Driver"
            username = "nsldev"
            password = "nsldev"
//            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            dialect = "au.org.biodiversity.nsl.ExtendedPostgreSQLDialect"
            url = "jdbc:postgresql://localhost:5432/nsl"
            formatSql = true
//            logSql = true
            //noinspection GroovyAssignabilityCheck
            properties {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED
                initialSize = 2
                maxActive = 5
                minEvictableIdleTimeMillis = 1800000
                timeBetweenEvictionRunsMillis = 1800000
                numTestsPerEvictionRun = 3
                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = true
                validationQuery = "SELECT 1"
            }
        }
    }
    test {
        dataSource_nsl {
            pooled = true
            driverClassName = "org.postgresql.Driver"
            username = "nsldev"
            password = "nsldev"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            dbCreate = "update"
            url = "jdbc:postgresql://localhost:5432/nslimport"
            formatSql = false
            logSql = false
            //noinspection GroovyAssignabilityCheck
            properties {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED
                initialSize = 2
                maxActive = 5
                minEvictableIdleTimeMillis = 1800000
                timeBetweenEvictionRunsMillis = 1800000
                numTestsPerEvictionRun = 3
                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = true
                validationQuery = "SELECT 1"
            }
        }
    }
    production {
        dataSource_nsl {
            pooled = true
            driverClassName = "org.postgresql.Driver"
            username = "nsldev"
            password = "nsldev"
            dialect = "org.hibernate.dialect.PostgreSQLDialect"
            url = "jdbc:postgresql://localhost:5432/nsl"
            formatSql = false
            logSql = false
            //noinspection GroovyAssignabilityCheck
            properties {
                defaultTransactionIsolation = Connection.TRANSACTION_READ_UNCOMMITTED
                initialSize = 2
                maxActive = 10
                minEvictableIdleTimeMillis = 1800000
                timeBetweenEvictionRunsMillis = 1800000
                numTestsPerEvictionRun = 3
                testOnBorrow = true
                testWhileIdle = true
                testOnReturn = true
                validationQuery = "SELECT 1"
            }
        }
    }
}
