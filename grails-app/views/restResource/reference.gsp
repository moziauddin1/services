<%@ page import="au.org.biodiversity.nsl.Reference" %>
<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>${reference.citation}</title>
</head>

<body>
<h2>Reference <help>
  <i class="fa fa-info-circle"></i>

  <div>
    A reference work with citation and author referred to by instances.
    <ul>
      <li>At the bottom of this page are the citable links to this Instance object or just use the <i
          class="fa fa-link"></i> icon.
      You can "right click" in most browsers to copy it or open it in a new browser tab.</li>
    </ul>
  </div>
</help>
</h2>

<div class="rest-resource-content">

  <reference data-referenceId="${reference.id}">
    <ref-citation>
      %{--don't reformat the citationHtml line--}%
      <b>${raw(reference?.citationHtml)}</b> <st:preferedLink target="${reference}"><i
        class="fa fa-link"></i></st:preferedLink>
      <a href="${g.createLink(controller: 'search', params: [publication: reference?.citation, search: true, advanced: true, display: 'apni'])}"
         title="search for names in this reference">
        <i class="fa fa-search"></i></a>
    </ref-citation>
    (<reference-type>${reference.refType.name}</reference-type>)

    <reference-author data-authorId="${reference.author?.id}"><i
        class="fa fa-user"></i> <st:preferedLink
        target="${reference.author}">${reference.author?.name}</st:preferedLink> <reference-author-role
        class="${reference.refAuthorRole.name}">${reference.refAuthorRole.name}</reference-author-role>
    </reference-author>

    <div>
      Instances: ${reference.instances.size()}
    </div>
  </reference>


</ol>

  <g:render template="links"/>
</div>
</body>
</html>
