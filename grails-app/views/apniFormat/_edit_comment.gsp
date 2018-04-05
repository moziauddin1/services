<shiro:hasRole name="treebuilder">
  <span class="toggleNext"><i class="fa fa-edit"></i><i class="fa fa-edit" style="display: none"></i></span>

  <div style="display: none">
    <g:form action="editComment" controller="treeElement">
      <g:hiddenField name="taxonUri" value="${tve.elementLink}"/>
      <label>Comment
      <g:textField name="comment" value="${val}" class="form-control"/>
      </label>
      <label>Reason
      <g:textField name="reason" value="Comment edit" class="form-control"/>
      </label>
      <g:submitButton name="Save" class="btn btn-success"/>
    </g:form>
  </div>
</shiro:hasRole>
