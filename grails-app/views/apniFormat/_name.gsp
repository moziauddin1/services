<cache:block key="${name.id}">
<div class="name" id="${name.id}">
  <family>
    <g:if test="${familyName}">
      ${raw(familyName.fullNameHtml)} <af:branch name="${name}" tree="APC"><i class="fa fa-code-fork"></i></af:branch>
    </g:if>
  </family>

  <div data-nameId="${name.id}">
    <st:primaryInstance name="${name}" var="primaryInstance">
    %{--do not reformat the next line it inserts a space between the comma and the fullName--}%
    <accepted-name><st:preferedLink target="${name}">${raw(name.fullNameHtml)}</st:preferedLink>
    </accepted-name><name-status class="${name.nameStatus.name}">, ${name.nameStatus.name}</name-status><name-type
      class="${name.nameType.name}">, ${name.nameType.name}</name-type>
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

    <span class="toggleNext vertbar">
      <i class="fa fa-caret-up"></i><i class="fa fa-caret-down" style="display: none"></i>
    </span>

    <g:if test="${name.nameType.cultivar && name.nameType.hybrid}">
      <br><span class="small text-muted">${name.parent?.simpleName}  x ${name.secondParent?.simpleName}</span>
    </g:if>

    <div class="well instances">
      <g:if test="${!references}">No references.</g:if>
      <g:each in="${references}" var="reference">
        <g:render template="/apniFormat/instance"
                  model="[reference: reference, instances: instancesByRef[reference], apc: apc]"/>
      </g:each>
      <div class="btn-group">
        <g:if test="${name.nameType.name != 'common'}">
          <span class="small" title="search for photos in APII">
            <a href="http://www.anbg.gov.au/cgi-bin/apiiName?name=${name.simpleName}">
              <span class="fa-stack">
                <i class="fa fa-picture-o fa-stack-1x"></i>
                <i class="fa fa-search fa-stack-2x see-through"></i>
              </span>
            </a>
          </span>
        </g:if>
      </div>
    </div>
    </st:primaryInstance>
  </div>

</div>
</cache:block>