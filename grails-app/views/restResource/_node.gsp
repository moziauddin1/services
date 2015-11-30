<%@ page import="au.org.biodiversity.nsl.Instance; au.org.biodiversity.nsl.tree.DomainUtils" %>
<%@ page import="au.org.biodiversity.nsl.NodeInternalType" %>

<tree:getNodeNameAndInstance node="${node}">
    <span class="node ${DomainUtils.getNodeTypeUri(node).asCssClass()}
    ${node.checkedInAt ? node.replacedAt ? 'replaced' : 'current' : 'draft'}
    ${node.synthetic ? 'synthetic' : ''}
    ">
        <g:if test="${node.internalType == NodeInternalType.S}">
            System node <st:preferedLink target="${node}">${node}</st:preferedLink>
        </g:if>
        <g:elseif test="${node.internalType == NodeInternalType.Z}">
            Temporary node  <st:preferedLink target="${node}">${node}</st:preferedLink>
        </g:elseif>
        <g:elseif test="${node.internalType == NodeInternalType.T}">
            <g:if test="${DomainUtils.hasName(node)}">
                <g:if test="${name}">
                    ${raw(name.simpleNameHtml)}
                </g:if>
                <g:else>
                    ${DomainUtils.getNameUri(node)?.asQNameIfOk()}
                </g:else>
            </g:if>

            <g:if test="${DomainUtils.hasTaxon(node)}">
                <g:if test="${instance}">
                    in ${raw(node.instance.reference?.citationHtml)}
                </g:if>
                <g:else>
                    as ${DomainUtils.getTaxonUri(node)?.asQNameIfOk()}
                </g:else>
            </g:if>

            <g:elseif test="${DomainUtils.hasResource(node)}">
                (see: ${DomainUtils.getResourceUri(node)?.asQNameIfOk()})
            </g:elseif>
        </g:elseif>
        <g:elseif test="${node.internalType == NodeInternalType.D || node.internalType == NodeInternalType.V}">
            <g:if test="${node.resourceUriIdPart}">
                <a href="${DomainUtils.getResourceUri(node).asUri()}">
                    <g:if test="${node.literal}">
                        ${node.literal}
                    </g:if>
                    <g:else>
                        ${DomainUtils.getResourceUri(node).asQNameIfOk()}
                    </g:else>
                </a>
            </g:if>
            <g:else>${node.literal}</g:else>
        </g:elseif>
    </span>
</tree:getNodeNameAndInstance>
