<html>
	<head>
		<meta name="layout" content="main-one-pane" />
		<title>tree browser scaffold</title>
		<asset:stylesheet src="treeView/tree.css"/>
		<asset:javascript src="treeView/angular.js"/>
		<asset:javascript src="treeView/recursionhelper.js"/>
		<asset:javascript src="treeView/angular-sanitize.js"/>
		<asset:javascript src="treeView/jquery.scrollintoview.js"/>
		<asset:javascript src="treeView/tree-view.js"/>
		
		<script type="text/javascript">

		function doGetName() {
			var name = $('#MAIN-TREE')[0].getSelectedName();
			$('#SELECTED-THING-GOES-HERE').text(JSON.stringify(name));
			$('#INPUT-NAME-ID').val(name ? name.id : name);
		}
		
		function doGetInstance() {
			var instance = $('#MAIN-TREE')[0].getSelectedInstance()
			$('#SELECTED-THING-GOES-HERE').text(JSON.stringify(instance));
			$('#INPUT-INST-ID').val(instance ? instance.id : null);
		}

		function doNavToName() {
			$('#MAIN-TREE')[0].navigateToNslName(parseInt($('#INPUT-NAME-ID').val()));
		}
		
		function doNavToInstance() {
			$('#MAIN-TREE')[0].navigateToNslInstance(parseInt($('#INPUT-INST-ID').val()));
		}
		
		$(function() {
			$('#MAIN-TREE').on('mouseenter.nsl-tree', function(event, data) {
				$('#INFO-PANE').text(JSON.stringify(data));
			});
			
			$('#MAIN-TREE').on('mouseleave.nsl-tree', function(event, data) {
				$('#INFO-PANE').text(' ');
			});
			
			$('#MAIN-TREE').on('change.nsl-tree', function(event, data) {
				$('#SELECTED-THING-GOES-HERE').text(JSON.stringify(data));
			});
		});
		
		</script>
	</head>
	<body>
		<div style="position: absolute; top:0; height: 10em">	
		This view serves as a container for a tree browser component. We are looking at name ${name ?: 'top layer'} in tree ${tree} aka ${tree?.label} 
		<br/>
		<button onclick="doGetName();">press me to get the current name</button><input id="INPUT-NAME-ID" type="number" value="115545"/> <button onclick="doNavToName();">press me to navigate to name</button>
		<br/>
		<button onclick="doGetInstance();">press me to get the current instance</button><input id="INPUT-INST-ID" type="number"/> <button onclick="doNavToInstance();">press me to navigate to instance</button>
		<div id="SELECTED-THING-GOES-HERE" style="font-size: 8pt; font-family: monospace; overflow:hidden; "></div>
		
		</div>
		
		<div style="position: absolute; top:10.5em; bottom:2px; left:2px; right:2px; border: thin dotted red;">
			<div 
				style="position: absolute; top:2px; height: 20px; left:2px; right:2px; background-color:#e0e0f0;" 
			>
				this is a menu bar
			</div>
			
			<div style="position: absolute; top:24px; bottom:24px; width:20em; right: 2px;">
				<div 
					id="INFO-PANE"
					style="position: absolute; top:0; bottom:0; left:4px; right: 0; border: thin dotted magenta; overflow: hidden; padding: 2px; font-size: 8pt; font-family: monospace;"  
				>
				</div>
			</div>
			
			<div 
				ng-app="au.org.biodiversity.nsl.tree-browser"
				id="MAIN-TREE"
				style="position: absolute; top:24px; bottom:24px; left:2px; right:20em; border: thin dotted green; overflow: scroll;" 
				main-tree
				data-tree-label="${tree.label}"
				data-name-id="${name?.id}"
				data-services-url="/services"
			/>
			

		</div>

<!-- 		
		{
			$('#MAIN-TREE').navigateToName(3456);
			
			alert("the selected name is " + $('#MAIN-TREE').getSelectedName());
			alert("the selected instance is " + $('#MAIN-TREE').getSelectedInstance());
			
			
			$('#MAIN-TREE').addSelectionEventListener(
				function(name, instance) {
					alert(name + ' pgot selected');
				}
			);
			
		
		}
 -->		
	</body>
</html>