package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.DomainUtils
import au.org.biodiversity.nsl.tree.Uri
import grails.converters.JSON
import grails.converters.XML
import grails.transaction.Transactional
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy

@Transactional
class JsonRendererService {

    def grailsApplication
    def linkService
    def instanceService

    def registerObjectMashallers() {
        JSON.registerObjectMarshaller(Name) { Name name -> marshallName(name) }
        JSON.registerObjectMarshaller(Instance) { Instance instance -> marshallInstance(instance) }
        JSON.registerObjectMarshaller(Reference) { Reference reference -> marshallReference(reference) }
        JSON.registerObjectMarshaller(Author) { Author author -> marshallAuthor(author) }
        JSON.registerObjectMarshaller(InstanceNote) { InstanceNote instanceNote -> marshallInstanceNote(instanceNote) }
        JSON.registerObjectMarshaller(NslSimpleName) { NslSimpleName nslSimpleName -> marshallNslSimpleName(nslSimpleName) }
        JSON.registerObjectMarshaller(Node) { Node node -> marshallNode(node) }
        JSON.registerObjectMarshaller(Link) { Link link -> marshallLink(link) }
        JSON.registerObjectMarshaller(Arrangement) { Arrangement arrangement -> marshallArrangement(arrangement) }
        JSON.registerObjectMarshaller(Event) { Event event -> marshallEvent(event) }

        XML.registerObjectMarshaller(new XmlMapMarshaller())
        XML.registerObjectMarshaller(Name) { Name name, XML xml -> xml.convertAnother(marshallName(name)) }
        XML.registerObjectMarshaller(Instance) { Instance instance, XML xml -> xml.convertAnother(marshallInstance(instance)) }
        XML.registerObjectMarshaller(Reference) { Reference reference, XML xml -> xml.convertAnother(marshallReference(reference)) }
        XML.registerObjectMarshaller(Author) { Author author, XML xml -> xml.convertAnother(marshallAuthor(author)) }
        XML.registerObjectMarshaller(InstanceNote) { InstanceNote instanceNote, XML xml -> xml.convertAnother(marshallInstanceNote(instanceNote)) }
        XML.registerObjectMarshaller(NslSimpleName) { NslSimpleName nslSimpleName, XML xml -> xml.convertAnother(marshallNslSimpleName(nslSimpleName)) }
        XML.registerObjectMarshaller(ResourceLink) { ResourceLink resourceLink, XML xml ->
            xml.startNode('link')
               .attribute('resources', resourceLink.resources.toString())
               .chars(resourceLink.link)
               .end()
        }

        XML.registerObjectMarshaller(Node) { Node node, XML xml -> xml.convertAnother(marshallNode(node)) }
        XML.registerObjectMarshaller(Link) { Link link, XML xml -> xml.convertAnother(marshallLink(link)) }
        XML.registerObjectMarshaller(Arrangement) { Arrangement arrangement, XML xml -> xml.convertAnother(marshallArrangement(arrangement)) }
        XML.registerObjectMarshaller(Event) { Event event, XML xml -> xml.convertAnother(marshallEvent(event)) }
    }

    Map getBriefName(Name name) {
        brief(name, [nameElement: name?.nameElement])
    }

    Map getBriefReference(Reference reference) {
        brief(reference, [
                citation    : reference?.citation,
                citationHtml: reference?.citationHtml
        ])
    }

    Map getBriefInstance(Instance instance) {
        brief(instance, [instanceType: instance?.instanceType?.name,
                         protologue  : instance?.instanceType?.protologue,
                         citation    : instance?.reference?.citation,
                         citationHtml: instance?.reference?.citationHtml,
        ])
    }

    Map getBriefNameWithHtml(Name name) {
        brief(name, [
                nameType       : name.nameType?.name,
                nameStatus     : name.nameStatus?.name,
                nameRank       : name.nameRank?.name,
                primaryInstance: instanceService.findPrimaryInstance(name)?.collect { Instance instance -> getBriefInstance(instance) },
                fullName       : name.fullName,
                fullNameHtml   : name.fullNameHtml,
                simpleName     : name.simpleName,
                simpleNameHtml : name.simpleNameHtml
        ])
    }

