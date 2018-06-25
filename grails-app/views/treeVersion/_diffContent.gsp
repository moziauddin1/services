<div class="rest-resource-content tree-gsp">
  <h2>Changes to ${data.payload.v1.tree.name} in ${data.payload.v2.draftName}</h2>

  <g:if test="${data.payload.changed == false}">
    <h3>Nothing to see here.</h3>

    <p>We have no changes, nothing, zip.</p>
  </g:if>
  <g:elseif test="${data.payload.overflow}">
    <h1>Too many changes</h1>

    <p>We have changes, so many changes.</p>
  </g:elseif>
  <g:else>
    <h3>Added</h3>

    <g:if test="${data.payload?.added}">
      <table class="table">
        <g:each in="${data.payload?.added}" var="tve">
          <tr>
            <td>
              <g:render template="treeElement" model="[tve: tve, syn: tve.treeElement.synonymsHtml]"/>
              <tree:profile profile="${tve.treeElement.profile}"/>
            </td>
          </tr>
        </g:each>
      </table>
    </g:if>
    <g:else>
      <p>nothing</p>
    </g:else>

    <h3>Removed</h3>

    <g:if test="${data.payload?.removed}">
      <table class="table">
        <g:each in="${data.payload?.removed}" var="tve">
          <tr>
            <td>
              <g:render template="treeElement" model="[tve: tve, syn: tve.treeElement.synonymsHtml]"/>
              <tree:profile profile="${tve.treeElement.profile}"/>
            </td>
          </tr>
        </g:each>
      </table>
    </g:if>
    <g:else>
      <p>nothing</p>
    </g:else>

    <h3>Modified</h3>

    <g:if test="${data.payload?.modified}">

      <table class="table">
        <thead>
        <tr>
          <th>Before</th>
          <th>After</th>
        </tr>
        </thead>
        <g:each in="${data.payload?.modified}" var="mod">
          <tree:diffPath a="${mod[1].namePath}"
                         b="${mod[0].namePath}">
            <tr class="noBottom sep">
              <td class="diffBefore">
                ${raw(pathA)}
              </td>
              <td class="diffAfter">
                ${raw(pathB)}
              </td>
            </tr>
          </tree:diffPath>
          <tree:diffSynonyms a="${mod[1].treeElement.synonymsHtml}"
                             b="${mod[0].treeElement.synonymsHtml}">
            <tr class="noTop noBottom">
              <td class="diffBefore">
                <g:render template="treeElement" model="[tve: mod[1], syn: diffA]"/>
              </td>
              <td class="diffAfter">
                <g:render template="treeElement" model="[tve: mod[0], syn: diffB]"/>
              </td>
            </tr>
          </tree:diffSynonyms>
          <tr class="noTop">
            <td class="diffBefore">
              <tree:profile profile="${mod[1].treeElement.profile}"/>
            </td>
            <td class="diffAfter">
              <tree:profile profile="${mod[0].treeElement.profile}"/>
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