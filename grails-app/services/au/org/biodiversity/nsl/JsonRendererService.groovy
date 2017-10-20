/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package au.org.biodiversity.nsl

import au.org.biodiversity.nsl.tree.DomainUtils
import au.org.biodiversity.nsl.tree.Message
import au.org.biodiversity.nsl.tree.ServiceException
import au.org.biodiversity.nsl.tree.Uri
import grails.converters.JSON
import grails.converters.XML
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException

class JsonRendererService {

    def grailsApplication
    def linkService
    def instanceService
    def treeService

    MessageSource messageSource

    def registerObjectMashallers() {
        JSON.registerObjectMarshaller(Namespace) { Namespace namespace -> getBriefNamespace(namespace) }
        JSON.registerObjectMarshaller(Name) { Name name -> marshallName(name) }
        JSON.registerObjectMarshaller(Instance) { Instance instance -> marshallInstance(instance) }
        JSON.registerObjectMarshaller(Reference) { Reference reference -> marshallReference(reference) }
        JSON.registerObjectMarshaller(Author) { Author author -> marshallAuthor(author) }
        JSON.registerObjectMarshaller(InstanceNote) { InstanceNote instanceNote -> marshallInstanceNote(instanceNote) }
        JSON.registerObjectMarshaller(Tree) { Tree tree -> marshallTree(tree) }
        JSON.registerObjectMarshaller(TreeVersion) { TreeVersion treeVersion -> marshallTreeVersion(treeVersion) }
        JSON.registerObjectMarshaller(TreeElement) { TreeElement treeElement -> marshallTreeElement(treeElement) }
        //todo remove
        JSON.registerObjectMarshaller(TreeVersionElement) { TreeVersionElement treeVersionElement -> marshallTreeVersionElement(treeVersionElement) }

        JSON.registerObjectMarshaller(Node) { Node node -> marshallNode(node) }
        JSON.registerObjectMarshaller(Link) { Link link -> marshallLink(link) }
        JSON.registerObjectMarshaller(Arrangement) { Arrangement arrangement -> marshallArrangement(arrangement) }
        JSON.registerObjectMarshaller(Event) { Event event -> marshallEvent(event) }
        JSON.registerObjectMarshaller(ServiceException) { ServiceException serviceException -> marshallTreeServiceException(serviceException) }
        JSON.registerObjectMarshaller(Message) { Message message -> marshallTreeServiceMessage(message) }

        XML.registerObjectMarshaller(new XmlMapMarshaller())
        XML.registerObjectMarshaller(Namespace) { Namespace namespace, XML xml -> xml.convertAnother(getBriefNamespace(namespace)) }
        XML.registerObjectMarshaller(Name) { Name name, XML xml -> xml.convertAnother(marshallName(name)) }
        XML.registerObjectMarshaller(Instance) { Instance instance, XML xml -> xml.convertAnother(marshallInstance(instance)) }
        XML.registerObjectMarshaller(Reference) { Reference reference, XML xml -> xml.convertAnother(marshallReference(reference)) }
        XML.registerObjectMarshaller(Author) { Author author, XML xml -> xml.convertAnother(marshallAuthor(author)) }
        XML.registerObjectMarshaller(InstanceNote) { InstanceNote instanceNote, XML xml -> xml.convertAnother(marshallInstanceNote(instanceNote)) }
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
        XML.registerObjectMarshaller(ServiceException) { ServiceException serviceException, XML xml -> xml.convertAnother(marshallTreeServiceException(serviceException)) }
        XML.registerObjectMarshaller(Message) { Message message, XML xml -> xml.convertAnother(marshallTreeServiceMessgage(message)) }
    }

    // we need this anywhere that citation and citationHtml appear as fields
    static String citationAuthYear(Reference reference) {
        if (reference) {
            return "${reference.author?.abbrev ?: reference.author?.name ?: reference.author?.fullName}, ${reference.year}"
        } else {
            return null
        }
    }

