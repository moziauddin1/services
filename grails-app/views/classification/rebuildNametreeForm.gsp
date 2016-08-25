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

    <div class="pull-right">
        <g:link controller="Admin" action="index">Back</g:link>
    </div>

    <h1>Rebuild Name Tree</h1>

    <div>
        <p>
            This form allows you to build a tree based on the parent_id fields in the name table. This operation relies on
            parent id being populated sensibly, ie: with no loops.
        </p>

        <p class="alert alert-danger">
            If there  is an existing tree with the label that you use, <u>then that tree and all of its history will be erased</u>. <strong>This cannot be undone.</strong>
        </p>

        <p>
            The new tree will be a "name tree" - it will not have instances and cannot be used as a classification.
        </p>
    </div>

    <g:form action="doRebuildNametree" role="form">
        <div class="form-group">

            <label for="nametreeLabelTxt" class="control-label">Name Tree Label</label>

            <g:textField id="nametreeLabelTxt" name="nametreeLabel" class="form-control"
                         placeholder="Put the tree label here and get it right first time, you do not get another warning."
                         value="${nametreeLabel}"/>
            <label for="nametreeDescriptionTxt" class="control-label">Description</label>

            <g:textField id="nametreeDescriptionTxt" name="nametreeDescription" class="form-control"
                         value="${nametreeDescription}"/>
        </div>

        <div class="form-group">
            <g:submitButton name="Rebuild" class="btn btn-primary"/>
        </div>
    </g:form>

    <div id='logs' class="row">

    </div>
</div>

</body>
</html>