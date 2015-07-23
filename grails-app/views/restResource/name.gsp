<%@ page import="au.org.biodiversity.nsl.Name" %>
<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>${name.simpleName}</title>
</head>

<body>
<h2>Name
  <help>
    <i class="fa fa-info-circle"></i>

    <div>
      The unique identifying name (text) referred to in instances.
      <ul>
        <li>Below is the APNI format representation of the Name with its' related Instances of usage.</li>
        <li>Instances of usage include relationship information like Taxonomic Synonym, Misapplication etc.</li>
      <li>At the bottom of this page are the citable links to this Name or just use the <i class="fa fa-link"></i> icon.
      You can "right click" in most browsers to copy it or open it in a new browser tab.</li>
      </ul>
    </div>
  </help>
</h2>

<div class="rest-resource-content">

  <g:render template="/apniFormat/name" model="[name: name, apc: apc]"/>

  <g:render template="links"/>

</div>

</body>
</html>
