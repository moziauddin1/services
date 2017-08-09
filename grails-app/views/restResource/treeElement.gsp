<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>Tree Element ${treeElement.elementLink}</title>
  <asset:stylesheet src="tree.css"/>

</head>

<body>

<g:set var="currentTreeVersion" value="${treeElement.treeVersion.tree.currentTreeVersion}"/>

<div>
  <h1>${treeElement.treeVersion.tree.name}<span
      class="small text-info">(${treeElement.treeVersion.id})</span> element

    <g:if test="${currentTreeVersion != treeElement.treeVersion}"><span class="small">
      <i class="fa fa-long-arrow-right"></i>
      <a href='${createLink(uri: "/tree/$currentTreeVersion.id/$treeElement.treeElementId")}'>
        current version ${currentTreeVersion.id}.
      </a>
    </span>
    </g:if>

    <help>
      <i class="fa fa-info-circle"></i>

      <div>
        A tree element is an element of a classification tree structure (also known as a Node). The element holds the position
        of a Taxon Concept in an arrangement of taxon that we refer to generically as a tree.
        <ul>
          <li>At the bottom of this page are the citable links to this object or just use the <i
              class="fa fa-link"></i> icon.
          You can "right click" in most browsers to copy it or open it in a new browser tab.</li>
        </ul>
      </div>
    </help>
  </h1>

  <a href="." title="Go to tree version ${treeElement.treeVersion.id}">
    <i class="fa fa-link"></i> ${treeElement.treeVersion.tree.name} (version ${treeElement.treeVersion.id})
  published ${treeElement.treeVersion.publishedAt.dateString} by ${treeElement.treeVersion.publishedBy}
  </a>
  <hr>
</div>


<div class="rest-resource-content">

  <div>
    <tree:elementPath element="${treeElement}" var="pathElement" separator="/">
      <a href="${pathElement.elementLink}">${pathElement.simpleName}</a>
    </tree:elementPath>
    <a href="${treeElement.elementLink}">${raw(treeElement.displayString)}</a>
    <span class="text-info">${children.size() - 1} sub taxa</span>
    <br>
    <tree:profile profile="${treeElement.profile}"/>
  </div>

  <div class="${tree.collapsedIndent(element: treeElement)}">
    <g:each in="${children}" var="childElement">
      <div class="wrap">
        <a href="${childElement[1]}">${raw(childElement[0])}</a>
        <a class="vertbar" href="${childElement[2]}/api/apni-format" title="View name in APNI format.">
          <i class="fa fa-list-alt see-through"></i>
        </a>
      </div>
    </g:each>
  </div>
</div>

<h4>link to here <help><i class="fa fa-info-circle"></i>

  <div>
    <ul>
      <li>To cite this object in a database or publication please use the following preferred link.</li>
      <li>The preferred link is the most specific of the permalinks to here and makes later comparisons of linked
      resources easier.</li>
      <li>Note you can access JSON and XML versions of this object by setting the
      correct mime type in the ACCEPTS header of your HTTP request or by appending &quot;.json&quot; or &quot;.xml&quot;
      to the end of the URL.</li>
    </ul>
  </div>
</help>
</h4>
Please cite using: <a href="${treeElement.elementLink}">
  ${treeElement.elementLink}
</a> <i class="fa fa-star green"></i>

</div>
</body>
</html>

