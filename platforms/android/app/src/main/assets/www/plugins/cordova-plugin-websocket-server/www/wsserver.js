cordova.define("cordova-plugin-websocket-server.WebSocketServer", function(require, exports, module) {
'use strict';
var exec = require('cordova/exec');

var fail = function(o) {
    console.error("Error " + JSON.stringify(o));
};

var connections = [];

var WebSocketServer = {

    getInterfaces : function(success, failure) {
        return exec(success, failure, "WebSocketServer", "getInterfaces", []);
    },

    start : function(port, options, success, failure) {
        return exec(function(result) {
            switch (result.action) {
            case 'onFailure':
                connections = [];
                var callback = options[result.action];
                if (callback) {
                    callback(result.addr, result.port, result.reason);
                }
                break;
            case 'onOpen':
                var conn = result.conn;
                conn.state = 'open';
                connections[conn.uuid] = conn;
                var callback = options[result.action];
                if (callback) {
                    callback(conn);
                }
                break;
            case 'onMessage':
                var conn = connections[result.uuid];
                if (conn) {
                    var callback = options[result.action];
                    if (callback) {
                        if(result.is_binary) {
                            // convert Base64 string to ArrayBuffer
                            var binary_string = window.atob(result.msg);
                            var len = binary_string.length;
                            var bytes = new Uint8Array(len);
                            for (var i = 0; i < len; i++) {
                                bytes[i] = binary_string.charCodeAt(i);
                            }
                            callback(conn, bytes.buffer);
                        }
                        else {
                            callback(conn, result.msg);
                        }
                    }
                }
                break;
            case 'onClose':
                var conn = connections[result.uuid];
                if (conn) {
                    conn.state = 'closed';
                    delete connections[conn.uuid];
                    var callback = options[result.action];
                    if (callback) {
                        callback(conn, result.code, result.reason, result.wasClean);
                    }
                }
                break;
            default:
                connections = [];
                if (success) {
                    success(result.addr, result.port);
                }
            }
        }, failure, "WebSocketServer", "start", [ port, options.origins, options.protocols, options.tcpNoDelay ]);
    },

    stop : function(success, failure) {
        return exec(function(result) {
            connections = [];
            if (success) {
                success(result.addr, result.port);
            }
        }, failure, "WebSocketServer", "stop", []);
    },

    send : function(conn, msg) {
        if (typeof msg == "string") {
            
            // send text frame (websocket opcode 1)
            return exec(null, fail, "WebSocketServer", "send", [ conn.uuid, msg ]);

        } else {
            // convert any iterable object to Base64 string
            var binary_string = '';
            var bytes = new Uint8Array(msg);
            var len = bytes.byteLength;
            for (var i = 0; i < len; i++) {
                binary_string += String.fromCharCode(bytes[i]);
            }
            var msg_base64 = window.btoa(binary_string);

            // send binary frame (websocket opcode 2)
            return exec(null, fail, "WebSocketServer", "send_binary", [ conn.uuid, msg_base64 ]);
        }
    },

    close : function(conn, code, reason) {
        return exec(null, fail, "WebSocketServer", "close", [ conn.uuid, code, reason ]);
    }

};

module.exports = WebSocketServer;

});
