package au.org.biodiversity.nsl

import grails.plugins.rest.client.RestResponse

import java.util.concurrent.atomic.AtomicBoolean

class PhotoService {

    def restCallService
    def configService

    private List<String> photoNames = null
    private AtomicBoolean updating = new AtomicBoolean(false)

    boolean hasPhoto(String simpleName) {
        if (!updating.get() && photoNames) {
            return photoNames.contains(simpleName)
        }
        return false
    }

    String searchUrl(String simpleName) {
        configService.getPhotoSearch(simpleName)
    }

    def refresh() {
        if (updating.compareAndSet(false, false)) {
            photoNames = getPhotoMatchList()
        }
    }

    private List getPhotoMatchList() {
        if (updating.compareAndSet(false, true)) {
            List<String> photoNames = []
            String url = configService.getPhotoServiceUri()
            if (url) { //no photo service so no photos
                log.debug(url)
                RestResponse response = restCallService.nakedJsonGet(url)
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
        return []
    }
}
