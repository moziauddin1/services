quartz {
    jdbcStore = false
    waitForJobsToCompleteOnShutdown = true
    exposeSchedulerInRepository = false
    interruptJobsOnShutdown = true
    threadCount = 2

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
