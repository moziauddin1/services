<%@ page import="au.org.biodiversity.nsl.*; au.org.biodiversity.nsl.tree.*" %>
<%
    tree = tree as Arrangement
%>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Tree ${tree?.label ?: tree?.id}</title>
</head>

<body>
<div class="rest-resource-content tree-gsp">
    <h3>Tree ${tree?.label ?: tree?.id}</h3>

    <dl class="dl-horizontal">
        <dt>Id</dt><dd>${tree.id}</dd>
        <g:if test="${tree.label}"><dt>Label</dt><dd>${tree.label}</dd></g:if>
        <g:if test="${tree.title}"><dt>Title</dt><dd>${tree.title}</dd></g:if>
        <g:if test="${tree.description}"><dt>Description</dt><dd>${tree.description}</dd></g:if>
        <dt>Root node</dt><dd><td><g:render template="node" model="${[node: tree.node]}"/></td></dd>
        <g:if test="${tree.arrangementType == ArrangementType.P}">
            <dt>Current node</dt><dd><td><g:render template="node" model="${[node: DomainUtils.getSingleSubnode(tree.node)]}"/></td></dd>
        </g:if>
    </dl>

    <% /* TODO: include some stats here */ %>

    <h3>Tree</h3>
    <g:render template="branch" model="${[node: DomainUtils.getSingleSubnode(tree.node), depth:1]}"/>


    <g:render template="links"/>

</div>
</body>
</html>

