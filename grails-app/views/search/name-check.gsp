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

  <g:if test="${results}">
    <table class="table">
      <th>Found?</th>
      <th>Search term</th>
      <th>Matched name(s)</th>
      <g:each in="${results}" var="result">
        <tr>
          <g:if test="${result.found}">
            <td>
              <b>Found</b>
            </td>
            <td>&quot;${result.query}&quot;</td>
            <td>
              <g:each in="${result.names}" var="nameData">
                <st:preferedLink target="${nameData.name}" api="api/apniFormat">
                  ${raw(nameData.name.fullNameHtml)}</st:preferedLink><name-status
                  class="${nameData.name.nameStatus.name}">, ${nameData.name.nameStatus.name}</name-status><name-type
                  class="${nameData.name.nameType.name}">, ${nameData.name.nameType.name}</name-type>
                <g:if test="${(nameData.apc)}">
                  <a href="${g.createLink(absolute: true, controller: 'apcFormat', action: 'display', id: nameData.name.id)}">
                    <g:if test="${(nameData.apc as au.org.biodiversity.nsl.Node)?.typeUriIdPart == 'ApcConcept'}">
                      <apc><i class="fa fa-check"></i>APC</apc>
                    </g:if>
                    <g:else>
                      <apc title="excluded from APC"><i class="fa fa-ban"></i>APC</apc>
                    </g:else>
                  </a>
                </g:if>
                <br>
              </g:each>
            </td>
          </g:if>
          <g:else>
            <td>
              <b>Not Found</b>
            </td>
            <td>&quot;${result.query}&quot;</td>
            <td>not found</td>
          </g:else>

        </tr>
      </g:each>
    </table>
  </g:if>
  <g:else>
    <div class="results">
      <h2>Your results will be here.</h2>
  </g:else>
</div>
</body>
</html>