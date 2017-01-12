<af:sortedInstances instances="${instances}" var="synonym">
  <has-synonym>
    <synonym-type class="${synonym.instanceType.hasLabel}">${synonym.instanceType.hasLabel}:</synonym-type>
    <st:preferedLink target="${synonym.name}" api="api/apni-format">${raw(synonym.name.fullNameHtml)}</st:preferedLink>
    <st:preferedLink target="${synonym.cites ?: synonym}"><i title="Link to use in reference" class="fa fa-link hidden-print"></i></st:preferedLink>
    <name-status class="${synonym.name.nameStatus.name}">${synonym.name.nameStatus.name}</name-status>
  </has-synonym>
</af:sortedInstances>
