package de.marioschreiner.connichiwa_android;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Created by BlackWolf on 23/02/15.
 */
public class CWWebLibraryManager {

    private CWWebApplicationState appState;
    private Context context;
    private Activity activity;

    public WebView webView;

    public CWWebLibraryManager(CWWebApplicationState appState, Context context, Activity activity) {
        this.appState = appState;
        this.context = context;
        this.activity = activity;
    }

    public void connect() {
        if (this.appState.isWebserverRunning() == false) return;
        if (this.webView == null) return;

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");
        webView.setWebChromeClient(new WebChromeClient() {
            private boolean sendDidStartLoading = false;

            @Override
            public void onProgressChanged (WebView view, int newProgress) {
                if (this.sendDidStartLoading == false) {
                    webViewDidStartLoading();
                    this.sendDidStartLoading = true;
                }

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
            json.put("logLevel", CWWebApplicationState.LOG_LEVEL);
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
            //Get the IP
            WifiManager mWifiManager = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            String ip = intToIP(mWifiInfo.getIpAddress());

            //Get PPI
            DisplayMetrics dm = new DisplayMetrics();
            this.activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            double ppi = Math.sqrt(dm.xdpi * dm.ydpi);

            //TODO get this info from AppState
            //TODO get missing info like PPI, ...
            json.put("_name", "localinfo");
            json.put("identifier", UUID.randomUUID().toString());
            json.put("launchDate", new Date().getTime());
            json.put("ips", new JSONArray(new String[]{ip}));
            json.put("port", this.appState.webserverPort);
            json.put("ppi", ppi);
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
                webView.loadUrl("javascript:CWNativeBridge.parse('" + message + "');");
            }
        });
    }

    private void webViewDidStartLoading() {
        this._registerJSCallbacks();
        webView.loadUrl("javascript:var _CW_NATIVE = {};");
    }

    private void webViewDidFinishLoading() {
    }

    private void _registerJSCallbacks() {
        webView.loadUrl("javascript:window.nativeCallLocalInfo = function(infoString) { Android.nativeCallLocalInfo(infoString); }");
        webView.loadUrl("javascript:window.nativeCallWebsocketDidOpen = function() { Android.nativeCallWebsocketDidOpen(); }");
        webView.loadUrl("javascript:window.nativeCallRemoteDidConnect = function(identifier) { Android.nativeCallRemoteDidConnect(identifier); }");
    }

    @JavascriptInterface
    public void nativeCallLocalInfo(String infoString) {
        JSONObject info = null;
        try {
            info = new JSONObject(infoString);
            this.appState.identifier = info.getString("identifier");
            this.appState.launchDate = new Date(info.getLong("launchDate"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void nativeCallWebsocketDidOpen() {
        this._sendToView_debugInfo();
        this._sendToView_localInfo();
    }

    @JavascriptInterface
    public void nativeCallRemoteDidConnect(String identifier) {
        Log.d("VIEW", "Remote device did connect");
    }

    /* HELPER */

    //Thank you http://stackoverflow.com/questions/12103898/how-can-i-get-ip-addresses-of-other-wifi-enabled-devices-by-programmatically-on
    public String intToIP(int i) {
        return (( i & 0xFF)+ "."+((i >> 8 ) & 0xFF)+
                "."+((i >> 16 ) & 0xFF)+"."+((i >> 24 ) & 0xFF));
    }
}
