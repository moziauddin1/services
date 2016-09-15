<%@ page import="au.org.biodiversity.nsl.ConfigService" %>
<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main"/>
  <title>NSL Services</title>

</head>

<body>

<div id="page-body" role="main" class="container">
  <div class="jumbotron">

    <h1>${grailsApplication.config.shard.product} NSL Services</h1>

    <p>In the menu above you can click on <a
        href="${ConfigService.nameTreeName}">Names (${ConfigService.nameTreeName})</a>
      to search bibliographic information on names or <a href="${ConfigService.classificationTreeName}">
      Taxonomy (${ConfigService.classificationTreeName})</a> to search the ${ConfigService.classificationTreeName} taxonomic data.
    </p>
  </div>

  <div>
    <h1>The National Species List</h1>
    <div class="col-sm-12 col-md-7">
      <h2 class="header">Vascular Plants - APNI/APC</h2>
      <div class="card horizontal">
        <div class="card-image">
          <asset:image src="apni-vert-200.png" />
        </div>
        <div class="card-stacked">
          <div class="card-content">
            <p>
              The vascular plants data includes the Australian Plant Name Index (APNI) as the bibliographic index of plant names,
              and the Australian Plant Census (APC) as the Nationally Accepted Taxonomy of Australian plants.
            </p>
          </div>
          <div class="card-action">
            <p><a href="https://biodiversity.org.au/nsl/services">Names - APNI</a></p>
            <p><a href="https://biodiversity.org.au/nsl/services">Nationally Accepted Taxonomy - APC</a></p>
          </div>
        </div>
      </div>
    </div>

    <div class="col-sm-12 col-md-7">
      <h2 class="header">Moss - AusMoss</h2>
      <div class="card horizontal">
        <div class="card-image">
          <asset:image src="moss-vert-200.png" />
        </div>
        <div class="card-stacked">
          <div class="card-content">
            <p>
              The moss data includes the AusMoss bibliographic index of moss names, and the AusMoss Taxanomic Checklist
            of Australian Mosses.
            </p>
          </div>
          <div class="card-action">
            <p><a href="https://moss.biodiversity.org.au/nsl/services">Names - AusMoss</a></p>
            <p><a href="https://moss.biodiversity.org.au/nsl/services">Nationally Accepted Taxonomy - AusMoss</a></p>
          </div>
        </div>
      </div>
    </div>

    <div class="col-sm-12 col-md-7">
      <h2 class="header">Lichen - ABRS Lichen</h2>
      <div class="card horizontal">
        <div class="card-image">
          <asset:image src="lichen-vert-200.png" />
        </div>
        <div class="card-stacked">
          <div class="card-content">
            <p>
              The lichen data includes the ABRS Lichen Name Index (LNI) of lichen names, and the ABRS Lichen Checklist (ABRSL).
            </p>
          </div>
          <div class="card-action">
            %{--<ul>--}%
            <p><a href="https://lichen.biodiversity.org.au/nsl/services">Names - LNI</a></p>
            <p><a href="https://lichen.biodiversity.org.au/nsl/services">Taxonomy - ABRSL</a></p>
            %{--</ul>--}%
          </div>
        </div>
      </div>
    </div>


  </div>


</div>

</body>
</html>
