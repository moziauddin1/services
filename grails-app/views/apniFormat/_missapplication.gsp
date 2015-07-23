<af:sortedInstances instances="${instances}" var="instance">
  <misapplication>
    ${instance.instanceType.name.replaceAll('misapplied', 'misapplication')}:
    <st:preferedLink target="${instance.cites}">${raw(instance.cites.name.fullNameHtml)}</st:preferedLink>
    <name-status class="${instance.cites.name.nameStatus.name}">${instance.cites.name.nameStatus.name}</name-status>

    by ${raw(instance?.cites?.reference?.citationHtml)}: ${instance?.cites?.page ?: '-'}

    <af:apniLink name="${instance.cites.name}"/>

  </misapplication>
</af:sortedInstances>