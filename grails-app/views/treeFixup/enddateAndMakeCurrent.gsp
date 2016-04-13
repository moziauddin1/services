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

<h1>Fix classification enddate issues
</h1>


<div>
    <p>Fix issues for <strong>${classification.label}.</strong></p>

    <p>This operation will enddate all tree nodes in ${classification.label} that are current but which are not 'visible' to
    the classifiction root.
    </p>

    <p>The following nodes will be enddated:</p>
    <table class="table">
        <g:each in="${nodes}" var="node">
            <tr><td><g:render template="nodeBlock" model="${[node: node]}"/></td></tr>
        </g:each>
    </table>
    <g:link action="doEnddateAndMakeCurrent" class="btn btn-danger pull-right"
            params="${[classificationId: classification.id]}">Perform this operation</g:link>

</div>

</body>
</html>