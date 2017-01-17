<%@ page import="au.org.biodiversity.nsl.*; au.org.biodiversity.nsl.tree.*" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Node ${node?.id}</title>
</head>

<body>

<g:set var="queryService" bean="queryService"/>

<%
    au.org.biodiversity.nsl.Name name = queryService.resolveName(node);
    au.org.biodiversity.nsl.Instance instance = queryService.resolveInstance(node);

    List path = queryService.getLatestPathForNode(node);
%>

<div class="rest-resource-content node-gsp">
    <div>
        <span><st:preferredLink target="${node.root}">${node.root.label ?: "Tree ${node.root.id}"}</st:preferredLink></span>
        <g:each var="l" in="${path.findAll { DomainUtils.getNodeTypeUri(it.subnode).asQName() != 'boatree-voc:classification-root' } .reverse()}">
            &gt;
            <st:preferredLink target="${l.subnode}">${raw(queryService.resolveName(l.subnode)?.simpleNameHtml ?: "unable to resolve name on ${l.subnode}")}</st:preferredLink>
        </g:each>
    </div>

    <h3><g:render template="node"/>
        <br>
        in <g:render template="tree" model="${[tree:node.root]}"/> from <tt> <g:render template="event" model="${[event:node.checkedInAt]}"/></tt>
        to <tt><g:render template="event" model="${[event:node.replacedAt]}"/></tt></h3>

    <dl class="dl-horizontal">
        <dt>Id</dt><dd>${node.id}</dd>
        <dt>Tree</dt><dd><g:render template="tree" model="${[tree: node.root]}"/></dd>
        <g:if test="${name}"><dt>Name</dt><dd><st:preferredLink target="${name}">${raw(name.fullNameHtml)}</st:preferredLink></dd></g:if>
        <g:if test="${instance}"><dt>Instance</dt><dd><st:preferredLink target="${instance}"> in ${raw(instance.reference?.citationHtml)}</st:preferredLink></dd></g:if>
        <dt>Type URI</dt><dd><a href="${DomainUtils.getNodeTypeUri(node).asUri()}">${DomainUtils.getNodeTypeUri(node).asQNameIfOk()}</a></dd>
        <g:if test="${DomainUtils.getNameUri(node)}"><dt>Name URI</dt><dd><a href="${DomainUtils.getNameUri(node).asUri()}">${DomainUtils.getNameUri(node).asQNameIfOk()}</a></dd></g:if>
        <g:if test="${DomainUtils.getTaxonUri(node)}"><dt>Taxon URI</dt><dd><a href="${DomainUtils.getTaxonUri(node).asUri()}">${DomainUtils.getTaxonUri(node).asQNameIfOk()}</a></dd></g:if>
        <g:if test="${DomainUtils.getResourceUri(node)}"><dt>Resource URI</dt><dd><a href="${DomainUtils.getResourceUri(node).asUri()}">${DomainUtils.getResourceUri(node).asQNameIfOk()}</a></dd></g:if>
        <g:if test="${node.literal}"><dt>Literal</dt><dd>${raw(node.literal)}</dd></g:if>
        <g:if test="${node.prev}"><dt>Copy of</dt><dd><g:render template="node" model="${[event: node.prev]}"/></dd></g:if>
        <g:if test="${node.checkedInAt}"><dt>Checked in</dt><dd><g:render template="event" model="${[event: node.checkedInAt]}"/></dd></g:if>
        <g:if test="${node.next}"><dt>Replaced by</dt><dd><g:render template="node" model="${[event: node.next]}"/></dd></g:if>
        <g:if test="${node.replacedAt}"><dt>Replaced</dt><dd><g:render template="event" model="${[event: node.replacedAt]}"/></dd></g:if>
    </dl>

    <%
        List<Link> profdata = node.subLink . findAll { it.subnode.internalType != NodeInternalType.T}.sort( { Link a, Link  b -> DomainUtils.getLinkTypeUri(it)?.asQNameIfOk()?:'' <=> DomainUtils.getLinkTypeUri(it)?.asQNameIfOk()?:''}) ;
    %>

    <g:if test="${profdata}">
        <h3>Profile Data</h3>
        <table width="100%">
                <g:each in="${profdata}">
                    <tr style="vertical-align: top;">
                        <td width="*" style="padding-right: 1em;"><a href="${DomainUtils.getLinkTypeUri(it).asUri()}">
                            ${DomainUtils.getLinkTypeUri(it)?.asQNameIfOk()}
                        </a></td>
                        <td width="100%"><g:render template="node" model="${[node: it.subnode]}"/></td>
                    </tr>
                </g:each>
        </table>

    </g:if>

    <g:if test="${node.subLink .find { it.subnode.internalType == NodeInternalType.T}}">
        <h3>Subtaxa</h3>
        <g:render template="branch" model="${[node: node, depth:2]}"/>
    </g:if>

    <g:render template="links"/>

</div>
</body>
</html>

