package de.marioschreiner.connichiwa_android;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
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
            this._sendJSONToView(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void _sendToView_debugInfo() {
        JSONObject json = new JSONObject();
        try {
            //TODO use actual debug info
            json.put("_name", "debuginfo");
            json.put("debug", true);
            json.put("logLevel", 3);
            _sendJSONToView(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void _sendToView_connectWebsocket() {
        JSONObject json = new JSONObject();
        try {
            json.put("_name", "connectwebsocket");
            _sendJSONToView(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void _sendToView_localInfo() {
        JSONObject json = new JSONObject();
        try {
            //TODO get this info from AppState
            //TODO get missing info like IP, PPI, ...
            json.put("_name", "localinfo");
            json.put("identifier", UUID.randomUUID().toString());
            json.put("launchDate", new Date().getTime());
            json.put("ips", "");
            json.put("port", 0);
            json.put("ppi", 150);
            json.put("supportsMC", false);
            _sendJSONToView(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

        this._sendToView_debugInfo();
        this._sendToView_connectWebsocket();
    }

    @JavascriptInterface
    public void nativeCallWebsocketDidOpen() {
        Log.d("View", "WEBSOCKET DID OPEN");
        this._sendToView_localInfo();
    }

    @JavascriptInterface
    public void nativeCallRemoteDidConnect(String identifier) {
        Log.d("VIEW", "REMOTE DID CONNECT");
    }
}
