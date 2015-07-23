package au.org.biodiversity.nsl

class DashboardController {

    def grailsApplication
    VocabularyTermsService vocabularyTermsService

    def index() {
        String url = grailsApplication.config.grails.serverURL
        log.debug "in dashboard: $url, $grailsApplication.config.grails.config.locations"

        Map stats = [:]
        stats.names = Name.count()
        stats.references = Reference.count()
        stats.authors = Author.count()
        stats.instancesOfNameUsage = Instance.count()

        stats.NameTypeStats = Name.executeQuery(
                'select nt.name, count(*) as total from Name n, NameType nt where nt = n.nameType group by nt.id order by total desc')
        stats.NameStatusStats = Name.executeQuery(
                'select ns.name, count(*) as total from Name n, NameStatus ns where ns = n.nameStatus group by ns.id order by total desc')
        stats.NameRankStats = Name.executeQuery(
                'select nr.name, count(*) as total from Name n, NameRank nr where nr = n.nameRank group by nr.id order by total desc')

        stats.instanceTypeStats = Instance.executeQuery(
                'select t.name, count(*) as total from Instance i, InstanceType t where t = i.instanceType group by t.id order by total desc')
        stats.recentNameUpdates = Name.executeQuery('select n from Name n order by n.updatedAt desc', [max: 10]).collect { [it, it.updatedAt, it.updatedBy] }
        stats.recentReferenceUpdates = Reference.executeQuery('select n from Reference n order by n.updatedAt desc', [max: 10]).collect { [it, it.updatedAt, it.updatedBy] }
        stats.recentAuthorUpdates = Author.executeQuery('select n from Author n order by n.updatedAt desc', [max: 10]).collect { [it, it.updatedAt, it.updatedBy] }
        stats.recentInstanceUpdates = Instance.executeQuery('select n from Instance n order by n.updatedAt desc', [max: 10]).collect { [it, it.updatedAt, it.updatedBy] }

        [stats: stats]
    }

    def error() {
        log.debug 'In error action: throwing an error.'
        throw new Exception('This is a test error. Have a nice day :-)')
        redirect(action: 'index')
    }

    def downloadVocabularyTerms() {
        File zip = vocabularyTermsService.getVocabularyZipFile();
        // create a temp zip file

        // ask the service to populate it

        // write it to the output, with a disposition etc

        response.setHeader("Content-disposition", "attachment; filename=NslVocabulary.zip");
        response.setContentType("application/zip");

        byte[] buf = new byte[1024];

        OutputStream os = response.getOutputStream();
        InputStream is = new FileInputStream(zip);

        int n;

        while((n=is.read(buf))>0) {
            os.write(buf, 0, n);
        }

        is.close();
        os.close();

        zip.delete();

        return null;
    }
}
