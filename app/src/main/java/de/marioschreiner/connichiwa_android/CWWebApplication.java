package de.marioschreiner.connichiwa_android;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import java.util.Date;
import java.util.UUID;

/**
 * Created by BlackWolf on 23/02/15.
 */
public class CWWebApplication extends CWWebApplicationState implements CWServerManagerDelegate {

    private static int DEFAULT_WEBSERVER_PORT = 8000;

    private Activity activity;
    private Context context;
    private WebView localWebView;

    private CWServerManager serverManager;
    private CWWebLibraryManager webLibManager;

    public CWWebApplication(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;

        this.identifier = UUID.randomUUID().toString();
        this.launchDate = new Date();

        this.serverManager =  new CWServerManager();
        this.serverManager.setDelegate(this);
        this.webLibManager = new CWWebLibraryManager(this, this.context);
    }

    public void launch(String documentRoot, WebView webView, int port) {
        this.webserverPort = port;

        this.localWebView = webView;

        this.serverManager.start(documentRoot, port, this.context);
        this.webserverDidStart(); //usually, this would be a callback from the webserver, but NanoHTTPD doesn't support it
    }

    public void launch(String documentRoot, WebView webView) {
        this.launch(documentRoot, webView, DEFAULT_WEBSERVER_PORT);
    }

    public void webserverDidStart() {
        this.webLibManager.webView = this.localWebView;

        this.webLibManager.connect();
    }

    @Override
    public boolean isWebserverRunning() {
        return this.serverManager.isStarted();
    }

    @Override
    public void runOnUiThread(Runnable thread) {
        this.activity.runOnUiThread(thread);
    }

    @Override
    public void remoteDidDisconnect(String identifier) {
        this.webLibManager.sendRemoteDisconnected(identifier);
    }
}
