# Cordova WebSocket Server Plugin

This plugin allows you to run a single, lightweight, barebone WebSocket Server from applications developed using PhoneGap/Cordova 3.0 or newer.

This is not a background service. When the cordova view is destroyed/terminated, the server is stopped.

[CHANGELOG](https://github.com/becvert/cordova-plugin-websocket-server/blob/master/CHANGELOG.md)

## Installation ##

In your application project directory:

```bash
cordova plugin add cordova-plugin-websocket-server
```

## Usage ##

```javascript
var wsserver = cordova.plugins.wsserver;
```

#### `start(port, options, success, failure)`
Starts the server on the given port (0 means any free port).
Binds to all available network interfaces ('0.0.0.0').

```javascript
 wsserver.start(port, {
    // WebSocket Server handlers
    'onFailure' :  function(addr, port, reason) {
        console.log('Stopped listening on %s:%d. Reason: %s', addr, port, reason);
    },
    // WebSocket Connection handlers
    'onOpen' : function(conn) {
        /* conn: {
         'uuid' : '8e176b14-a1af-70a7-3e3d-8b341977a16e',
         'remoteAddr' : '192.168.1.10',
         'httpFields' : {...},
         'resource' : '/?param1=value1&param2=value2'
         } */
        console.log('A user connected from %s', conn.remoteAddr);
    },
    'onMessage' : function(conn, msg) {
        console.log(conn, msg); // msg can be a String (text message) or ArrayBuffer (binary message)
    },
    'onClose' : function(conn, code, reason, wasClean) {
        console.log('A user disconnected from %s', conn.remoteAddr);
    },
    // Other options
    'origins' : [ 'file://' ], // validates the 'Origin' HTTP Header.
    'protocols' : [ 'my-protocol-v1', 'my-protocol-v2' ], // validates the 'Sec-WebSocket-Protocol' HTTP Header.
    'tcpNoDelay' : true // disables Nagle's algorithm.
}, function onStart(addr, port) {
    console.log('Listening on %s:%d', addr, port);
}, function onDidNotStart(reason) {
    console.log('Did not start. Reason: %s', reason);
});
```

#### `stop(success,failure)`
Stops the server.

```javascript
wsserver.stop(function onStop(addr, port) {
    console.log('Stopped listening on %s:%d', addr, port);
});
```

#### `send(conn, msg)`
Sends a message to the given connection.

```javascript
// provide a String to send a text frame (websocket opcode 1)
wsserver.send({'uuid':'8e176b14-a1af-70a7-3e3d-8b341977a16e'}, 'hello friend!');

// provide a TypedArray / ArrayBuffer to send a binary frame (websocket opcode 2)
wsserver.send({'uuid':'8e176b14-a1af-70a7-3e3d-8b341977a16e'}, Uint8Array.from([1, 2, 3, 4]));
```

#### `close(conn, code, reason)`
Closes a websocket connection. Close event code and reason are optional.

```javascript
wsserver.close({'uuid':'8e176b14-a1af-70a7-3e3d-8b341977a16e'}, 4000, 'my reason');
```

#### `getInterfaces(callback)`
Returns the non-loopback IPv4 and IPv6 network interfaces.

```javascript
wsserver.getInterfaces(function(result) {
    for (var interface in result) {
        if (result.hasOwnProperty(interface)) {
            console.log('interface', interface);
            console.log('ipv4', result[interface].ipv4Addresses);
            console.log('ipv6', result[interface].ipv6Addresses);
        }
    }
});
```

## Credits

#### Android
It depends on [the TooTallNate WebSocket Server](https://github.com/TooTallNate/Java-WebSocket).

#### iOS
It depends on [the couchbasedeps PocketSocket Server](https://github.com/couchbasedeps/PocketSocket) forked from [the zwopple PocketSocket Server](https://github.com/zwopple/PocketSocket). 

## Licence ##

The MIT License