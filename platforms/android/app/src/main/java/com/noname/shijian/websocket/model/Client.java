package com.noname.shijian.websocket.model;

import org.java_websocket.WebSocket;

public class Client {
    public WebSocket ws;
    public String id;
    public String pageUrl;
    public String ua;
    public String time;
    public String title;
    public String favicon;

    public Client() {}

    public Client(WebSocket ws, String id, String pageUrl, String ua, String time, String title, String favicon) {
        this.ws = ws;
        this.id = id;
        this.pageUrl = pageUrl;
        this.ua = ua;
        this.time = time;
        this.title = title;
        this.favicon = favicon;
    }
}
