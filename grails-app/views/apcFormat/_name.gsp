  <div class="apc name" id="${name.id}">
    <st:primaryInstance var="primaryInstance" name="${name}">
      <g:if test="${apcInstance}">

        <div data-nameId="${name.id}">
          <g:if test="${!excluded}">
            <accepted-name title='Accepted name'><st:preferedLink
                target="${primaryInstance ?: name}">${raw(name.fullNameHtml)}</st:preferedLink></accepted-name>
          </g:if>
          <g:else>
            <excluded-name title='excluded name'><st:preferedLink
                target="${primaryInstance ?: name}">${raw(name.fullNameHtml)}</st:preferedLink>
              <apc title="excluded from APC"><i class="fa fa-ban"></i>APC</apc></excluded-name>
          </g:else>
          <name-status class="${name.nameStatus.name}">${name.nameStatus.name}</name-status>
          <af:branch name="${name}" tree="APC"><i class="fa fa-code-fork"></i></af:branch>
          | sensu
          <st:preferedLink
              target="${apcInstance?.reference}">${raw(apcInstance?.reference?.citationHtml)}</st:preferedLink>
          <a class="vertbar"
             href="${af.refAPCSearchLink(citation: apcInstance?.reference?.citation, product: params.product)}">
            <i class="fa fa-search"></i>
          </a>

          <af:apniLink name="${name}"/>

          <span class="vertbar">
            <st:preferedLink target="${name}"><i title="citable link to name" class="fa fa-link"></i></st:preferedLink>
          </span>

          <span class="toggleNext vertbar">
            <i class="fa fa-caret-up"></i><i class="fa fa-caret-down" style="display: none"></i>
          </span>

          <div class="well instances">
            <g:if test="${instances}">
              <g:render template="hasSynonym" model="[instances: instances]"/>
            </g:if>
            <ul class="instance-notes list-unstyled">
              <af:getAPCNotes instance="${apcInstance}" var="instanceNote">
                <li>
                  <instance-note>${raw(instanceNote.value)}</instance-note>
                </li>
              </af:getAPCNotes>
            </ul>
          </div>
        </div>

        <g:if test="${misapplied}">
          <af:sortedReferences instances="${misapplied}" var="synonym" sortOn="cites">
            <div data-nameId="${name.id}">
              <g:if test="${synonym.instanceType.misapplied}">
                <st:preferedLink target="${primaryInstance ?: name}">${raw(name.simpleNameHtml)}</st:preferedLink>
                auct. non <af:author name="${synonym.name}"/>: <af:harvard reference="${synonym.cites.reference}"/>
                [fide <af:harvard reference="${synonym.citedBy.reference}"/>]
              </g:if>
              <g:else>
                <st:preferedLink target="${primaryInstance ?: name}">${raw(name.simpleNameHtml)}</st:preferedLink>
                <name-status class="${name.nameStatus.name}">${name.nameStatus.name}</name-status>
              </g:else>
              <g:if test="${synonym.instanceType.proParte}">, p.p.</g:if>
              =
              <accepted-name title='Accepted name'>
                <st:preferedLink target="${synonym.citedBy}">${raw(synonym.citedBy.name.fullNameHtml)}</st:preferedLink>
              </accepted-name>
              <af:branch name="${synonym.citedBy.name}" tree="APC"><i class="fa fa-code-fork"></i></af:branch>

              <af:apniLink name="${synonym.citedBy.name}"/>

              <span class="vertbar">
                <st:preferedLink target="${name}"><i title="citable link to name"
                                                     class="fa fa-link"></i></st:preferedLink>
              </span>

            </div>
          </af:sortedReferences>
        </g:if>

      </g:if>
      <g:elseif test="${synonymOf}">
        <af:sortedReferences instances="${synonymOf}" var="synonym" sortOn="cites">
          <div data-nameId="${name.id}">
            <g:if test="${synonym.instanceType.misapplied}">
              <st:preferedLink target="${primaryInstance ?: name}">${raw(name.fullNameHtml)}</st:preferedLink>
              sensu ${raw(synonym.cites.reference.citationHtml)}
            </g:if>
            <g:else>
              <st:preferedLink target="${primaryInstance ?: name}">${raw(name.fullNameHtml)}</st:preferedLink>
              <name-status class="${name.nameStatus.name}">${name.nameStatus.name}</name-status>
            </g:else>
            <g:if test="${synonym.instanceType.proParte}">, p.p.</g:if>
            =
            <accepted-name title='Accepted name'>
              <st:preferedLink target="${synonym.citedBy}">${raw(synonym.citedBy.name.fullNameHtml)}</st:preferedLink>
            </accepted-name>
            <af:branch name="${synonym.citedBy.name}" tree="APC"><i class="fa fa-code-fork"></i></af:branch>

            <af:apniLink name="${synonym.citedBy.name}"/>

            <span class="vertbar">
              <st:preferedLink target="${name}"><i title="citable link to name"
                                                   class="fa fa-link"></i></st:preferedLink>
            </span>

          </div>
        </af:sortedReferences>
      </g:elseif>
      <g:else>
        <span class="text-muted">${raw(name.fullNameHtml)} not in APC. (Perhaps restrict your search to APC?)</span>
      </g:else>
    </st:primaryInstance>
  </div>
