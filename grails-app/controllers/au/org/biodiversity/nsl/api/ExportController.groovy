package au.org.biodiversity.nsl.api

import org.grails.plugins.metrics.groovy.Timed


class ExportController implements UnauthenticatedHandler {

    def flatViewService

    def index() {}

    @SuppressWarnings("GroovyUnusedDeclaration")
    static responseFormats = [
            index      : ['html'],
            namesCsv   : ['json', 'xml', 'html'],
            apcTaxonCsv: ['json', 'xml', 'html'],
    ]

    static allowedMethods = [
            namesCsv   : ["GET"],
            apcTaxonCsv: ["GET"],
    ]

    static namespace = "api"

    @Timed()
    def namesCsv() {
        File exportFile = flatViewService.exportNamesToCSV()
        render(file: exportFile, fileName: exportFile.name, contentType: 'text/plain')
    }

    @Timed()
    def apcTaxonCsv() {
        File exportFile = flatViewService.exportApcTaxonToCSV()
        render(file: exportFile, fileName: exportFile.name, contentType: 'text/plain')
    }
}
