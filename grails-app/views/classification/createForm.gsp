<%--
  Created by IntelliJ IDEA.
  User: ibis
  Date: 9/06/15
  Time: 12:25 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main">
    <title>Service administration</title>
</head>

<body>
<div class="">

    <g:if test="${flash.message}">
        <div class="row alert alert-info" role="status">${flash.message}</div>
    </g:if>
    <g:if test="${flash.validation}">
        <div class="row alert alert-warning" role="status">${flash.validation}</div>
    </g:if>
    <g:if test="${flash.error}">
        <div class="row alert alert-danger" role="status">${flash.error}</div>
    </g:if>

    <h1>Create new classification</h1>

    <div>
        <g:link action="index">Back</g:link>
    </div>

    <g:form action="doCreate" role="form">
        <g:render template="formFields"/>

        <div class="form-group">

            <label for="copyNameCheck" class="col-sm-2 control-label text-right">As a copy of?  <g:checkBox id="copyNameCheck" name="copyNameChk" checked="${copyNameChk == 'on'}"/></label>

            <div class="col-sm-10 form-group from-inline" id="copyNameSubform">
                <label for="inputCopyName" class="col-sm-1 control-label text-right">Name</label>
                <div class="col-sm-11">
                    <g:textField id="inputCopyName" name="inputCopyName" class="form-control" placeholder="Whole of classification" value="${inputCopyName}"/>
                </div>

                <label for="inputCopyNameIn" class="col-sm-1 control-label text-right">In</label>
                <div class="col-sm-11">
                    <g:select name='inputCopyNameIn'
                              id="inputCopyNameIn"
                              value="${inputCopyNameIn}"
                              from='${list}'
                              optionKey="label"
                              optionValue='label'
                              class="form-control">
                    </g:select>
                </div>
            </div>
        </div>

        <script type="text/javascript">
            function manageCopyNameSubform() {
                var checked = $("#copyNameCheck").is(":checked");
                $("#copyNameSubform input").prop('disabled', !checked);
                $("#copyNameSubform select").prop('disabled', !checked);
                if (checked) {
                    $("#copyNameSubform label").removeClass('text-muted');
                }
                else {
                    $("#copyNameSubform label").addClass('text-muted');
                }
            }

            $(function () {
                $("#copyNameCheck").change(manageCopyNameSubform);
                manageCopyNameSubform();
            });
        </script>

        <div class="form-group">
            <span class="col-sm-2">&nbsp;</span>
            <span class="col-sm-10">
                <g:submitButton name="Create" class="btn btn-primary"/>
            </span>
        </div>
    </g:form>

    <div id='logs' class="row">

    </div>
</div>

</body>
</html>