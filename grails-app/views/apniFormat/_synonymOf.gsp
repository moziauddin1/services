<synonym-of>
  <synonym-type>${instance.instanceType.name} of:</synonym-type>
  <st:preferedLink target="${instance.citedBy}">${raw(instance.citedBy.name.fullNameHtml)}</st:preferedLink>
  <name-status class="${instance.citedBy.name.nameStatus?.name}">${instance.citedBy.name.nameStatus?.name}</name-status>

  <af:apniLink name="${instance.citedBy.name}"/>

</synonym-of>