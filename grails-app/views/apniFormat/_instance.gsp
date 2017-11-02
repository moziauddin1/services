<%@ page import="au.org.biodiversity.nsl.ConfigService" %>
<reference data-referenceId="${reference.id}">

  <af:sortedInstances instances="${instances}" var="instance">
    <g:if test="${newPage}">
      <ref-citation>
        %{--don't reformat the citationHtml line--}%
        <st:preferredLink target="${instance}">${raw(reference?.citationHtml)}</st:preferredLink>:
      </ref-citation>
      <page><af:page instance="${instance}"/></page>
      <g:if test="${instance.sourceId && instance.sourceSystem == 'PLANT_NAME_REFERENCE'}">
        <protologue-pdf
            data-id="https://biodiversity.org.au/images/pnrid-pdf/${instance.sourceId}.pdf">
        </protologue-pdf>
      </g:if>
      <g:if test="${instance.bhlUrl}">
        <bhl-link>
          <a href="${instance.bhlUrl}" title="BHL link"><asset:image src="BHL.svg" alt="BHL" height="12"/></a>
        </bhl-link>
      </g:if>

      <a href="${af.refNameTreeSearchLink(citation: reference?.citation, product: params.product)}" class="hidden-print"><i
          class="fa fa-search"></i></a>
    </g:if>

    <af:onTree element="${treeVersionElement}" instance="${instance}"/>

    <instance-type class="${instance?.instanceType?.name}">[${instance?.instanceType?.name}]</instance-type>
    <instance data-instanceId="${instance.id}">

      <ul class="instance-notes list-unstyled">
        <af:getTypeNotes instance="${instance}" var="instanceNote">
          <li>
            <instance-note-key
                class="${instanceNote.instanceNoteKey.name}">${instanceNote.instanceNoteKey.name}:</instance-note-key>
            <instance-note>${instanceNote.value}</instance-note>
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
                    it.instanceType.misapplied && !it.instanceType.unsourced
                  }]"/>
        <g:render template="/apniFormat/hasSynonym"
                  model="[instances: instance.instancesForCitedBy.findAll { it.instanceType.unsourced }]"/>
      %{--other synonyms--}%
        <g:render template="/apniFormat/hasSynonym" model="[instances: instance.instancesForCitedBy.findAll {
          (!it.instanceType.synonym && !it.instanceType.misapplied)
        }]"/>
      </g:if>

      <g:if test="${instance.instanceType.misapplied && !instance.instanceType.unsourced}">
        <g:render template="/apniFormat/missappliedTo" model="[instance: instance]"/>
      </g:if>


      <ul class="instance-notes list-unstyled">
        <af:getDisplayableNonTypeNotes instance="${instance}" var="instanceNote">
          <li>
            <instance-note-key
                class="${instanceNote.instanceNoteKey.name}">${instanceNote.instanceNoteKey.name}:</instance-note-key>
            <instance-note>${instanceNote.value}</instance-note>
          </li>
        </af:getDisplayableNonTypeNotes>
      </ul>
    </instance>
  </af:sortedInstances>
</reference>
