<%--
  User: pmcneil
  Date: 15/09/14
--%>
<%@ page import="au.org.biodiversity.nsl.ConfigService" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>NSL Export</title>
</head>

<body>
<div class="container">

  <h2>Export</h2>

  <ul>
    <li><a href="namesCsv">export ${ConfigService.nameTreeName} Names as CSV</a></li>
    <li><a href="taxonCsv">export ${ConfigService.classificationTreeName} Taxon as CSV</a></li>
  </ul>

</div>
</body>
</html>