    private List treeElementChildren(TreeVersionElement treeVersionElement) {
        List<DisplayElement> childDisplayElements = treeService.childDisplayElements(treeVersionElement)
        List kids = []
        childDisplayElements.each { DisplayElement item ->
            kids.add(item.asMap())
        }
        return kids
    }


    Map getBriefNamespace(Namespace namespace) {
        [
                class          : namespace?.class?.name,
                name           : namespace?.name,
                descriptionHtml: namespace?.descriptionHtml
        ]
    }

    Map getBriefName(Name name) {
        brief(name, [
                nameElement : name?.nameElement,
                fullNameHtml: name?.fullNameHtml
        ])
    }

    Map getBriefReference(Reference reference) {
        brief(reference, [
                citation        : reference?.citation,
                citationHtml    : reference?.citationHtml,
                citationAuthYear: citationAuthYear(reference)
        ])
    }

    Map getBriefInstance(Instance instance) {
        brief(instance, [
                instanceType    : instance?.instanceType?.name,
                page            : instance?.page,
                name            : instance?.name?.fullNameHtml,
                protologue      : instance?.instanceType?.protologue,
                citation        : instance?.reference?.citation,
                citationHtml    : instance?.reference?.citationHtml,
                citationAuthYear: citationAuthYear(instance?.reference)
        ])
    }

    Map getBriefNameWithHtml(Name name) {
        brief(name, [
                nameType       : name?.nameType?.name,
                nameStatus     : name?.nameStatus?.name,
                nameRank       : name?.nameRank?.name,
                primaryInstance: instanceService.findPrimaryInstance(name)?.collect { Instance instance -> getBriefInstance(instance) },
                fullName       : name?.fullName,
                fullNameHtml   : name?.fullNameHtml,
                simpleName     : name?.simpleName,
                simpleNameHtml : name?.simpleNameHtml
        ])
    }

    /** Get instance with html not containing the name. This is used for the picklist of instances belonging to a name */

