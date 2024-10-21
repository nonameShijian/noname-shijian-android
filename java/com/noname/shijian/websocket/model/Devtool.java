package com.noname.shijian.websocket.model;

import org.java_websocket.WebSocket;

public class Devtool {
    public WebSocket ws;
    public Client client;
    public String id;
    public String clientId;

    public Devtool() {}

    public Devtool(WebSocket ws, Client client, String id, String clientId) {
        this.ws = ws;
        this.client = client;
        this.id = id;
        this.clientId = clientId;
    }
}
