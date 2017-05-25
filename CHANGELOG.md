## 3.4.0 (May 25, 2017)
- Passing execution errors to fallback

## 3.3.0  (Feb 21, 2017)
- Upgrading to Jetty 9.4.1.v20170120

## 3.2.0  (Jan 2, 2017)
- Included snapshot version fixes

## 3.2.0-SNAPSHOT (Nov 22, 2016)
- Upgrade to Hystrix 1.5.8
- Customizable core and max thread pool sizes for Task

## 3.1.0  (Nov 22, 2016)
- Included snapshot version fixes

## 3.1.0-SNAPSHOT (Nov 17, 2016)
- Non blocking http and thrift proxy

## 3.0.1  (Oct 18, 2016)
- Using fasterxml jackson consistently

## 3.0.0  (Oct 14, 2016)
- Included snapshot version fixes
- Upgrade to Trooper 2.0.1 to remove a bottleneck in event publishing

## 3.0.0-SNAPSHOT (Oct 2, 2016)
- Generic params in TaskHandler's execute
- Zipkin headers ingestion for TaskHandlers

## 2.0.1  (Jul 1, 2016)
- Included snapshot version fixes

## 2.0.1-SNAPSHOT (Jun 30, 2016)
- ZipkinSpanCollectorParams failOnSetup is configurable
- Bug fix: Fix for ConcurrentModificationException during shutdown

## 2.0.0  (May 25, 2016)
- Included snapshot version fixes

## 2.0.0-SNAPSHOT (May 23, 2016)
- JSON serialization of TaskResult data in command protocol
- Upgrade to Trooper 2.0.0

## 1.4.4  (Mar 9, 2016)
- Support generic return type in TaskHandler's execute
- Bug fix: Fix for closing connection/release resources in case of Hystrix timeouts

## 1.4.3  (Dec 16, 2015)
- Included snapshot version fixes
- Bug fix: Fix for IllegalStateException in ClientRequestInterceptor
- Bug fix: Trace/span ids in hex are now reliably converted to 64 bits 

## 1.4.3-SNAPSHOT (Nov 27, 2015)
- Fixed TCPNettyServer to use worker thread count, if specified

## 1.4.2 (Nov 18, 2015)
- Upgrade to Spring 4.x
- Bug fix to Http proxy to return response content length
- Task interface change to support generic return type, not just byte array

## 1.4.2-SNAPSHOT (Sep 8, 2015)
- Upgrade to Spring 4.x

## 1.4.1 (Jul 2, 2015)
- Fixed bug in async execution that was causing sync execution

## 1.4.0 (Jun 25, 2015)
- Fixes related to distributed tracing

## 1.3.9 (May 21, 2015)
- Upgrade to Jetty 9.x

## 1.3.8 (May 20, 2015)
- Upgrade snapshot to release
- Upgrading to Trooper 1.3.2 release

## 1.3.7-SNAPSHOT (Mar 31, 2015)
- Upgrade to Jetty 9

## 1.3.6 (Mar 30, 2015)
- Upgrading snapshot to release version

## 1.3.6-SNAPSHOT (Feb 26, 2015)
- Bug fix for service request logger
- Dependencies cleanup
- Hystrix metrics snapshot servlet enhancements

## 1.3.5 (Jan 16, 2015)
- Added Zipkin distributed tracing support.
- Metrics snapshot with configurable frequency. Useful when pushing service metrics to time series databases

## 1.3.4-SNAPSHOT (Oct 10, 2014)
- Added support for parallel init for Task handlers
- Upgrade to Trooper 1.3.2-SNAPSHOT version

## 1.3.3 (Oct 05, 2014)
- Bug fix for logging handler execution warnings and errors

## 1.3.2-SNAPSHOT (Sep 21, 2014)
- RequestCacheableHystrixTaskHandler.getCacheKey() also receives data
- Handlers can now veto or allow container init to continue
- Support for re-loading handlers via REST call

