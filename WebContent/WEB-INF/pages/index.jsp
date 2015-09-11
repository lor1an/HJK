<!DOCTYPE html>
<html>
<head>
<title>Tail</title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<script src="resources/javascripts/jquery-1.4.3.js"></script>
<script src="resources/javascripts/jquery.form.js"></script>
<script src="resources/javascripts/jquery.atmosphere.js"></script>
<script src="resources/javascripts/jquery.atmosphere.js"></script>
<script src="resources/js/scripts.js"></script>
<link href="resources/css/style.css" rel="stylesheet">

</head>
<body>

	<div id="selector">
		<select>
			<option value="" selected>-- select a log --</option>
		</select>
	</div>
	<div id="info" class="trebuchet"></div>
	<div id="tail" class="monospace selection"></div>
	<input id="refresh" value="Refresh" type="button">
	<button class="start_stop flag">Pause</button>
<!-- 	<button class="prev">Prev 200</button> -->
	<button class="download">Download</button>
	<button class="gist">Upload all to gist</button>
	<button class="gistSel">Upload selected to gist</button>
	<iframe id="downloadFrame" style="display:none"></iframe>
</body>
</html>
