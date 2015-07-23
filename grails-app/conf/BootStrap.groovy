class BootStrap {
    def grailsApplication
    def jsonRendererService
    def searchService
    def nslDomainService
    def shiroSecurityManager
    def shiroSubjectDAO

    def init = { servletContext ->
        if(!nslDomainService.checkUpToDate()) {
            log.error "Database is not up to date. Run update script on the DB before restarting."
            throw new Exception('Database not at expected version.')
        }
        searchService.registerSuggestions()
        jsonRendererService.registerObjectMashallers()

        if(shiroSecurityManager) {
            shiroSecurityManager.setSubjectDAO(shiroSubjectDAO)
            println "Set subject DAO on security manager."
        }
    }
    def destroy = {
    }
}
