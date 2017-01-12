<%@ page import="au.org.biodiversity.nsl.*; au.org.biodiversity.nsl.tree.DomainUtils" %>
<g:set var="queryService" bean="queryService"/>
<%
    /* should be using the tablib for this */

    Name name = queryService.resolveName(node);
    Instance instance = queryService.resolveInstance(node);

    List<Link> subnodes = new ArrayList<Link>(DomainUtils.getSubtaxaAsList(node));
    Collections.sort(subnodes) { Link a, Link b -> DomainUtils.simpleNameCompare(a.subnode, b.subnode)}

%>
<div class="branch-branch">
    <div class="branch-branch-node">
        <st:preferedLink target="${node}">
            <span class="branch_node ${DomainUtils.getNodeTypeUri(node).asCssClass()}
            ${node.checkedInAt ? node.replacedAt ? 'replaced' : 'current' : 'draft'}
            ${node.synthetic ? 'synthetic' : ''}
            ">

                <g:if test="${DomainUtils.hasName(node)}">
                    <g:if test="${name}">
                        <st:encodeWithHTML text='${name.fullNameHtml}'/>
                    </g:if>
                    <g:else>
                        ${DomainUtils.getNameUri(node)?.asQNameIfOk()}
                    </g:else>
                </g:if>

                <g:if test="${DomainUtils.hasTaxon(node)}">
                    <g:if test="${instance}">
                        in <st:encodeWithHTML text='${instance.reference?.citationHtml}'/>
                    </g:if>
                    <g:else>
                        as ${DomainUtils.getTaxonUri(node)?.asQNameIfOk()}
                    </g:else>
                </g:if>

                <g:elseif test="${DomainUtils.hasResource(node)}">
                    (see: ${DomainUtils.getResourceUri(node)?.asQNameIfOk()})
                </g:elseif>
            </span>
        </st:preferedLink>

        <g:if test="${subnodes && depth < 1 && subnodes.size()>3}">
            <span style="font-size: smaller;"> (${subnodes.size()} subnames)</span>
        </g:if>

    </div>
    <g:if test="${subnodes && (depth >= 1 || subnodes.size()<=3)}">
        <div class="branch-branch-subnodelist" style="margin-top: 0em; margin-bottom: 0em; margin-left:2em;" width="100%">
            <g:each in="${subnodes}">
                <div>
                    <g:render template="branch" model="${[node: it.subnode, depth: subnodes.size() == 1 ? depth : depth-1]}"/>
                </div>
            </g:each>
        </div>
    </g:if>
</div>
