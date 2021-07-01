package com.mobiledgex.tritonclient;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.mobiledgex.matchingengine.util.RequestPermissions;
import com.mobiledgex.tritonclient.ui.home.HomeFragment;
import com.mobiledgex.tritonlib.InceptionProcessorActivity;
import com.mobiledgex.tritonlib.InceptionProcessorFragment;
import com.mobiledgex.tritonlib.Yolov4ProcessorActivity;
import com.mobiledgex.tritonlib.Yolov4ProcessorFragment;
import com.mobiledgex.tritonlib.SettingsActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private RequestPermissions mRpUtil;

    protected static String mHostname;
    protected static String mCarrierName;
    protected static String mRegionName;
    protected static String mAppName;
    protected static String mAppVersion;
    protected static String mOrgName;

    public static final int RC_STATS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * MatchingEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION.
         *
         * The example RequestPermissions utility creates a UI dialog, if needed.
         *
         * You can do this anywhere, MainApplication.onActivityResumed(), or a subset of permissions
         * onResume() on each Activity.
         *
         * Permissions must exist prior to API usage to avoid SecurityExceptions.
         */
        mRpUtil = new RequestPermissions();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_yolov4, R.id.nav_inception)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);

        displayView(R.id.nav_home);

        mAppName = getResources().getString(R.string.dme_app_name);
        mAppVersion = getResources().getString(R.string.app_version);
        mOrgName = getResources().getString(R.string.org_name);
        mHostname = "eu-mexdemo.dme.mobiledgex.net";
        mCarrierName = "TDG";

    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Log.i(TAG, "onNavigationItemSelected id="+id);

        if (id == R.id.nav_settings) {
            // Open "Settings" UI
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.nav_about) {
            // Handle the About action
            showAboutDialog();
            return true;
        } else if (id == R.id.nav_yolov4) {
            if (mRpUtil.getNeededPermissions(this).size() > 0) {
                // Opens a UI. When it returns, onResume() is called again.
                mRpUtil.requestMultiplePermissions(this);
                return true;
            }
            Intent intent = new Intent(this, Yolov4ProcessorActivity.class);
            startActivityForResult(intent, RC_STATS);

        } else if (id == R.id.nav_yolov4) {
            if (mRpUtil.getNeededPermissions(this).size() > 0) {
                // Opens a UI. When it returns, onResume() is called again.
                mRpUtil.requestMultiplePermissions(this);
                return true;
            }
            Intent intent = new Intent(this, InceptionProcessorActivity.class);
            startActivityForResult(intent, RC_STATS);

        }
        return false;
    }

    public void displayView(int viewId) {

        Fragment fragment = null;
        String title = getString(R.string.app_name);

        switch (viewId) {
            case R.id.nav_home:
                fragment = new HomeFragment();
                title  = "Home";
                break;
            case R.id.nav_yolov4:
                fragment = new Yolov4ProcessorFragment();
                title = "YOLOv4";
                break;
            case R.id.nav_inception:
                fragment = new InceptionProcessorFragment();
                title = "Inception";
                break;
        }

        if (mRpUtil.getNeededPermissions(this).size() > 0) {
            // Opens a UI. When it returns, onResume() is called again.
            mRpUtil.requestMultiplePermissions(this);
            return;
        }

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();
        }

        // set the toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

    }

    private void showAboutDialog() {
        String appName = "";
        String appVersion = "";
        try {
            // App
            ApplicationInfo appInfo = getApplicationInfo();
            if (getPackageManager() != null) {
                CharSequence seq = appInfo.loadLabel(getPackageManager());
                if (seq != null) {
                    appName = seq.toString();
                }
                PackageInfo pi  = getPackageManager().getPackageInfo(getPackageName(), 0);
                appVersion = pi.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = getAssets().open("about_dialog.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            String htmlData = sb.toString();
            htmlData = htmlData.replace("${androidAppVersion}", appVersion)
                    .replace("${appName}", mAppName)
                    .replace("${appVersion}", mAppVersion)
                    .replace("${orgName}", mOrgName)
                    .replace("${carrier}", mCarrierName)
                    .replace("${region}", mHostname)
                    .replace(".dme.mobiledgex.net", "");

            // The WebView to show our HTML.
            WebView webView = new WebView(MainActivity.this);
            webView.loadData(htmlData, "text/html; charset=UTF-8",null);
            new AlertDialog.Builder(MainActivity.this)
                    .setView(webView)
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setTitle(appName)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
