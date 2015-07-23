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
    <title>Service administration</title>
</head>

<body>

<div class="">

    <g:if test="${flash.message}">
        <div class="row alert alert-info" role="status">${flash.message}</div>
    </g:if>
    <g:if test="${flash.success}">
        <div class="row alert alert-success" role="status">${flash.success}</div>
    </g:if>

    <h1>Classification Management</h1>

    <div>
        <g:link action="createForm">Create a new classification</g:link>
    </div>

    <div>
        Edit/Delete existing classification:
        <ul>
            <g:each in="${list}">
                <li>
                    <g:link action="editForm" params="${[classification: it.label]}">${it.label} &mdash; ${it.description}</g:link>

                </li>
            </g:each>

        </ul>

    </div>

    <div id='logs' class="row">

    </div>
</div>
</body>
</html>