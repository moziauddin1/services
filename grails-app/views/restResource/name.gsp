<%@ page import="au.org.biodiversity.nsl.Name" %>
<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>${name.simpleName}</title>
</head>

<body>
<h2>Name
  <help>
    <i class="fa fa-info-circle"></i>

    <div>
      The unique identifying name (text) referred to in references.
      <ul>
        <li>Below is the Name and protologue.</li>
        <li>At the bottom of this page are the citable links to this Name or just use the <i
            class="fa fa-link"></i> icon.
        You can "right click" in most browsers to copy it or open it in a new browser tab.</li>
      </ul>
    </div>
  </help>
</h2>

<div class="rest-resource-content">

  <div class="name" id="${name.id}">
    <family>
      <g:if test="${familyName}">
        ${raw(familyName.fullNameHtml)} <af:branch name="${name}" tree="APC"><i class="fa fa-code-fork"></i></af:branch>
      </g:if>
    </family>

    <div data-nameId="${name.id}">
      %{--do not reformat the next line it inserts a space between the comma and the fullName--}%
      <accepted-name><st:preferedLink target="${name}">${raw(name.fullNameHtml)}</st:preferedLink>
      </accepted-name><name-status class="${name.nameStatus.name}">, ${name.nameStatus.name}</name-status><name-type
        class="show-always">${name.nameType.name}</name-type>
      <g:if test="${!familyName}">
        <af:branch name="${name}" tree="APC"><i class="fa fa-code-fork"></i></af:branch>
      </g:if>
      <g:each in="${name.tags}" var="tag">
        <name-tag>${tag.tag.name}<i class="fa fa-tag"></i></name-tag>
      </g:each>
      <editor>
        <st:editorLink nameId="${name.id}"><i class="fa fa-edit" title="Edit"></i></st:editorLink>
      </editor>

      <af:apniLink name="${name}"/>

      <span class="vertbar">
        <st:preferedLink target="${name}"><i title="citable link to name" class="fa fa-link"></i></st:preferedLink>
      </span>
      <g:if test="${apc}">
        <span class="vertbar">
          <a href="${g.createLink(controller: 'apcFormat', action: 'display', id: name.id)}">
            <g:if test="${(apc as au.org.biodiversity.nsl.Node)?.typeUriIdPart == 'ApcConcept'}">
              <apc><i class="fa fa-check"></i>APC</apc>
            </g:if>
            <g:else>
              <apc><i class="fa fa-ban"></i>APC</apc>
            </g:else>
          </a>
        </span>
      </g:if>

      <g:if test="${name.nameType.cultivar && name.nameType.hybrid}">
        <br><span class="small text-muted">${name.parent?.simpleName}  x ${name.secondParent?.simpleName}</span>
      </g:if>

      <st:primaryInstance name="${name}" var="instance">
        <g:if test="${!instance}">
          <g:set var="instance" value="${instancesByRef[references[0]][0]}"/>
        </g:if>

        <reference data-referenceId="${instance.reference.id}">
          <ref-citation>
            %{--don't reformat the citationHtml line--}%
            <st:preferedLink target="${instance}">${raw(instance.reference?.citationHtml)}</st:preferedLink>:
          </ref-citation>

          <page><af:page instance="${instance}"/></page>

          <instance-type class="${instance?.instanceType?.name}">[${instance?.instanceType?.name}]</instance-type>
          <span title="Reference link">
            <st:preferedLink target="${instance.reference}"><i class="fa fa-book"></i></st:preferedLink>
          </span>
          <span class="vertbar">
            <a href="${g.createLink(controller: 'search', params: [publication: instance.reference?.citation, search: true, advanced: true, display: 'apni'])}"
               title="Search for names in this reference.">
              <i class="fa fa-search"></i></a>
          </span>
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

            <g:if test="${instance.cites}">
              <has-synonym>
                Cites <synonym-type
                  class="${instance.cites.instanceType.name}">${instance.cites.instanceType.name}:</synonym-type>
                <st:preferedLink target="${instance.cites}">${raw(instance.cites.name.fullNameHtml)}</st:preferedLink>
                <name-status
                    class="${instance.cites.name.nameStatus.name}">${instance.cites.name.nameStatus.name}</name-status>

                <af:apniLink name="${instance.cites.name}"/>

              </has-synonym>
            </g:if>

            <g:if test="${instance.instancesForCitedBy}">
              <g:render template="/apniFormat/hasSynonym"
                        model="[instances: instance.instancesForCitedBy.findAll { it.instanceType.nomenclatural }]"/>
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

            <g:if test="${instance.instancesForCites}">
              <h4>Nomenclatural links</h4>
              <af:sortedInstances instances="${instance.instancesForCites.findAll { it.instanceType.nomenclatural }}"
                                  var="synonym">
                <g:render template="/apniFormat/synonymOf" model="[instance: synonym]"/>
              </af:sortedInstances>

              <g:render template="/apniFormat/missapplication"
                        model="[instances: instance.instancesForCites.findAll { it.instanceType.misapplied }]"/>
            </g:if>

          </instance>
        </reference>
      </st:primaryInstance>
    </div>

  </div>

  <g:render template="links"/>

</div>

</body>
</html>

