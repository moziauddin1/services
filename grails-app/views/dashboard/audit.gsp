<%--
  User: pmcneil
  Date: 15/09/14
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>NSL Dashboard</title>
</head>

<body>
<div class="container">


  <g:form name="search" role="form" controller="dashboard" action="audit" method="GET">
    <div class="form-group">
      <h2>Audit</h2>
        <label>User name
          <input type="text" name="userName" placeholder="Enter a user name" value="${params.name}"
                 class="form-control" size="30"/>
        </label>
        <button type="submit" name="search" value="true" class="btn btn-primary">Search</button>
    </div>
  </g:form>

  <table class="table">
    <g:each in="${auditRows}" var="row">
      <tr>
        <td>${row.when()}</td>
        <td><b>${row.updatedBy()}</b></td>
        <td>${[U: 'Updated', I: 'Created', D: 'Deleted'].get(row.action)}</td>
        <td>
          <g:if test="${row.auditedObj}">
            <st:nicerDomainString domainObj="${row.auditedObj}"/>
          </g:if>
          <g:else>
            ${"$row.table $row.rowData.id (deleted?)"}
          </g:else>
        </td>
        <td>
          <g:each in="${row.fieldDiffs()}" var="diff">
            <div>
              <div><b>${diff.fieldName}</b></div>

              <div class="diffBefore">${diff.before}</div>

              <div class="diffAfter">${diff.after}</div>
            </div>
          </g:each>
        </td>
      </tr>
    </g:each>
  </table>

</div>
</body>
</html>