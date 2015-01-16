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

	<!-- HystrixThreadPool -->
	<script type="text/javascript" src="../hystrix-dashboard/components/hystrixThreadPool/hystrixThreadPool.js"></script>
	<link rel="stylesheet" type="text/css" href="../hystrix-dashboard/components/hystrixThreadPool/hystrixThreadPool.css" />

	<div class="container">
 		<div class="row">
 			<div class="menubar">
 				<div class="title">
 				Thread Pools
 				</div>
 			</div>
 		</div>
 		<div id="dependencyThreadPools" class="row dependencyThreadPools"><span class="loading">Loading ...</span></div>
	</div>



<script>
		/**
		 * Queue up the monitor to start once the page has finished loading.
		 *
		 * This is an inline script and expects to execute once on page load.
		 */

		// get thread pool name
		var tp = location.pathname.split("/")[2];

		// handler for thread pools
		var dependencyThreadPoolMonitor = new HystrixThreadPoolMonitor('dependencyThreadPools', [tp]);

        // thread pool stream
		var poolStream = "../../turbine.stream.tp";

		$(window).load(function() { // within load with a setTimeout to prevent the infinite spinner
			setTimeout(function() {

				// sort by volume
				dependencyThreadPoolMonitor.sortByVolume();

				// start the EventSource which will open a streaming connection to the server
				var source = new EventSource(poolStream);

				// add the listener that will process incoming events
				source.addEventListener('message', dependencyThreadPoolMonitor.eventSourceMessageListener, false);

				// error handler
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