    /** Get instance with html not containing the name. This is used for the picklist of instances belonging to a name */

    Map getBriefInstanceForNameWithHtml(Instance instance) {
        brief(instance, [
                instanceType: instance.instanceType.name,
                page        : instance.page,
                citation    : instance.reference.citation,
                citationHtml: instance.reference.citationHtml,
                parent      : instance.parent?.id,
                cites       : instance.cites?.id,
                citedBy     : instance.citedBy?.id
        ])
    }

    Map getBriefAuthor(Author author) {
        brief(author, [name: author?.abbrev])
    }

    Map getBriefTreeUri(Uri uri) {
        if (!uri) {
            return [:]
        }
        [
                idPart    : uri?.idPart,
                // zero is a magic number, it is the 'no namespace' namespace
                // ns of zero means that the uri is simply the entire string in the idPart
                nsPart    : ((uri?.nsPart?.id ?: 0) != 0) ? uri?.nsPart?.label : null,
                uri       : uri?.asUri(),
                uriEncoded: uri?.getEncoded(),
                qname     : uri?.asQNameIfOk(),
                css       : uri?.asCssClass()
        ]
    }

    Map brief(target, Map extra = [:]) {
        if (!target) {
            return [:]
        }
        target = initializeAndUnproxy(target)
        return [
                class : target.class.name,
                _links: getPreferredLink(target),
        ] << extra
    }

    Map getBaseInfo(target) {
        if (!target) {
            return [:]
        }
        target = initializeAndUnproxy(target)
        def links = getLinks(target)
        Map inner = [
                class    : target.class.name,
                _links   : links,
                audit    : getAudit(target),
                namespace: target.namespace.name
        ]
        String targetKey = target.class.simpleName.toLowerCase()
        Map outer = [:]
        outer[targetKey] = inner
        return outer
    }

    Map getAudit(target) {
        [
                created: [by: target.createdBy, at: target.createdAt],
                updated: [by: target.updatedBy, at: target.updatedAt]
        ]
    }

    Map getLinks(target) {
        try {
            ArrayList links = linkService.getLinksForObject(target)
            if (links && links.size() > 0) {
                List<ResourceLink> resourceLinks = links.collect { link -> new ResourceLink(link.resourceCount as Integer, link.link as String, link.preferred as Boolean) }
                return [permalinks: resourceLinks]
            }
        } catch (e) {
            log.debug e.message
        }
        return [permalinks: []]
    }

    Map getPreferredLink(target) {
        def link = linkService.getPreferredLinkForObject(target)
        if (link) {
            ResourceLink resourceLink = new ResourceLink(1 as Integer, link.link as String, true)
            return [permalink: resourceLink]
        }
        return [permalink: []]
    }

    Map instanceType(InstanceType instanceType) {
        [
                name           : instanceType.name,
                flags          : [
                        primaryInstance  : instanceType.primaryInstance,
                        secondaryInstance: instanceType.secondaryInstance,
                        relationship     : instanceType.relationship,
                        protologue       : instanceType.protologue,
                        taxonomic        : instanceType.taxonomic,
                        nomenclatural    : instanceType.nomenclatural,
                        synonym          : instanceType.synonym,
                        proParte         : instanceType.proParte,
                        doubtful         : instanceType.doubtful,
                        misapplied       : instanceType.misapplied,
                        standalone       : instanceType.standalone,
                        unsourced        : instanceType.unsourced,
                        citing           : instanceType.citing,
                        deprecated       : instanceType.deprecated
                ],
                sortOrder      : instanceType.sortOrder,
                rdfId          : instanceType.rdfId,
                descriptionHtml: instanceType.descriptionHtml
        ]
    }

    Map language(Language language) {
        [
                iso6391Code: language.iso6391Code,
                iso6393Code: language.iso6393Code,
                name       : language.name
        ]
    }

