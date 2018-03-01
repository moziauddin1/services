package au.org.biodiversity.nsl

/**
 * User: pmcneil
 * Date: 26/02/18
 *
 */
class Synonym {
    final Instance instance
    final String simpleName
    final String type
    final Long nameId
    final String nameLink
    final String fullNameHtml
    final Boolean nom
    final Boolean tax
    final Boolean mis
    final String cites

    Synonym(Instance synonymInstance, String nameLink) {
        instance = synonymInstance
        simpleName = synonymInstance.name.simpleName
        type = synonymInstance.instanceType.name
        nameId = synonymInstance.name.id
        this.nameLink = nameLink
        fullNameHtml = synonymInstance.name.fullNameHtml
        nom = synonymInstance.instanceType.nomenclatural
        tax = synonymInstance.instanceType.taxonomic
        mis = synonymInstance.instanceType.misapplied
        cites = synonymInstance.cites ? synonymInstance.cites.reference.citationHtml : ''
    }

    Synonym(Map synonymMap) {
        instance = Instance.get(synonymMap.instance_id as Long)
        simpleName = synonymMap.simple_name as String
        type = synonymMap.type as String
        nameId = synonymMap.name_id
        nameLink = synonymMap.name_link as String
        fullNameHtml = synonymMap.full_name_html as String
        nom = synonymMap.nom as Boolean
        tax = synonymMap.tax as Boolean
        mis = synonymMap.mis as Boolean
        cites = synonymMap.cites as String
    }

    String html() {
        if (tax) {
            return "<tax>${fullNameHtml} <type>${type}</type></tax>"
        }
        if (nom) {
            return "<nom>${fullNameHtml} <type>${type}</type></nom>"
        }
        if (mis) {
            return "<mis>${fullNameHtml} <type>${type}</type> by <citation>${cites ?: ''}</citation></mis>"
        }
    }

    Map asMap() {
        [
                instance_id   : instance.id,
                simple_name   : simpleName,
                type          : type,
                name_id       : nameId,
                name_link     : nameLink,
                full_name_html: fullNameHtml,
                nom           : nom,
                tax           : tax,
                mis           : mis,
                cites         : cites
        ]
    }
}

class Synonyms {
    @Delegate
    final List<Synonym> synonyms = []
    Closure sortSyn = { a, b ->
        a.simpleName <=> b.simpleName ?: a.instance.reference.year <=> a.instance.reference.year
    }

    Synonyms() {}

    Synonyms(List<Map> synList) {
        synList.each {
            synonyms.add(new Synonym(it))
        }
    }

    List<Map> asListMap() {
        synonyms.collect { it.asMap() } ?: null
    }

    List<Synonym> nomSynonyms() {
        synonyms.findAll { it.nom }.sort(sortSyn)
    }

    List<Synonym> taxSynonyms() {
        synonyms.findAll { it.tax }.sort(sortSyn)
    }

    List<Synonym> misSynonyms() {
        synonyms.findAll { it.mis }.sort(sortSyn)
    }


    String html() {
        """<synonyms>
${nomSynonyms().collect { it.html() }.join('\n')}
${taxSynonyms().collect { it.html() }.join('\n')}
${misSynonyms().collect { it.html() }.join('\n')}
</synonyms>"""
    }
}