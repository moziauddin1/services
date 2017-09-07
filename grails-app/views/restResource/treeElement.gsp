<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>Tree Element ${treeVersionElement.elementLink}</title>
  <asset:stylesheet src="tree.css"/>

</head>

<body>

<g:set var="currentTreeVersion" value="${treeVersionElement.treeVersion.tree.currentTreeVersion}"/>

<div>
  <h1>
    <g:if test="${treeVersionElement.treeElement.excluded}">
      <apc style="font-size: 1em" title="excluded from ${treeVersionElement.treeVersion.tree.name}"><i
          class="fa fa-ban"></i> ${treeVersionElement.treeVersion.tree.name}</apc>
    </g:if>
    <g:else>
      ${treeVersionElement.treeVersion.tree.name}
    </g:else>
    <span
        class="small text-info">(${treeVersionElement.treeVersion.id})</span> element

    <g:if test="${currentTreeVersion != treeVersionElement.treeVersion}"><span class="small">
      <i class="fa fa-long-arrow-right"></i>
      <a href='${createLink(uri: "/tree/$currentTreeVersion.id/$treeVersionElement.treeElement.treeElementId")}'>
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

  <a href="." title="Go to tree version ${treeVersionElement.treeVersion.id}">
    <i class="fa fa-link"></i> ${treeVersionElement.treeVersion.tree.name} (version ${treeVersionElement.treeVersion.id})
  published ${treeVersionElement.treeVersion.publishedAt.dateString} by ${treeVersionElement.treeVersion.publishedBy}
  </a>
  <hr>
</div>


<div class="rest-resource-content">

  <div>
    <tree:elementPath element="${treeVersionElement}" var="pathElement" separator="/">
      <a href="${pathElement.elementLink}">${pathElement.treeElement.simpleName}</a>
    </tree:elementPath>
    <div class="tr ${treeVersionElement.treeElement.excluded ? 'excluded' : ''}">
      <a href="${treeVersionElement.elementLink}">${raw(treeVersionElement.treeElement.displayHtml)}</a>
    </div>
    <span class="text-info">${children.size() - 1} sub taxa</span>
    <br>
    <tree:profile profile="${treeVersionElement.treeElement.profile}"/>
  </div>

  <div class="indented indent${treeVersionElement.treeElement.depth}">
    <g:each in="${children}" var="childElement">
      <div class="tr ${childElement.excluded ? 'excluded' : ''} level${childElement.depth}">
        <div class="wrap">
          <a href="${childElement.elementLink}">${raw(childElement.displayHtml)}</a>
          <a href="${childElement.nameLink}/api/apni-format" title="View name in APNI format.">
            <i class="fa fa-list-alt see-through"></i>
          </a>
          ${raw(childElement.synonymsHtml)}
        </div>
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
Please cite using: <a href="${treeVersionElement.elementLink}">
  ${treeVersionElement.elementLink}
</a> <i class="fa fa-star green"></i>

</div>
</body>
</html>

