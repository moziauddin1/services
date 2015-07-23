<%--
  User: pmcneil
  Date: 16/09/14
--%>

<%@ page import="au.org.biodiversity.nsl.Arrangement; au.org.biodiversity.nsl.NameRank; au.org.biodiversity.nsl.NameType" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>
    <g:if test="${params.product}">
      ${params.product.toUpperCase()} -
    </g:if>
    <g:else>Search -</g:else>
    <g:if test="${query.name}">${query.name}?</g:if>
    <g:if test="${query.sparql}">SPARQL</g:if>
  </title>
</head>

<body>
<div class="${params.product}">

  <g:render template="/search/searchTabs"/>

  <g:if test="${names}">
    <div class="panel  ${(params.product == 'apc' ? 'panel-success' : 'panel-info')} ">
      <div class="panel-heading">
        <g:if test="${names}">
          <strong>Found ${count} names</strong>
          <span class="text-muted">Limited to ${max} result<g:if test="${max == 0 || max > 1}">s</g:if>.</span>
        </g:if>
        <div class="btn-group hideSearch">
          <button id="expandAll" class="btn btn-success"><i class="fa fa-caret-down"></i> view detail</button>
          <button id="collapseAll" class="btn btn-primary"><i class="fa fa-caret-up"></i> hide detail</button>
        </div>

        <div class="btn-group">
          <button id="fontToggle" class="btn btn-default" title="change font"><i class="fa fa-font"></i></button>
        </div>
        <div class="text text-info">
          <g:message code="product.search.tip.${params.product}" default=""/>
        </div>
      </div>

      <div class="panel-body">
        <div class="results">
          <g:each in="${names}" var="name">
            <div>
              <div class='unfetched name' id="${name.id}" data-format="${params.display}Format"
                   data-nameId="${name.id}" data-product="${params.product ?: ''}">
                <a href="${g.createLink(controller: (params.display + 'Format'), action: 'display', id: name.id, params: [product: params.product ?: ''])}">
                %{--do not reformat the next line it inserts a space between the comma and the fullName--}%
                  ${raw(name.fullNameHtml)}</a><name-status
                  class="${name.nameStatus.name}">, ${name.nameStatus.name}</name-status><name-type
                  class="${name.nameType.name}">, ${name.nameType.name}</name-type>
                <editor>
                  <a href="http://biodiversity.org.au/nsl-editor/search?query_on=name&query_field=id&query=${name.id}">
                    <span class="fa fa-edit" title="Edit"></span>
                  </a>
                </editor>
                |
                <a class="loadFormat"
                   href="${g.createLink(controller: (params.display + 'Format'), action: 'name', id: name.id)}">
                  <i class="fa fa-caret-down"></i>
                </a>
              </div>
            </div>
            <hr>
          </g:each>
        </div>
      </div>
    </div>
  </g:if>

  <g:if test="${query.sparql}">
    <g:render template="/search/sparql-results"/>
  </g:if>
</div>
</body>
</html>