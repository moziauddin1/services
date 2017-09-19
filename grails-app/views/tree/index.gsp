<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>Tree Services</title>
  <asset:stylesheet src="tree.css"/>

</head>

<body>
<div class="rest-resource-content tree-gsp">
  <h1>Tree Services</h1>

  <div>
    <p>A tree is a classification tree structure. The tree has tree_elements that hold the position of a Taxon Concept
    in an arrangement of taxon that we refer to generically as a tree.</p>

    <p>A tree structure can change, so a tree contains many versions. Each version of a tree is immutable, i.e it
    doesn't change. You can cite a tree element with confidence that the thing you cite will not change over time,
    while being able to trace the history all the way to the current placement.</p>
  </div>

  <h2>Available Trees</h2>
  <g:if test="${trees.empty}">
    <p>No trees currently listed in this service.</p>
  </g:if>
  <g:else>
    <dl class="dl-horizontal">
      <g:each in="${trees}" var="tree">
        <dt>${tree.name}</dt>
        <dd>
          <st:preferredLink target="${tree}">
            ${tree.currentTreeVersion.draftName} published ${tree.currentTreeVersion.publishedAt.dateString} by ${tree.currentTreeVersion.publishedBy}.
            (${tree.currentTreeVersion.logEntry})
          </st:preferredLink>
        </dd>
      </g:each>
    </dl>
  </g:else>

  <h2>API end points</h2>
  <dl class="dl-horizontal">
    <dt>createTree</dt> <dd>['PUT']</dd>
    <dt>editTree</dt> <dd>['POST']</dd>
    <dt>copyTree</dt> <dd>['PUT']</dd>
    <dt>deleteTree</dt> <dd>['DELETE']</dd>
    <dt>createTreeVersion</dt> <dd>['PUT']</dd>
    <dt>setDefaultDraftTreeVersion</dt> <dd>['PUT']</dd>
    <dt>editTreeVersion</dt> <dd>['POST']</dd>
    <dt>validateTreeVersion</dt> <dd>['GET']</dd>
    <dt>publishTreeVersion</dt> <dd>['PUT']</dd>
    <dt>placeTaxon</dt> <dd>['PUT']</dd>
    <dt>moveElement</dt> <dd>['POST']</dd>
    <dt>removeElement</dt> <dd>['DELETE']</dd>
    <dt>editElementProfile</dt> <dd>['POST']</dd>
    <dt>editElementStatus</dt> <dd>['POST']</dd>
  </dl>

</div>
</body>
</html>

