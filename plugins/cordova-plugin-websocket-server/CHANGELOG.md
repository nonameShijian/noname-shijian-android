## [1.6.0] - 2020-04-18

- Support for binary messages (websocket opcode 2)
- [iOS] updating cordova-plugin-add-swift-support dependency to 2.0.2
- [iOS] support Swift 5

## [1.5.0] - 2019-04-02

- [Android] upgrading to [org:java-websocket:1.4.0](https://github.com/TooTallNate/Java-WebSocket)
- [iOS] updating cordova-plugin-add-swift-support dependency to 2.0.1
- [iOS] support Swift 4

## [1.4.11] - 2018-11-07

- [Android] replacing compile with implementation (Android Gradle plugin 3.0)
- [Android] upgrading to [org:java-websocket:1.3.9](https://github.com/TooTallNate/Java-WebSocket)

## [1.4.10] - 2018-04-04

- [Android] fixed "Error during WebSocket handshake: code: 404" when using the protocols option

## [1.4.9] - 2018-04-03

- [Android] preventing crash in the onError handler
- [Android] upgrading to [org:java-websocket:1.3.8](https://github.com/TooTallNate/Java-WebSocket)
- [iOS] updating cordova-plugin-add-swift-support dependency to 1.7.2

## [1.4.8] - 2018-01-02

- [Android] upgrading to [org:java-websocket:1.3.7](https://github.com/TooTallNate/Java-WebSocket)
- [iOS] updating cordova-plugin-add-swift-support dependency to version 1.7.1

## [1.4.7] - 2017-08-31

- setting version of cordova-plugin-add-swift-support dependency.
- [iOS] fixed not working with firefox client

## [1.4.6] - 2017-07-20

- Changelog moved to CHANGELOG.md
- [Android] fixed wasClean in the onClose handler (now returns true on normal termination)
- [Android] fixed closing a connection server-side was not triggering the onClose handler

## [1.4.5]

- [Android] upgrading to [org:java-websocket:1.3.4](https://github.com/TooTallNate/Java-WebSocket)
- [iOS] fixing crash when tcpNoDelay not defined

## [1.4.4]

- [Android] fixed NPE when getting HostAddress. 'addr' or 'remoteAddr' fields may be null.
- [Android] fixed WebsocketNotConnectedException when calling send
- [Android] upgrading to [org:java-websocket:1.3.3](https://github.com/TooTallNate/Java-WebSocket)

## [1.4.3]

- plugin.xml: moving js clobbers from global to only supported platforms
- [Android] switching back to [org:java-websocket:1.3.2](https://github.com/TooTallNate/Java-WebSocket)

## [1.4.2]

- fixed error when a connection is open before the start success callback occurs

## [1.4.1]

- new tcpNoDelay option

## [1.4.0]

- onStart, onDidNotStart and onStop handlers replaced with success and failure callbacks
- added generic onFailure handler (assume the server is unexpectedly stopped in this handler)
- [iOS] fixed crash on stop and close (dealloc)

## [1.3.1]

- adding state 'open' or 'closed' to the `conn` object

## [1.3.0]

- getInterfaces returns the ipv4 and ipv6 addresses organized by network interface
- onOpen, onMessage and onClose handlers share the same `conn` instances
- [Android] switching to [com.pusher:java-websocket:1.4.1](https://github.com/pusher/java-websocket)
- [iOS] IPv6 support [30a98b0](https://github.com/couchbasedeps/PocketSocket/commit/30a98b0c62763e11ee5b3e7097a8c8b4b66674f9)


## [1.2.1]

- [iOS] fixed crash (error retrieving URL query string)

## [1.2.0]

- new onDidNotStart handler in the start method
- getInterfaces returns ipv4 and ipv6 addresses