    Map externalRef(ExternalRef externalRef) {
        [
                objectType        : externalRef.objectType,
                externalIdSupplier: externalRef.externalIdSupplier,
                externalId        : externalRef.externalId,
                originalProvider  : externalRef.originalProvider
        ]
    }

/** ********************/

    Map marshallName(Name name) {
        Map data = getBaseInfo(name)
        data.name << [
                fullName       : name.fullName,
                fullNameHtml   : name.fullNameHtml,
                nameElement    : name.nameElement,
                simpleName     : name.simpleName,
                rank           : [name: name.nameRank.name, sortOrder: name.nameRank.sortOrder],
                type           : name.nameType.name,
                status         : name.nameStatus.name,
                tags           : name.tags.collect { it.name },
                parent         : getBriefName(name.parent),
                secondParent   : getBriefName(name.secondParent),
                instances      : name.instances.collect { getBriefInstance(it) },
                externalRefs   : name.externalRefs.collect { externalRef(it) },
                author         : getBriefAuthor(name.author),
                baseAuthor     : getBriefAuthor(name.baseAuthor),
                exAuthor       : getBriefAuthor(name.exAuthor),
                exBaseAuthor   : getBriefAuthor(name.exBaseAuthor),
                primaryInstance: instanceService.findPrimaryInstance(name)?.collect { Instance instance -> getBriefInstance(instance) }
        ]

        return data
    }

    Map marshallInstance(Instance instance) {
        Map data = getBaseInfo(instance)
        data.instance << [
                verbatimNameString : instance.verbatimNameString,
                page               : instance.page,
                pageQualifier      : instance.pageQualifier,
                nomenclaturalStatus: instance.nomenclaturalStatus,
                bhlUrl             : instance.bhlUrl,
                instanceType       : instanceType(instance.instanceType),
                name               : getBriefName(instance.name),
                reference          : getBriefReference(instance.reference),
                parent             : getBriefInstance(instance.parent),
                cites              : getBriefInstance(instance.cites),
                citedBy            : getBriefInstance(instance.citedBy),
                externalRefs       : instance.externalRefs.collect { externalRef(it) },
                instancesForCitedBy: instance.instancesForCitedBy.collect { getBriefInstance(it) },
                instancesForCites  : instance.instancesForCites.collect { getBriefInstance(it) },
                instancesForParent : instance.instancesForParent.collect { getBriefInstance(it) },
                instanceNotes      : instance.instanceNotes

        ]
        return data
    }

    Map marshallReference(Reference reference) {
        Map data = getBaseInfo(reference)
        data.reference << [
                doi              : reference.doi,
                title            : reference.title,
                displayTitle     : reference.displayTitle,
                abbrevTitle      : reference.abbrevTitle,
                year             : reference.year,
                volume           : reference.volume,
                edition          : reference.edition,
                pages            : reference.pages,
                verbatimReference: reference.verbatimReference,
                verbatimCitation : reference.verbatimCitation,
                verbatimAuthor   : reference.verbatimAuthor,
                citation         : reference.citation,
                citationHtml     : reference.citationHtml,
                notes            : reference.notes,
                published        : reference.published,
                publisher        : reference.publisher,
                publishedLocation: reference.publishedLocation,
                publicationDate  : reference.publicationDate,
                isbn             : reference.isbn,
                issn             : reference.issn,
                bhlUrl           : reference.bhlUrl,
                tl2              : reference.tl2,
                refType          : reference.refType.name,

                parent           : getBriefReference(reference.parent),
                author           : getBriefAuthor(reference.author),

                refAuthorRole    : reference.refAuthorRole.name,
                duplicateOf      : getBriefReference(reference.duplicateOf),
                language         : language(reference.language),
                externalRefs     : reference.externalRefs.collect { externalRef(it) },
                instances        : reference.instances.collect { getBriefInstance(it) },
                parentOf         : reference.referencesForParent.collect { getBriefReference(it) }
        ]
    }

