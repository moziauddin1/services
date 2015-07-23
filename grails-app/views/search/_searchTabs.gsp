<div role="tabpanel">
  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation"
        class="${!(query.advanced || query.experimental || query.nameCheck || query.sparql) ? 'active' : ''}">
      <a href="#name" aria-controls="name" role="tab" data-toggle="tab">
        Name search
      </a>
    </li>
    <li role="presentation" class="${query.experimental ? 'active' : ''}">
      <a href="#experimental" aria-controls="experiemental" role="tab" data-toggle="tab">
        Advanced search
      </a>
    </li>
    <li role="presentation" class="${query.nameCheck ? 'active' : ''}">
      <a href="#nameCheck" aria-controls="nameCheck" role="tab" data-toggle="tab">
        Name check
      </a>
    </li>
    <g:if test="${!params.product}">
      <li role="presentation" class="${query.sparql ? 'active' : ''}">
        <a href="#sparql" role="tab" data-toggle="tab">
          Sparql
        </a>
      </li>
    </g:if>
  </ul>

  <div class="tab-content">

    <div role="tabpanel"
         class="tab-pane ${!(query.advanced || query.experimental || query.nameCheck || query.sparql) ? 'active' : ''}"
         id="name">
      <div class="panel ${(params.product == 'apc' ? 'panel-success' : 'panel-info')} ">

        <div class="panel-heading">
          <g:render template="/search/common-search-heading"/>
        </div>

        <div class="panel-body">
          <g:render template="/search/simple-search-form"/>
        </div>
      </div>

    </div>

    <div role="tabpanel" class="tab-pane ${query.experimental ? 'active' : ''}" id="experimental">

      <div class="panel  ${(params.product == 'apc' ? 'panel-success' : 'panel-info')} ">
        <div class="panel-heading">
          <g:render template="/search/common-search-heading"/>
          <g:render template="/search/hide-show"/>
        </div>

        <div class="panel-body">
          <g:render template="/search/exp-search-form"/>
        </div>
      </div>
    </div>

    <div role="tabpanel" class="tab-pane ${query.nameCheck ? 'active' : ''}" id="nameCheck">

      <div class="panel  ${(params.product == 'apc' ? 'panel-success' : 'panel-info')} ">
        <div class="panel-heading">
          <g:render template="/search/common-search-heading"/>
          <g:render template="/search/hide-show"/>
        </div>

        <div class="panel-body">
          <g:render template="/search/name-check-form"/>
        </div>
      </div>
    </div>

    <g:if test="${!params.product}">
      <div role="tabpanel" class="tab-pane ${query.sparql ? 'active' : ''}" id="sparql">
        <g:render template="/search/sparql-panel"/>
      </div>
    </g:if>
  </div>
</div>