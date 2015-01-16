<#include "./../header.ftl">

	<!-- Setup base for everything -->
	<link rel="stylesheet" type="text/css" href="../hystrix-dashboard/css/global.css" />

	<!-- Our custom CSS -->
	<link rel="stylesheet" type="text/css" href="../hystrix-dashboard/monitor/monitor.css" />

	<!-- d3 -->
	<script type="text/javascript" src="../hystrix-dashboard/js/d3.v2.min.js"></script>

	<!-- Javascript to monitor and display -->
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js" type="text/javascript"></script>
	<script type="text/javascript" src="../hystrix-dashboard/js/jquery.tinysort.min.js"></script>
	<script type="text/javascript" src="../hystrix-dashboard/js/tmpl.js"></script>

	<!-- HystrixCommand -->
	<script type="text/javascript" src="../hystrix-dashboard/components/hystrixCommand/hystrixCommand.js?v=2"></script>
	<link rel="stylesheet" type="text/css" href="../hystrix-dashboard/components/hystrixCommand/hystrixCommand.css" />

	<div class="container">

		<div class="row">
			<div class="menubar">
				<div class="title">
                Circuit
				</div>
				<div class="menu_legend">
					<span class="success">Success</span> | <span class="latent">Latent</span> | <span class="shortCircuited">Short-Circuited</span> | <span class="timeout">Timeout</span> | <span class="rejected">Rejected</span> | <span class="failure">Failure</span> | <span class="errorPercentage">Error %</span>
				</div>
			</div>
		</div>
		<div id="dependencies" class="row dependencies"><span class="loading">Loading ...</span></div>

	</div>



<script>
		/**
		 * Queue up the monitor to start once the page has finished loading.
		 *
		 * This is an inline script and expects to execute once on page load.
		 */

		// get command name
		var command = location.pathname.split("/")[2];

		// handler for commands
		var hystrixMonitor = new HystrixCommandMonitor('dependencies', {includeDetailIcon:false}, [command]);

	    // command stream
		var commandStream =  "../../turbine.stream.command";

		$(window).load(function() { // within load with a setTimeout to prevent the infinite spinner
			setTimeout(function() {

				// sort by error+volume by default
				hystrixMonitor.sortByErrorThenVolume();

				// start the EventSource which will open a streaming connection to the server
				var source = new EventSource(commandStream);

				// add the listener that will process incoming events
				source.addEventListener('message', hystrixMonitor.eventSourceMessageListener, false);

				source.addEventListener('error', function(e) {
					if (e.eventPhase == EventSource.CLOSED) {
						// Connection was closed.
						console.log("Connection was closed on error: " + e);
					} else {
						console.log("Error occurred while streaming: " + e);
					}
				}, false);

			},0);
		});

	</script>


<#include "./../footer.ftl">