    Map marshallAuthor(Author author) {
        Map data = getBaseInfo(author)
        data.author << [
                abbrev          : author.abbrev,
                name            : author.name,
                fullName        : author.fullName,
                dateRange       : author.dateRange,
                notes           : author.notes,
                ipniId          : author.ipniId,
                duplicateOf     : author.duplicateOf,
                references      : author.references.collect { getBriefReference(it) },
                names           : author.namesForAuthor.collect { getBriefName(it) },
                baseNames       : author.namesForBaseAuthor.collect { getBriefName(it) },
                exNames         : author.namesForExAuthor.collect { getBriefName(it) },
                exBaseNames     : author.namesForExBaseAuthor.collect { getBriefName(it) },
                sanctioningNames: author.namesForSanctioningAuthor.collect { getBriefName(it) }
        ]
    }

    Map marshallInstanceNote(InstanceNote instanceNote) {
        Map data = getBaseInfo(instanceNote)
        data.instancenote << [
                instanceNoteKey: instanceNote.instanceNoteKey.name,
                value          : instanceNote.value,
                instance       : getBriefInstance(instanceNote.instance)
        ]
    }

    Map marshallLink(Link link) {
        Map data = brief(link, [
                typeUri         : getBriefTreeUri(DomainUtils.getLinkTypeUri(link)),
                superNode       : brief(link.supernode, [id: link.supernodeId]),
                subNode         : brief(link.subnode, [id: link.subnodeId]),
                linkSeq         : link.linkSeq,
                versioningMethod: link.versioningMethod,
                isSynthetic     : link.synthetic
        ]);

        return data;
    }

    Map marshallNode(Node node) {
        //noinspection GroovyAssignabilityCheck
        Map data = brief node, [
                id         : node.id,
                type       : node.internalType.name(),
                typeUri    : getBriefTreeUri(DomainUtils.getNodeTypeUri(node)),
                prev       : node.prev ? brief(node.prev, [id: node.prev.id]) : null,
                next       : node.next ? brief(node.next) : null,
                arrangement: brief(node.root, [id: node.root.id] << (node.root.label ? [label: node.root.label] : [:])),
                checkedInAt: node.checkedInAt ? brief(node.checkedInAt, [id: node.checkedInAt.id, timeStamp: node.checkedInAt.timeStamp]) : null,
                replacedAt : node.replacedAt ? brief(node.replacedAt, [id: node.replacedAt.id, timeStamp: node.replacedAt.timeStamp]) : null,
                isCurrent  : node.checkedInAt && !node.replacedAt,
                isDraft    : (node.checkedInAt == null),
                isReplaced : (node.replacedAt != null),
                isSynthetic: node.synthetic
        ]

        switch (node.internalType) {
            case NodeInternalType.S:
            case NodeInternalType.Z:
                break;

            case NodeInternalType.T:
                if (DomainUtils.hasName(node)) data.nameUri = getBriefTreeUri(DomainUtils.getNameUri(node));
                if (DomainUtils.hasTaxon(node)) data.taxonUri = getBriefTreeUri(DomainUtils.getTaxonUri(node));
                if (node.instance) data.instance = node.instance;
                if (node.name) data.name = node.name;
        // fall through
            case NodeInternalType.D:
                if (DomainUtils.hasResource(node)) data.resourceUri = getBriefTreeUri(DomainUtils.getResourceUri(node));
                break;

            case NodeInternalType.V:
                if (DomainUtils.hasResource(node)) {
                    data.resourceUri = getBriefTreeUri(DomainUtils.getResourceUri(node))
                } else {
                    data.literal = node.literal;
                }
                break;
        }

        return data;
    }

    Map marshallArrangement(Arrangement arrangement) {
        Map data = brief arrangement, [
                arrangementType: arrangement.arrangementType,
                label          : arrangement.label,
                title          : arrangement.title,
                description    : arrangement.description,
                synthetic      : arrangement.synthetic == 'Y' ? true : arrangement.synthetic == 'N' ? false : null,
                node           : brief(arrangement.node, [:]),
        ];
        return data;
    }

