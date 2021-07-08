/**
 * Copyright 2018-2021 MobiledgeX, Inc. All rights and licenses reserved.
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

package com.mobiledgex.matchingenginehelper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mobiledgex.matchingengine.MatchingEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.DEFAULT_CARRIER_NAME;
import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.DEFAULT_DME_HOSTNAME;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String EXTRA_SHOW_FRAGMENT = "show_fragment";
    private static final String TAG = "ME/SettingsActivity";
    private static final String TITLE_TAG = "settingsActivityTitle";
    private boolean finishOnNavigateUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        Fragment fragment;
        Intent intent = getIntent();
        String showFragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);

        Log.i(TAG, "showFragment="+showFragment);
        Log.i(TAG, "savedInstanceState="+savedInstanceState);
        if (showFragment != null && showFragment.endsWith("MatchingEngineSettingsFragment")) {
            fragment = new MatchingEngineSettingsFragment();
            setTitle(getResources().getString(R.string.preference_matching_engine_settings));
        } else {
            fragment = new HeaderFragment();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, fragment)
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_headers, rootKey);
        }
    }

    public static class MatchingEngineSettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_matching_engine, rootKey);
        }
    }

    public static class GeneralSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        String prefKeyMeLocationVerification;
        String prefKeyDmeHostname;
        String prefKeyOperatorName;
        String prefKeyDefaultDmeHostname;
        String prefKeyDefaultOperatorName;
        String prefKeyDefaultAppInfo;
        String prefKeyAppName;
        String prefKeyAppVersion;
        String prefKeyOrgName;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            prefKeyMeLocationVerification = getResources().getString(R.string.pref_matching_engine_location_verification);
            prefKeyDefaultOperatorName = getResources().getString(R.string.pref_default_operator_name);
            prefKeyDefaultDmeHostname = getResources().getString(R.string.pref_default_dme_hostname);
            prefKeyOperatorName = getResources().getString(R.string.pref_operator_name);
            prefKeyDmeHostname = getResources().getString(R.string.pref_dme_hostname);
            prefKeyDefaultAppInfo = getResources().getString(R.string.pref_default_app_definition);
            prefKeyAppName = getResources().getString(R.string.pref_app_name);
            prefKeyAppVersion = getResources().getString(R.string.pref_app_version);
            prefKeyOrgName = getResources().getString(R.string.pref_org_name);

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            // Initialize summary values for these keys.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            onSharedPreferenceChanged(prefs, prefKeyMeLocationVerification);
            onSharedPreferenceChanged(prefs, prefKeyDefaultOperatorName);
            onSharedPreferenceChanged(prefs, prefKeyDefaultDmeHostname);
            onSharedPreferenceChanged(prefs, prefKeyOperatorName);
            onSharedPreferenceChanged(prefs, prefKeyDefaultAppInfo);
            onSharedPreferenceChanged(prefs, prefKeyAppName);
            onSharedPreferenceChanged(prefs, prefKeyAppVersion);
            onSharedPreferenceChanged(prefs, prefKeyOrgName);
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
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.i(TAG, "onSharedPreferenceChanged(" + key + ")");
            Preference pref = findPreference(key);

            if (key.equals(prefKeyMeLocationVerification)) {
                boolean allowed = sharedPreferences.getBoolean(prefKeyMeLocationVerification, false);
                MatchingEngine.setMatchingEngineLocationAllowed(allowed);
            }

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
                if (operatorName.isEmpty()) {
                    operatorName = "<blank>";
                }
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

            if (key.equals(prefKeyDefaultAppInfo)) {
                String summary = getResources().getString(R.string.pref_summary_default_app_definition);
                String appName = getResources().getString(R.string.dme_app_name);
                String appVersion = getResources().getString(R.string.app_version);
                String orgName = getResources().getString(R.string.org_name);
                summary = summary + "\n    Name=" + appName + "\n    Version=" + appVersion + "\n    Org=" + orgName;
                pref.setSummary(summary);
            }

            if (key.equals(prefKeyAppName) || key.equals(prefKeyAppVersion) || key.equals(prefKeyOrgName)) {
                pref.setSummary(((EditTextPreference)pref).getText());
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

}