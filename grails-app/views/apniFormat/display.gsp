<%--
  User: pmcneil
  Date: 15/09/14
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>APNI - ${name.simpleName}</title>
</head>

<body>
<div class="${params.product}">
  <g:render template="/search/searchTabs"/>
  <g:set var="panelClass"
         value="panel ${((params.product == st.primaryClassification()) ? 'panel-success' : 'panel-info')}"/>

  <div class="${panelClass}">
    <div class="panel-heading">
        <strong>Showing ${name.simpleName}</strong>

      <div class="btn-group">
        <button id="fontToggle" class="btn btn-default" title="change font"><i class="fa fa-font"></i></button>
      </div>
    </div>

    <div class="panel-body">
      <div class="results">
        <g:render template="name"
                  model="[name: name, treeVersionElement: treeVersionElement, preferredNameLink: preferredNameLink]"/>
      </div>
    </div>

  </div>
</div>
</body>
</html>
