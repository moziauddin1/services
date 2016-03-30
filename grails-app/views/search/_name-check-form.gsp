<%@ page import="au.org.biodiversity.nsl.Arrangement" %>
<g:form name="search" role="form" controller="search" action="nameCheck" method="POST" class="closable">

  <div class="form-group">
    <div class="col-md-6">
      <label>Names <span class="text-muted small">Enter each name to check on a new line.</span>
        <help><i class="fa fa-info-circle"></i>

          <div>
            <ul>
              <li>This search uses the simple name without author.</li>
              <li>The names must be an exact match, including an 'x' in hybrid names</li>
              <li>You can use a wild card but only the first match will be returned.</li>
            </ul>
          </div>
        </help>

        <textarea name="name" placeholder="Enter each name to check on a new line."
                  class="form-control" rows="20">${query.name}</textarea>
      </label>
    </div>
  <div class="col-md-6">
    <div>
      <h2>Hints</h2>
      <ul>
        <li>This search uses the simple name without author.</li>
        <li>The names must be an exact match, including an 'x' in hybrid names.</li>
        <li>You can use a wild card but only the first match will be returned. Wildcards might help for abreviations like "subsp." e.g. s% </li>
        <li>Duplicated names will be removed.</li>
        <li>You can copy and paste from word and excel, including across excel columns.</li>
      </ul>
      <h3>Example search</h3>
      <pre>
        anksia spinulosa ‘Birthday Candles’
        Banksia spinulosa ‘Coastal Cushion’
        Banksia spinulosa var. collina
        Callistemon pachyphyllus ‘Smoked Salmon’
        Callistemon subulatus
        Correa ‘Just a Touch’
        Correa alba
        Doodia aspera
        Epacris impressa
        Abelia × grandiflora
        Acacia adoxa var. adoxa x Acacia spondylophylla
      </pre>
    </div>
  </div>
  </div>

  <g:set var="formName" value="nameCheck"/>
  <g:render template="/search/submit"/>

</g:form>