package au.org.biodiversity.nsl

import grails.util.Environment
import groovy.sql.Sql
import org.grails.plugins.metrics.groovy.Timed

class SearchService {

    def suggestService
    def nameTreePathService
    def linkService

    @Timed(name = "SearchTimer")
    Map searchForName(Map params, Integer max) {

        Map queryParams = [:]

        Set<String> from = ['Name n']
        Set<String> and = []

        if (params.withNoInstances == 'on') {
            and << 'n.instances.size = 0'
        } else {
            and << 'n.instances.size > 0'
        }

        if ((params.name as String)?.trim()) {
            List<String> nameStrings = (params.name as String).trim().split('\n').collect {
                cleanUpName(it).toLowerCase()
            }

            if (nameStrings.size() > 100) {
                queryParams.names = nameStrings
                if (params.nameCheck) {
                    and << "lower(n.simpleName) in (:names)"
                } else {
                    and << "lower(n.fullName) in (:names)"
                }
            } else {
                List<String> ors = []
                if (params.nameCheck) {
                    nameStrings.findAll { it }.eachWithIndex { n, i ->
                        queryParams["name${i}"] = tokenizeQueryString(n)
                        ors << "lower(n.simpleName) like :name${i}"
                    }
                    and << "(${ors.join(' or ')})"
                } else {
                    nameStrings.findAll { it }.eachWithIndex { n, i ->
                        queryParams["name${i}"] = tokenizeQueryString(n)
                        ors << "lower(n.fullName) like :name${i}"
                    }
                    and << "(${ors.join(' or ')})"
                }
            }
        }

        if (params.nameStatus) {
            queryParams.nameStatus = params.nameStatus
            and << "n.nameStatus.name in (:nameStatus)"
        }

        if (params.author) {
            queryParams.author = params.author.toLowerCase()
            and << "lower(n.author.abbrev) like :author"
        }


        if (params.ofRank?.id) {
            queryParams.ofRankId = params.ofRank.id as Long

            and << "n.nameRank.id = :ofRankId"
        }

        if (params.nameType?.id) {
            queryParams.nameTypeId = params.nameType.id as Long
            and << "n.nameType.id = :nameTypeId"
        }

        if (params.advanced && params.cultivars != 'on' && params.cultivarsOnly != 'on') {
            and << "n.nameType.cultivar is false"
        } else {
            if (params.cultivarsOnly == 'on') {
                and << "n.nameType.cultivar is true"
            }
        }

        if (params.protologue == 'on') {
            from.add('Instance i')
            and << "i.instanceType.protologue is true"
            and << "i.name = n"
        }

        if (params.publication) {
            queryParams.publication = "${tokenizeQueryString((params.publication as String).trim().toLowerCase(), true)}"
            from.add('Instance i')
            from.add('Reference r')
            and << "lower(r.citation) like :publication"
            and << "i.reference = r"
            and << "i.name = n"
        }

        if (params.year) {
            queryParams.year = params.year.toInteger()
            from.add('Instance i')
            from.add('Reference r')
            and << "r.year = :year"
            and << "i.reference = r"
            and << "i.name = n"
        }

        if (params.experimental && params.inc) {
            List<String> ors = []
            params.inc.each { k, v ->
                if (v == 'on') {
                    if (k == 'other') {
                        ors << "(n.nameType.scientific = false and n.nameType.cultivar = false)"
                    } else {
                        ors << "n.nameType.${k} = true"
                    }
                }
            }
            if (ors.size()) {
                and << "(${ors.join(' or ')})"
            }
        }

        if (params.experimental && params.ex) {
            params.ex.each { k, v ->
                if (v == 'on') {
                    and << "n.nameType.${k} = false"
                }
            }
        }

        if (params.experimental && params.nameTag) {
            from.add('NameTagName tag')
            queryParams.nameTag = params.nameTag
            and << "tag.tag.name = :nameTag and tag.name = n"
        }

        if (params.tree?.id) {
            Arrangement root = Arrangement.get(params.tree.id as Long)
            queryParams.root = root
            from.add('Node node')
            and << "node.root = :root and node.checkedInAt is not null and node.next is null and node.internalType = 'T'"

            //todo remove this as APNI specific
            if (root.label == 'APNI' || params.exclSynonym == 'on') {
                and << "cast(n.id as string) = node.nameUriIdPart"
            } else {
                from.add('Instance i')
                from.add('Instance s')
                and << "n = s.name and (s.citedBy = i or s = i) and cast(i.id as string) = node.taxonUriIdPart"
            }

            if (params.inRank?.id) {
                NameRank inRank = NameRank.get(params.inRank?.id as Long)
                String rankNameString = params.rankName.trim()
                if (rankNameString) {
                    List<Name> rankNames = Name.findAllByFullNameIlikeAndNameRank("${rankNameString}%", inRank)
                    if (rankNames && !rankNames.empty) {
                        List<NameTreePath> ntps = nameTreePathService.findAllCurrentNameTreePathsForNames(rankNames, root)
                        if (ntps && !ntps.empty) {
                            from.add('NameTreePath ntp')
                            if (root.label == 'APNI' || params.exclSynonym == 'on') {
                                and << "n = ntp.name and ntp.tree = :root"
                            } else {
                                and << "i.name = ntp.name and ntp.tree = :root"
                            }
                            Set<String> pathOr = []
                            ntps.eachWithIndex { ntp, i ->
                                queryParams["path$i"] = "${ntp.path}%"
                                pathOr << "ntp.path like :path$i"
                            }
                            and << "(${pathOr.join(' or ')})"
                        } else {
                            return [count: 0, names: [], message: "Name tree path for ${params.rankName} not found in ${root.label}"]
                        }
                    } else {
                        return [count: 0, names: [], message: "Rank name ${params.rankName} does not exist in rank ${inRank.name} in ${root.label}"]
                    }
                } else {
                    params.remove('inRank') //blank name so set it to any
                }
            }
        }

        String fromClause = "from ${from.join(',')}"
        String whereClause = "where ${and.join(' and ')}"

        String countQuery = "select count(distinct n) $fromClause $whereClause"
        String query = "select distinct(n) $fromClause $whereClause order by n.fullName asc"

        log.debug query
        log.debug queryParams
        List counter = Name.executeQuery(countQuery, queryParams, [max: max])
        Integer count = counter[0] as Integer
        List<Name> names = Name.executeQuery(query, queryParams, [max: max])
        return [count: count, names: names]
    }

