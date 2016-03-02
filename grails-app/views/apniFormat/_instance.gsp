<reference data-referenceId="${reference.id}">

  <af:sortedInstances instances="${instances}" var="instance">
    <g:if test="${newPage}">
      <ref-citation>
        %{--don't reformat the citationHtml line--}%
        <st:preferedLink target="${instance}">${raw(reference?.citationHtml)}</st:preferedLink>:
      </ref-citation>
      <page><af:page instance="${instance}"/></page>
      <g:if test="${instance.sourceId && instance.sourceSystem == 'PLANT_NAME_REFERENCE'}">
        <protologue-pdf
            data-id="https://biodiversity.org.au/images/pnrid-pdf/${instance.sourceId}.pdf">
        </protologue-pdf>
      </g:if>

      <a href="${af.refAPNISearchLink(citation: reference?.citation, product: params.product)}"><i
          class="fa fa-search"></i></a>
    </g:if>
    <g:if test="${(apc as au.org.biodiversity.nsl.Node)?.taxonUriIdPart == instance.id.toString()}">
      <a href="${g.createLink(absolute: true, controller: 'apcFormat', action: 'display', id: name.id)}">
        <g:if test="${(apc as au.org.biodiversity.nsl.Node)?.typeUriIdPart == 'ApcConcept'}">
          <apc><i class="fa fa-check"></i>APC</apc>
        </g:if>
        <g:else>
          <apc title="excluded from APC"><i class="fa fa-ban"></i>APC</apc>
        </g:else>
      </a>
    </g:if>

    <instance-type class="${instance?.instanceType?.name}">[${instance?.instanceType?.name}]</instance-type>
    <instance data-instanceId="${instance.id}">

      <ul class="instance-notes list-unstyled">
        <af:getTypeNotes instance="${instance}" var="instanceNote">
          <li>
            <instance-note-key
                class="${instanceNote.instanceNoteKey.name}">${instanceNote.instanceNoteKey.name}:</instance-note-key>
            <instance-note><af:replaceXics text="${instanceNote.value}"/></instance-note>
          </li>
        </af:getTypeNotes>
      </ul>

      <g:if test="${instance.instanceType.synonym || instance.instanceType.unsourced}">
        <g:render template="/apniFormat/synonymOf" model="[instance: instance]"/>
      </g:if>

      <g:if test="${instance.instancesForCitedBy}">
        <g:render template="/apniFormat/hasSynonym"
                  model="[instances: instance.instancesForCitedBy.findAll { it.instanceType.synonym }]"/>
        <g:render template="/apniFormat/missapplication"
                  model="[instances: instance.instancesForCitedBy.findAll {
                    it.instanceType.name.contains('misapplied')
                  }]"/>
      %{--other synonyms--}%
        <g:render template="/apniFormat/hasSynonym" model="[instances: instance.instancesForCitedBy.findAll {
          (!it.instanceType.synonym && !it.instanceType.name.contains('misapplied'))
        }]"/>
      </g:if>

      <g:if test="${instance.instanceType.misapplied}">
        <g:render template="/apniFormat/missappliedTo" model="[instance: instance]"/>
      </g:if>


      <ul class="instance-notes list-unstyled">
        <af:getDisplayableNonTypeNotes instance="${instance}" var="instanceNote">
          <li>
            <instance-note-key
                class="${instanceNote.instanceNoteKey.name}">${instanceNote.instanceNoteKey.name}:</instance-note-key>
            <instance-note><af:replaceXics text="${instanceNote.value}"/></instance-note>
          </li>
        </af:getDisplayableNonTypeNotes>
      </ul>
    </instance>
  </af:sortedInstances>
</reference>
