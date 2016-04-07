<%@ page import="au.org.biodiversity.nsl.NodeInternalType" %>
<div class="container-fluid">
    <div class="row">
        <div class="col-md-3">Node</div>

        <div class="col-md-9"><g:link mapping="restResource" params="${[action: 'node', shard: node.root.namespace.name, idNumber: node.id]}">${node.id}</g:link> in tree ${node.root?.label} type ${node.typeUriNsPart?.label}:${node.typeUriIdPart}</div>
    </div>

    <g:if test="${node.internalType==NodeInternalType.V}">
        <g:if test="${node.resourceUriNsPart}">
            <div class="row">
                <div class="col-md-3">Resource</div>

                <div class="col-md-9">${node.resourceUriNsPart?.label}:${node.resourceUriIdPart}</div>
            </div>
        </g:if>
        <g:if test="${node.literal}">
            <div class="row">
                <div class="col-md-3">Value</div>

                <div class="col-md-9">${raw(node.literal)}</div>
            </div>
        </g:if>
    </g:if>
    <g:else>

        <div class="row">
            <div class="col-md-3">Checkin</div>

            <div class="col-md-9">${node.checkedInAt?.id}, by ${node.checkedInAt?.authUser}, at ${node.checkedInAt?.timeStamp}
                <g:if test="${node.checkedInAt?.note}">: ${raw(node.checkedInAt?.note)}</g:if></div>
        </div>

        <g:if test="${node.nameUriIdPart || node.name}">
            <div class="row">
                <div class="col-md-3">Name</div>

                <div class="col-md-9">

                    <g:if test="${node.nameUriIdPart}">
                        ${node.nameUriNsPart?.label}:${node.nameUriIdPart}
                    </g:if>
                    <g:if test="${node.name}">
                        ${raw(node.name.fullNameHtml)}
                    </g:if>

                </div>
            </div>

        </g:if>
        <g:if test="${node.taxonUriIdPart || node.instance}">
            <div class="row">
                <div class="col-md-3">Instance</div>

                <div class="col-md-9">

                    <g:if test="${node.taxonUriIdPart}">
                        ${node.taxonUriNsPart?.label}:${node.taxonUriIdPart}
                    </g:if>
                    <g:if test="${node.instance}">
                        ${raw(node.instance.reference.citationHtml)}
                    </g:if>

                </div>
            </div>
        </g:if>
    </g:else>
</div>
