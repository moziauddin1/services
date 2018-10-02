<div class="name">
  <family>
    <g:if test="${name.family}">
      ${raw(name.family.fullNameHtml)} <af:branch name="${name}"><i class="fa fa-code-fork"></i></af:branch>
    </g:if>
  </family>

  <div data-nameId="${name.id}">
    <accepted-name>${raw(name.fullNameHtml)}
    </accepted-name><name-status class="${name.nameStatus.name}">, ${name.nameStatus.name}</name-status><name-type
      class="${name.nameType.name}">, ${name.nameType.name}</name-type>
    <g:each in="${name.tags}" var="tag">
      <name-tag>${tag.tag.name}<i class="fa fa-tag"></i></name-tag>
    </g:each>
    <editor class="hidden-print">
      <st:editorLink nameId="${name.id}"><i class="fa fa-edit" title="Edit"></i></st:editorLink>
    </editor>

    %{--<span class="vertbar hidden-print">--}%
    %{--<a href="${preferredNameLink}"><i title="Link to Name" class="fa fa-link"></i></a>--}%
    %{--</span>--}%

    <span class="toggleNext vertbar  hidden-print">
      <i class="fa fa-caret-up"></i><i class="fa fa-caret-down" style="display: none"></i>
    </span>

    <g:if test="${name.nameType.cultivar && name.nameType.hybrid}">
      <br><span class="small text-muted">${name.parent?.simpleName} x ${name.secondParent?.simpleName}</span>
    </g:if>

    <div class="instances">
      <g:if test="${name.apniJson.detail.empty}">No references.</g:if>
      <g:each in="${name.apniJson.detail}" var="ref">
        <reference>
          <ref-citation data-instanceId="${ref.instance_id as int}">${raw(ref.ref_citation_html)}</ref-citation> %{--TODO Link, draft --}%
          <page>${ref.page}</page> %{--TODO handle common name page ~ see af:page--}%
          <g:each in="${ref.resources}" var="resource">
            <a href="${resource.url}" title="${resource.description}">
              <i class="${resource.css_icon}"></i> ${resource.type}
            </a>
          </g:each>
          <a href="${g.createLink(absolute: true, controller: 'search', action: 'search',
              params: [publication: ref.citation, search: true, advanced: true, display: 'apni'])}"
             class="hidden-print"><i class="fa fa-search"></i></a>
          %{--End reference section--}%
          <instance-type class="${ref.instance_type}">[${ref.instance_type}]</instance-type>
          <instance>
            <ul class="instance-notes list-unstyled">
              <g:each in="${ref.type_notes}" var="instanceNote">
                <li>
                  <instance-note-key
                      class="${instanceNote.note_key}">${instanceNote.note_key}:</instance-note-key>
                  <instance-note>${raw(instanceNote.note_value)}</instance-note>
                </li>
              </g:each>
            </ul>

            <g:each in="${ref.synonyms}" var="synonym">
              <g:if test="${synonym.misapplied}">
                <misapplication data_syntype="${synonym.instance_type}">
                  ${synonym.instance_type}: ${raw(synonym.full_name_html)} <name-status
                    class="${synonym.name_status}">${synonym.name_status}</name-status>
                  by ${raw(synonym.citation_html)}
                </misapplication>
              </g:if>
              <g:else>
                <has-synonym data_syntype="${synonym.instance_type}">
                  ${synonym.instance_type}: ${raw(synonym.full_name_html)} <name-status
                    class="${synonym.name_status}">${synonym.name_status}</name-status>
                </has-synonym>
              </g:else>
            </g:each>

            <g:if test="${ref.profile?.dist_key}">
              <ul class="instance-notes list-unstyled">
                <g:if test="${ref.profile.comment_value}">
                  <li>%{--Comment--}%
                    <instance-note-key
                        class="${ref.profile.comment_key}">${ref.profile.comment_key}:</instance-note-key>
                    <instance-note>${raw(ref.profile.comment_value)}</instance-note>
                  </li>
                </g:if>
                <g:if test="${ref.profile.dist_value}">
                  <li>%{--Distribution--}%
                    <instance-note-key
                        class="${ref.profile.dist_key}">${ref.profile.dist_key}:</instance-note-key>
                    <instance-note>${raw(ref.profile.dist_value)}</instance-note>
                  </li>
                </g:if>
              </ul>
            </g:if>
            <ul class="instance-notes list-unstyled">
              <g:each in="${ref.non_type_notes}" var="instanceNote">
                <li>
                  <instance-note-key
                      class="${instanceNote.note_key}">${instanceNote.note_key}:</instance-note-key>
                  <instance-note>${raw(instanceNote.note_value)}</instance-note>
                </li>
              </g:each>
            </ul>

          </instance>
        </reference>
      </g:each>
      <div class="btn-group hidden-print">
        <g:if test="${photo}">
          <span class="small" title="photos in APII">
            <a href="${photo}">
              <span class="fa-stack">
                <i class="fa fa-picture-o fa-stack-2x"></i>
              </span>
            </a>
          </span>
        </g:if>
      </div>
      <hr>
    </div>
  </div>

</div>