    private static String cleanUpName(String name) {
        name.trim()
    }

    public static String tokenizeQueryString(String query, boolean leadingWildCard = false) {
        if (query.startsWith('"') && query.endsWith('"')) {
            return  query.size() > 2 ? query[1..-2] : ""
        }
        (leadingWildCard ? '%' : '') + query.replaceAll(/[ ]+/, ' ')
                                            .replaceAll(/([a-zA-Z0-9\.,']) ([a-zA-Z0-9\.,'])/, '$1 %$2')
                                            .replaceAll(/\+/, ' ') +
                '%'
    }

    Sql getNSL() {
        (Environment.executeForCurrentEnvironment {
            development {
                Sql.newInstance('jdbc:postgresql://localhost:5432/nsl', 'nsldev', 'nsldev', 'org.postgresql.Driver')
            }
            test {
                Sql.newInstance('jdbc:postgresql://localhost:5432/nsl', 'nsldev', 'nsldev', 'org.postgresql.Driver')
            }
            production {
                Sql.newInstance('jdbc:postgresql://localhost:5432/nsl', 'nsl', 'nsl', 'org.postgresql.Driver')
            }
        } as Sql)
    }

    def makeAllTreePathsSql() {
        log.debug "in makeAllTreePathsSql"
        runAsync {
            try {
                Sql sql = getNSL()
                log.info "Truncating name tree path."
                sql.execute('TRUNCATE TABLE ONLY name_tree_path RESTART IDENTITY')
                sql.close()
                makeTreePathsSql('APNI')
                makeTreePathsSql('APC')
                log.info "Completed making APNI and APC tree paths"
            } catch (e) {
                log.error e
                throw e
            }
        }
    }

    def makeTreePathsSql(String arrangementLabel) {
        log.info "Making Tree Paths for $arrangementLabel."
        Long start = System.currentTimeMillis()
        Sql sql = getNSL()
        sql.connection.autoCommit = false

        try {
            sql.execute([tree: arrangementLabel], '''
WITH RECURSIVE level(node_id, tree_id, parent_id, node_path, name_path, name_id)
AS (
  SELECT
    l2.subnode_id                AS node_id,
    a.id                         AS tree_id,
    NULL :: BIGINT               AS parent_id,
    n.id :: TEXT                 AS node_path,
    n.name_uri_id_part :: TEXT   AS name_path,
    n.name_uri_id_part :: BIGINT AS name_id
  FROM tree_arrangement a, tree_link l, tree_link l2, tree_node n
  WHERE a.label = :tree
        AND l.supernode_id = a.node_id
        AND l2.supernode_id = l.subnode_id
        AND n.id = l2.subnode_id

  UNION ALL
  SELECT
    subnode.id                                            AS node_id,
    parent.tree_id                                        AS tree_id,
    parentnode.id                                         AS parent_id,
    (parent.node_path || '.' || subnode.id :: TEXT)       AS node_path,
    (parent.name_path || '.' || subnode.name_uri_id_part) AS name_path,
    subnode.name_uri_id_part :: BIGINT                    AS name_id
  FROM level parent, tree_node parentnode, tree_node subnode, tree_link l
  WHERE parentnode.id = parent.node_id -- this node is now parent
        AND l.supernode_id = parentnode.id
        AND l.subnode_id = subnode.id
        AND subnode.tree_arrangement_id = parent.tree_id
        AND subnode.internal_type = 'T\'
        AND subnode.checked_in_at_id IS NOT NULL
        AND subnode.next_node_id IS NULL
)
INSERT INTO name_tree_path (id, tree_id, parent_id, tree_path, path, name_id, inserted, version)
(SELECT
   l.node_id,
   l.tree_id,
   l.parent_id,
   l.node_path,
   l.name_path,
   l.name_id,
   0,
   0
 FROM level l
 where name_id is not null
       and name_path is not null)''') //not null tests for DeclaredBT that don't exists see NSL-1017
            sql.commit()
        } catch (e) {
            sql.rollback()
            while (e) {
                log.error(e.message)
                if (e.message.contains('getNextException')) {
                    log.error e
                    e = e.getNextException()
                } else {
                    e = e.cause
                }
            }
        }
        sql.close()
        log.info "Made Tree Paths in ${System.currentTimeMillis() - start}ms"
    }

