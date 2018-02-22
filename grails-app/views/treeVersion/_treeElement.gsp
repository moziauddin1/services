<div class="tr">
  <div class="text-muted">
    Updated by ${tve.updatedBy} <date>${tve.updatedAt}</date>
  </div>

  <div class="tr ${tve.treeElement.excluded ? 'excluded' : ''} level${tve.depth}">
    <div class="wrap">
      <a href="${tve.elementLink}" title="link to tree element">${raw(tve.treeElement.displayHtml)}</a>
      <a href="${tve.treeElement.nameLink}/api/apni-format?versionId=${tve.treeVersionId}&draft=true"
         title="View name in APNI format.">
        <i class="fa fa-list-alt see-through"></i>
      </a>
      ${raw(tve.treeElement.synonymsHtml)}
    </div>
  </div>
  <g:if test="${tve.treeElement.profile}">
    <dl class="dl-horizontal">
      <g:each in="${tve.treeElement.profile}" var="profile">
        <dt>${profile.key}</dt><dd>${profile.value['value']}</dd>
      </g:each>
    </dl>
  </g:if>
</div>