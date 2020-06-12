/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.sdkdemo;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mobiledgex.sdkdemo.MainActivity.DEFAULT_CARRIER_NAME;
import static com.mobiledgex.sdkdemo.MainActivity.DEFAULT_DME_HOSTNAME;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = "SettingsActivity";

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                Log.i(TAG, "Not a ListPreference " + preference.getTitle());
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * This is needed for the Back Arrow button to work on Android version 6.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<PreferenceActivity.Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || LocationSettingsFragment.class.getName().equals(fragmentName)
                || GeneralSettingsFragment.class.getName().equals(fragmentName)
                || FaceDetectionSettingsFragment.class.getName().equals(fragmentName)
                || SpeedTestSettingsFragment.class.getName().equals(fragmentName);
    }

    // Mex Enhanced Location Preference.
    public static class LocationSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.location_preferences);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    // Speed Test Preferences.
    public static class SpeedTestSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_speed_test);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    // General Preferences.
    public static class GeneralSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        String prefKeyDmeHostname;
        String prefKeyOperatorName;
        String prefKeyDefaultDmeHostname;
        String prefKeyDefaultOperatorName;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.i(TAG, "onCreate()");
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            prefKeyDefaultOperatorName = getResources().getString(R.string.pref_default_operator_name);
            prefKeyDefaultDmeHostname = getResources().getString(R.string.pref_default_dme_hostname);
            prefKeyOperatorName = getResources().getString(R.string.pref_operator_name);
            prefKeyDmeHostname = getResources().getString(R.string.pref_dme_hostname);

            // Initialize summary values for these keys.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            onSharedPreferenceChanged(prefs, prefKeyDefaultOperatorName);
            onSharedPreferenceChanged(prefs, prefKeyDefaultDmeHostname);
            onSharedPreferenceChanged(prefs, prefKeyOperatorName);
            // prefKeyDmeHostname does not need initialized here, because it is initialized with
            // the results of the dme-list.html call below.

            // Instantiate the RequestQueue.
            RequestQueue queue = Volley.newRequestQueue(getActivity());
            String url = "http://dme-inventory.mobiledgex.net/dme-list.html";

            // Request a string response from the provided URL.
            // If the dme-inventory request fails, or can't be parsed, the DME list will retain the
            // default values from the preferences XML.
            StringRequest stringRequest = new StringRequest(url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.i(TAG, "dme-inventory response=" + response);
                            try {
                                List<String> listNames = new ArrayList<String>();
                                List<String> listAddresses = new ArrayList<String>();
                                JSONArray jsonArray = new JSONArray(response);
                                Log.d(TAG, "jsonArray=" + jsonArray);
                                for(int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    String name = jsonObject.getString("name");
                                    String address = jsonObject.getString("address");
                                    listNames.add(name);
                                    listAddresses.add(address);
                                    Log.d(TAG, "name=" + name + " address=" + address);
                                }
                                CharSequence[] charSequenceNames = listNames.toArray(new CharSequence[listNames.size()]);
                                CharSequence[] charSequenceAddresses = listAddresses.toArray(new CharSequence[listAddresses.size()]);
                                String prefKeyDmeHostname = getResources().getString(R.string.pref_dme_hostname);
                                PreferenceScreen screen = getPreferenceScreen();
                                ListPreference dmeListPref = (ListPreference) screen.findPreference(prefKeyDmeHostname);
                                dmeListPref.setEntries(charSequenceNames);
                                dmeListPref.setEntryValues(charSequenceAddresses);
                                String summary = getResources().getString(R.string.pref_summary_dme_hostname);
                                dmeListPref.setSummary(summary + ": " + getRegionFromDme(dmeListPref));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "That didn't work! error=" + error);
                }
            });

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onResume() {
            Log.i(TAG, "onResume()");
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.i(TAG, "onSharedPreferenceChanged(" + key + ")");
            Preference pref = findPreference(key);
            if (key.equals(prefKeyDefaultDmeHostname)) {
                String summary = getResources().getString(R.string.pref_summary_default_dme_hostname);
                String prefKeyValueDefaultDmeHostname = getResources().getString(R.string.pref_value_default_dme_hostname);
                String dmeHostname = sharedPreferences.getString(prefKeyValueDefaultDmeHostname, DEFAULT_DME_HOSTNAME);
                summary = summary + ": " + dmeHostname;
                pref.setSummary(summary);
            }

            if (key.equals(prefKeyDefaultOperatorName)) {
                String summary = getResources().getString(R.string.pref_summary_default_operator_name);
                String prefKeyValueDefaultOperatorName = getResources().getString(R.string.pref_value_default_operator_name);
                String operatorName = sharedPreferences.getString(prefKeyValueDefaultOperatorName, DEFAULT_CARRIER_NAME);
                summary = summary + ": " + operatorName;
                pref.setSummary(summary);
            }

            if (key.equals(prefKeyDmeHostname)) {
                String summary = getResources().getString(R.string.pref_summary_dme_hostname);
                pref.setSummary(summary + ": " + getRegionFromDme((ListPreference) pref));
            }

            if (key.equals(prefKeyOperatorName)) {
                String summary = getResources().getString(R.string.pref_summary_operator_name);
                pref.setSummary(summary + ": " + ((EditTextPreference)pref).getText());
            }
        }
    }

    private static String getRegionFromDme(ListPreference dmeListPref) {
        String region;
        // See if we can get a simplified version of the selected region.
        int index = dmeListPref.findIndexOfValue(dmeListPref.getValue());
        if (index >= 0) {
            region = (String) dmeListPref.getEntries()[index];
        } else {
            region = dmeListPref.getValue();
        }
        return region;
    }

    // Face Detection Preferences.
    public static class FaceDetectionSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_face_detection);
            setHasOptionsMenu(true);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Reinitialize this screen of preferences, since values may have changed.
            // If an NPE occurs because the PreferenceManager has gone away,
            // there's no need for any action. Just don't crash the app.
            try {
                getPreferenceScreen().removeAll();
                addPreferencesFromResource(R.xml.pref_face_detection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // TODO: Implement this
    // About Preferences.
    public static class AboutFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_speed_test);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
