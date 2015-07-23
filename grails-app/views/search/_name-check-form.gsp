<%@ page import="au.org.biodiversity.nsl.Arrangement" %>
<g:form name="search" role="form" controller="search" action="search" method="POST" class="closable">
  <div class="form-group">
    <g:render template="/search/using-tree"/>

  </div>

  <div class="checkbox" title="Search only for names on the selected tree. i.e. Accepted names on APC.">
    <label>
      <g:checkBox name="exclSynonym" value="${query.exclSynonym}"/> Not synonyms
    </label>
  </div>

  <div class="form-group">
    <label>Names <span class="text-muted small">Enter each name to check on a new line.</span>
      <help><i class="fa fa-info-circle"></i>

        <div>
          <ul>
            <li>This search uses the simple name without author.</li>
            <li>If you have more than 100 names, they must be an exact match, including an 'x' in hybrid names, and can not use wild cards.</li>
            <li>Lists with less than 100 names are treated like the normal name search.</li>
          </ul>
        </div>
      </help>

      <textarea name="name" placeholder="Enter each name to check on a new line."
                class="form-control" rows="20">${query.name}</textarea>
    </label>
  </div>

  <g:set var="formName" value="nameCheck"/>
  <g:render template="/search/submit"/>

</g:form>