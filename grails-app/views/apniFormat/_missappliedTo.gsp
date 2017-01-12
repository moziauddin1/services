<missapplied-to>
  ${instance.instanceType.ofLabel}:
  <st:preferedLink target="${instance.citedBy.name}" api="api/apni-format"><st:encodeWithHTML text='${instance.citedBy.name.fullNameHtml}'/></st:preferedLink>
  <st:preferedLink target="${instance.citedBy}"><i title="Link to use in reference" class="fa fa-link hidden-print"></i></st:preferedLink>
  <name-status class="${instance.citedBy.name.nameStatus.name}">${instance.citedBy.name.nameStatus.name}</name-status>

  by <st:encodeWithHTML text='${instance?.cites?.reference?.citationHtml}'/>: ${instance?.cites?.page ?: '-'}

</missapplied-to>