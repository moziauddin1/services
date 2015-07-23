<%@ page import="au.org.biodiversity.nsl.Arrangement" %>
<g:form name="search" role="form" controller="search" action="search" method="GET">
  <div class="form-group">
    <g:render template="/search/using-tree"/>
    <label>name
      <help><i class="fa fa-info-circle"></i>

        <div>
          <ul>
            <li>You will get suggestions as you type in your query, they tell you what your query will return, and you can select one for an exact match.</li>
            <li>The query is <b>not</b> case sensitive.</li>
            <li>This search uses an automatic wild card at the end of the query to match all endings.</li>
            <li>The query is an ordered set of search terms, so viola l. will match Viola suffruticosa L. and others.</li>
            <li>Putting double quotes around your entire query will cause it to be matched exactly (except case). e.g. "Viola L." will match just Viola L.</li>
            <li>You can use a % as a wild card inside the search query e.g. hakea elon% benth  to find "Hakea ceratophylla var. elongata Benth."</li>
            <li>You can use a + in place of a space to make the space match exactly. e.g. viola+L. will match "Viola L." and "Viola L. sect. Viola"</li>
          </ul>
        </div>
      </help><span class="text-muted small"> click the <i class="fa fa-info-circle"></i> for help.</span>


    <input type="text" name="name" placeholder="Enter a name" value="${query.name}"
             class="suggest form-control "
             data-subject="apni-search" data-quoted="yes" size="30"/>
    </label>
    <g:set var="formName" value="search"/>
    <g:render template="/search/submit"/>
  </div>
</g:form>