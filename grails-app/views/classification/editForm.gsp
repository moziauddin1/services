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
    <g:if test="${flash.validation}">
        <div class="row alert alert-warning" role="status">${flash.validation}</div>
    </g:if>
    <g:if test="${flash.error}">
        <div class="row alert alert-danger" role="status">${flash.error}</div>
    </g:if>

    <h1>Edit ${classification?.label}</h1>

    <div>
        <g:link action="index">Back</g:link>
    </div>

    <g:form action="doEdit" role="form">
        <g:hiddenField name="classification" value="${classification.label}"/>

        <g:render template="formFields"/>

        <g:if test="${needsConfirmation}">
            <div class="bg-warning form-group">
                <g:hiddenField name="confirmationAction" value="${confirmationAction}"/>
                <div class="col-sm-2">&nbsp;</div>

                <div class="col-sm-10 alert alert-danger ">
                    <div class="col-sm-12 text-danger">
                        <i class="fa fa-warning"></i> ${confirmationMessage}
                    </div>

                    <div class="col-sm-12">
                        <div class="btn-group">
                            <g:submitButton name="Ok" class="btn btn-danger"/>
                            <g:submitButton name="Cancel" class="btn btn-inverse"/>
                        </div>
                    </div>
                </div>
            </div>
        </g:if>

        <g:else>
            <div class="col-sm-2">&nbsp;</div>

            <div class="col-sm-10">
                <div class="form-group btn-group">
                    <g:submitButton name="Save" class="btn btn-primary"/>
                    <g:submitButton name="Delete" class="btn btn-danger"/>
                </div>
            </div>
        </g:else>
    </g:form>

    <div id='logs' class="row">

    </div>
</div>

</body>
</html>