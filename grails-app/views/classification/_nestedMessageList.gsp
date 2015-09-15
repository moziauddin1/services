<g:if test="${msgList}">
    <ul>
        <g:each in="${msgList}" var="msg">
            <li><span class="text-${msg.level}">${msg.msg}</span>
                <g:if test="${msg.link}"> 
                    <g:link controller="${msg.link.controller ?: 'TreeFixup'}" action="${msg.link.action ?: 'index'}" params="${msg.link.params}">&#8594; repair</g:link>
                </g:if> 
                <g:if test="${msg.nested}">
                    <g:render template="nestedMessageList" model="${[msgList: msg.nested]}"/>
                </g:if>
            </li>
        </g:each>
    </ul>
</g:if>