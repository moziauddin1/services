<%--
  Created by IntelliJ IDEA.
  User: ibis
  Date: 9/06/15
  Time: 12:25 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main">
    <title>Tree fixup</title>
</head>

<body>
<br class="">

<g:if test="${flash.message}">
    <div class="row alert alert-info" role="status">${flash.message}</div>
</g:if>
<g:if test="${flash.validation}">
    <div class="row alert alert-warning" role="status">${flash.validation}</div>
</g:if>
<g:if test="${flash.error}">
    <div class="row alert alert-danger" role="status">${flash.error}</div>
</g:if>

<div class="pull-right">
    <g:link controller="Classification" action="index">Back</g:link>
</div>

<h1>Fix name appearing multiple times
</h1>


<div>
    <p><strong>${raw(name.fullNameHtml)} appears multiple times in ${classification.label}.</strong></p>

    <p>The name may appear in more than one place, or it may appear under different instances. In normal use via the appliaciton, this should never happen.</p>

    <p>Choose one of the placements below as the correct node for ${classification.label} to be using. This will cause all child taxa
    of all other nodes to be moved to the chosen node.</p>

    <p>If an error is returned indicating that there are "orphan" nodes, then this means that one of the other nodes has a 'document' type node. We do not
    have a general strategy for dealing with these at this point. More programming may be needed to provide such a facility.
    </p>
</div>

<div>
    <g:each in="${nodes}" var="node">
        <div class="well container-fluid">
            <div class="row">
                <div class="col-md-12">
                    <g:render template="nodeBlock" model="${[node: node]}"/>
                </div></div>

        <g:each in="${node.supLink.findAll({
            it.supernode.root == classification && it.supernode.next == null
        })}"

                var="sup">
            <div class="row">
                <div class="col-md-3">placed as ${sup.typeUriNsPart?.label}:${sup.typeUriIdPart} of </div>

                <div class="col-md-9">
                            <g:render template="nodeBlock" model="${[node: sup.supernode]}"/>
                </div>
            </div>
    </g:each>
            <g:each in="${node.subLink}"
                    var="sup">
                <div class="row">
                    <div class="col-md-3">${sup.typeUriNsPart?.label}:${sup.typeUriIdPart}</div>

                    <div class="col-md-9">

                            <g:render template="nodeBlock" model="${[node: sup.subnode]}"/>
                    </div>
                </div>
            </g:each>

            <g:link action="doUseNameNode" class="btn btn-danger pull-right"
                    params="${[classification: classification.label, nameId: name.id, nodeId: node.id]}">Use this node</g:link>
        </div>
    </g:each>

</div>

</body>
</html>