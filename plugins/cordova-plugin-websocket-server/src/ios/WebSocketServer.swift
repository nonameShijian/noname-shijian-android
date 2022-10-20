import Foundation

@objc(WebSocketServer) public class WebSocketServer : CDVPlugin, PSWebSocketServerDelegate {

    fileprivate var wsserver: PSWebSocketServer?
    fileprivate var port: Int?
    fileprivate var origins: [String]?
    fileprivate var protocols: [String]?
    fileprivate var UUIDSockets: [String: PSWebSocket]!
    fileprivate var socketsUUID: [PSWebSocket: String]!
    fileprivate var didCloseUUIDs: [String]!
    fileprivate var startCallbackId: String?
    fileprivate var stopCallbackId: String?
    fileprivate var didStopOrDidFail: Bool = false

    override public func pluginInitialize() {
        UUIDSockets  = [:]
        socketsUUID = [:]
        didCloseUUIDs = []
    }

    override public func onAppTerminate() {
        if let server = wsserver {
            server.stop()
            wsserver = nil
            UUIDSockets.removeAll()
            socketsUUID.removeAll()
            didCloseUUIDs.removeAll()
        }
    }

    @objc public func getInterfaces(_ command: CDVInvokedUrlCommand) {

        commandDelegate?.run(inBackground: {

            var obj = [String: [String: [String]]]()
            var intfobj: [String: [String]]
            var ipv4Addresses: [String]
            var ipv6Addresses: [String]

            for intf in Interface.allInterfaces() {
                if !intf.isLoopback {
                    if let ifobj = obj[intf.name] {
                        intfobj = ifobj
                        ipv4Addresses = intfobj["ipv4Addresses"]!
                        ipv6Addresses = intfobj["ipv6Addresses"]!
                    } else {
                        intfobj = [:]
                        ipv4Addresses = []
                        ipv6Addresses = []
                    }

                    if intf.family == .ipv6 {
                        if ipv6Addresses.firstIndex(of: intf.address!) == nil {
                            ipv6Addresses.append(intf.address!)
                        }
                    } else if intf.family == .ipv4 {
                        if ipv4Addresses.firstIndex(of: intf.address!) == nil {
                            ipv4Addresses.append(intf.address!)
                        }
                    }

                    if (!ipv4Addresses.isEmpty) || (!ipv6Addresses.isEmpty) {
                        intfobj["ipv4Addresses"] = ipv4Addresses
                        intfobj["ipv6Addresses"] = ipv6Addresses
                        obj[intf.name] = intfobj
                    }
                }
            }

            #if DEBUG
                print("WebSocketServer: getInterfaces: \(obj)")
            #endif

            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: obj)
            pluginResult?.setKeepCallbackAs(false)
            self.commandDelegate?.send(pluginResult, callbackId: command.callbackId)

        })

    }

    @objc public func start(_ command: CDVInvokedUrlCommand) {

        #if DEBUG
            print("WebSocketServer: start")
        #endif
        
        if didStopOrDidFail {
            wsserver = nil
            didStopOrDidFail = false
        }

        if wsserver != nil {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Server already running")
            self.commandDelegate?.send(pluginResult, callbackId: command.callbackId)
            return
        }

        port = command.argument(at: 0) as? Int
        
        if port! < 0 || port! > 65535 {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Port number error")
            self.commandDelegate?.send(pluginResult, callbackId: command.callbackId)
            return
        }
        
        origins = command.argument(at: 1) as? [String]
        protocols = command.argument(at: 2) as? [String]
        let tcpNoDelay = command.argument(at: 3) as? Bool

        if let server = PSWebSocketServer(host: nil, port: UInt(port!)) {
            startCallbackId = command.callbackId
            wsserver = server
            server.delegate = self
            
            if tcpNoDelay != nil {
                server.setTcpNoDelay(tcpNoDelay!)
            }

            commandDelegate?.run(inBackground: {
                server.start()
            })

            let pluginResult = CDVPluginResult(status: CDVCommandStatus_NO_RESULT)
            pluginResult?.setKeepCallbackAs(true)
        }

    }

    @objc public func stop(_ command: CDVInvokedUrlCommand) {

        #if DEBUG
            print("WebSocketServer: stop")
        #endif
        
        if didStopOrDidFail {
            wsserver = nil
            didStopOrDidFail = false
        }
        
        if wsserver == nil {
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: "Server is not running")
            self.commandDelegate?.send(pluginResult, callbackId: command.callbackId)
            return
        }

        if let server = wsserver {
            stopCallbackId = command.callbackId

            commandDelegate?.run(inBackground: {
                server.stop()
            })
            
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_NO_RESULT)
            pluginResult?.setKeepCallbackAs(true)
        }
    }

    @objc public func send(_ command: CDVInvokedUrlCommand) {

        #if DEBUG
            print("WebSocketServer: send")
        #endif

        let uuid = command.argument(at: 0) as? String
        let msg = command.argument(at: 1) as? String

        if uuid != nil && msg != nil {
            if let webSocket = UUIDSockets[uuid!] {

                commandDelegate?.run(inBackground: {
                    webSocket.send(msg)
                })

            } else {
                #if DEBUG
                    print("WebSocketServer: Send: unknown socket.")
                #endif
            }
        } else {
            #if DEBUG
                print("WebSocketServer: Send: UUID or msg not specified.")
            #endif
        }
    }
    
    @objc public func send_binary(_ command: CDVInvokedUrlCommand) {

        #if DEBUG
            print("WebSocketServer: send_binary")
        #endif

        let uuid = command.argument(at: 0) as? String
        let msg = command.argument(at: 1) as? String

        if uuid != nil && msg != nil {
            if let webSocket = UUIDSockets[uuid!] {

                commandDelegate?.run(inBackground: {
                    webSocket.send(NSData(base64Encoded: msg!, options: .ignoreUnknownCharacters))
                })

            } else {
                #if DEBUG
                    print("WebSocketServer: Send: unknown socket.")
                #endif
            }
        } else {
            #if DEBUG
                print("WebSocketServer: Send: UUID or msg not specified.")
            #endif
        }
    }

    @objc public func close(_ command: CDVInvokedUrlCommand) {

        #if DEBUG
            print("WebSocketServer: close")
        #endif

        let uuid = command.argument(at: 0) as? String
        let code = command.argument(at: 1, withDefault: -1) as! Int
        let reason = command.argument(at: 2) as? String

        if uuid != nil {
            if let webSocket = UUIDSockets[uuid!] {

                commandDelegate?.run(inBackground: {
                    if (code == -1) {
                        webSocket.close()
                    } else {
                        webSocket.close(withCode: code, reason: reason)
                    }
                })

            } else {
                #if DEBUG
                    print("WebSocketServer: Close: unknown socket.")
                #endif
            }
        } else {
            #if DEBUG
                print("WebSocketServer: Close: UUID not specified.")
            #endif
        }
    }

    public func serverDidStart(_ server: PSWebSocketServer!) {

        #if DEBUG
            print("WebSocketServer: Server did start…")
        #endif

        let status: NSDictionary = NSDictionary(objects: ["0.0.0.0", Int(server.realPort)], forKeys: ["addr" as NSCopying, "port" as NSCopying])
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: (status as! [AnyHashable: Any]))
        pluginResult?.setKeepCallbackAs(true)
        commandDelegate?.send(pluginResult, callbackId: startCallbackId)
    }

    public func serverDidStop(_ server: PSWebSocketServer!) {

        #if DEBUG
            print("WebSocketServer: Server did stop…")
        #endif

        didStopOrDidFail = true
        UUIDSockets.removeAll()
        socketsUUID.removeAll()
        didCloseUUIDs.removeAll()

        let status: NSDictionary = NSDictionary(objects: ["0.0.0.0", Int(server.realPort)], forKeys: ["addr" as NSCopying, "port" as NSCopying])
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: (status as! [AnyHashable: Any]))
        pluginResult?.setKeepCallbackAs(false)
        commandDelegate?.send(pluginResult, callbackId: stopCallbackId)
    }

    public func server(_ server: PSWebSocketServer!, didFailWithError error: Error!) {

        #if DEBUG
            print("WebSocketServer: Server did fail with error: \(error)")
        #endif
        
        // normally already stopped. just making sure!
        wsserver?.stop()

        didStopOrDidFail = true
        UUIDSockets.removeAll()
        socketsUUID.removeAll()
        didCloseUUIDs.removeAll()

        let status: NSDictionary = NSDictionary(objects: ["onFailure", "0.0.0.0", port!, error.localizedDescription], forKeys: ["action" as NSCopying, "addr" as NSCopying, "port" as NSCopying, "reason" as String as NSCopying])
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: (status as! [AnyHashable: Any]))
        pluginResult?.setKeepCallbackAs(false)
        commandDelegate?.send(pluginResult, callbackId: startCallbackId)
    }

    public func server(_ server: PSWebSocketServer!, acceptWebSocketFrom address: Data, with request: URLRequest, trust: SecTrust, response: AutoreleasingUnsafeMutablePointer<HTTPURLResponse?>) -> Bool {

        #if DEBUG
            print("WebSocketServer: Server should accept request: \(request)")
        #endif

        if let o = origins {
            let origin = request.value(forHTTPHeaderField: "Origin")
            if o.firstIndex(of: origin!) == nil {
                #if DEBUG
                    print("WebSocketServer: Origin denied: \(origin)")
                #endif
                return false
            }
        }

        if let _ = protocols {
            if let acceptedProtocol = getAcceptedProtocol(request) {
                let headerFields = [ "Sec-WebSocket-Protocol" : acceptedProtocol ]
                let r = HTTPURLResponse.init(url: request.url!, statusCode: 200, httpVersion: "1.1", headerFields: headerFields )!
                response.pointee = r
            } else {
                #if DEBUG
                    let secWebSocketProtocol = request.value(forHTTPHeaderField: "Sec-WebSocket-Protocol")
                    print("WebSocketServer: Sec-WebSocket-Protocol denied: \(secWebSocketProtocol)")
                #endif
                return false
            }
        }

        return true;
    }

    public func server(_ server: PSWebSocketServer!, webSocketDidOpen webSocket: PSWebSocket!) {

        #if DEBUG
            print("WebSocketServer: WebSocket did open")
        #endif
        
        // clean previously closed sockets
        for closedUUID in didCloseUUIDs {
            if let webSocket = UUIDSockets[closedUUID] {
                socketsUUID.removeValue(forKey: webSocket)
            }
            UUIDSockets.removeValue(forKey: closedUUID)
        }
        didCloseUUIDs.removeAll()

        var uuid: String!
        while uuid == nil || UUIDSockets[uuid] != nil {
            // prevent collision
            uuid = UUID().uuidString
        }
        UUIDSockets[uuid] = webSocket
        socketsUUID[webSocket] = uuid

        let remoteAddr = webSocket.remoteHost!

        var acceptedProtocol = ""
        if (protocols != nil) {
            acceptedProtocol = getAcceptedProtocol(webSocket.urlRequest)!
        }

        let httpFields = webSocket.urlRequest.allHTTPHeaderFields!

        var resource = ""
        if (webSocket.urlRequest.url!.query != nil) {
            resource = String(cString: (webSocket.urlRequest.url!.query?.cString(using: String.Encoding.utf8))! )
        }

        let conn: NSDictionary = NSDictionary(objects: [uuid!, remoteAddr, acceptedProtocol, httpFields, resource], forKeys: ["uuid" as NSCopying, "remoteAddr" as NSCopying, "acceptedProtocol" as NSCopying, "httpFields" as NSCopying, "resource" as NSCopying])
        let status: NSDictionary = NSDictionary(objects: ["onOpen", conn], forKeys: ["action" as NSCopying, "conn" as NSCopying])
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: (status as! [AnyHashable: Any]))
        pluginResult?.setKeepCallbackAs(true)
        commandDelegate?.send(pluginResult, callbackId: startCallbackId)
    }

    public func server(_ server: PSWebSocketServer!, webSocket: PSWebSocket!, didReceiveMessage message: Any) {

        #if DEBUG
            print("WebSocketServer: Websocket did receive message: \(message)")
        #endif

        if let uuid = socketsUUID[webSocket] {
            let objects: [Any];
            if let data = message as? NSData{
                objects = ["onMessage", uuid, data.base64EncodedString(), true];
            } else {
                objects = ["onMessage", uuid, message, false];
            }
            let status: NSDictionary = NSDictionary(objects: objects, forKeys: ["action" as NSCopying, "uuid" as NSCopying, "msg" as NSCopying, "is_binary" as NSCopying])
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: (status as! [AnyHashable: Any]))
            pluginResult?.setKeepCallbackAs(true)
            commandDelegate?.send(pluginResult, callbackId: startCallbackId)
        } else {
            #if DEBUG
                print("WebSocketServer: unknown socket")
            #endif
        }
    }

    public func server(_ server: PSWebSocketServer!, webSocket: PSWebSocket!, didCloseWithCode code: Int, reason: String, wasClean: Bool) {

        #if DEBUG
            print("WebSocketServer: WebSocket did close with code: \(code), reason: \(reason), wasClean: \(wasClean)")
        #endif

        if let uuid = socketsUUID[webSocket] {
            
            didCloseUUIDs.append(uuid)
            
            let status: NSDictionary = NSDictionary(objects: ["onClose", uuid, code, reason, wasClean], forKeys: ["action" as NSCopying, "uuid" as NSCopying, "code" as NSCopying, "reason" as NSCopying, "wasClean" as NSCopying])
            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: (status as! [AnyHashable: Any]))
            pluginResult?.setKeepCallbackAs(true)
            commandDelegate?.send(pluginResult, callbackId: startCallbackId)
        } else {
            #if DEBUG
                print("WebSocketServer: unknown socket")
            #endif
        }
    }

    public func server(_ server: PSWebSocketServer!, webSocket: PSWebSocket!, didFailWithError error: Error!) {

        #if DEBUG
            print("WebSocketServer: WebSocket did fail with error: \(error)")
        #endif

        if (webSocket.readyState == PSWebSocketReadyState.open) {
            webSocket.close(withCode: 1011, reason: "")
        }
    }

    fileprivate func getAcceptedProtocol(_ request: URLRequest) -> String? {
        var acceptedProtocol: String?
        if let secWebSocketProtocol = request.value(forHTTPHeaderField: "Sec-WebSocket-Protocol") {
            let requestedProtocols = secWebSocketProtocol.components(separatedBy: ", ")
            for requestedProtocol in requestedProtocols {
                if protocols!.firstIndex(of: requestedProtocol) != nil {
                    // returns first matching protocol.
                    // assumes in order of preference.
                    acceptedProtocol = requestedProtocol
                    break
                }
            }
            #if DEBUG
                print("WebSocketServer: Sec-WebSocket-Protocol: \(secWebSocketProtocol)")
                print("WebSocketServer: Accepted Protocol: \(acceptedProtocol)")
            #endif
        }
        return acceptedProtocol
    }

}
