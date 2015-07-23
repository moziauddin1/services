<%@ page import="au.org.biodiversity.nsl.Node" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Branch ${node?.id}</title>
</head>

<body>
<div class="rest-resource-content branch-gsp">
    <h3><g:render template="branch_node"/><br>
        in <g:render template="branch_tree" model="${[tree: node.root]}"/>, from <tt>${node.checkedInAt?.timeStamp ?: '(draft)'}</tt> to <tt>${node.replacedAt?.timeStamp ?: 'current'}</tt></h3>

    <table width="100%">
        <g:if test="${tree.paths}">
            <tr><td colspan="4"><h3>Placements</h3></td></tr>

            <g:each status="ppct" var="pp" in="${tree.paths.sort({a,b->a.get(0).from.supernode.checkedInAt.timeStamp.compareTo(b.get(0).from.supernode.checkedInAt.timeStamp)})}">
                <g:if test="ppct != 0">
                    <tr><td colspan="4">&nbsp;</td></tr>
                </g:if>
                <g:each var="placement" status="placement_idx" in="${pp}">
                    <tr style="vertical-align: top;">
                        <g:if test="${placement_idx == 0}">
                            <TD rowspan="${pp.size()}"><g:render template="branch_tree" model="${[tree: pp.get(0).from.supernode.root]}"/></TD>
                            <td rowspan="${pp.size()}><g:render template="event" model="${[event: pp.get(0).to.supernode.checkedInAt, ifnull: '(draft)']}"/></td>
                            <td rowspan=" ${pp.size()}><g:render template="event" model="${[event: pp.get(0).to.supernode.replacedAt, ifnull: '(current)']}"/></td>
                        </g:if>
                        <td><g:render template="branch_link" model="${[link: placement.from]}"/></td>
                        <td><g:render template="branch_node" model="${[node: placement.from.subnode]}"/></td>
                    </tr>
                </g:each>
            </g:each>

        </g:if>

        <g:if test="${tree.prev}">
            <tr><td colspan="4"><h3>Previous Versions</h3></td></tr>
            <g:each in="${tree.prev}">
                <tr>
                    <TD><g:render template="branch_tree" model="${[tree: it.root]}"/></TD>
                    <td><g:render template="event" model="${[event: it.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.replacedAt, ifnull: '(current)']}"/></td>
                    <td></td>
                    <td><g:render template="branch_node" model="${[node: it]}"/></td>
                </tr>
            </g:each>
        </g:if>

        <g:if test="${tree.merges}">
            <tr><td colspan="4"><h3>Merges</h3></td></tr>
            <g:each in="${tree.merges}">
                <tr>
                    <TD><g:render template="branch_tree" model="${[tree: it.root]}"/></TD>
                    <td><g:render template="event" model="${[event: it.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.replacedAt, ifnull: '(current)']}"/></td>
                    <td></td>
                    <td><g:render template="branch_node" model="${[node: it]}"/></td>
                </tr>
            </g:each>
        </g:if>

        <g:if test="${tree.copies}">
            <tr><td colspan="4"><h3>Copies</h3></td></tr>
            <g:each in="${tree.copies}">
                <tr>
                    <TD><g:render template="branch_tree" model="${[tree: it.root]}"/></TD>
                    <td><g:render template="event" model="${[event: it.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.replacedAt, ifnull: '(current)']}"/></td>
                    <td></td>
                    <td><g:render template="branch_node" model="${[node: it]}"/></td>
                </tr>
            </g:each>
        </g:if>

        <g:if test="${tree.next}">
            <tr><td colspan="4"><h3>Subsequent Versions</h3></td></tr>
            <g:each in="${tree.next}">
                <tr>
                    <TD><g:render template="branch_tree" model="${[tree: it.root]}"/></TD>
                    <td><g:render template="event" model="${[event: it.checkedInAt, ifnull: '(draft)']}"/></td>
                    <td><g:render template="event" model="${[event: it.replacedAt, ifnull: '(current)']}"/></td>
                    <td></td>
                    <td><g:render template="branch_node" model="${[node: it]}"/></td>
                </tr>
            </g:each>
        </g:if>
    </table>

    <h3>Branch</h3>

    <div id="BRANCH-CONTENT-CONTAINER">
        <g:render template="branch_branch" model="${[branch: tree.branch]}"/>
    </div>

    <g:render template="links"/>
</div>
</body>
</html>

