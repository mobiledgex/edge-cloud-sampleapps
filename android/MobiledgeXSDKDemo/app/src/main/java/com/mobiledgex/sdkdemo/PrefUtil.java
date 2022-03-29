package com.mobiledgex.sdkdemo;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class PrefUtil {
    private static final String TAG = "PrefUtil";

    private static String buildSeparator(String text) {
        StringBuffer out = new StringBuffer("");
        for (int i = 0; i < text.length(); i++) {
            out.append("-");
        }
        return out.toString();
    }

    /**
     * This method parses an XML resource file, and from each entry, pulls the resource IDs
     * of the Preference title and key, then uses the key to query SharedPreferences for the
     * actual value. It builds and returns a String list of all titles and values.
     * @param title  Header for section
     * @param rsrcs  App Resouces
     * @param prefs  SharedPreferences to pull values from
     * @param xmlRsrc  Resource ID of the XML file.
     * @return  String list of all titles and values.
     */
    protected static String getPrefsFromResource(String title, Resources rsrcs, SharedPreferences prefs, int xmlRsrc) {
        final String NS = "http://schemas.android.com/apk/res/android";
        StringBuffer out = new StringBuffer("");
        out.append(title).append("\n").append(buildSeparator(title)).append("\n");
        XmlResourceParser xrp = rsrcs.getXml(xmlRsrc);
        try {
            int eventType = xrp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    Log.i(TAG, "xrp.getText()=" + xrp.getText() + " attr count=" + xrp.getAttributeCount());
                    int titleRes = xrp.getAttributeResourceValue(NS, "title", 0);
                    int keyRes = xrp.getAttributeResourceValue(NS, "key", 0);
                    Log.i(TAG, "titleRes=" + titleRes + " keyRes=" + keyRes + " name=" + xrp.getName());
                    if (titleRes > 0 && keyRes > 0) {
                        Log.i(TAG, "rsrcs.getString(" + titleRes + ")=" + rsrcs.getString(titleRes));
                        Log.i(TAG, "rsrcs.getString(" + keyRes + ")=" + rsrcs.getString(keyRes));
                        out.append(rsrcs.getString(titleRes) + ": ");
                        if (xrp.getName().equals("CheckBoxPreference") || xrp.getName().equals("SwitchPreference")) {
                            out.append(prefs.getBoolean(rsrcs.getString(keyRes), false)).append("\n");
                        } else if (xrp.getName().equals("EditTextPreference") || xrp.getName().equals("ListPreference")) {
                            String value = prefs.getString(rsrcs.getString(keyRes), "");
                            // Special case for Operator when wifiOnly is turned on.
                            if (keyRes == R.string.pref_operator_name) {
                                boolean wifiOnly = prefs.getBoolean(rsrcs.getString(R.string.pref_use_wifi_only), false);
                                if (wifiOnly) {
                                    value = "blank (wildcard)";
                                }
                            }
                            out.append(value).append("\n");
                        } else if (xrp.getName().equals("SeekBarPreference")) {
                            out.append(prefs.getInt(rsrcs.getString(keyRes), 0)).append("\n");
                        } else {
                            out.append("\n");
                        }
                    }
                }
                eventType = xrp.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            out.append("XmlPullParserException attempting to parse resource file");
        } catch (IOException e) {
            e.printStackTrace();
            out.append("IOException attempting to parse resource file");
        }
        out.append("\n");
        return out.toString();
    }
}
