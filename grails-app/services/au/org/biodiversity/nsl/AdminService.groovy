package au.org.biodiversity.nsl

import grails.transaction.Transactional

@Transactional
class AdminService {

    private Boolean servicing = false

    public Boolean serviceMode() {
        return servicing
    }

    public Boolean enableServiceMode(Boolean on) {
        servicing = on
    }

}