    Map marshallEvent(Event event) {
        Map data = brief event, [
                timeStamp: event.timeStamp,
                note     : event.note

        ];
        return data;
    }

    Map marshallNslSimpleName(NslSimpleName nslSimpleName) {
        nslSimpleName = initializeAndUnproxy(nslSimpleName)
        Name name = Name.get(nslSimpleName.id)
        Map data = [nslSimpleName: [:]]
        data.nslSimpleName << [
                id                 : getPreferredLink(name),
                name               : nslSimpleName.name,
                taxonName          : nslSimpleName.taxonName,
                nameElement        : nslSimpleName.nameElement,
                cultivarName       : nslSimpleName.cultivarName,
                simpleNameHtml     : nslSimpleName.simpleNameHtml,
                fullNameHtml       : nslSimpleName.fullNameHtml,
                nameType           : nslSimpleName.nameTypeName,
                homonym            : nslSimpleName.homonym,
                autonym            : nslSimpleName.autonym,
                hybrid             : nslSimpleName.hybrid,
                cultivar           : nslSimpleName.cultivar,
                formula            : nslSimpleName.formula,
                scientific         : nslSimpleName.scientific,
                authority          : nslSimpleName.authority,
                baseNameAuthor     : nslSimpleName.baseNameAuthor,
                exBaseNameAuthor   : nslSimpleName.exBaseNameAuthor,
                author             : nslSimpleName.author,
                exAuthor           : nslSimpleName.exAuthor,
                sanctioningAuthor  : nslSimpleName.sanctioningAuthor,
                rank               : nslSimpleName.rank,
                rankSortOrder      : nslSimpleName.rankSortOrder,
                rankAbbrev         : nslSimpleName.rankAbbrev,
                classifications    : nslSimpleName.classifications,
                apni               : nslSimpleName.apni,
                protoYear          : nslSimpleName.protoYear,
                nomStat            : nslSimpleName.nomStat,
                nomIlleg           : nslSimpleName.nomIlleg,
                nomInval           : nslSimpleName.nomInval,
                updatedBy          : nslSimpleName.updatedBy,
                updatedAt          : nslSimpleName.updatedAt,
                createdBy          : nslSimpleName.createdBy,
                createdAt          : nslSimpleName.createdAt,

                parentNsl          : getBriefName(nslSimpleName.parentNsl),
                secondParentNsl    : getBriefName(nslSimpleName.secondParentNsl),
                familyNsl          : getBriefName(nslSimpleName.familyNsl),
                genusNsl           : getBriefName(nslSimpleName.genusNsl),
                speciesNsl         : getBriefName(nslSimpleName.speciesNsl),

                classis            : nslSimpleName.classis,
                subclassis         : nslSimpleName.subclassis,
                apcFamilia         : nslSimpleName.apcFamilia,
                family             : nslSimpleName.familia,
                genus              : nslSimpleName.genus,
                species            : nslSimpleName.species,
                infraspecies       : nslSimpleName.infraspecies,

                apcInstance        : getBriefInstance(nslSimpleName.apcInstance),
                apcName            : nslSimpleName.apcName,
                apcRelationshipType: nslSimpleName.apcRelationshipType,
                apcProparte        : nslSimpleName.apcProparte,
                apcComment         : nslSimpleName.apcComment,
                apcDistribution    : nslSimpleName.apcDistribution,
                apcExcluded        : nslSimpleName.apcExcluded

        ]
        return data
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    public static <T> T initializeAndUnproxy(T entity) {
        if (entity == null) {
            throw new NullPointerException("Entity passed for initialization is null");
        }

        Hibernate.initialize(entity);
        if (entity instanceof HibernateProxy) {
            entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer()
                                                  .getImplementation();
        }
        return entity;
    }
}

class ResourceLink {
    final Integer resources
    final String link
    final Boolean preferred

    ResourceLink(Integer count, String link, Boolean preferred) {
        this.resources = count
        this.link = link
        this.preferred = preferred
    }
}