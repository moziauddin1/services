<%@ page import="au.org.biodiversity.nsl.*; au.org.biodiversity.nsl.tree.DomainUtils" %>
<!DOCTYPE html>
<html>

<g:set var="queryService" bean="queryService"/>

<%
    /* should be using the tablib for this */

    au.org.biodiversity.nsl.Name name = queryService.resolveName(node);
    au.org.biodiversity.nsl.Instance instance = queryService.resolveInstance(node);

    List path = queryService.getLatestPathForNode(node);
%>


<head>
    <meta name="layout" content="main">
    <g:if test="${name}">
        <title>${name.simpleName} in ${node.root.label ?: "Tree ${node.root.id}"}</title>
    </g:if>
    <g:else>
        <title>Branch ${node?.id}</title>
    </g:else>
</head>



<body>
<div class="rest-resource-content branch-gsp">
    <div>
        <span><st:preferedLink target="${node.root}">${node.root.label ?: "Tree ${node.root.id}"}</st:preferedLink></span>
        <g:each var="l" in="${path.findAll { DomainUtils.getNodeTypeUri(it.subnode).asQName() != 'boatree-voc:classification-root' } .reverse()}">
            &gt;
            <% /* We use the grails link rather than the mapper because this is a link to a display page, not to the semweb object */ %>
            <g:link mapping="restResource" params="[namespace: 'api', shard: shard, idNumber: l.subnode.id]" action="branch">${raw(queryService.resolveName(l.subnode)?.simpleNameHtml ?: "unable to resolve name on ${l.subnode}")}</g:link>
        </g:each>
    </div>
    <h3><st:preferedLink target="${name}">${name ? raw(name.simpleNameHtml) : DomainUtils.getNameUri(node)?.asQNameIfOk()}</st:preferedLink> in <st:preferedLink target="${node.root}">${node.root.label ?: "Tree ${node.root.id}"}</st:preferedLink></h3>

    <div id="BRANCH-CONTENT-CONTAINER">
        <g:render template="branch_branch" model="${[node: node, depth:2]}"/>
    </div>

    <g:render template="links"/>
</div>
</body>
</html>

