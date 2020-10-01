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

package com.mobiledgex.computervision;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;

import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.List;

import static com.mobiledgex.computervision.ImageProcessorFragment.DEF_HOSTNAME_PLACEHOLDER;

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

    private static final String TAG = "CV/SettingsActivity";
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
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        Log.i(TAG, "isValidFragment("+fragmentName+")");
        return PreferenceFragment.class.getName().equals(fragmentName)
                || FaceDetectionSettingsFragment.class.getName().equals(fragmentName);
    }

    // Face Detection Preferences.
    public static class FaceDetectionSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private String prefKeyResetCvHosts;
        private String prefKeyHostCloudOverride;
        private String prefKeyHostCloud;
        private String prefKeyHostEdgeOverride;
        private String prefKeyHostEdge;
        private String prefKeyHostGpuOverride;
        private String prefKeyHostGpu;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_face_detection);
            setHasOptionsMenu(true);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            prefs.registerOnSharedPreferenceChangeListener(this);

            prefKeyResetCvHosts = getResources().getString(R.string.preference_fd_reset_all_hosts);
            prefKeyHostCloudOverride = getResources().getString(R.string.pref_override_cloud_cloudlet_hostname);
            prefKeyHostCloud = getResources().getString(R.string.preference_fd_host_cloud);
            prefKeyHostEdgeOverride = getResources().getString(R.string.pref_override_edge_cloudlet_hostname);
            prefKeyHostEdge = getResources().getString(R.string.preference_fd_host_edge);
            prefKeyHostGpuOverride = getResources().getString(R.string.pref_override_gpu_cloudlet_hostname);
            prefKeyHostGpu = getResources().getString(R.string.preference_gpu_host_edge);

            bindPreferenceSummaryToValue(findPreference(prefKeyHostCloud));
            bindPreferenceSummaryToValue(findPreference(prefKeyHostEdge));
            bindPreferenceSummaryToValue(findPreference(prefKeyHostGpu));
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
            Log.i(TAG, "onSharedPreferenceChanged("+key+")");

            if(key.equals(prefKeyResetCvHosts)) {
                String value = sharedPreferences.getString(prefKeyResetCvHosts, "No");
                Log.i(TAG, prefKeyResetCvHosts+" "+value);
                if(value.startsWith("Yes")) {
                    Log.i(TAG, "Resetting Computer Vision server hosts.");
                    sharedPreferences.edit().putString(prefKeyHostCloud, DEF_HOSTNAME_PLACEHOLDER)
                        .putString(prefKeyHostEdge, DEF_HOSTNAME_PLACEHOLDER)
                        .putString(prefKeyHostGpu, DEF_HOSTNAME_PLACEHOLDER)
                        .putBoolean(prefKeyHostCloudOverride, false)
                        .putBoolean(prefKeyHostEdgeOverride, false)
                        .putBoolean(prefKeyHostGpuOverride, false).apply();
                    Toast.makeText(getContext(), "Computer Vision hosts reset to default.", Toast.LENGTH_SHORT).show();
                }
                //Always set the value back to something so that either clicking Yes or No in the dialog
                //will activate this "changed" call.
                sharedPreferences.edit().putString(prefKeyResetCvHosts, "XXX_garbage_value").apply();

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
    }

}
