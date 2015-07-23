<%@ page import="au.org.biodiversity.nsl.tree.DomainUtils" %>

<span class="branch_link ${DomainUtils.getLinkTypeUri(link).asCssClass()}
${link.synthetic ? 'synthetic' : ''}
">
    <a href="${DomainUtils.getLinkTypeUri(link).asUri()}">
        ${DomainUtils.getLinkTypeUri(link).idPart ?: link.typeUriNsPart.label ?: link.typeUriNsPart.uri}
    </a>
</span>