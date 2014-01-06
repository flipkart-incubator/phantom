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
