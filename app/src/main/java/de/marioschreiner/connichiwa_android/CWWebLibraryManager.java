package de.marioschreiner.connichiwa_android;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by BlackWolf on 23/02/15.
 */
public class CWWebLibraryManager {

    private CWWebApplicationState appState;

    public WebView webView;

    public CWWebLibraryManager(CWWebApplicationState appState) {
        this.appState = appState;
    }

    public void connect() {
        if (this.appState.isWebserverRunning() == false) return;
        if (this.webView == null) return;

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged (WebView view, int newProgress) {
                if (newProgress == 100) {
                    webViewDidFinishLoading();
                }
            }
        });
        webView.setWebViewClient(new WebViewClient());

        String url = "http://127.0.0.1:"+this.appState.webserverPort;
        webView.loadUrl(url);
    }

    public void sendRemoteDisconnected(String identifier) {
        JSONObject json = new JSONObject();
        try {
            json.put("_name", "remotedisconnected");
            json.put("identifier", identifier);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this._sendJSONToView(json);
    }

    private void _sendJSONToView(JSONObject json) {
        this._sendToView(json.toString());
    }

    private void _sendToView(final String message) {
        this.appState.runOnUiThread(new Runnable() {
            public void run() {
                webView.loadUrl("javascript:CWNativeMasterCommunication.parse('" + message + "');");
            }
        });
    }

    private void webViewDidFinishLoading() {
        webView.loadUrl("javascript:window.nativeCallWebsocketDidOpen = function() { Android.nativeCallWebsocketDidOpen(); }");
        webView.loadUrl("javascript:window.nativeCallRemoteDidConnect = function(identifier) { Android.nativeCallRemoteDidConnect(identifier); }");

        //Need to: Setup JS Callbacks, send debuginfo, send connectwebsocket
        //Then the usual stuff begins: when we receive the connectwebsocket callback, we send the localinfo, a.s.o.
        JSONObject json = new JSONObject();
        try {
            json.put("_name", "connectwebsocket");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        _sendJSONToView(json);
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

        this._sendJSONToView(json);
    }

    @JavascriptInterface
    public void nativeCallRemoteDidConnect(String identifier) {
        Log.d("VIEW", "REMOTE DID CONNECT");
    }
}
