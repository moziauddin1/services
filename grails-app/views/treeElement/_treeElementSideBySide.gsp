<div class="tr">
  ${raw(tve.treeElement.displayHtml)}
  <dl class="dl-horizontal">
    <g:each in="${tve.treeElement.profile}" var="profile">
      <dt>${profile.key}</dt><dd>${profile.value['value']}</dd>
    </g:each>
  </dl>
</div>