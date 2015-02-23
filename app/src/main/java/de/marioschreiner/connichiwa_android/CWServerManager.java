package de.marioschreiner.connichiwa_android;

import android.content.Context;

import java.io.IOException;

/**
 * Created by BlackWolf on 23/02/15.
 */
public class CWServerManager {

    private CWServerManagerDelegate delegate;
    private String documentRoot;
    private int port;
    private Context context;

    private boolean started;
    private CWHTTPServer httpServer;
    private CWWebSocketServer websocketServer;

    public void start(String documentRoot, int port, Context context) {
        this.documentRoot = documentRoot;
        this.port = port;
        this.context = context;

        this.httpServer = new CWHTTPServer(port, this.context);

        //Add the necessary handlers to the http server
        //Web application resides in the "www" folder of the "assets" folder
        httpServer.addPathHandler("/", this.documentRoot);
        httpServer.addPathHandler("/connichiwa", "/weblib");
        httpServer.addPathHandler("/connichiwa/scripts", "/scripts");
        httpServer.addFileHandler("/remote", "remote.html");
        httpServer.addTextHandler("/check", "Hic sunt draconis");

        this.websocketServer = new CWWebSocketServer(port + 1);

        try {
            this.setDelegate(this.delegate); //to make sure its set on the two servers as well

            httpServer.start();
            websocketServer.start(0);
            this.started = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isStarted() {
        return this.started;
    }

    public void setDelegate(CWServerManagerDelegate delegate) {
        this.delegate = delegate;
        if (this.httpServer != null) this.httpServer.delegate = delegate;
        if (this.websocketServer != null) this.websocketServer.delegate = delegate;
    }
}
