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
%>

<%/*

TODO: This page should display profile data for taxonomic nodes. This leaves the branch page free to display only branch

*/%>


<div class="rest-resource-content node-gsp">
    <h3><g:render template="node"/>
        <br>
        in
        ${node.root.label ?: "Tree ${node.root.id}"}, from <tt>${node.checkedInAt?.timeStamp ?: '(draft)'}</tt> to <tt>${node.replacedAt?.timeStamp ?: 'current'}</tt></h3>

    <g:link mapping="restResource" params="[namespace: 'api', shard: shard, idNumber: node.id]" action="branch">View branch</g:link>.

    <dl class="dl-horizontal">
        <dt>Id</dt><dd>${node.id}</dd>
        <dt>Tree</dt><dd><g:render template="branch_tree" model="${[tree: node.root]}"/></dd>
        <g:if test="${name}"><dt>Name</dt><dd><g:link mapping="restResource" params="[namespace: 'api', shard: shard, idNumber: name.id]" action="name">${raw(name.fullNameHtml)}</g:link></dd></g:if>
        <g:if test="${instance}"><dt>Instance</dt><dd><g:link mapping="restResource" params="[namespace: 'api', shard: shard, idNumber: instance.id]" action="instance">in ${raw(instance.reference?.citationHtml)}</g:link></dd></g:if>
        <dt>Type URI</dt><dd><a href="${DomainUtils.getNodeTypeUri(node).asUri()}">${DomainUtils.getNodeTypeUri(node).asQNameIfOk()}</a></dd>
        <g:if test="${DomainUtils.getNameUri(node)}"><dt>Name URI</dt><dd><a href="${DomainUtils.getNameUri(node).asUri()}">${DomainUtils.getNameUri(node).asQNameIfOk()}</a></dd></g:if>
        <g:if test="${DomainUtils.getTaxonUri(node)}"><dt>Taxon URI</dt><dd><a href="${DomainUtils.getTaxonUri(node).asUri()}">${DomainUtils.getTaxonUri(node).asQNameIfOk()}</a></dd></g:if>
        <g:if test="${DomainUtils.getResourceUri(node)}"><dt>Resource URI</dt><dd><a href="${DomainUtils.getResourceUri(node).asUri()}">${DomainUtils.getResourceUri(node).asQNameIfOk()}</a></dd></g:if>
        <g:if test="${node.literal}"><dt>Literal</dt><dd>${raw(node.literal)}</dd></g:if>
        <g:if test="${node.checkedInAt}"><dt>Checked in</dt><dd><g:render template="event" model="${[event: node.checkedInAt]}"/></dd></g:if>
        <g:if test="${node.replacedAt}"><dt>Replaced</dt><dd><g:render template="event" model="${[event: node.replacedAt]}"/></dd></g:if>
    </dl>

    <%
        List<Node> allprevs = [];
        List<Node> allnexts = [];
        List<Link> allSupers = [];
        List<Link> allSubs = [];


        if (true) {
            Node n;

            n = node.prev;
            while (n) {
                allprevs.add(n);
                n = n.prev;
            }
            allprevs.sort { Node a, Node b -> QueryService.sortEventsByTime(a.checkedInAt, b.checkedInAt) }
            n = node.next;
            while (n) {
                allnexts.add(n);
                n = n.next;
            }
            allnexts.sort { Node a, Node b -> QueryService.sortEventsByTime(a.checkedInAt, b.checkedInAt) }

            allSupers.addAll(node.supLink);
            allSupers.sort { Link a, Link b -> QueryService.sortEventsByTime(a.supernode.checkedInAt, b.supernode.checkedInAt) }

            allSubs.addAll(node.subLink);
            allSubs.sort { Link a, Link b -> return (b.linkSeq ?: 0) - (a.linkSeq ?: 0) }
        }
    %>


    <table width="100%">
        <g:if test="${allprevs}">
            <tr><td colspan="4"><h3>Previous Versions</h3></td></tr>
            <g:each in="${allprevs}">
                <tr style="vertical-align: top;">
                    <TD>
                        <g:render template="branch_tree" model="${[tree: it.root]}"/>
                    </TD>
                    <td><g:render template="event" model="${[event: it.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.replacedAt, ifnull: '(current)']}"/></td>
                    <td></td>
                    <td><g:render template="node" model="${[node: it]}"/></td>
                </tr>
            </g:each>
        </g:if>


        <g:if test="${allnexts}">
            <tr><td colspan="4"><h3>Subsequent Versions</h3></td></tr>
            <g:each in="${allnexts}">
                <tr style="vertical-align: top;">
                    <TD><g:render template="branch_tree" model="${[tree: it.root]}"/></TD>
                    <td><g:render template="event" model="${[event: it.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.replacedAt, ifnull: '(current)']}"/></td>
                    <td></td>
                    <td><g:render template="node" model="${[node: it]}"/></td>
                </tr>
            </g:each>
        </g:if>

        <g:if test="${allSupers}">
            <tr><td colspan="4"><h3>Placements</h3></td></tr>
            <g:each in="${allSupers}">
                <tr style="vertical-align: top;">
                    <TD>
                        <g:render template="branch_tree" model="${[tree: it.supernode.root]}"/>
                    </TD>
                    <td><g:render template="event" model="${[event: it.supernode.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.supernode.replacedAt, ifnull: '(current)']}"/></td>
                    <td>(as) <g:render template="branch_link" model="${[link: it]}"/></td>
                    <td><g:render template="node" model="${[node: it.supernode]}"/></td>
                </tr>
            </g:each>
        </g:if>

        <g:if test="${allSubs}">
            <tr><td colspan="4"><h3>Subnodes</h3></td></tr>
            <g:each in="${allSubs}">
                <tr style="vertical-align: top;">
                    <td colspan="3"></td>
                    <td><g:render template="branch_link" model="${[link: it]}"/></td>
                    <td><g:render template="node" model="${[node: it.subnode]}"/></td>
                </tr>
            </g:each>
        </g:if>
    </table>


    <g:render template="links"/>

</div>
</body>
</html>

