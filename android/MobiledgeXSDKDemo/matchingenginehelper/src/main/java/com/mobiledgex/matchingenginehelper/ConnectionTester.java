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
    public URL mUrl;
    public String mAppInstUrl;
    public String mExpectedResponse;
    public String mOutput;

    public ConnectionTester(String appInstUrl, String expectedResponse) {
        mAppInstUrl = appInstUrl;
        mExpectedResponse = expectedResponse;
    }

    protected boolean testConnection() {
        HttpURLConnection urlConnection = null;
        Log.i(TAG, "testConnection mAppInstUrl="+mAppInstUrl+" mExpectedResponse="+ mExpectedResponse);
        try {
            mUrl = new URL(mAppInstUrl);
            urlConnection = (HttpURLConnection) mUrl.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = br.readLine()) != null) {
                mOutput += line;
            }
            return mOutput.indexOf(mExpectedResponse) >= 0;
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
