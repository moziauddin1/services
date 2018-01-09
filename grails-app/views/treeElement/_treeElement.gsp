<div class="tr">
  <a href="${tve.elementLink}">
    ${raw(tve.treeElement.displayHtml)}
  </a>
  <g:if test="${tve.treeElement.profile}">
    <dl class="dl-horizontal">
      <g:each in="${tve.treeElement.profile}" var="profile">
        <dt>${profile.key}</dt><dd>${profile.value['value']}</dd>
      </g:each>
    </dl>
  </g:if>
  <g:if test="${tve.treeElement.synonymsHtml && tve.treeElement.synonymsHtml != '<synonyms></synonyms>'}">
    <span class="toggleNext hidden-print">(=)
      <i class="fa fa-caret-up"></i><i class="fa fa-caret-down" style="display: none"></i>
    </span>

    <div style="display: none">
      ${raw(tve.treeElement.synonymsHtml)}
    </div>
  </g:if>
</div>