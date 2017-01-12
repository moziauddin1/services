<af:sortedInstances instances="${instances}" var="instance">
  <misapplication>
    ${instance.instanceType.hasLabel}:
    <st:preferedLink target="${instance.cites.name}" api="api/apni-format"><st:encodeWithHTML text='${instance.cites.name.fullNameHtml}'/></st:preferedLink>
    <st:preferedLink target="${instance.cites}"><i title="Link to use in reference" class="fa fa-link hidden-print"></i></st:preferedLink>
    <name-status class="${instance.cites.name.nameStatus.name}">${instance.cites.name.nameStatus.name}</name-status>

    by <st:encodeWithHTML text='${instance?.cites?.reference?.citationHtml}'/>: ${instance?.cites?.page ?: '-'}

  </misapplication>
</af:sortedInstances>