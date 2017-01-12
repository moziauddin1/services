<div>
  <af:apcSortedInstances instances="${instances.findAll { !it.instanceType.misapplied && it.instanceType.synonym }}" var="synonym">
    <has-synonym>
      <g:if test="${synonym.instanceType.doubtful}">?</g:if>
      <st:preferedLink target="${synonym.name}" api="api/apni-format"><st:encodeWithHTML text='${synonym.name.fullNameHtml}'/></st:preferedLink>
      <st:preferedLink target="${synonym}"><i title="Use in reference" class="fa fa-book"></i></st:preferedLink>
      <name-status class="${synonym.name.nameStatus.name}">${synonym.name.nameStatus.name}</name-status>

      <g:if test="${synonym.instanceType.proParte}">p.p.</g:if>
    </has-synonym>
  </af:apcSortedInstances>
</div>

<div>
  <af:apcSortedInstances instances="${instances.findAll { it.instanceType.misapplied }}" var="synonym">
    <has-synonym>
      <g:if test="${synonym.instanceType.doubtful}">?</g:if>
      <st:preferedLink target="${synonym.name}" api="api/apni-format"><st:encodeWithHTML text='${synonym.name.fullNameHtml}'/></st:preferedLink>
      <st:preferedLink target="${synonym}"><i title="Use in reference" class="fa fa-book"></i></st:preferedLink>

      <g:if test="${synonym.instanceType.misapplied}">
        auct. non <af:author name="${synonym.name}"/>:
      </g:if>
      <g:else>
        <name-status class="${synonym.name.nameStatus.name}">${synonym.name.nameStatus.name}</name-status>
        sensu
      </g:else>
      <st:encodeWithHTML text='${synonym.cites.reference.citationHtml}'/><g:if test="${synonym.instanceType.proParte}">, p.p.</g:if>: ${synonym?.cites?.page ?: '-'}
    </has-synonym>
  </af:apcSortedInstances>
</div>
