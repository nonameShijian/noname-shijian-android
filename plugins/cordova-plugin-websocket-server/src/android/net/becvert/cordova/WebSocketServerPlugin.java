package net.becvert.cordova;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.IExtension;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class WebSocketServerPlugin extends CordovaPlugin {

    public static final String TAG = "WebSocketServer";

    private WebSocketServerImpl wsserver = null;

    public static final String ACTION_GET_INTERFACES = "getInterfaces";
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_SEND = "send";
    public static final String ACTION_SEND_BINARY = "send_binary";
    public static final String ACTION_CLOSE = "close";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Log.v(TAG, "Initialized");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wsserver != null) {
            try {
                wsserver.stop();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                wsserver = null;
            }
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {

        if (wsserver != null && wsserver.failed) {
            wsserver = null;
        }

        if (ACTION_GET_INTERFACES.equals(action)) {

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject addresses = getInterfaces();

                        Log.d(TAG, "Addresses: " + addresses);

                        PluginResult result = new PluginResult(Status.OK, addresses);
                        callbackContext.sendPluginResult(result);

                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                        callbackContext.error("Error: " + e.getMessage());
                    } catch (SocketException e) {
                        Log.e(TAG, e.getMessage(), e);
                        callbackContext.error("Error: " + e.getMessage());
                    }
                }
            });

        } else if (ACTION_START.equals(action)) {

            if (wsserver != null) {
                callbackContext.error("Server already running.");
                return false;
            }

            final int port = args.optInt(0);

            List<String> _origins = null;
            List<String> _protocols = null;
            Boolean _tcpNoDelay = null;

            try {
                _origins = jsonArrayToArrayList(args.optJSONArray(1));
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error("Origins option error");
                return false;
            }
            try {
                _protocols = jsonArrayToArrayList(args.optJSONArray(2));
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error("Protocols option error");
                return false;
            }
            if (!args.isNull(3)) {
                _tcpNoDelay = Boolean.valueOf(args.optBoolean(3));
            }

            final List<String> origins = _origins;
            final List<String> protocols = _protocols;
            final Boolean tcpNoDelay = _tcpNoDelay;

            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    WebSocketServerImpl newServer = null;
                    try {
                        if (protocols != null) {
                            ArrayList<Draft_6455> drafts = new ArrayList<Draft_6455>();
                            for (String protocol : protocols) {
                                drafts.add(new Draft_6455(Collections.<IExtension> emptyList(), Collections.<IProtocol> singletonList(new Protocol(protocol))));
                            }
                            newServer = new WebSocketServerImpl(port, (List) drafts);
                        } else {
                            newServer = new WebSocketServerImpl(port);
                        }
                        newServer.setCallbackContext(callbackContext);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, e.getMessage(), e);
                        callbackContext.error("Port number error");
                        return;
                    }

                    if (origins != null) {
                        newServer.setOrigins(origins);
                    }

                    if (tcpNoDelay != null) {
                        newServer.setTcpNoDelay(tcpNoDelay);
                    }

                    try {
                        newServer.start();
                    } catch (IllegalStateException e) {
                        Log.e(TAG, e.getMessage(), e);
                        callbackContext.error("Can only be started once.");
                        return;
                    }

                    wsserver = newServer;
                }
            });

        } else if (ACTION_STOP.equals(action)) {

            if (wsserver == null) {
                callbackContext.error("Server is not running.");
                return false;
            }

            final WebSocketServerImpl oldServer = wsserver;
            wsserver = null;

            this.cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        oldServer.stop(2000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, e.getMessage(), e);
                        callbackContext.error("Error: " + e.getMessage());
                        return;
                    }

                    try {
                        JSONObject status = new JSONObject();
                        status.put("addr", oldServer.getHostAddress());
                        status.put("port", oldServer.getPort());

                        Log.d(TAG, "stop result: " + status.toString());
                        callbackContext.success(status);

                    } catch (JSONException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });

        } else if (ACTION_SEND.equals(action) || ACTION_SEND_BINARY.equals(action)) {

            final String uuid = args.optString(0);
            final String msg = args.optString(1);
            if (uuid != null && msg != null) {
                this.cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (wsserver != null) {
                            wsserver.send(uuid, msg, ACTION_SEND_BINARY.equals(action));
                        }
                    }
                });
            } else {
                callbackContext.error("UUID or msg not specified.");
                return false;
            }

        } else if (ACTION_CLOSE.equals(action)) {

            final String uuid = args.optString(0);
            final int code = args.optInt(1, -1);
            final String reason = args.optString(2);

            if (uuid != null) {
                this.cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (wsserver != null) {
                            wsserver.close(uuid, code, reason);
                        }
                    }
                });
            } else {
                callbackContext.error("UUID not specified.");
                return false;
            }

        } else {
            Log.w(TAG, "Invalid action: " + action);
            callbackContext.error("Invalid action: " + action);
            return false;
        }

        return true;
    }

    public static ArrayList<String> jsonArrayToArrayList(JSONArray jsonArray) throws JSONException {
        ArrayList<String> list = null;
        if (jsonArray != null && jsonArray.length() > 0) {
            list = new ArrayList<String>();
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        }
        return list;
    }

    // return IP4 & IP6 addresses
    public static JSONObject getInterfaces() throws JSONException, SocketException {
        JSONObject obj = new JSONObject();
        JSONObject intfobj;
        JSONArray ipv4Addresses;
        JSONArray ipv6Addresses;

        List<NetworkInterface> intfs = Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface intf : intfs) {
            if (!intf.isLoopback()) {
                intfobj = new JSONObject();
                ipv4Addresses = new JSONArray();
                ipv6Addresses = new JSONArray();

                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        if (addr instanceof Inet6Address) {
                            ipv6Addresses.put(addr.getHostAddress());
                        } else if (addr instanceof Inet4Address) {
                            ipv4Addresses.put(addr.getHostAddress());
                        }
                    }
                }

                if ((ipv4Addresses.length() > 0) || (ipv6Addresses.length() > 0)) {
                    intfobj.put("ipv4Addresses", ipv4Addresses);
                    intfobj.put("ipv6Addresses", ipv6Addresses);
                    obj.put(intf.getName(), intfobj);
                }
            }
        }

        return obj;
    }

}