    Map getBriefInstanceForNameWithHtml(Instance instance) {
        brief(instance, [
                instanceType    : instance?.instanceType?.name,
                page            : instance?.page,
                citation        : instance?.reference?.citation,
                citationHtml    : instance?.reference?.citationHtml,
                citationAuthYear: citationAuthYear(instance?.reference),
                parent          : instance?.parent?.id,
                cites           : instance?.cites?.id,
                citedBy         : instance?.citedBy?.id
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
                class : target.class.name,
                _links: links,
                audit : getAudit(target),
        ]
        String targetKey = target.class.simpleName.toLowerCase()
        Map outer = [:]
        outer[targetKey] = inner
        return outer
    }

    Map getAudit(target) {

        if (target.hasProperty('createdBy') && target.hasProperty('updatedBy')) {
            return [
                    created: [by: target.createdBy, at: target.createdAt],
                    updated: [by: target.updatedBy, at: target.updatedAt]
            ]
        }
        if (target.hasProperty('updatedBy')) {
            return [
                    updated: [by: target.updatedBy, at: target.updatedAt]
            ]
        }
        return null
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
        String link = linkService.getPreferredLinkForObject(target)
        if (link) {
            ResourceLink resourceLink = new ResourceLink(1 as Integer, link, true)
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
                instancesForCitedBy: instance.instancesForCitedBy.sort {
                    Instance a, Instance b ->
                        a.instanceType.sortOrder != b.instanceType.sortOrder ?
                                a.instanceType.sortOrder <=> b.instanceType.sortOrder :
                                a.name.simpleName <=> b.name.simpleName
                }.collect { getBriefInstance(it) },
                instancesForCites  : instance.instancesForCites.sort {
                    Instance a, Instance b ->
                        a.instanceType.sortOrder != b.instanceType.sortOrder ?
                                a.instanceType.sortOrder <=> b.instanceType.sortOrder :
                                a.name.simpleName <=> b.name.simpleName
                }.collect { getBriefInstance(it) },
                instancesForParent : instance.instancesForParent.sort {
                    Instance a, Instance b ->
                        a.instanceType.sortOrder != b.instanceType.sortOrder ?
                                a.instanceType.sortOrder <=> b.instanceType.sortOrder :
                                a.name.simpleName <=> b.name.simpleName
                }.collect { getBriefInstance(it) },
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
                citationAuthYear : citationAuthYear(reference),
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

    Map briefTree(Tree tree) {
        tree = initializeAndUnproxy(tree)
        Map data = getBaseInfo(tree)
        data.tree << [name: tree.name]
    }

    Map marshallTree(Tree tree) {
        tree = initializeAndUnproxy(tree)
        Map data = getBaseInfo(tree)
        data.tree << [
                name               : tree.name,
                groupName          : tree.groupName,
                referenceId        : tree.referenceId,
                currentVersion     : briefTreeVersion(tree.currentTreeVersion),
                defaultDraftVersion: briefTreeVersion(tree.defaultDraftTreeVersion),
                descriptionHtml    : tree.descriptionHtml,
                linkToHomePage     : tree.linkToHomePage,
                acceptedTree       : tree.acceptedTree,
                versions           : tree.treeVersions.collect { TreeVersion v -> briefTreeVersion(v) }
        ]
    }

    Map briefTreeVersion(TreeVersion treeVersion) {
        if (treeVersion) {
            treeVersion = initializeAndUnproxy(treeVersion)
            return [
                    versionNumber: treeVersion.id,
                    draftName    : treeVersion.draftName
            ]
        }
        return null
    }

    Map marshallTreeVersion(TreeVersion treeVersion) {
        treeVersion = initializeAndUnproxy(treeVersion)
        Map data = getBaseInfo(treeVersion)
        data.treeversion <<
                [
                        versionNumber     : treeVersion.id,
                        draftName         : treeVersion.draftName,
                        tree              : briefTree(treeVersion.tree),
                        firstOrderChildren: treeService.displayElementsToDepth(treeVersion, 1)
                ]
    }

    Map marshallTreeVersionElement(TreeVersionElement treeVersionElement) {
        treeVersionElement = initializeAndUnproxy(treeVersionElement)
        TreeVersionElement parent = treeService.getParentTreeVersionElement(treeVersionElement)
        TreeElement treeElement = treeVersionElement.treeElement
        return [treeElement:
                        [
                                class        : treeElement.class.name,
                                _links       : [
                                        elementLink      : treeVersionElement.elementLink,
                                        taxonLink        : treeVersionElement.taxonLink,
                                        parentElementLink: parent?.elementLink,
                                        nameLink         : treeElement.nameLink,
                                        instanceLink     : treeElement.instanceLink,
                                        sourceElementLink: treeElement.sourceElementLink,
                                ],
                                tree         : briefTree(treeVersionElement.treeVersion.tree),
                                simpleName   : treeElement.simpleName,
                                rankPath     : treeElement.rankPath,
                                namePath     : treeElement.namePath,
                                displayString: treeElement.displayHtml,
                                sourceShard  : treeElement.sourceShard,
                                synonyms     : treeElement.synonyms,
                                profile      : treeElement.profile,
                                children     : treeElementChildren(treeVersionElement)
                        ]
        ]
    }

    Map marshallTreeElement(TreeElement treeElement) {
        treeElement = initializeAndUnproxy(treeElement)
        return [treeElement:
                        [
                                class        : treeElement.class.name,
                                _links       : [
                                        elementLink      : treeElement.elementLink,
                                        parentElementLink: treeElement.parentElement?.elementLink,
                                        nameLink         : treeElement.nameLink,
                                        instanceLink     : treeElement.instanceLink,
                                        sourceElementLink: treeElement.sourceElementLink,
                                ],
                                simpleName   : treeElement.simpleName,
                                rankPath     : treeElement.rankPath,
                                namePath     : treeElement.namePath,
                                displayString: treeElement.displayHtml,
                                sourceShard  : treeElement.sourceShard,
                                synonyms     : treeElement.synonyms,
                                profile      : treeElement.profile
                        ],
                NOTE       : 'You probably want a TreeVersionElement'
        ]
    }

    /*** Old tree ***************************************/

    Map marshallLink(Link link) {
        // links do not have mapper ids in and of themselves.
        // so rather than use brief(), this gets done by hand

        def data
        link = initializeAndUnproxy(link)

        if (link.subnode.internalType == NodeInternalType.V) {
            data = getBriefLiteralLinkNoSupernode(link)
        } else {
            data = getBriefLinkNoSupernode(link)
        }

        data << [
                superNode: brief(link.supernode, [id: link.supernodeId]),
                namespace: getBriefNamespace(link.supernode.root.namespace),
        ]

        return data
    }

    Map getBriefLinkNoSupernode(Link link) {
        // links do not have mapper ids in and of themselves.
        // so rather than use brief(), this gets done by hand

        Map subNode = brief(link.subnode, [
                id     : link.subnodeId,
                type   : link.subnode.internalType.name(),
                typeUri: getBriefTreeUri(DomainUtils.getNodeTypeUri(link.subnode)),
        ])

        Map data = [
                class           : link.class.name,
                typeUri         : getBriefTreeUri(DomainUtils.getLinkTypeUri(link)),
                subNode         : subNode,
                linkSeq         : link.linkSeq,
                versioningMethod: link.versioningMethod,
                isSynthetic     : link.synthetic,
        ]

        return data
    }

    Map getBriefLiteralLinkNoSupernode(Link link) {
        // links do not have mapper ids in and of themselves.
        // so rather than use brief(), this gets done by hand

        ValueNodeUri uri = ValueNodeUri.find {
            root == (link.supernode.root.baseArrangement ?: link.supernode.root) &&
                    linkUriNsPart == link.typeUriNsPart &&
                    linkUriIdPart == link.typeUriIdPart
        }
        Map data = [
                class      : link.class.name,
                linkTypeUri: getBriefTreeUri(DomainUtils.getLinkTypeUri(link)),
                linkSeq    : link.linkSeq,
                valueType  : getBriefTreeUri(DomainUtils.getNodeTypeUri(link.subnode)),
                valueUri   : DomainUtils.hasResource(link.subnode) ? getBriefTreeUri(DomainUtils.getResourceUri(link.subnode)) : null,
                value      : DomainUtils.hasResource(link.subnode) ? null : link.subnode.literal,
                label      : uri?.label,
                title      : uri?.title
        ]

        return data
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
                isSynthetic: node.synthetic,
                namespace  : getBriefNamespace(node.root.namespace),

                // a node's sublinks are part-of the node itself, so the JSON should provide them
                // a node's supernodes are not part-of the node. we provide separate 'get placements of node' services

                subnodes   : node.subLink.sort { Link a, Link b ->
                    if (a.subnode.internalType != b.subnode.internalType) {
                        return a.subnode.internalType <=> b.subnode.internalType
                    } else if (a.subnode.internalType == NodeInternalType.T) {
                        return (a.subnode.name?.fullName ?: '') <=> (b.subnode.name?.fullName ?: '')
                    } else {
                        return a <=> b
                    }
                }.findAll { it.subnode.internalType != NodeInternalType.V }.collect { getBriefLinkNoSupernode(it) },

                // a node's literal values are also part-of the node itself
                values     : node.subLink.sort().findAll { it.subnode.internalType == NodeInternalType.V }.collect {
                    getBriefLiteralLinkNoSupernode(it)
                },
        ]

        switch (node.internalType) {
            case NodeInternalType.S:
            case NodeInternalType.Z:
                break

            case NodeInternalType.T:
                if (DomainUtils.hasName(node)) data.nameUri = getBriefTreeUri(DomainUtils.getNameUri(node))
                if (DomainUtils.hasTaxon(node)) data.taxonUri = getBriefTreeUri(DomainUtils.getTaxonUri(node))
                if (node.instance) data.instance = getBriefInstance(node.instance)
                if (node.name) data.name = getBriefName(node.name)
        // fall through
            case NodeInternalType.D:
                if (DomainUtils.hasResource(node)) data.resourceUri = getBriefTreeUri(DomainUtils.getResourceUri(node))
                break

        // this should never happen. Value nodes are not directly used by anything, they appear as values on the
        // supernode. However, we will handle the case here.
            case NodeInternalType.V:
                if (DomainUtils.hasResource(node)) {
                    data.resourceUri = getBriefTreeUri(DomainUtils.getResourceUri(node))
                } else {
                    data.literal = node.literal
                }
                break
        }

        return data
    }

    Map marshallArrangement(Arrangement arrangement) {
        Map data = brief(arrangement, [
                arrangementType: arrangement.arrangementType,
                label          : arrangement.label,
                title          : arrangement.title,
                description    : arrangement.description,
                owner          : arrangement.owner,
                shared         : arrangement.shared,
                synthetic      : arrangement.synthetic == 'Y' ? true : arrangement.synthetic == 'N' ? false : null,
                node           : brief(arrangement.node, [:]),
                currentRoot    : arrangement.arrangementType == ArrangementType.P && arrangement.node && arrangement.node.subLink.size() == 1 && arrangement.node.subLink.first().versioningMethod == VersioningMethod.T ? brief(arrangement.node.subLink.first().subnode, [:]) : null,
                namespace      : getBriefNamespace(arrangement.namespace),
                baseArrangement: (brief(arrangement.baseArrangement, [
                        label: arrangement.baseArrangement?.label,
                        title: arrangement.baseArrangement?.title
                ]))
        ])
        return data
    }

    Map marshallEvent(Event event) {
        Map data = brief event, [
                timeStamp: event.timeStamp,
                note     : event.note,
                namespace: getBriefNamespace(event.namespace),
        ]
        return data
    }

    Map marshallTreeServiceException(ServiceException exception) {
        return [
                treeServiceException: [
                        plainText: exception.getLocalizedMessage(),
                        message  : exception.msg,
                ]
        ]
    }

    Map marshallTreeServiceMessage(Message msg) {
        String message
        String rawMessage
        String plainText

        try {
            message = messageSource.getMessage(msg, (Locale) null)
        }
        catch (NoSuchMessageException ex) {
            message = msg.msg.key
        }

        try {
            rawMessage = messageSource.getMessage(msg.msg.getKey(), (Object[]) null, (Locale) null)
        }
        catch (NoSuchMessageException ex) {
            rawMessage = msg.msg.key
        }

        try {
            plainText = msg.getHumanReadableMessage()
        }
        catch (NoSuchMessageException ex) {
            plainText = msg.getLocalisedString()
        }

        return [
                msg       : msg.msg.name(),
                plainText : plainText,
                message   : message,
                rawMessage: rawMessage,
                args      : msg.args.collect { it ->
                    if (it in Node || it in Arrangement || it in Name || it in Instance || it in Reference) {
                        brief(it)
                    } else if (it in Link) {
                        marshallLink(it as Link)
                    } else if (it instanceof Message) {
                        marshallTreeServiceMessage(it)
                    } else {
                        it
                    }
                },
                nested    : msg.nested.collect { Message it -> marshallTreeServiceMessage(it) }
        ]
    }

    /** ****************************************************/

    @SuppressWarnings("GroovyAssignabilityCheck")
    static <T> T initializeAndUnproxy(T entity) {
        if (entity == null) {
            throw new NullPointerException("Entity passed for initialization is null")
        }

        Hibernate.initialize(entity)
        if (entity instanceof HibernateProxy) {
            entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer()
                                                  .getImplementation()
        }
        return entity
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