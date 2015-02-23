package de.marioschreiner.connichiwa_android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD;


public class FullscreenActivity extends Activity {

    private static int PORT = 8000;

    private WebView localWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        //Make sure webview is fullscreen and in immersive mode
        View v = findViewById(R.id.webView);
        v.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);

        CWHTTPServer httpServer = new CWHTTPServer(PORT, this.getApplicationContext());

        //Add the necessary handlers to the http server
        //Web application resides in the "www" folder of the "assets" folder
        httpServer.addPathHandler("/", "/www");
        httpServer.addPathHandler("/connichiwa", "/weblib");
        httpServer.addPathHandler("/connichiwa/scripts", "/scripts");
        httpServer.addFileHandler("/remote", "remote.html");
        httpServer.addTextHandler("/check", "Hic sunt draconis");

        CWWebSocketServer wsServer = new CWWebSocketServer(PORT+1);

        try {
            httpServer.start();
            wsServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        localWebView = (WebView)(findViewById(R.id.webView));
        localWebView.getSettings().setJavaScriptEnabled(true);
        localWebView.addJavascriptInterface(this, "Android");
        localWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged (WebView view, int newProgress) {
                if (newProgress == 100) {

                    localWebView.loadUrl("javascript:window.nativeCallWebsocketDidOpen = function() { Android.nativeCallWebsocketDidOpen(); }");
                    localWebView.loadUrl("javascript:window.nativeCallRemoteDidConnect = function(identifier) { Android.nativeCallRemoteDidConnect(identifier); }");

                    //Need to: Setup JS Callbacks, send debuginfo, send connectwebsocket
                    //Then the usual stuff begins: when we receive the connectwebsocket callback, we send the localinfo, a.s.o.
                    JSONObject json = new JSONObject();
                    try {
                        json.put("_name", "connectwebsocket");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    localWebView.loadUrl("javascript:CWNativeMasterCommunication.parse('"+json.toString()+"');");
                }
            }
        });
        localWebView.setWebViewClient(new WebViewClient());

        localWebView.loadUrl("http://127.0.0.1:8000");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @JavascriptInterface
    public void nativeCallWebsocketDidOpen() {
        Log.d("View", "WEBSOCKET DID OPEN");

        final JSONObject json = new JSONObject();
        try {
            json.put("_name", "localinfo");
            json.put("identifier", UUID.randomUUID().toString());
            json.put("supportsMC", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        runOnUiThread(new Runnable() {
            public void run() {
                localWebView.loadUrl("javascript:CWNativeMasterCommunication.parse('" + json.toString() + "');");
            }
        });
    }

    @JavascriptInterface
    public void nativeCallRemoteDidConnect(String identifier) {
        Log.d("VIEW", "REMOTE DID CONNECT");
    }
}
