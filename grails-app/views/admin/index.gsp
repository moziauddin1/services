<%--
  User: pmcneil
  Date: 27/03/15
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>Service administration</title>
</head>

<body>

<div class="">

  <g:if test="${flash.message}">
    <div class="row alert alert-info" role="status">${flash.message}</div>
  </g:if>

  <h1>Admin dashboard</h1>

  <g:if test="${servicing}">
    <h2>Servicing Mode On.</h2>
  </g:if>

  <ul>
    <li>
      <a class=""
         href="${g.createLink(controller: "admin", action: "setAdminModeOn")}">
        Turn on servicing
      </a>
    </li>
    <li>
      <a class=""
         href="${g.createLink(controller: "admin", action: "setAdminModeOff")}">
        Turn off servicing
      </a>
    </li>
  </ul>


  <div role="tabpanel">
    <ul class="nav nav-tabs" role="tablist">
      <li role="presentation" class="active">
        <a href="#names" aria-controls="names" role="tab" data-toggle="tab">
          Names
        </a>
      </li>
      <li role="presentation" class="">
        <a href="#references" aria-controls="references" role="tab" data-toggle="tab">
          References
        </a>
      </li>
      <li role="presentation" class="">
        <a href="#trees" aria-controls="trees" role="tab" data-toggle="tab">
          Trees
        </a>
      </li>
      <li role="presentation" class="">
        <a href="#views" aria-controls="views" role="tab" data-toggle="tab">
          Views
        </a>
      </li>
    </ul>

    <div class="tab-content">

      <div role="tabpanel"
           class="tab-pane active" id="names">
        <div class="panel panel-info">

          <div class="panel-heading">
            Admin tasks for names
          </div>

          <div class="panel-body">
            <div>
              <label>Name updater ${pollingNames}
                <div class="btn-toolbar">
                  <div class="btn-group">
                    <a class="btn btn-success" href="${g.createLink(controller: 'admin', action: 'resumeUpdates')}">
                      <i class="fa fa-play"></i>
                    </a>
                    <a class="btn btn-warning" href="${g.createLink(controller: 'admin', action: 'pauseUpdates')}">
                      <i class="fa fa-pause"></i>
                    </a>
                  </div>
                </div>
              </label>
            </div>

            <ul>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "checkNames")}">
                  Check name strings
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "reconstructNames")}">
                  Reconstruct name strings
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "reconstructSortNames")}">
                  Reconstruct sort name strings
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "constructMissingNames")}">
                  Construct missing name strings (${stats.namesNeedingConstruction})
                </a>
              </li>
              <li>
                Deleted names:
                <ul>
                  <g:each in="${stats.deletedNames}" var="name">
                    <li>${name}</li>
                  </g:each>
                </ul>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div role="tabpanel"
           class="tab-pane" id="references">
        <div class="panel panel-info">

          <div class="panel-heading">
            Admin tasks for References
          </div>

          <div class="panel-body">

            <ul>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "reconstructCitations")}">
                  Reconstruct reference citations
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "autoDedupeAuthors")}">
                  Deduplicate Authors
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "deduplicateMarkedReferences")}">
                  Deduplicate marked references
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div role="tabpanel"
           class="tab-pane" id="trees">
        <div class="panel panel-info">

          <div class="panel-heading">
            Operations on Trees and Tree support.
          </div>

          <div class="panel-body">
            <ul class="">
              <li>
                <div>
                  Names not in APNI NTP: ${stats.namesNotInApniTreePath}<br>
                  Names not in APC NTP: ${stats.namesNotInApcTreePath}
                </div>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "makeTreePaths")}">
                  Remake all tree paths
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "notifyMissingApniNames")}">
                  Add names not in APNI via notifications (${stats.namesNotInApni})
                </a>
              </li>
              <li>
                Apply APC comments and distribution text on instances to the APC tree.
                <div class="alert-danger" style="margin-left:2em; padding: 1em; border: thick solid red;">
                    <strong>WARNING:</strong> clicking this link will irrevocably erase all distribution and comment information currently on the tree without further warning.
                We intend to do this import once and once only, after which we will remove this section of this page.
                    <a style="display: block; margin-left:4em; margin-right: 4em;"
                       href="${g.createLink(controller: "admin", action: "transferApcProfileData")}">
                        <strong>YES!</strong> I want to permanently and irrevocably erase all distribution and comment data and replace them with
                    whatever is in the instance notes, without further warning!
                    </a>
                </div>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "classification", action: "index")}">
                  Manage classifications
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "classification", action: "validateClassifications")}">
                  Validate classifications
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "classification", action: "rebuildNametreeForm")}">
                  Rebuild name tree
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div role="tabpanel"
           class="tab-pane" id="views">
        <div class="panel panel-info">

          <div class="panel-heading">
            Admin tasks for Views
          </div>

          <div class="panel-body">

            <ul>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "refreshViews")}">
                  Refresh all views
                </a>
              </li>
              <li>
                <a class=""
                   href="${g.createLink(controller: "admin", action: "recreateViews")}">
                  Recreate all views
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <div id='logs' class="row">

    </div>

  </div>

</body>
</html>