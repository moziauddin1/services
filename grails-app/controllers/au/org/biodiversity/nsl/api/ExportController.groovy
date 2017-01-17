package au.org.biodiversity.nsl.api

import org.grails.plugins.metrics.groovy.Timed


class ExportController implements UnauthenticatedHandler {

    def flatViewService

    def index() {}

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = [
            index      : ['html'],
            namesCsv   : ['json', 'xml', 'html'],
            taxonCsv   : ['json', 'xml', 'html'],
    ]

    static allowedMethods = [
            namesCsv   : ["GET"],
            taxonCsv   : ["GET"],
    ]

    static namespace = "api"

    @Timed()
    namesCsv() {
        File exportFile = flatViewService.exportNamesToCSV()
        render(file: exportFile, fileName: exportFile.name, contentType: 'text/plain')
    }

    @Timed()
    taxonCsv() {
        File exportFile = flatViewService.exportTaxonToCSV()
        render(file: exportFile, fileName: exportFile.name, contentType: 'text/plain')
    }
}
