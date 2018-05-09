<div class="rest-resource-content tree-gsp">

  <g:if test="${data.ok == false}">
    <h2>Check All Synonyms Report</h2>

    <h3>Problem.</h3>

    <p>${data.status}: ${data.error}</p>
  </g:if>
  <g:else>
    <h2>Check All Synonyms Report for ${tree.name}</h2>

    <h3>Changed Synonymy</h3>

    <g:if test="${data.payload}">
      <g:form controller="treeElement" action="updateSynonymyByInstance">
        <input type="hidden" name="treeId" value="${tree.id}"/>
        <table class="table">
          <thead>
          <tr>
            <th>Synonymy on the Tree</th>
            <th>Current Instance synonymy</th>
          </tr>
          </thead>
          <g:each in="${data.payload}" var="report">
            <tr>
              <td class="diffBefore">
                <div class="text-muted">
                  Updated by ${report.treeVersionElement.updatedBy} <date>${report.treeVersionElement.updatedAt}</date>
                </div>

                <div
                    class="tr ${report.treeVersionElement.treeElement.excluded ? 'excluded' : ''} level${report.treeVersionElement.depth}">
                  <div class="wrap">
                    <a href="${report.treeVersionElement.fullElementLink()}"
                       title="link to tree element">${raw(report.treeVersionElement.treeElement.displayHtml)}</a>
                    <a href="${report.treeVersionElement.treeElement.nameLink}/api/apni-format?versionId=${report.treeVersionElement.treeVersionId}&draft=true"
                       title="View name in APNI format.">
                      <i class="fa fa-list-alt see-through"></i>
                    </a>
                    ${raw(report.treeVersionElement.treeElement.synonymsHtml)}
                  </div>
                </div>
              </td>
              <td class="diffAfter">
                <div class="text-muted">
                  Updated by ?
                </div>

                <div
                    class="tr ${report.treeVersionElement.treeElement.excluded ? 'excluded' : ''} level${report.treeVersionElement.depth}">
                  <div class="wrap">
                    <a href="${report.treeVersionElement.fullElementLink()}"
                       title="link to tree element">${raw(report.treeVersionElement.treeElement.displayHtml)}</a>
                    <a href="${report.treeVersionElement.treeElement.nameLink}/api/apni-format?versionId=${report.treeVersionElement.treeVersionId}&draft=true"
                       title="View name in APNI format.">
                      <i class="fa fa-list-alt see-through"></i>
                    </a>
                    ${raw(report.taxonData.synonymsHtml)}
                  </div>
                </div>

                <shiro:hasRole name="treebuilder">
                  <div class="form-inline" style="float:right">
                    <label>Select
                      <input type="checkbox" name="instances" class="form-control" value="${report.instanceId}"
                             checked="checked"/>
                    </label>
                  </div>
                </shiro:hasRole>
              </td>
            </tr>
          </g:each>
        </table>
        <shiro:hasRole name="treebuilder">
          <g:if test="${tree.defaultDraftTreeVersion}">
            <g:submitButton class="btn btn-primary"
                            name="Update selected in draft '${tree.defaultDraftTreeVersion.draftName}'"/>
          </g:if>
          <g:else>
            <h2 class="text-warning"><i class="fa fa-exclamation-triangle"></i> Please create a draft tree to update.
            </h2>
          </g:else>
        </shiro:hasRole>
      </g:form>
    </g:if>
    <g:else>
      <p>No changes to synonymy found.</p>
    </g:else>
  </g:else>
</div>