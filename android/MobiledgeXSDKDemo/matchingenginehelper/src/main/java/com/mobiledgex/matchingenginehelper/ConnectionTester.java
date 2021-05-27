package com.mobiledgex.matchingenginehelper;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ConnectionTester {
    private static final String TAG = "ConnectionTester";
    private final int mPort;
    public boolean mSuccess = false;
    public URL mUrl;
    public String mHostName;
    public String mAppName;
    public String mOutput;

    public ConnectionTester(String hostname, String appName, int port) {
        mHostName = hostname;
        mAppName = appName;
        mPort = port;
    }

    protected boolean testConnection() {
        HttpURLConnection urlConnection = null;
        String appInstUrl = "http://"+mHostName+":"+mPort+"/test/";
        String expectedResponse = "Valid GET Request to server";
        if (mAppName.equals("sdktest") || mAppName.equals("automation-sdk-porttest")) {
            appInstUrl = "http://"+mHostName+":"+mPort+"/automation.html";
            expectedResponse = "test server is running";
        }
        Log.i(TAG, "testConnection appName="+mAppName+" url="+appInstUrl);
        try {
            mUrl = new URL(appInstUrl);
            urlConnection = (HttpURLConnection) mUrl.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = br.readLine()) != null) {
                mOutput += line;
            }
            return mOutput.indexOf(expectedResponse) >= 0;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            urlConnection.disconnect();
        }
        return false;
    }


}
