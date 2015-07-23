<%@ page import="au.org.biodiversity.nsl.tree.DomainUtils" %>
<div class="branch-branch">
    <div class="branch-branch-node">
        <g:render template="branch_node" model="${[node: branch.node]}"/>
        <g:if test="${branch.branchTruncated && branch.subnodesCount > 0}"> <span style="font-size:80%;" class="text-muted">(${branch.subnodesCount} subnodes)</span></g:if>
    </div>
    <g:if test="${branch.subnodesCount > 0 && !branch.branchTruncated}">
        <table class="branch-branch-subnodelist" style="margin-top: .5em; margin-bottom: 1em;" width="100%">
            <g:each in="${branch.subnodes}">
                <tr class="${DomainUtils.getLinkTypeUri(it.link).asCssClass()}">
                    <td style="vertical-align: top"><g:render template="branch_link" model="${[link: it.link]}"/></td>
                    <td style="vertical-align: top"><g:render template="branch_branch" model="${[branch: it]}"/></td>
                </tr>
            </g:each>
        </table>
    </g:if>
</div>
