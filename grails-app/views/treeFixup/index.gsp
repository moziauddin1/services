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

	<h1>Tree fixup</h1>

	<div>
		<g:link controller="Classification" action="index">Back</g:link>
	</div>
</div>

</body>
</html>