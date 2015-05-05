package de.marioschreiner.connichiwa_android;

import java.util.Date;
import java.util.Map;

/**
 * Created by BlackWolf on 23/02/15.
 */
public abstract class CWWebApplicationState {
    public static int LOG_LEVEL = 1;

    public String identifier;
    public Date launchDate;
    public String deviceName;
    public int ppi;
    public Map<String, String> deviceInfo;
    public int webserverPort;

    public abstract boolean isWebserverRunning();
    public abstract void runOnUiThread(Runnable thread);
}
