package de.marioschreiner.connichiwa_android;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
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

            if (msg.getString("_name").equalsIgnoreCase("localinfo")) {
                this.localSocket = socket;
            }

            if (msg.getString("_name").equalsIgnoreCase("localinfo") || msg.getString("_name").equalsIgnoreCase("remoteinfo")) {
                String identifier = msg.getString("identifier");
                this.socketIdentifiers.put(identifier, socket);

                this.push("master", message); //forward message to master to inform it about the new device
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
                this.pushToAll(message);
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
        if (target.equalsIgnoreCase("broadcast")) {
            this.pushToAll(payload);
            return;
        }
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
        for (CWWebSocket socket : this.socketIdentifiers.values()) {
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
