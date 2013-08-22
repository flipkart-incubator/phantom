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
