<%--
  User: pmcneil
  Date: 16/09/14
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>
    Name Check
  </title>
</head>

<body>
<div>

  <g:if test="${names}">
    <g:each in="${names}" var="name">
      <div>
        <g:if test="${name.found}">
          <b>Found</b> &quot;${name.query}&quot; <a
            href="${g.createLink(controller: (params.display + 'Format'), action: 'display', id: name.found.id, params: [product: params.product ?: ''])}">
        %{--do not reformat the next line it inserts a space between the comma and the fullName--}%
          ${raw(name.found.fullNameHtml)}</a><name-status
            class="${name.found.nameStatus.name}">, ${name.found.nameStatus.name}</name-status><name-type
            class="${name.found.nameType.name}">, ${name.found.nameType.name}</name-type>
          <g:if test="${(name.apc)}">
            <a href="${g.createLink(absolute: true, controller: 'apcFormat', action: 'display', id: name.found.id)}">
              <g:if test="${(name.apc as au.org.biodiversity.nsl.Node)?.typeUriIdPart == 'ApcConcept'}">
                <apc><i class="fa fa-check"></i>APC</apc>
              </g:if>
              <g:else>
                <apc title="excluded from APC"><i class="fa fa-ban"></i>APC</apc>
              </g:else>
            </a>
          </g:if>

        </g:if>
        <g:else>
          <b>Not Found</b> &quot;${name.query}&quot;
        </g:else>

      </div>
    </g:each>
  </g:if>
  <g:else>
    <div class="results">
      <h2>Your results will be here.</h2>
  </g:else>
</div>
</body>
</html>