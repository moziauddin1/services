package au.org.biodiversity.nsl

import grails.plugins.rest.client.RestResponse
import grails.transaction.Transactional

import java.util.concurrent.atomic.AtomicBoolean

@Transactional
class PhotoService {

    def restCallService
    def configService

    private List<String> photoNames = null
    private AtomicBoolean updating = new AtomicBoolean(false)

    boolean hasPhoto(String simpleName) {
        while (updating.get()) {
            Thread.sleep(1000)
            log.debug "waiting for update..."
        }
        if (!updating.get() && !photoNames) {
            log.debug "updating photo list"
            photoNames = getPhotoMatchList()
        }
        return photoNames.contains(simpleName)
    }

    def refresh() {
        if (!updating.get()) {
            photoNames = getPhotoMatchList()
        }
    }

    private List getPhotoMatchList() {
        updating.compareAndSet(false, true)
        List<String> photoNames = []
        String url = configService.getPhotoServiceUri()
        if (url) { //no photo service so no photos
            log.debug(url)
            RestResponse response = restCallService.nakedGet(url)
            if (response.status == 200) {
                String csvText = response.text
                if (csvText) {
                    csvText.eachLine { String line ->
                        String name = line.replaceAll(/^"([^"]*).*$/, '$1')
                        photoNames.add(name.trim())
                    }
                } else {
                    log.error "No data from $url"
                }
            } else {
                log.error "Error from $url ${response.status}"
            }
        }
        log.debug photoNames
        updating.compareAndSet(true, false)
        return photoNames
    }
}
