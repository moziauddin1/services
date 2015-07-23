<missapplied-to>
  ${instance.instanceType.name} to:
  <st:preferedLink target="${instance.citedBy}">${raw(instance.citedBy.name.fullNameHtml)}</st:preferedLink>
  <name-status class="${instance.citedBy.name.nameStatus.name}">${instance.citedBy.name.nameStatus.name}</name-status>

  by ${raw(instance?.cites?.reference?.citationHtml)}: ${instance?.cites?.page ?: '-'}

  <af:apniLink name="${instance.citedBy.name}"/>

</missapplied-to>