## 1.3.0-SNAPSHOT (Sep 10, 2014)
- Added Request caching support to TaskHandlers
- Added Decoder for clients to be able to decode the response with default implementation as ByteArrayDecoder
- Templatized TaskResult
- Cleanup stupid shitty code of SystemUtils and StringUtils
- Moved to jackson mapper 2.x

## 1.2.9-SNAPSHOT (Sep 01, 2014)
- Adding support for setting Max concurrency for semaphore isolated requests
- exposing timeToLive for socket in HttpConnectionPool
- Adding empty "openCircuitHostNames" if circuit is closed
- adding snapshot servlet for service proxy instrumentation
-  **Bug Fixes:**
    - Incorrect Logging of failed events in RequestLogged for async commands.


## 1.2.8 (June 05, 2014)
- Upgrade to Hystrix 1.4.0-RC4

## 1.2.8-SNAPSHOT (February 14, 2014)
- Externalized dashboard thread settings to configurations
- Customized Hystrix Metrics Puller and Stream Servlet to report short-circuited host names

## 1.2.7 (January 31, 2014)
- Decreased default timeout of Hysterix TaskHandlers to 1 sec from 10 sec.
- **Bug Fixes:**
    - Fixes for PUT request when headers are forwarded.

## 1.2.6 (January 29, 2014)
- Introduce Common Proxy Handler Config which can be used to load handlers which should be loaded first.

## 1.2.5 (January 06, 2014)
- Better fallback handling in Thrift and Http proxies, empty response for timeout Http requests

## 1.2.3 (December 27, 2013)
- Changed command name being published for Thrift Handlers. Uncommented sending of profiling metrics.

## 1.2.2 (December 12, 2013)
- Added RequestId, Execution time to ServiceProxyEvent. RequestLogger now logs added fields.

## 1.2.1 (December 10, 2013)
- Refactoring ThriftProxy ,ThriftExecutor classes

## 1.2.0 (November 18, 2013)
- Upgrade to Trooper 1.3.0 and therefore to Spring 3.2.5.RELEASE
- **Bug Fixes:**
  - Selective ability to pass request headers to down-stream http services. Issue #5
  
## 1.1.9 (November 08, 2013)
- **Bug Fixes:**
  - Ability to pass request headers to down-stream http services

## 1.1.8 (November 07, 2013)
- **Bug Fixes:**
  - Removing casting of executor repositories which made the abstraction logic obsolete
  - Adding getHostName method in task context

## 1.1.7 (October 29, 2013)
- **New Features:**
  - Abstraction of Executor and Executor Repository.
  - Event publish/consumption frame-work for request

## 1.1.5 (October 1, 2013)
- **Bug fixes:**
  - Making sure the task repository is set on the singleton task context
  - Allowing _ in valid command names


## 1.1.3 (September 29, 2013)
- **Bug fixes:**
  - Single task handler executor repository and task handler registry initialization in common-beans-context
  - UI fixes for dashboard
  - Initializing all listener servers at the end


## 1.1.0 (September 25, 2013)
- **New features:**
  - Support for async execution of commands
  - Support for thrift method level timeouts and thread pools in Thrift proxy


## 1.0.0 (August 22, 2013)
- **New features:**
  - Task module that defines API interfaces for writing proxy handlers
  - Runtime module that manages deployed proxy handlers and socket listeners
  - Netty based socket listeners for TCP/IP and UDS(Unix domain sockets)
  - Independent UDS transport for Netty (may be used outside of Phantom)
  - Codecs for Http and Thrift (available as optional modules)
  - Integrated Hystrix dashboard via embedded Jetty in Runtime module
  - Configuration console for editing proxy handler properties
  - Sample projects introducing Phantom capabilities
<br />
- **Docs changes:**  
  - https://github.com/Flipkart/phantom/wiki/Maven-dependencies [Info on Maven settings for building/using Phantom]
  - https://github.com/Flipkart/phantom/wiki/Getting-started [Getting started with examples]
