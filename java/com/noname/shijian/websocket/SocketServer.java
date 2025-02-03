package com.noname.shijian.websocket;

import android.util.Log;
import android.webkit.WebView;


import com.noname.shijian.websocket.model.Client;
import com.noname.shijian.websocket.model.Devtool;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SocketServer extends WebSocketServer {
    private static final String TAG = "SocketServer";

    public final Map<String, Client> clients = new HashMap<>();
    public final Map<String, Devtool> devtools = new HashMap<>();

    private final WebView devWebView;

    public SocketServer(int port, WebView devWebView) {
        this(new InetSocketAddress(port), devWebView);
    }

    public SocketServer(InetSocketAddress address, WebView devWebView) {
        super(address);
        this.devWebView = devWebView;
        this.setReuseAddr(true);
    }

    // Create a client ws connection
    public void createClientSocketConnect(WebSocket ws, Client connectInfo) {
        String id = connectInfo.id;
        clients.put(id, connectInfo);
    }

    // Create a devtools ws connection
    public void createDevtoolsSocketConnect(WebSocket ws, Devtool connectInfo) {
        String id = connectInfo.id;
        devtools.put(id, connectInfo);
    }

    private void sendToDevtools(String id, String message) {
        for (HashMap.Entry<String, Devtool> devtoolsEntry : devtools.entrySet()) {
            Devtool devtool = devtoolsEntry.getValue();
            if (Objects.equals(id, devtool.clientId)) {
                devtool.ws.send(message);
                break;
            }
        }
    }

    private void closeToDevtools(String id, WebSocket ws) {
        for (HashMap.Entry<String, Devtool> devtoolsEntry : devtools.entrySet()) {
            Devtool devtool = devtoolsEntry.getValue();
            if (Objects.equals(id, devtool.clientId)) {
                devtool.ws.close();
                devtools.remove(devtool.id);
                break;
            }
        }
    }

    @Override
    public void onOpen(WebSocket ws, ClientHandshake handshake) {
        Log.e(TAG, ws.getRemoteSocketAddress() + " connected");
        String resourcePath = handshake.getResourceDescriptor();
        Log.e(TAG, resourcePath);
        try {
            URI urlParse = new URI("http://0.0.0.0" + resourcePath);
            String pathname = urlParse.getPath().replace("/remote/debug", "");
            String from = pathname.split("/")[1];
            String id = pathname.split("/")[2];
            Log.e(TAG, "pathname: " + pathname);
            Log.e(TAG, "from: " + from);
            Log.e(TAG, "id: " + id);
            if (!"devtools".equals(from) && !"client".equals(from)) return;
            // 获取查询参数部分
            HashMap<String, String> searchParams = new HashMap<>();
            for (String param : urlParse.getQuery().split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length > 1) {
                    searchParams.put(keyValue[0], keyValue[1]);
                } else {
                    // 处理没有值的参数
                    searchParams.put(keyValue[0], "");
                }
            }
            if ("client".equals(from)) {
                // Create a connection for client sources
                String pageUrl = searchParams.get("url");
                createClientSocketConnect(ws,
                        new Client(
                                ws,
                                id,
                                pageUrl,
                                searchParams.get("ua"),
                                searchParams.get("time"),
                                searchParams.get("title"),
                                searchParams.get("favicon")
                        )
                );
                devWebView.post(() -> {
                    String version = "";
                    String userAgent = devWebView.getSettings().getUserAgentString();
                    // 匹配Chromium版本号
                    String regex = " Chrome\\/([\\d.]+)";
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                    java.util.regex.Matcher matcher = pattern.matcher(userAgent);

                    if (matcher.find()) {
                        Log.i(TAG, "Chromium version from User-Agent: " + matcher.group(1));
                        version = matcher.group(1);
                    } else {
                        Log.w(TAG, "Could not find Chromium version in User-Agent: " + userAgent);
                    }
                    Log.e(TAG, "webView: devWebView loadUrl");
                    String url = "https://localhost/devtools-frontend/devtools_app.html" +
                    "?remoteVersion=" + version +
                    "&ws=localhost:9222/remote/debug/devtools/"
                            + java.util.UUID.randomUUID()
                            + "?clientId=" + id;
                    Log.e(TAG, "DevTools WebView url: " + url);
                    // MainActivity.devWebViewUrl = url;
                    devWebView.loadUrl(url);
                });
            }
            else {
                // Create a connection sourced from devtools
                String clientId = searchParams.get("clientId");
                Client client = this.clients.get(clientId);
                createDevtoolsSocketConnect(ws, new Devtool(ws, client, id, clientId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket ws, int code, String reason, boolean remote) {
        Log.e(TAG, ws + " disconnected");
        for (HashMap.Entry<String, Client> clientEntry : clients.entrySet()) {
            String id = clientEntry.getKey();
            Client client = clientEntry.getValue();
            if (ws == client.ws) {
                clients.remove(client.id);
                closeToDevtools(id, ws);
            }
        }

        for (HashMap.Entry<String, Devtool> devtoolsEntry : devtools.entrySet()) {
            Devtool devtool = devtoolsEntry.getValue();
            if (ws == devtool.ws) {
                devtools.remove(devtool.id);
            }
        }
    }

    @Override
    public void onMessage(WebSocket ws, String message) {
        // Log.e(TAG, "onMessage: " + message);
        if ("{}".equals(message)) return;

        for (HashMap.Entry<String, Client> clientEntry : clients.entrySet()) {
            String id = clientEntry.getKey();
            Client client = clientEntry.getValue();
            if (ws == client.ws) {
                sendToDevtools(id, message);
            }
        }

        for (HashMap.Entry<String, Devtool> devtoolsEntry : devtools.entrySet()) {
            Devtool devtool = devtoolsEntry.getValue();
            if (ws == devtool.ws) {
                Client client = clients.get(devtool.clientId);
                if (client != null) {
                    client.ws.send(message);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket ws, Exception ex) {
        Log.e(TAG, "onError: " + ex);
    }

    @Override
    public void onStart() {
        Log.e(TAG, "SocketServer onStart");
    }
}
