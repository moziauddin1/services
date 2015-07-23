<!DOCTYPE html>
<head>
  <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title><g:layoutTitle default="Grails"/></title>
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="shortcut icon" href="${assetPath(src: 'gears.png')}?v=2.1">
  <script type="application/javascript">
    baseContextPath = "${request.getContextPath()}";
  </script>

  <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
  <asset:stylesheet src="application.css"/>
  <asset:stylesheet src="application.css" media="print"/>

  <asset:javascript src="application.js"/>
  <!--[if lt IE 9 ]>
  <style>
  @media(min-width:992px) {
    div.logo img {
        width: 50px;
    }
  }
  </style>
  <script type="application/javascript">
     internetExplorer = "<9";
  </script>
  <![endif]-->
  <!--[if IE 9 ]>
  <script type="application/javascript">
     internetExplorer = "9";
  </script>
  <![endif]-->
  <g:layoutHead/>
</head>

<body>
<g:render template="/common/service-navigation" model="[links: [
    [class: 'dashboard', url: createLink(controller: 'dashboard', action: 'index'), label: 'Dash', icon: 'fa-bar-chart-o'],
    [class: 'search', url: createLink(controller: 'search', action: 'search'), label: 'Search', icon: 'fa-search', loggedIn: true],
    [class: 'search', url: createLink(controller: 'search', action: 'search', params: [product: 'apni']), label: 'APNI', icon: 'fa-search'],
    [class: 'search', url: createLink(controller: 'search', action: 'search', params: [product: 'apc']), label: 'APC', icon: 'fa-search'],
]]"/>
<st:systemNotification/>
<div class="container-fluid">
  <div class="row">
    <div class="col-md-2 col-lg-2 logo">

      <a href="http://www.anbg.gov.au/chah/">
        <asset:image src="CHAH-logo.png"/>
      </a>

      <div class="nsl">
        National Species List
      </div>

      <div class="sublogo"><st:productName/></div>

      <hr>
      <twitter>
        <a class="twitter-timeline"
           data-chrome="nofooter noborders transparent noscrollbar"
           href="https://twitter.com/AuBiodiversity"
           data-widget-id="573301073391169536">Tweets by @AuBiodiversity</a>
        <script>!function (d, s, id) {
          var js, fjs = d.getElementsByTagName(s)[0], p = /^http:/.test(d.location) ? 'http' : 'https';
          if (!d.getElementById(id)) {
            js = d.createElement(s);
            js.id = id;
            js.src = p + "://platform.twitter.com/widgets.js";
            fjs.parentNode.insertBefore(js, fjs);
          }
        }(document, "script", "twitter-wjs");</script>
      </twitter>
    </div>

    <div class="col-sm-12 col-md-10 col-lg-10">
      <g:layoutBody/>
    </div>
  </div>
</div>

<div class="footer text-muted container-fluid" role="contentinfo">
  <div class="row">
    Version: <g:meta name="app.version"/>
  </div>
</div>

<div id="spinner" class="spinner" style="display:none;"><g:message code="spinner.alt" default="Loading&hellip;"/></div>
</body>
</html>
