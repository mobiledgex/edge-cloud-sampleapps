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
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;

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

import static com.mobiledgex.matchingenginehelper.MatchingEngineHelper.DEFAULT_APP_INSTANCES_LIMIT;
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
            setTitle(getResources().getString(R.string.pref_matching_engine_settings));
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
        private String prefKeyAppInstancesLimit;
        private EditTextPreference prefAppInstancesLimit;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_matching_engine, rootKey);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            prefKeyAppInstancesLimit = getResources().getString(R.string.pref_app_instances_limit);
            prefAppInstancesLimit = findPreference(prefKeyAppInstancesLimit);
            prefAppInstancesLimit.setSummaryProvider(preference -> getResources().getString(R.string.pref_app_instances_limit_summary, prefAppInstancesLimit.getText()));
            prefAppInstancesLimit.setOnBindEditTextListener(editText -> {
                SetEditTextNumerical(editText);
            });
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

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);

            // Disable/Enable "Operator" items based on pref from another page. Can't use
            // regular `android:dependency` to control this because that's already used
            // for the "Use Default Operator" checkbox dependency.
            boolean wifiOnly = prefs.getBoolean(getResources().getString(R.string.pref_use_wifi_only), false);
            Log.i(TAG, "wifiOnly="+wifiOnly);
            findPreference(prefKeyDefaultOperatorName).setEnabled(!wifiOnly);
            findPreference(prefKeyOperatorName).setEnabled(!wifiOnly);

            // Initialize summary values for these keys.
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
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Log.i(TAG, "onSharedPreferenceChanged(" + key + ")");
            Preference pref = findPreference(key);

            if (key.equals(prefKeyMeLocationVerification)) {
                boolean allowed = prefs.getBoolean(prefKeyMeLocationVerification, false);
                MatchingEngine.setMatchingEngineLocationAllowed(allowed);
            }

            if (key.equals(prefKeyDefaultDmeHostname)) {
                String summary = getResources().getString(R.string.pref_summary_default_dme_hostname);
                String prefKeyValueDefaultDmeHostname = getResources().getString(R.string.pref_value_default_dme_hostname);
                String dmeHostname = prefs.getString(prefKeyValueDefaultDmeHostname, DEFAULT_DME_HOSTNAME);
                summary = summary + ": " + dmeHostname;
                pref.setSummary(summary);
            }

            if (key.equals(prefKeyDefaultOperatorName)) {
                String summary = getResources().getString(R.string.pref_summary_default_operator_name);
                String prefKeyValueDefaultOperatorName = getResources().getString(R.string.pref_value_default_operator_name);
                String operatorName = prefs.getString(prefKeyValueDefaultOperatorName, DEFAULT_CARRIER_NAME);
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
                summary += ": " + ((EditTextPreference)pref).getText();
                boolean wifiOnly = prefs.getBoolean(getResources().getString(R.string.pref_use_wifi_only), false);
                if (wifiOnly) {
                    summary += ("\n(Disabled due to \"Use Wifi Only\" being turned on.)");
                }
                pref.setSummary(summary);
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

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
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

    public static class EdgeEventsConfigFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private String prefKeyLatencyTestPort;
        private String prefKeyLatencyThreshold;
        private String prefKeyAutoMigration;
        private String prefKeyPerfMarginSwitch;

        private EditTextPreference prefLatencyTestPort;
        private EditTextPreference prefLatencyThreshold;
        private CheckBoxPreference prefAutoMigration;
        private SeekBarPreference prefPerfMarginSwitch;

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            MatchingEngineHelper.mEdgeEventsConfigUpdated = true;
            // Updating the config with changed values is handled in
            // MatchingEngineHelper.onSharedPreferenceChanged().
        }

        @Override
        public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            prefKeyLatencyTestPort = getResources().getString(R.string.pref_latency_test_port);
            prefKeyLatencyThreshold = getResources().getString(R.string.pref_latency_threshold_ms);
            prefKeyAutoMigration = getResources().getString(R.string.pref_automigration);
            prefKeyPerfMarginSwitch = getResources().getString(R.string.pref_perf_margin_switch);

            prefLatencyTestPort = findPreference(prefKeyLatencyTestPort);
            prefLatencyTestPort.setSummaryProvider(preference -> getResources().getString(R.string.pref_latency_test_port_summary, prefLatencyTestPort.getText()));
            prefLatencyTestPort.setOnBindEditTextListener(editText -> {
                SetEditTextNumerical(editText);
            });

            prefLatencyThreshold = findPreference(prefKeyLatencyThreshold);
            prefLatencyThreshold.setSummaryProvider(preference -> getResources().getString(R.string.pref_latency_threshold_ms_summary, prefLatencyThreshold.getText()));
            prefLatencyThreshold.setOnBindEditTextListener(editText -> {
                SetEditTextNumerical(editText);
            });

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            MatchingEngineHelper.mEdgeEventsConfigUpdated = false;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_edge_events, rootKey);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    public static class LocationUpdateConfigFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        String prefKeyUpdatePattern;
        String prefKeyUpdateInterval;
        String prefKeyMaxNumUpdates;

        ListPreference prefUpdatePattern;
        EditTextPreference prefUpdateInterval;
        EditTextPreference prefMaxNumUpdates;

        protected void defineKeys() {
            prefKeyUpdatePattern = getResources().getString(R.string.pref_update_pattern_location);;
            prefKeyUpdateInterval = getResources().getString(R.string.pref_update_interval_location);
            prefKeyMaxNumUpdates = getResources().getString(R.string.pref_max_num_updates_location);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_location_update_config, rootKey);
        }

        @Override
        public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            Log.i(TAG, "onCreate");
            super.onCreate(savedInstanceState);
            defineKeys();

            prefUpdatePattern = findPreference(prefKeyUpdatePattern);
            prefUpdatePattern.setSummaryProvider(preference -> getResources().getString(R.string.pref_update_pattern_summary, prefUpdatePattern.getEntry().toString()));

            prefUpdateInterval = findPreference(prefKeyUpdateInterval);
            prefUpdateInterval.setSummaryProvider(preference -> getResources().getString(R.string.pref_update_interval_summary, prefUpdateInterval.getText()));
            prefUpdateInterval.setOnBindEditTextListener(editText -> {
                SetEditTextNumerical(editText);
            });

            prefMaxNumUpdates = findPreference(prefKeyMaxNumUpdates);
            prefMaxNumUpdates.setSummaryProvider(preference -> getResources().getString(R.string.pref_max_num_updates_summary, prefMaxNumUpdates.getText()));
            prefMaxNumUpdates.setOnBindEditTextListener(editText -> {
                SetEditTextNumerical(editText);
            });

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);

            // Initialize preferences.
            onSharedPreferenceChanged(prefs, prefKeyUpdatePattern);
            onSharedPreferenceChanged(prefs, prefKeyUpdateInterval);
            onSharedPreferenceChanged(prefs, prefKeyMaxNumUpdates);

            MatchingEngineHelper.mEdgeEventsConfigUpdated = false;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Log.i(TAG, "onSharedPreferenceChanged(" + key + ")");
            Preference pref = findPreference(key);

            if (key.equals(prefKeyUpdatePattern)) {
                String value = prefs.getString(key, "onInterval");
                boolean visible = value.equals("onInterval");
                prefUpdateInterval.setVisible(visible);
                prefMaxNumUpdates.setVisible(visible);
            }

            MatchingEngineHelper.mEdgeEventsConfigUpdated = true;
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    public static class LatencyUpdateConfigFragment extends LocationUpdateConfigFragment {
        protected void defineKeys() {
            prefKeyUpdatePattern = getResources().getString(R.string.pref_update_pattern_latency);;
            prefKeyUpdateInterval = getResources().getString(R.string.pref_update_interval_latency);
            prefKeyMaxNumUpdates = getResources().getString(R.string.pref_max_num_updates_latency);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_latency_update_config, rootKey);
        }
    }

    protected static void SetEditTextNumerical(EditText editText) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setSelectAllOnFocus(true);
    }
}
