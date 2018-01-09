<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>Tree</title>
  <asset:stylesheet src="tree.css"/>

</head>

<body>
<div class="rest-resource-content tree-gsp">
  <g:if test="${data.payload.changed == false}">
    <h1>Nothing to see here.</h1>

    <p>We have no changes, nothing, zip.</p>
  </g:if>
  <g:elseif test="${data.payload.overflow}">
    <h1>Too many changes</h1>

    <p>We have changes, so many changes.</p>
  </g:elseif>
  <g:else>

    <h1>Added</h1>

    <g:if test="${data.payload?.added}">
      <table class="table">
        <g:each in="${data.payload?.added}" var="tve">
          <tr>
            <td><g:render template="treeElement" model="[tve: tve]"/></td>
          </tr>
        </g:each>
      </table>
    </g:if>
    <g:else>
      <p>nothing</p>
    </g:else>

    <h1>Removed</h1>

    <g:if test="${data.payload?.removed}">
      <table class="table">
        <g:each in="${data.payload?.removed}" var="tve">
          <tr>
            <td><g:render template="treeElement" model="[tve: tve]"/></td>
          </tr>
        </g:each>
      </table>
    </g:if>
    <g:else>
      <p>nothing</p>
    </g:else>

    <h1>Modified</h1>

    <g:if test="${data.payload?.modified}">

      <table class="table">
        <thead>
        <tr>
          <th>Before</th>
          <th>After</th>
        </tr>
        </thead>
        <g:each in="${data.payload?.modified}" var="mod">
          <tr class="toggleNextRow">
            <td class="diffBefore">
              <g:render template="treeElementSideBySide" model="[tve: mod[1]]"/>
              (=)<i class="fa fa-caret-up"></i><i class="fa fa-caret-down" style="display: none"></i>
            </td>
            <td class="diffAfter">
              <g:render template="treeElementSideBySide" model="[tve: mod[0]]"/>
            </td>
          </tr>
          <tr style="display: none">
            <td class="diffBefore">
              <div class="tr">
                ${raw(mod[1].treeElement.synonymsHtml)}
              </div>
            </td>
            <td class="diffAfter">
              <div class="tr">
                ${raw(mod[0].treeElement.synonymsHtml)}
              </div>
            </td>
          </tr>
        </g:each>
      </table>
    </g:if>
    <g:else>
      <p>nothing</p>
    </g:else>
  </g:else>
</div>

</body>
</html>