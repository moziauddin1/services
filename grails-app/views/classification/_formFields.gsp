<div class="form-horizontal">
    <div class="form-group">

        <label for="inputLabel" class="col-sm-2 control-label">Label</label>

        <div class="col-sm-10">
            <g:textField name="inputLabel" class="form-control" value="${inputLabel}"/>
        </div>
        <label for="inputDescription" class="col-sm-2 control-label">Description</label>

        <div class="col-sm-10">
            <g:textField name="inputDescription" class="form-control" value="${inputDescription}"/>
        </div>

        <label for="sharedCheck" class="col-sm-2 control-label text-right">Shared (public)?</label>
        <div class="col-sm-10">
            <g:checkBox id="sharedCheck" name="sharedChk" value="true" checked="${sharedChk}"/>
        </div>
    </div>
</div>

