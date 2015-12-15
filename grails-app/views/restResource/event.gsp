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

    %{--<tree:getEventInfo event="${event}">--}%
        %{--<g:if test="${eventInfo.pairs}">--}%
            %{--<table>--}%
                %{--<tr><td><h3>Old node</h3></td><td><h3>New node</h3></td></tr>--}%
                %{--<g:each in="${eventInfo.pairs}">--}%
                    %{--<tr><td><g:if test="${it.prev}"><g:render template="node" model="${[node: it.prev]}"/></g:if></td><td><g:if test="${it.next}"><g:render template="node" model="${[node: it.next]}"/></g:if></td></tr>--}%
                %{--</g:each>--}%
            %{--</table>--}%
        %{--</g:if>--}%
    %{--</tree:getEventInfo>--}%

    <g:render template="links"/>

</div>
</body>
</html>
