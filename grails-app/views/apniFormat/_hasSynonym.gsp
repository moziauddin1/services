<af:sortedInstances instances="${instances}" var="synonym">
  <has-synonym>
    <synonym-type class="${synonym.instanceType.name}">${synonym.instanceType.name}:</synonym-type>
    <st:preferedLink target="${synonym.cites ?: synonym}">${raw(synonym.name.fullNameHtml)}</st:preferedLink>
    <name-status class="${synonym.name.nameStatus.name}">${synonym.name.nameStatus.name}</name-status>

    <af:apniLink name="${synonym.name}"/>

  </has-synonym>
</af:sortedInstances>