    def registerSuggestions() {
        // add apc name search
        suggestService.addSuggestionHandler('apc-search') { String query ->
            return Name.executeQuery('''
select n from Name n 
where lower(n.fullName) like :query
and exists (
  select 1 
  from Node nd
  where nd.root.label = 'APC'
  and nd.checkedInAt is not null
  and nd.replacedAt is null
  and nd.nameUriNsPart.label = 'nsl-name'
  and nd.nameUriIdPart = cast(n.id as string)
)
order by n.simpleName asc, n.fullName asc''',
                    [query: "${query.toLowerCase()}%"], [max: 10])
                       .collect { name -> [id: name.id, fullName: name.fullName, fullNameHtml: name.fullNameHtml] }
        }

        suggestService.addSuggestionHandler('apni-search') { String query ->
            query = query.trim()
            if (query.contains('\n')) {
                query = query.split('\n').last().trim()
            }
            List<String> names = Name.executeQuery('''select n from Name n where lower(n.fullName) like :query and n.instances.size > 0 and n.nameType.scientific = true order by n.fullName asc''',
                    [query: tokenizeQueryString(query.toLowerCase())], [max: 15])
                                     .collect { name -> name.fullName }
            if (names.size() == 15) {
                names.add('...')
            }
            return names
        }

        suggestService.addSuggestionHandler('simpleName') { String query ->
            return Name.executeQuery('''select n from Name n where lower(n.simpleName) like :query and n.instances.size > 0 order by n.simpleName asc''',
                    [query: tokenizeQueryString(query.toLowerCase())], [max: 15])
                       .collect { name -> name.simpleName }
        }

        suggestService.addSuggestionHandler('acceptableName') { String query ->
            List<String> status = ['legitimate', 'manuscript', 'nom. alt.', 'nom. cons.', 'nom. cons., nom. alt.', 'nom. cons., orth. cons.', 'nom. et typ. cons.', 'orth. cons.', 'typ. cons.']
            return Name.executeQuery('''select n from Name n where (lower(n.fullName) like :query or lower(n.simpleName) like :query) and n.instances.size > 0 and n.nameStatus.name in (:ns) order by n.simpleName asc''',
                    [query: tokenizeQueryString(query.toLowerCase()), ns: status], [max: 15])
                       .collect { name -> [name: name.fullName, link: linkService.getPreferredLinkForObject(name)?.link] }
        }

        suggestService.addSuggestionHandler('author') { String query ->
            return Author.executeQuery('''select a from Author a where lower(a.abbrev) like :query order by a.abbrev asc''',
                    [query: "${query.toLowerCase()}%"], [max: 15])
                         .collect { author -> author.abbrev }
        }

        suggestService.addSuggestionHandler('publication') { String query ->
            return Reference.executeQuery('''select r from Reference r where lower(r.citation) like :query order by r.citation asc''',
                    [query: "${tokenizeQueryString(query.trim().toLowerCase(), true)}"], [max: 15])
                            .collect { reference -> reference.citation }
        }

        suggestService.addSuggestionHandler('epithet') { String query ->
            return Name.executeQuery('''select distinct (n.nameElement) from Name n
where lower(n.nameElement) like :query and n.instances.size > 0 and n.nameType.cultivar = false order by n.nameElement asc''',
                    [query: "${query.toLowerCase()}%"], [max: 15])
        }

        suggestService.addSuggestionHandler('nameType') { String query ->
            return NameType.executeQuery('''select n from NameType n where n.deprecated = false and lower(n.name) like :query order by n.name asc''',
                    [query: "${query.toLowerCase()}%"], [max: 15])
                           .collect { type ->
                type.name
            }
        }

    }

}
