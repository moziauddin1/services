quartz {
    jdbcStore = false
    waitForJobsToCompleteOnShutdown = true
    exposeSchedulerInRepository = false
    interruptJobsOnShutdown = true

    props {
        scheduler.skipUpdateCheck = true
    }
}

environments {
    development {
        quartz {
            autoStartup = false
        }
    }
    test {
        quartz {
            autoStartup = false
        }
    }
    production {
        quartz {
            autoStartup = true
        }
    }
}
