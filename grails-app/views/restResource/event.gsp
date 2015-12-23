<%@ page import="au.org.biodiversity.nsl.*; au.org.biodiversity.nsl.tree.*" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Event ${event?.id}</title>
</head>

<body>

<div class="rest-resource-content event-gsp">
    <h3>Tree event ${event.id}</h3>

    <dl class="dl-horizontal">
        <dt>Id</dt><dd>${event.id}</dd>
        <dt>Timestamp</dt><dd>${event.timeStamp}</dd>
        <g:if test="${event.note}"><dt>Note</dt><dd>${event.note}</dd></g:if>
    </dl>

    <g:render template="links"/>

</div>
</body>
</html>
