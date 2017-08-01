<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>Tree ${treeVersion.tree.name}</title>
  <asset:stylesheet src="tree.css"/>

</head>

<body>
<div class="rest-resource-content tree-gsp">
  <h1>Tree <help>
    <i class="fa fa-info-circle"></i>

    <div>
      <p>A tree is a classification tree structure. The tree has tree_elements that hold the position of a Taxon Concept
      in an arrangement of taxon that we refer to generically as a tree.</p>

      <p>A tree structure can change, so a tree contains many versions. Each version of a tree is immutable, i.e it
      doesn't change. You can cite a tree element with confidence that the thing you cite will not change over time,
      while being able to trace the history all the way to the current placement.</p>
      <ul>
        <li>At the bottom of this page are the citable links to this Instance object or just use the <i
            class="fa fa-link"></i> icon.
        You can "right click" in most browsers to copy it or open it in a new browser tab.</li>
      </ul>
    </div>
  </help>
  </h1>

  <div class="rest-resource-content col-xs-6 col-sm-6  col-md-5 col-lg-4">
    <div>
      <h3>${treeVersion.tree.name} version ${treeVersion.id} (<tree:versionStatus version="${treeVersion}"/>)</h3>
      <tree:versionStats version="${treeVersion}">
        ${elements} elements
      </tree:versionStats>
      <a href='${createLink(uri: "/tree/$treeVersion.id")}'>
        published ${treeVersion.publishedAt.dateString} by ${treeVersion.publishedBy}
      </a>

      <h4>Notes</h4>

      <p>${treeVersion.logEntry}</p>
    </div>

    <g:if test="${treeVersion != treeVersion.tree.currentTreeVersion}">
      <g:set var="currentTreeVersion" value="${treeVersion.tree.currentTreeVersion}"/>
      <div>
        <h3>Current ${treeVersion.tree.name} version ${currentTreeVersion.id}</h3>

        <tree:versionStats version="${currentTreeVersion}">
          ${elements} elements
        </tree:versionStats>

        <a href='${createLink(uri: "/tree/$currentTreeVersion.id")}'>
          published ${currentTreeVersion.publishedAt.dateString} by ${currentTreeVersion.publishedBy}
        </a>

        <h4>Notes</h4>

        <p>${currentTreeVersion.logEntry}</p>
      </div>
    </g:if>

    <div>
      <h3>Other versions</h3>
      <table class="table">
        <tr><th>Version</th><th>published</th><th>Notes</th></tr>
        <g:each in="${versions}" var="version">
          <tr>
            <td><a href='${createLink(uri: "/tree/$version.id")}'>${version.id}</a></td>
            <td>${version.publishedAt.dateString}</td>
            <td>${version.logEntry}</td>
          </tr>
        </g:each>
      </table>
    </div>

  </div>

  <div class="col-xs-6 col-sm-6 col-md-7 col-lg-8 indent">
    <g:each in="${children}" var="childElement">
      <a href="${childElement[1]}">${raw(childElement[0])}</a>
    </g:each>
  </div>
</div>
</body>
</html>

