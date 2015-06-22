package de.marioschreiner.connichiwa_android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoWebSocketServer;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;

/**
 * Created by BlackWolf on 22/02/15.
 */
public class CWWebSocketServer extends NanoWebSocketServer {

    public CWServerManagerDelegate delegate;
    private Map<String, CWWebSocket> socketIdentifiers = new HashMap<String, CWWebSocket>();
    private CWWebSocket localSocket;

    public CWWebSocketServer(int port) {
        super(port);
    }

    @Override
    public WebSocket openWebSocket(IHTTPSession handshake) {
        return new CWWebSocket(handshake) {
            @Override
            protected void onMessage(WebSocketFrame messageFrame) {
                String identifier = getIdentifierForSocket(this);

                if (identifier == null) {
                    onUnidentifiedWebSocketMessage(this, messageFrame.getTextPayload());
                } else {
                    onIdentifiedWebSocketMessage(identifier, messageFrame.getTextPayload());
                }
            }

            @Override
            protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
                String identifier = getIdentifierForSocket(this);

                if (identifier != null) {
                    onIdentifiedWebSocketClose(identifier);
                }
            }
        };
    }

    private void onUnidentifiedWebSocketMessage(CWWebSocket socket, String message) {
        try {
            JSONObject msg = new JSONObject(message);

            if (msg.getString("_name").equalsIgnoreCase("_master_identification")) {
                this.localSocket = socket;
            }

            if (msg.getString("_name").equalsIgnoreCase("_master_identification") || msg.getString("_name").equalsIgnoreCase("_remote_identification")) {
                String identifier = msg.getString("identifier");
                this.socketIdentifiers.put(identifier, socket);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void onIdentifiedWebSocketMessage(String identifier, String message) {
        try {
            JSONObject msg = new JSONObject(message);
            String target = msg.getString("_target");

            if (target.equalsIgnoreCase("broadcast")) {
                Boolean backToSource = false;
                try {
                    msg.getBoolean("_broadcastToSource");
                } catch (JSONException e) {

                }
                if (backToSource) {
                    this.pushToAll(message);
                } else {
                    List exception = new ArrayList<String>();
                    exception.add(identifier);
                    this.pushToAll(message, exception);
                }
            } else {
                this.push(target, message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onIdentifiedWebSocketClose(String identifier) {
        this.socketIdentifiers.remove(identifier);

        if (this.delegate != null) this.delegate.remoteDidDisconnect(identifier);
    }

    public void push(String target, String payload) {
        CWWebSocket socket = this.getSocketForIdentifier(target);

        if (socket != null) {
            try {
                socket.send(payload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void pushToAll(String payload) {
        this.pushToAll(payload, null);
    }

    public void pushToAll(String payload, List<String> exceptions) {
        for (CWWebSocket socket : this.socketIdentifiers.values()) {
            String identifier = this.getIdentifierForSocket(socket);
            if (exceptions != null && exceptions.contains(identifier)) continue;

            try {
                socket.send(payload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private CWWebSocket getSocketForIdentifier(String identifier) {
        if (identifier.equalsIgnoreCase("master")) {
            return this.localSocket;
        }

        return this.socketIdentifiers.get(identifier);
    }

    private String getIdentifierForSocket(CWWebSocket socket) {
        for (Map.Entry<String, CWWebSocket> kvp : this.socketIdentifiers.entrySet()) {
            if (kvp.getValue() == socket) return kvp.getKey();
        }

        return null;
    }
}
