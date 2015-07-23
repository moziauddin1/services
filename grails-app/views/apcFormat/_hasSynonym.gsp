<div>
  <af:apcSortedInstances instances="${instances.findAll { !it.instanceType.misapplied && it.instanceType.synonym }}" var="synonym">
    <has-synonym>
      <g:if test="${synonym.instanceType.doubtful}">?</g:if>
      <st:preferedLink target="${synonym}">${raw(synonym.name.fullNameHtml)}</st:preferedLink>
      <name-status class="${synonym.name.nameStatus.name}">${synonym.name.nameStatus.name}</name-status>

      <g:if test="${synonym.instanceType.proParte}">p.p.</g:if>
    </has-synonym>
  </af:apcSortedInstances>
</div>

<div>
  <af:apcSortedInstances instances="${instances.findAll { it.instanceType.misapplied }}" var="synonym">
    <has-synonym>
      <g:if test="${synonym.instanceType.doubtful}"><span class="fa fa-question-circle"></span></g:if>
      <st:preferedLink target="${synonym}">${raw(synonym.name.simpleNameHtml)}</st:preferedLink>

      <g:if test="${synonym.instanceType.misapplied}">
        auct. non <af:author name="${synonym.name}"/>:
      </g:if>
      <g:else>
        <name-status class="${synonym.name.nameStatus.name}">${synonym.name.nameStatus.name}</name-status>
        sensu
      </g:else>
      ${raw(synonym.cites.reference.citationHtml)}<g:if test="${synonym.instanceType.proParte}">, p.p.</g:if>: ${synonym?.cites?.page ?: '-'}
    </has-synonym>
  </af:apcSortedInstances>
</div>
