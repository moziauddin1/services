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
      <th>Census</th>
      <th>Matched name(s)</th>
      <th>Tags</th>
      <g:each in="${results}" var="result">
        <tr>
          <g:if test="${result.found}">
            <td>
              <b>Found</b>
            </td>
            <td>&quot;${result.query}&quot;</td>
            <td>
              <g:each in="${result.names}" var="nameData">
                <g:if test="${(nameData.apc)}">
                  <a href="${g.createLink(absolute: true, controller: 'apcFormat', action: 'display', id: nameData.name.id)}">
                    <g:if test="${(nameData.apc as au.org.biodiversity.nsl.Node)?.typeUriIdPart == 'ApcConcept'}">
                      <apc><i class="fa fa-check"></i>APC</apc>
                    </g:if>
                    <g:else>
                      <apc title="excluded from APC"><i class="fa fa-ban"></i>APC ex.</apc>
                    </g:else>
                  </a>
                </g:if>
                <g:else>&nbsp;</g:else>
                <br>
              </g:each>
            </td>
            <td>
              <g:each in="${result.names}" var="nameData">
                <st:preferedLink target="${nameData.name}" api="api/apniFormat">
                  ${raw(nameData.name.fullNameHtml)}</st:preferedLink><name-status
                  class="${nameData.name.nameStatus.name}">, ${nameData.name.nameStatus.name}</name-status><name-type
                  class="${nameData.name.nameType.name}">, ${nameData.name.nameType.name}</name-type>
                <br>
              </g:each>
            </td>
            <td>
              <g:each in="${result.names}" var="nameData">
                <g:each in="${nameData.name.tags}" var="tag">
                  <name-tag>${tag.tag.name}<i class="fa fa-tag"></i></name-tag>
                </g:each>
                <br>
              </g:each>
            </td>
          </g:if>
          <g:else>
            <td>
              <b>Not Found</b>
            </td>
            <td>&quot;${result.query}&quot;</td>
            <td>&nbsp;</td>
            <td>not found</td>
            <td>&nbsp;</td>
          </g:else>
        </tr>
      </g:each>
    </table>
    <tip><i
        class="fa fa-info"></i> You can copy and paste the above table into a Spreadsheet like Libreoffice Calc or MS Office Excel.
    </tip>
  </g:if>
  <g:else>
    <div class="results">
      <h2>Your results will be here.</h2>
    </div>
  </g:else>
</div>
</body>
</html>