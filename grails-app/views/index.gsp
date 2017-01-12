<%@ page import="au.org.biodiversity.nsl.ConfigService" %>
<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main"/>
  <title>NSL Services</title>

</head>

<body>

<div id="page-body" role="main" class="container">
  <h1 class="header">${ConfigService.bannerText} NSL Services</h1>

  <div class="card horizontal">

    <div class="card-image">
      <asset:image src="${ConfigService.cardImage}" height="300px"/>
    </div>

    <div class="card-stacked">
      <div class="card-content">
        <st:encodeWithHTML text='${ConfigService.shardDescriptionHtml}'/>
      </div>

      <div class="card-action">
        <p>In the menu above you can:-</p>
        <ul>
          <li>
            click <a href="${ConfigService.nameTreeName}">Names (${ConfigService.nameTreeName})</a>
            to search bibliographic information on names, or
          </li>
          <li>
            click <a href="${ConfigService.classificationTreeName}">
            Taxonomy (${ConfigService.classificationTreeName})</a> to search the ${ConfigService.classificationTreeName} taxonomic data.
          </li>
        </ul>
      </div>
    </div>
  </div>

</div>

</body>
</html>
