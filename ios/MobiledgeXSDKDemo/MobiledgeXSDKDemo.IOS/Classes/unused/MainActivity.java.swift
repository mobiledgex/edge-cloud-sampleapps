// package com.mobiledgex.sdkdemo;
//
// import android.content.DialogInterface;
// import android.content.Intent;
// import android.content.SharedPreferences;
// import android.content.pm.ApplicationInfo;
// import android.content.pm.PackageInfo;
// import android.content.pm.PackageManager;
// import android.graphics.Bitmap;
// import android.graphics.Canvas;
// import android.graphics.Paint;
// import android.graphics.PorterDuff;
// import android.graphics.drawable.Drawable;
// import android.location.Location;
// import android.os.Bundle;
// import android.os.Looper;
// import android.preference.PreferenceManager;
// import android.support.annotation.NonNull;
// import android.support.design.widget.FloatingActionButton;
// import android.support.design.widget.NavigationView;
// import android.support.design.widget.Snackbar;
// import android.support.v4.view.GravityCompat;
// import android.support.v4.widget.DrawerLayout;
// import android.support.v7.app.ActionBarDrawerToggle;
// import android.support.v7.app.AlertDialog;
// import android.support.v7.app.AppCompatActivity;
// import android.support.v7.widget.Toolbar;
// import android.util.ArrayMap;
// import android.util.Log;
// import android.view.Menu;
// import android.view.MenuItem;
// import android.view.View;
// import android.widget.Toast;
//
// import com.android.volley.Request;
// import com.android.volley.RequestQueue;
// import com.android.volley.Response;
// import com.android.volley.VolleyError;
// import com.android.volley.toolbox.StringRequest;
// import com.android.volley.toolbox.Volley;
// import com.google.android.gms.auth.api.signin.GoogleSignIn;
// import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
// import com.google.android.gms.auth.api.signin.GoogleSignInClient;
// import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
// import com.google.android.gms.common.api.ApiException;
// import com.google.android.gms.location.FusedLocationProviderClient;
// import com.google.android.gms.location.LocationCallback;
// import com.google.android.gms.location.LocationRequest;
// import com.google.android.gms.location.LocationResult;
// import com.google.android.gms.location.LocationServices;
// import com.google.android.gms.maps.CameraUpdate;
// import com.google.android.gms.maps.CameraUpdateFactory;
// import com.google.android.gms.maps.GoogleMap;
// import com.google.android.gms.maps.OnMapReadyCallback;
// import com.google.android.gms.maps.SupportMapFragment;
// import com.google.android.gms.maps.model.BitmapDescriptor;
// import com.google.android.gms.maps.model.BitmapDescriptorFactory;
// import com.google.android.gms.maps.model.LatLng;
// import com.google.android.gms.maps.model.LatLngBounds;
// import com.google.android.gms.maps.model.Marker;
// import com.google.android.gms.maps.model.MarkerOptions;
// import com.google.android.gms.maps.model.Polyline;
// import com.google.android.gms.maps.model.PolylineOptions;
// import com.google.android.gms.tasks.OnCompleteListener;
// import com.google.android.gms.tasks.Task;
// import com.google.maps.android.SphericalUtil;
// import com.mobiledgex.matchingengine.FindCloudletResponse;
// import com.mobiledgex.matchingengine.MatchingEngine;
// import com.mobiledgex.matchingengine.util.RequestPermissions;
// import com.mobiledgex.sdkdemo.camera.Camera2BasicFragment;
// import com.mobiledgex.sdkdemo.camera.CameraActivity;
//
// import org.json.JSONException;
// import org.json.JSONObject;
//
// import java.io.UnsupportedEncodingException;
// import java.util.List;
// import java.util.UUID;
//
// import distributed_match_engine.AppClient;
//
// import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_FIND_CLOUDLET;
// import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_GET_CLOUDLETS;
// import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_REGISTER_CLIENT;
// import static com.mobiledgex.sdkdemo.MatchingEngineHelper.RequestType.REQ_VERIFY_LOCATION;
// import static distributed_match_engine.AppClient.Match_Engine_Loc_Verify.GPS_Location_Status.LOC_VERIFIED;
// import static distributed_match_engine.AppClient.Match_Engine_Loc_Verify.GPS_Location_Status.LOC_ROAMING_COUNTRY_MATCH;

// JT 18.10.23
import UIKit

// import GoogleSignInClient // JT 18.11.05
// import GoogleSignInAccount  // JT 18.11.05
import CoreLocation
import GoogleMaps // JT 18.10.23
import GoogleSignIn // JT 18.10.24

public class MainActivity: UIViewController, GMSMapViewDelegate, MatchingEngineResultsListener // JT 18.11.01
// JT 18.10.23 todo
// extends AppCompatActivity
//        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
//            GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMapLongClickListener,
//            SharedPreferences.OnSharedPreferenceChangeListener, GoogleMap.OnMarkerDragListener, MatchingEngineResultsListener
{
    func onGetCloudletList(_: DistributedMatchEngine_AppInstListRequest)
    {
        // JT 18.11.06
    }

    private static let TAG: String = "MainActivity"
    public static let COLOR_NEUTRAL: UInt32 = 0xFF67_6798
    public static let COLOR_VERIFIED: UInt32 = 0xFF00_9933
    public static let COLOR_FAILURE: UInt32 = 0xFFFF_3300
    public static let COLOR_CAUTION: UInt32 = 0xFF00_B33C // Amber: ffbf00;
    private static let RC_SIGN_IN: Int = 1
    private var mHostname: String? // JT 18.11.06

    private var mGoogleMap: GMSMapView? // GoogleMap
    private var mUserLocationMarker: GMSMarker?
    private var mLastKnownLocation: CLLocationCoordinate2D? // Location LatLng
    private var mLocationForMatching: CLLocationCoordinate2D // Location
    private var mMatchingEngineHelper: MatchingEngineHelper

//    private var mRpUtil: RequestPermissions   // JT 18.10.24 todo?
    ///    private var mFusedLocationClient: FusedLocationProviderClient
    //  private var mLocationRequest: LocationRequest   // JT 18.10.24

    private var mLocationRequest2: LocationRequest? // JT 18.11.06
    private var mDoLocationUpdates: Bool = false

    private var gpsInitialized: Bool = false
    private var fabFindCloudlets: UIButton // FloatingActionButton  // JT 18.10.24
    private var locationVerified: Bool = false
    private var locationVerificationAttempted: Bool = false
    private var mGpsLocationAccuracyKM: Double = 1.0
    private var defaultLatencyMethod: String = "ping"
    private var defaultLatencyMethod2 = CloudletListHolder.LatencyTestMethod.ping // JT 18.10.24
    let prefs = UserDefaults.standard // JT 18.11.01

//    private var mGoogleSignInClient: GoogleSignInClient   // JT 18.10.24 todo
//    private var signInMenuItem: MenuItem  // JT 18.10.24 todo
//    private var signOutMenuItem: MenuItem

    var manager: OneShotLocationManager? // JT 18.10.24 todo just use swiftLocation

    public required init?(coder aDecoder: NSCoder) // JT 18.11.06
    {
        super.init(coder: aDecoder)
        //  setup()
    }

//    override
//    init()  // JT 18.11.02    // JT 18.11.06
//    {
//
//    }
    // override func viewDidLoad()
    func getCurrentLocation()
    {
        manager = OneShotLocationManager()
        manager!.fetchWithCompletion
        { location, error in
            // fetch location or an error
            if let loc = location
            {
                Swift.print("location \(loc)")
                // println(location)
            }
            else if let err = error
            {
                Swift.print(err.localizedDescription)
            }
            self.manager = nil
        }
    }

    func init2() // JT 18.10.23
    {
        SKToast.backgroundStyle(.light)
        SKToast.messageTextColor(UIColor.black)
        let myFont = UIFont(name: "AvenirNext-DemiBold", size: 16)
        SKToast.messageFont(myFont!)

        UserDefaults.standard.addObserver(self, forKeyPath: "preference_mex_location_verification", options: NSKeyValueObservingOptions.new, context: nil) // JT 18.11.06

        let notificationCenter: Void = NotificationCenter.default.addObserver(self, selector: Selector("prefChanged:"), name: UserDefaults.didChangeNotification, object: nil)
    }

    func onCreate() // ( savedInstanceState: Bundle)
    {
        //    super.onCreate(savedInstanceState);
        // Log.i(TAG, "onCreate()");
        Swift.print("onCreate()")
        /**
         * MatchEngine APIs require special user approved permissions to READ_PHONE_STATE and
         * one of the following:
         * ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. This creates a dialog, if needed.
         */
        ///        mRpUtil =  RequestPermissions();

        ///        setContentView(R.layout.activity_main);

        ///        let navigationView:NavigationView = findViewById(R.id.nav_view);
        //  navigationView.setNavigationItemSelectedListener(self);

        var account: Account? // JT 18.10.24 todo    // JT 18.11.02
        #if false // JT 18.10.24
            // Configure sign-in to request the user's ID, email address, and basic
            // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
            let gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

            // Build a GoogleSignInClient with the options specified by gso.
            mGoogleSignInClient = GoogleSignIn.getClient(self, gso)

            // Check for existing Google Sign In account, if the user is already signed in
            // the GoogleSignInAccount will be non-null.
            let account: GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
            Account.getSingleton().setGoogleSignInAccount(account)

        #endif

//        signInMenuItem = navigationView.getMenu().findItem(R.id.nav_google_signin);
//        signOutMenuItem = navigationView.getMenu().findItem(R.id.nav_google_signout);

        if account != nil
        {
            // This means we're already signed in.
            ///            signInMenuItem.setVisible(false);    // JT 18.10.24 todo
            ///           signOutMenuItem.setVisible(true);
        }
        else
        {
            ///            signInMenuItem.setVisible(true);
            ///            signOutMenuItem.setVisible(false);
        }

        ///       let toolbar:Toolbar = findViewById(R.id.toolbar);
        ///       setSupportActionBar(toolbar);

//        DrawerLayout drawer = findViewById(R.id.drawer_layout);
//        let toggle: ActionBarDrawerToggle =  ActionBarDrawerToggle( self, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
//        drawer.addDrawerListener(toggle);
//        toggle.syncState();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        let mapFragment: SupportMapFragment =  getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);

        // let prefs:SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // mHostname = prefs.getString(getResources().getString(R.string.dme_hostname), "mexdemo.dme.mobiledgex.net");

        mHostname = prefs.string(forKey: "???") ?? "mexdemo.dme.mobiledgex.net" // JT 18.10.31 todo

        //     Log.i(TAG, "mHostname="+mHostname);
        Swift.print("mHostname= \(mHostname)")

        mMatchingEngineHelper = MatchingEngineHelper(Context(), mHostname!, view /* mapFragment.getView() */ ) // JT 18.11.02 todo
        mMatchingEngineHelper.setMatchingEngineResultsListener(self)

//        let networkSwitchingAllowed:Bool = prefs.getBoolean(getResources()
//                        .getString(R.string.preference_net_switching_allowed),false);
//    //    Log.i(TAG, "networkSwitchingAllowed="+networkSwitchingAllowed);
//       Swift.print("networkSwitchingAllowed= \(networkSwitchingAllowed)")
//        mMatchingEngineHelper.getMatchingEngine().setNetworkSwitchingEnabled(networkSwitchingAllowed);
//        mMatchingEngineHelper.getMatchingEngine().setSSLEnabled(false);

        ///        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(self);   // JT 18.10.31 todo? // JT 18.11.06

        // Restore mex location preference, defaulting to false:
        let mexLocationAllowed: Bool =
            // prefs.getBoolean(getResources()
//                        .getString(R.string.preference_mex_location_verification),
//                false);
            prefs.bool(forKey: "preference_mex_location_verification")
        mMatchingEngineHelper.getMatchingEngine().setMexLocationAllowed(mexLocationAllowed)

        // Watch allowed preference:
        //      prefs.registerOnSharedPreferenceChangeListener(self);

        // Client side FusedLocation updates.
        mDoLocationUpdates = true

        ///       let fab:FloatingActionButton = findViewById(R.id.fab);
        // JT 18.10.23 todo
//        fab.setOnClickListener(new View.OnClickListener() {
//
//             func onClick(View view) {
//                onFloatingActionBarClicked();
//            }
//        });

        ///       fabFindCloudlets = findViewById(R.id.fab2);
        // JT 18.10.23 todo
//        fabFindCloudlets.setOnClickListener(new View.OnClickListener() {
//
//            public func onClick(View view) {
//                matchingEngineRequest(REQ_FIND_CLOUDLET);
//            }
//        });
//        boolean allowFindBeforeVerify = prefs.getBoolean(getResources().getString(R.string.preference_allow_find_before_verify), true);
//        fabFindCloudlets.setEnabled(allowFindBeforeVerify);
//
//        // Open dialog for MEX if this is the first time the app is created:
//        String firstTimeUsePrefKey = getResources().getString(R.string.preference_first_time_use);
//        boolean firstTimeUse = prefs.getBoolean(firstTimeUsePrefKey, true);
//        if (firstTimeUse) {
//             let intent:Intent  = new Intent(this, FirstTimeUseActivity.class);
//            startActivity(intent);
//        }

        // Set, or create create an App generated UUID for use in MatchingEngine, if there isn't one:
        let uuidKey: String = // getResources().getString(R.string.preference_mex_user_uuid);
            prefs.string(forKey: "preference_mex_user_uuid") ?? "uuid" // JT 18.11.01 todo
        let currentUUID: String = prefs.string(forKey: uuidKey) ?? "" // JT 18.10.31
        if currentUUID == ""
        {
            let uuid: UUID = mMatchingEngineHelper.getMatchingEngine().createUUID()
            mMatchingEngineHelper.getMatchingEngine().setUUID(uuid)
//            prefs.edit()
//                    .putString(uuidKey, uuid.toString())
//                    .apply();

            UserDefaults.standard.set(uuid.uuidString, forKey: uuidKey) // JT 18.07.23
            UserDefaults.standard.synchronize()
        }
        else
        {
            mMatchingEngineHelper.getMatchingEngine().setUUID(UUID(uuidString: currentUUID)!)
        }

        let latencyTestMethod: String =
            // prefs.getString(getResources().getString(R.string.latency_method), defaultLatencyMethod);

            prefs.string(forKey: "latency_method") ?? defaultLatencyMethod

        let latencyTestAutoStart: Bool =
            // prefs.getBoolean(getResources().getString(R.string.pref_latency_autostart), true);

            prefs.bool(forKey: "pref_latency_autostart")

        //  Log.i(TAG, "latencyTestMethod from prefs: "+latencyTestMethod);
        Swift.print("latencyTestMethod from prefs: \(latencyTestMethod)")
        // Log.i(TAG, "latencyAutoStart from prefs: "+latencyTestAutoStart);
        Swift.print("latencyAutoStart from prefs: \(latencyTestAutoStart)")

        CloudletListHolder.getSingleton().setLatencyTestMethod(
            //  CloudletListHolder.LatencyTestMethod(rawValue: latencyTestMethod)
            defaultLatencyMethod == "ping" ? .ping : .socket
        ) // JT 18.11.01
        CloudletListHolder.getSingleton().setLatencyTestAutoStart(latencyTestAutoStart) // JT 18.11.01
    }

    /**
     * Perform the floatingActionBar action. Currently this is to perform the multi-step
     * matching engine process.
     */
    private func onFloatingActionBarClicked()
    {
//        if (mRpUtil.getNeededPermissions(self).size() > 0) {
//            mRpUtil.requestMultiplePermissions(self);
//            return;
//        }
        // JT 18.10.31 todo get perm to use location
        mMatchingEngineHelper.doEnhancedLocationUpdateInBackground(mLocationForMatching)
    }

    /**
     * Use the MatchingEngineHelper to perform a request with the Matching Engine.
     *
     * @param reqType  The request to perform.
     */
    private func matchingEngineRequest(_ reqType: MatchingEngineHelper.RequestType)
    {
        //   Log.i(TAG, "matchingEngineRequest("+reqType+") mLastKnownLocation="+mLastKnownLocation);
        Swift.print("matchingEngineRequest( \(reqType)) mLastKnownLocation= \(String(describing: mLastKnownLocation))")
        // As of Android 23, permissions can be asked for while the app is still running.
//        if (mRpUtil.getNeededPermissions(self).size() > 0) {
//            mRpUtil.requestMultiplePermissions(self);
//            return;
//        }
//        // JT 18.10.31 todo get perm to use location

        if mLastKnownLocation == nil
        {
            startLocationUpdates()
            //  Toast.makeText(MainActivity.self, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();

            SKToast.show(withMessage: "Waiting for GPS signal. Please retry in a moment.")

            // JT 18.10.31 todo some sort of toast
            return
        }
//        getCloudletList();
        mMatchingEngineHelper.doRequestInBackground(reqType, mLocationForMatching)
    }

    private func showAboutDialog()
    {
        var appName: String = ""
        var appVersion: String = ""
        do
        {
            // App
            // let appInfo:ApplicationInfo = getApplicationInfo();
            appVersion = (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String)!

            appName = Bundle.main.infoDictionary![kCFBundleNameKey as String] as! String

//           if (getPackageManager() != null) {
//                let seq:CharSequence = appInfo.loadLabel(getPackageManager());
//                if (seq != null) {
//                    appName = seq.toString();
//                }
//                let pi:PackageInfo  = getPackageManager().getPackageInfo(getPackageName(), 0);
//                appVersion = pi.versionName+"."+pi.versionCode;
//
//
//                   let appName: String = Bundle.main.infoDictionary![kCFBundleNameKey as String] as! String;
//            }
        }
        catch /* (e: PackageManager.NameNotFoundException ) */
        {
            // e.printStackTrace();
        }

//         AlertDialog.Builder( MainActivity.self)
//                .setIcon(R.mipmap.ic_launcher_foreground)
//                .setTitle("About")
//                .setMessage(appName+"\nVersion: "+appVersion)
//                .setPositiveButton("OK", null)
//                .show();

        let alert = UIAlertController(title: "About", message: "\(appName) Version: \(appVersion)", preferredStyle: .alert)

        alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))

        present(alert, animated: true) // JT 18.10.31 todo make ViewController
    }

    // ??
//    public func onBackPressed() {
//        let drawer:DrawerLayout = findViewById(R.id.drawer_layout);
//        if (drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START);
//        } else {
//            super.onBackPressed();
//        }
//    }

//    public func onCreateOptionsMenu(_ menu: Menu) ->Bool
//    {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//
//        return true;
//    }

    public func onOptionsItemSelected(_ itemIndex: Int) // ->Bool
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //   let id:Int = item.getItemId();

        if itemIndex == 0 // R.id.action_register_client)
        {
            matchingEngineRequest(.REQ_REGISTER_CLIENT)
        }
        if itemIndex == 1 // R.id.action_get_app_inst_list)
        {
            getCloudlets()
        }
        if itemIndex == 2 // R.id.action_reset_location)
        {
            // Reset spoofed GPS
            if mLastKnownLocation == nil
            {
                startLocationUpdates()
                //     Toast.makeText(MainActivity.self, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
                SKToast.show(withMessage: "Waiting for GPS signal. Please retry in a moment.") // JT 18.11.01

                //  return true;
            }
            if mUserLocationMarker == nil
            {
                // Log.w(TAG, "No marker for user location");
                Swift.print("No marker for user location") // JT 18.11.01
                // Toast.makeText(MainActivity.self, "No user location marker. Please retry in a moment.", Toast.LENGTH_LONG).show();
                SKToast.show(withMessage: "No user location marker. Please retry in a moment.") // JT 18.11.01

                //  return true;
            }
            mMatchingEngineHelper.setSpoofedLocation(spoofLocation: nil)
            mUserLocationMarker!.position = mLastKnownLocation! // JT 18.11.01
            // ( LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
            mUserLocationMarker!.snippet = prefs.string(forKey: "drag_to_spoof") ?? "drag_to_spoof" // JT 18.10.31

            updateLocSimLocation((mLastKnownLocation?.latitude)!, (mLastKnownLocation?.longitude)!)

            locationVerified = false
            locationVerificationAttempted = false
            getCloudlets()
            //  return true;
        }
        if itemIndex == 3 // action_verify_location
        {
            matchingEngineRequest(.REQ_VERIFY_LOCATION)
        }
        if itemIndex == 4 // R.id.action_find_cloudlet)
        {
            matchingEngineRequest(.REQ_FIND_CLOUDLET)
        }

        return // super.onOptionsItemSelected(item);
    }

    // @SuppressWarnings("StatementWithEmptyBody")

    public func onNavigationItemSelected(_ selectedItemIndex: Int) -> Bool
    {
        // Handle navigation view item clicks here.
        //   let id:Int = item.getItemId();

        if selectedItemIndex == 0 // R.id.nav_settings)
        {
            // Open "Settings" UI
//            let intent:Intent =  Intent(self, SettingsActivity.class);
//            startActivity(intent);
            Swift.print("todo nav_settings")
            return true
        }
        else if selectedItemIndex == 1 // R.id.nav_about)
        {
            // Handle the About action
            showAboutDialog()
            return true
        }
        else if selectedItemIndex == 2 // R.id.nav_camera)
        {
            Swift.print("todo: Start the face detection Activity ")
            // Start the face detection Activity
//             let intent:Intent  =  Intent(self, CameraActivity.class);
//            startActivity(intent);
            return true
        }
        else if selectedItemIndex == 3 // R.id.nav_face_recognition)
        {
            // Start the face recognition Activity
//             let intent:Intent  =  Intent(self, CameraActivity.class);
//            intent.putExtra(Camera2BasicFragment.EXTRA_FACE_RECOGNITION, true);
//            startActivity(intent);
            Swift.print("todo: Start the face detection Activity 3")

            return true
//        } else if (id == R.id.nav_benchmark_edge) {
//            // Start the face detection Activity in Edge benchmark mode
//             let intent:Intent  = new Intent(this, CameraActivity.class);
//            intent.putExtra(Camera2BasicFragment.EXTRA_BENCH_EDGE, true);
//            startActivity(intent);
//            return true;
//        } else if (id == R.id.nav_benchmark_local) {
//            // Start the face detection Activity in local benchmark mode
//             let intent:Intent  = new Intent(this, CameraActivity.class);
//            intent.putExtra(Camera2BasicFragment.EXTRA_BENCH_LOCAL, true);
//            startActivity(intent);
//            return true;
        }
        else if selectedItemIndex == 4 // R.id.nav_google_signin)
        {
            // let signInIntent:Intent = mGoogleSignInClient.getSignInIntent();
            // startActivityForResult(signInIntent, RC_SIGN_IN);
            Swift.print("todo nav_google_signin")
        }
        else if selectedItemIndex == 5 // R.id.nav_google_signout)
        {
            // JT 18.10.23 todo
            Swift.print("todo nav_google_signout")
//            mGoogleSignInClient.signOut()
//                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
//
//                        public func onComplete(@NonNull Task<Void> task) {
//                            Toast.makeText(MainActivity.this, "Sign out successful.", Toast.LENGTH_LONG).show();
//                            signInMenuItem.setVisible(true);
//                            signOutMenuItem.setVisible(false);
//                            Account.getSingleton().setGoogleSignInAccount(null);
//                        }
//                    });
        }

//        let drawer: DrawerLayout = findViewById(R.id.drawer_layout);
//        drawer.closeDrawer(GravityCompat.START);

        return true
    }

    public func onMapReady(_ googleMap: GMSMapView)
    {
        // Log.i(TAG, "onMapReady()");
        Swift.print("onMapReady()")
        mGoogleMap = googleMap

//        mGoogleMap.setOnMarkerClickListener(this);
//        mGoogleMap.setOnMapClickListener(this);
//        mGoogleMap.setOnInfoWindowClickListener(this);
//        mGoogleMap.setOnMapLongClickListener(this);
//        mGoogleMap.setOnMarkerDragListener(this);

        // As of Android 23, permissions can be asked for while the app is still running.
//        if (mRpUtil.getNeededPermissions(this).size() > 0) {
//            return;
//        } else {
//            startLocationUpdates();
//        }
        startLocationUpdates() // JT 18.11.01
    }

    /**
     * This makes a web service call to the location simulator to update the current IP address
     * entry in the database with the given latitude/longitude.
     *
     * @param lat
     * @param lng
     */
    public func updateLocSimLocation(_ lat: Double, _ lng: Double)
    {
//        var jsonBody:JSONObject =  JSONObject();
//        do {
//            jsonBody.put("latitude", lat);
//            jsonBody.put("longitude", lng);
//        } catch ( e: JSONException) {
//        //    e.printStackTrace();
//            return;
//        }

        let jd = ["latitude": lat, "latitude": lng] // JT 18.11.01

        do
        {
            let jsonData = try JSONSerialization.data(withJSONObject: jd, options: .prettyPrinted)
            // here "jsonData" is the dictionary encoded in JSON data

            // let decoded = try JSONSerialization.jsonObject(with: jsonData, options: [])
            // here "decoded" is of type `Any`, decoded from JSON data

            // you can now cast it with the right type
//            if let dictFromJSON = decoded as? [String:String] {
//                // use dictFromJSON
//            }

            // let  requestBody: String = jsonBody.toString();
            let requestBody: String = String(data: jsonData, encoding: .utf8)!

            // Instantiate the RequestQueue.
            ///    let queue:RequestQueue = Volley.newRequestQueue(self);
            //      let hostName:String = mHostname.replace("dme", "locsim");
            let hostName: String = mHostname!.replacingOccurrences(of: "dme", with: "locsim") // JT 18.11.01

            let urlString: String = "http://\(hostName):8888/updateLocation" // JT 18.11.06

            let url = URL(string: urlString) // JT 18.11.06
            let request = NSMutableURLRequest(url: url!) // JT 18.11.06
            request.httpMethod = "POST" // JT 18.11.06

            let data: NSData = NSKeyedArchiver.archivedData(withRootObject: jd) as NSData

            request.httpBody = data as Data // JT 18.11.06

            let task = URLSession.shared.dataTask(with: request as URLRequest)
            {
                data, response, error in

                guard let data = data, error == nil else
                {
                    print(error?.localizedDescription ?? "No data")
                    return
                }
                print(response)
                // Your completion handler code here
            }
            task.resume()
            //   Log.i(TAG, "updateLocSimLocation url="+url);
            Swift.print("updateLocSimLocation url= \(url)")
            //   Log.i(TAG, "updateLocSimLocation body="+requestBody);
            Swift.print("updateLocSimLocation body= \(requestBody)")

            // Add the request to the RequestQueue.
            ///  queue.add(stringRequest);    // JT 18.11.06
        }
        catch
        {
            print(error.localizedDescription)
            return
        }

        // JT 18.10.23 todo
        // Request a string response from the provided URL.
//        let stringRequest:StringRequest =  StringRequest(Request.Method.POST, url,
//                 Response.Listener<String>() {
//
//                     func onResponse(String response) {
//                        Log.i(TAG, "updateLocSimLocation response="+response);
//                        Snackbar.make(findViewById(android.R.id.content), response, Snackbar.LENGTH_SHORT).show();
//                    }
//                }, new Response.ErrorListener() {
//
//            public func onErrorResponse(VolleyError error) {
//                Log.e(TAG, "That didn't work! error="+error);
//                Snackbar.make(findViewById(android.R.id.content), error.getMessage(), Snackbar.LENGTH_LONG).show();
//            }
//        }) {
//
//            public String getBodyContentType() {
//                return "application/json; charset=utf-8";
//            }
//
//
//            public byte[] getBody() {
//                try {
//                    return requestBody == null ? null : requestBody.getBytes("utf-8");
//                } catch (UnsupportedEncodingException uee) {
//                    Log.wtf(TAG, "Unsupported Encoding while trying to get the body bytes");
//                    return null;
//                }
//            }
//        };
    }

    /**
     * Gets list of cloudlets from DME, and populates map with markers.
     *
     */
    public func getCloudlets()
    {
        // Log.i(TAG, "getCloudletList() mLastKnownLocation="+mLastKnownLocation);
        Swift.print("getCloudletList() mLastKnownLocation= \(mLastKnownLocation)")
        if mLastKnownLocation == nil
        {
            startLocationUpdates()
            //    Toast.makeText(MainActivity.this, "Waiting for GPS signal. Please retry in a moment.", Toast.LENGTH_LONG).show();
            SKToast.show(withMessage: "Waiting for GPS signal. Please retry in a moment.") // JT 18.11.01

            return
        }

        if mMatchingEngineHelper.getSpoofedLocation() == nil
        {
            mLocationForMatching = mLastKnownLocation!
        }
        else
        {
            mLocationForMatching = mMatchingEngineHelper.getSpoofedLocation()!
        }

        mMatchingEngineHelper.doRequestInBackground(.REQ_GET_CLOUDLETS, mLocationForMatching)
    }

    private func makeMarker(_: Int, _: Int, _: String) -> UIImage? // BitmapDescriptor // JT 18.11.01
    {
        Swift.print("todo makeMarker") // JT 18.11.01
//        let iconDrawable:Drawable = getResources().getDrawable(resourceId);
//        iconDrawable.setColorFilter(color, PorterDuff.Mode.MULTIPLY );
//
//        return getMarkerIconFromDrawable(iconDrawable, color, badgeText);
        return nil
    }

    /**
     * Create map marker icon, based on given drawable, color, and add badge text if non-empty.
     *
     * @param drawable
     * @param color
     * @param badgeText
     * @return
     */
    // JT 18.10.23 todo

//    private  getMarkerIconFromDrawable(Drawable drawable, int color, String badgeText) ->BitmapDescriptor {
//    var canvas:Canvas =  Canvas();
//        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
//        canvas.setBitmap(bitmap);
//        if(badgeText != null && badgeText.length() != 0) {
//            float scale = getResources().getDisplayMetrics().density;
//            Log.d(TAG, "scale=" + scale + " x,y=" + drawable.getIntrinsicWidth() + "," + drawable.getIntrinsicHeight());
//            Paint paint = new Paint();
//            paint.setStrokeWidth(5);
//            paint.setTextAlign(Paint.Align.CENTER);
//            float textSize = 22 * scale;
//            float badgeWidth = paint.measureText(badgeText);
//            paint.setTextSize(textSize);
//            paint.setColor(color);
//            canvas.drawText(badgeText, drawable.getIntrinsicWidth() / 2, drawable.getIntrinsicHeight() / 2 + textSize / 2, paint);
//        }
//        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
//        drawable.draw(canvas);
//        return BitmapDescriptorFactory.fromBitmap(bitmap);
//    }

    /**
     * When releasing a dragged marker or long-clicking on the map, the user will be prompted
     * if they want to either spoof the GPS at the dropped location, or to update the GPS location
     * for their IP address in the simulator.
     *
     * @param spoofLatLng  The location to use.
     */
    private func showSpoofGpsDialog(_ spoofLatLng: CLLocationCoordinate2D) // LatLng)   // JT 18.11.01
    {
        mUserLocationMarker!.position = spoofLatLng // JT 18.11.01
        let charSequence: [String] = ["Spoof GPS at this location", "Update location in GPS database"]

//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//        alertDialogBuilder.setSingleChoiceItems(charSequence, -1, new DialogInterface.OnClickListener() {
//
//            public func onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//                Location location = new Location("MEX");
//                location.setLatitude(spoofLatLng.latitude);
//                location.setLongitude(spoofLatLng.longitude);
//                LatLng oldLatLng = new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude());
//                switch (which) {
//                    case 0:
//                        Log.i(TAG, "Spoof GPS at "+location);
//                        Toast.makeText(MainActivity.this, "GPS spoof enabled.", Toast.LENGTH_LONG).show();
//                        double distance = SphericalUtil.computeDistanceBetween(oldLatLng, spoofLatLng)/1000;
//                        mUserLocationMarker.setSnippet("Spoofed "+String(format:"%.2f", distance)+" km from actual location");
//                        mMatchingEngineHelper.setSpoofedLocation(location);
//                        locationVerificationAttempted = locationVerified = false;
//                        getCloudlets();
//                        break;
//                    case 1:
//                        Log.i(TAG, "Update GPS in simulator to "+location);
//                        mUserLocationMarker.setSnippet((String) getResources().getText(R.string.drag_to_spoof));
//                        updateLocSimLocation(mUserLocationMarker.getPosition().latitude, mUserLocationMarker.getPosition().longitude);
//                        mMatchingEngineHelper.setSpoofedLocation(location);
//                        locationVerificationAttempted = locationVerified = false;
//                        getCloudlets();
//                        break;
//                    default:
//                        Log.i(TAG, "Unknown dialog selection.");
//                }
//
//            }
//        });

//        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            public func onClick(DialogInterface dialog, int which) {
//                mUserLocationMarker.setPosition(new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude()));
//            }
//        });
//
//        alertDialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//
//            public func onDismiss(DialogInterface dialog) {
//                mUserLocationMarker.setPosition(new LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude()));
//            }
//        });

//        AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
    }

    func onRegister(_: String)
    {
        Swift.print("onRegister") // JT 18.10.23 todo
//        runOnUiThread(new Runnable() {
//
//            public func run() {
//                Toast.makeText(MainActivity.this, "Successfully registered client. sessionCookie=\n"+sessionCookie, Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    /**
     * Callback for Matching Engine's verifyLocation results.
     *
     * @param status  GPS_Location_Status to determine success, fail, or caution
     * @param gpsLocationAccuracyKM  location accuracy, the location is verified to
     */
    func onVerifyLocation(_ status: DistributedMatchEngine_VerifyLocationReply.GPS_Location_Status, // JT 18.11.01
                          // AppClient.Match_Engine_Loc_Verify.GPS_Location_Status,  // JT 18.11.01
                          _ gpsLocationAccuracyKM: Double)
    {
        locationVerificationAttempted = true
        // runOnUiThread( Runnable()
        DispatchQueue.global(qos: .background).async // JT 18.11.01
        {
            //  func run() {
            var message: String
            var message2: String = ""
            if self.mUserLocationMarker == nil
            {
                // Log.w(TAG, "No marker for user location");
                Swift.print("No marker for user location")
                return
            }
            // mUserLocationMarker.hideInfoWindow();
            Swift.print("todo hideInfoWindow")
            if status == .locVerified // JT 18.11.01AppClient.Match_Engine_Loc_Verify.GPS_Location_Status.LOC_VERIFIED) //LOC_VERIFIED)  // JT 18.11.01
            {
                self.fabFindCloudlets.isEnabled = true // setEnabled(true);
                // mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_VERIFIED, ""));

                let iconTemplate = UIImage(named: "ic_marker_mobile-web") // JT 18.11.01
                let tint = self.getColorByHex(MainActivity.COLOR_VERIFIED) // JT 18.11.05

                let tinted = iconTemplate!.imageWithColor(tint)

                self.mUserLocationMarker!.icon = tinted

                message = "User Location - Verified"
                self.mGpsLocationAccuracyKM = gpsLocationAccuracyKM
                message2 = "\n( \(self.mGpsLocationAccuracyKM) km accuracy)"
            }
            else if status == .locRoamingCountryMatch // LOC_ROAMING_COUNTRY_MATCH)   // JT 18.11.01
            {
                // mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_CAUTION, ""));

                self.mUserLocationMarker!.icon = UIImage(named: "ic_marker_mobile") // JT 18.11.01
                // JT 18.11.01 todo tint image COLOR_CAUTION
                Swift.print("todo tint image COLOR_CAUTION")
                // message = ""+status;
                message = "User Location - Verified"
                self.mGpsLocationAccuracyKM = gpsLocationAccuracyKM
            }
            else
            {
                // mUserLocationMarker.setIcon(makeMarker(R.mipmap.ic_marker_mobile, COLOR_FAILURE, ""));
                // JT 18.11.01 todo tint image COLOR_CAUTION
                Swift.print("todo tint image COLOR_CAUTION")
                self.mUserLocationMarker!.icon = UIImage(named: "ic_marker_mobile") // JT 18.11.01

                // message = ""+status;
                message = "User Location - Failed Verify"
            }
            self.mUserLocationMarker!.snippet = message // JT 18.11.02
            //   Toast.makeText(MainActivity.this, message+message2, Toast.LENGTH_LONG).show();

            SKToast.show(withMessage: message + message2) // JT 18.11.02
        }
        //  });
    }

    /**
     * Determine what if any badge text should be shown for the given cloudlet.
     *
     * @param cloudlet
     * @return
     */
    private func getBadgeText(_ cloudlet: Cloudlet) -> String
    {
        var badgeText: String = ""
        if cloudlet.getCarrierName().caseInsensitiveCompare("gcp") == .orderedSame
        {
            badgeText = "G"
        }
        else if cloudlet.getCarrierName().caseInsensitiveCompare("azure") == .orderedSame
        {
            badgeText = "A"
        }
        return badgeText
    }

    /**
     * Callback for Matching Engine's findCloudlet results. Looks through cloudlet list
     * and marks this one as closest by setting the color.
     *
     * @param closestCloudlet  Object encapsulating the closest cloudlet characteristics.
     */

    func onFindCloudlet(_ closestCloudlet: DistributedMatchEngine_FindCloudletReply /* FindCloudletResponse */ )
    {
        func runOnUiThread()
        {
            DispatchQueue.global().async
            {
                run() // JT 18.11.02
            }
        }
        //   runOnUiThread( Runnable()

        func run()
        {
            var cloudlet: Cloudlet?
            let cnt = CloudletListHolder.getSingleton().getCloudletList().count // JT 18.11.06

            for i in 0 ..< cnt // JT 18.11.05getCloudletList
            {
                let list = CloudletListHolder.getSingleton().getCloudletList()

                cloudlet = Array(list)[i].value // .valueAt(i);   // JT 18.11.05

                if (cloudlet!.getMarker().position.latitude == closestCloudlet.cloudletLocation.lat) &&
                    (cloudlet!.getMarker().position.longitude == closestCloudlet.cloudletLocation.long)
                {
                    // Log.i(TAG, "Got a match! "+cloudlet.getCloudletName());
                    Swift.print("Got a match! \(cloudlet!.getCloudletName())")

                    let iconTemplate = UIImage(named: "ic_marker_cloudlet-web") // JT 18.11.01

                    let tint = getColorByHex(MainActivity.COLOR_VERIFIED) // JT 18.11.05
                    let tinted = iconTemplate!.imageWithColor(tint)

                    cloudlet!.getMarker().icon = tinted // JT 18.11.05

                    // JT 18.11.05 todo badge
                    // cloudlet.getMarker().setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_VERIFIED, getBadgeText(cloudlet)));
                    cloudlet!.setBestMatch(true)
                    break
                }
            }
            if cloudlet != nil
            {
                let path = GMSMutablePath() // JT 18.11.05

                path.add(mUserLocationMarker!.position)
                path.add(cloudlet!.getMarker().position)

                let poly = GMSPolyline(path: path)
                poly.strokeWidth = 8
                poly.strokeColor = getColorByHex(MainActivity.COLOR_VERIFIED)

//                    let pl = PolylineOptions()
//                        .add(mUserLocationMarker.getPosition(), cloudlet.getMarker().getPosition())
//
//                    let line:GMSPolyline = mGoogleMap.addPolyline(
//                            .width(8)
//                            .color(COLOR_VERIFIED));
            }
        }
    }

    /**
     * Callback for Matching Engine's getCloudletList results. Creates ArrayMap of cloudlets
     * keyed on the cloudlet name. A map marker is also created for each cloudlet.
     *
     * @param cloudletList  List of found cloudlet instances.
     */

    func onGetCloudletList(_ cloudletList: DistributedMatchEngine_AppInstListReply) // JT 18.11.02 // JT 18.11.05
    // AppClient.Match_Engine_AppInst_List)
    {
        // Log.i(TAG, "onGetCloudletList()");
        Swift.print("onGetCloudletList()")

        DispatchQueue.main.async // JT 18.11.01 updates Ui
        {
            self.mGoogleMap!.clear()
            var tempCloudlets = [String: Cloudlet]() // new ArrayMap<>();
            //  var builder:  LatLngBounds.Builder =  LatLngBounds.Builder();
            var boundsBuilder = GMSCoordinateBounds() // JT 18.11.06

            if cloudletList.cloudlets.count == 0
            {
                // JT 18.10.23 todo
//                    new AlertDialog.Builder(MainActivity.this)
//                            .setTitle("Error")
//                            .setMessage("No cloudlets available.\nContact MobiledgeX support.")
//                            .setPositiveButton("OK", null)
//                            .show();

                let alert = UIAlertController(title: "Error", message: "No cloudlets available.\nContact MobiledgeX support.", preferredStyle: .alert)

                alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))

                self.present(alert, animated: true) // JT 18.10.31 todo make ViewController
            }

//                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            let result = UserDefaults.standard.string(forKey: "download_size") ?? "1048576" // JT 18.11.05

            let speedTestBytes = Int(result)
            // Integer.parseInt(prefs.getString(getResources().getString(R.string.download_size), "1048576"));

            let speedTestPackets: Int = Int(UserDefaults.standard.string(forKey: "latency_packets") ?? "5")! // JT 18.10.31
            // Int(parseInt(prefs.getString(getResources().getString(R.string.latency_packets), "5")))

            // First get the new list into an ArrayMap so we can index on the cloudletName
            //   for(AppClient.CloudletLocation cloudletLocation:cloudletList.getCloudletsList())
            for cloudletLocation in cloudletList.cloudlets // JT 18.11.05
            {
                // Log.i(TAG, "getCloudletName()="+cloudletLocation.getCloudletName()+" getCarrierName()="+cloudletLocation.getCarrierName());
                Swift.print("getCloudletName()= \(cloudletLocation.cloudletName) getCarrierName()= \(cloudletLocation.carrierName)") // JT 18.11.05
                let carrierName: String = cloudletLocation.carrierName
                let cloudletName: String = cloudletLocation.cloudletName
                let appInstances = cloudletLocation.appinstances // List<AppClient.Appinstance>
                //      let uri = appInstances.get(0).getUri();
                let uri = appInstances[0].fqdn // getUri(); // JT 18.11.06
                let appName: String = appInstances[0].appname
                let distance: Double = cloudletLocation.distance
                let latLng: CLLocationCoordinate2D = CLLocationCoordinate2D(latitude: cloudletLocation.gpsLocation.lat, longitude: cloudletLocation.gpsLocation.long)
                // LatLng(cloudletLocation.gpsLocation.lat, cloudletLocation.gpsLocation.long);
                // CLLocationCoordinate2D
                // public init(latitude: CLLocationDegrees, longitude: CLLocationDegrees)

                let marker: GMSMarker = GMSMarker(position: CLLocationCoordinate2D(latitude: cloudletLocation.gpsLocation.lat, longitude: cloudletLocation.gpsLocation.long)) // JT 18.11.05
                marker.title = "\(cloudletName) Cloudlet"
                marker.snippet = "Click for details"
                marker.map = self.mGoogleMap // JT 18.11.05

//        let marker:GMSMarker = mGoogleMap.addMarker( MarkerOptions().position(latLng).title(cloudletName + " Cloudlet").snippet("Click for details"));

                //  marker.tag = (cloudletName);    // JT 18.11.05 todo? ask bruce

                var userData = Dictionary<String, String>()
                userData["tag"] = cloudletName
                marker.userData = userData

                var cloudlet: Cloudlet
                let d = CloudletListHolder.getSingleton().getCloudletList()

                if d[cloudletName] != nil
                {
                    cloudlet = CloudletListHolder.getSingleton().getCloudletList()[cloudletName]!
                }
                else
                {
                    cloudlet = Cloudlet() // JT 18.11.06

                    cloudlet.setup(cloudletName, appName, carrierName, latLng, distance, uri, marker, speedTestBytes!, speedTestPackets)
                }
                // marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_NEUTRAL, getBadgeText(cloudlet)));

                let iconTemplate = UIImage(named: "ic_marker_cloudlet-web") // JT 18.11.01

                let tint = self.getColorByHex(MainActivity.COLOR_NEUTRAL) // JT 18.11.05
                let tinted = iconTemplate!.imageWithColor(tint)

                marker.icon = tinted // JT 18.11.05

                cloudlet.update(cloudletName, appName, carrierName, latLng, distance, uri, marker, speedTestBytes!, speedTestPackets)

                tempCloudlets[cloudletName] = cloudlet // JT 18.11.05

                // builder.include(marker.getPosition());  // JT 18.11.05 ??? ask bruce
                boundsBuilder.includingCoordinate(marker.position) // JT 18.11.06
            }

            // Now see if all cloudlets still exist. If removed, show as transparent.
            let cnt = CloudletListHolder.getSingleton().getCloudletList().count
            for i in 0 ..< cnt
            {
                let cloudlet: Cloudlet = // CloudletListHolder.getSingleton().getCloudletList().valueAt(i);

                    Array(CloudletListHolder.getSingleton().getCloudletList())[i].value
                if !(tempCloudlets[cloudlet.mCloudletName] != nil)
                {
                    //  Log.i(TAG, cloudlet.getCloudletName() + " has been removed");
                    Swift.print("\(cloudlet.getCloudletName()) has been removed")
//                        let marker:Marker = mGoogleMap.addMarker( MarkerOptions().position( LatLng(cloudlet.getLatitude(), cloudlet.getLongitude()))
//                                .title( "\(cloudlet.getCloudletName())   Cloudlet").snippet("Has been removed"));

                    let marker: GMSMarker = GMSMarker(position: CLLocationCoordinate2D(latitude: cloudlet.getLatitude(), longitude: cloudlet.getLongitude())) // JT 18.11.05
                    marker.title = "\(cloudlet.getCloudletName()) Cloudlet"
                    marker.snippet = "Has been removed"

                    marker.map = self.mGoogleMap // JT 18.11.05
//                        marker.setIcon(makeMarker(R.mipmap.ic_marker_cloudlet, COLOR_FAILURE, getBadgeText(cloudlet)));
                    // marker.alpha = (  0.33);  // JT 18.11.05 todo alpha does not exist

                    let iconTemplate = UIImage(named: "ic_marker_cloudlet-web") // JT 18.11.01

                    let tint = self.getColorByHex(MainActivity.COLOR_FAILURE) // JT 18.11.05
                    let tinted = iconTemplate!.imageWithColor(tint)

                    marker.icon = tinted // JT 18.11.05
                }
            }
            CloudletListHolder.getSingleton().setCloudlets(mCloudlets: tempCloudlets)

            // Create the marker representing the user/mobile device.
            let tag: String = "User"
            var title: String = "User Location - Not Verified"
            //   let snippet:String =  getResources().getText(R.string.drag_to_spoof);
            var snippet: String = UserDefaults.standard.string(forKey: "drag_to_spoof") ?? "drag_to_spoof" // JT 18.10.31

            // let icon:BitmapDescriptor = makeMarker(R.mipmap.ic_marker_mobile, COLOR_NEUTRAL, "");

            let iconTemplate = UIImage(named: "ic_marker_mobile-web") // JT 18.11.01

            let tint = self.getColorByHex(MainActivity.COLOR_NEUTRAL) // JT 18.11.05
            var icon = iconTemplate!.imageWithColor(tint)

            if self.mUserLocationMarker != nil
            {
                snippet = self.mUserLocationMarker!.snippet!
                if self.locationVerificationAttempted
                {
                    let iconTemplate = UIImage(named: "ic_marker_mobile-web") // JT 18.11.01

                    if self.locationVerified
                    {
                        //  icon = makeMarker(R.mipmap.ic_marker_mobile, COLOR_VERIFIED, "");

                        let tint = self.getColorByHex(MainActivity.COLOR_VERIFIED) // JT 18.11.05
                        icon = iconTemplate!.imageWithColor(tint)

                        title = "User Location - Verified"
                    }
                    else
                    {
                        // icon = makeMarker(R.mipmap.ic_marker_mobile, COLOR_FAILURE, "");

                        let tint = self.getColorByHex(MainActivity.COLOR_FAILURE) // JT 18.11.05
                        icon = iconTemplate!.imageWithColor(tint)

                        title = "User Location - Failed Verify"
                    }
                }
            }

            //      let latLng:LatLng =  LatLng(mLocationForMatching.getLatitude(), mLocationForMatching.getLongitude());

            let latLng: CLLocationCoordinate2D = self.mLocationForMatching

//                mUserLocationMarker = mGoogleMap.addMarker( MarkerOptions().position(latLng)
//                        .title(title).snippet(snippet)
//                        .icon(icon).draggable(true));
//
            self.mUserLocationMarker = GMSMarker(position: latLng) // JT 18.11.05
            self.mUserLocationMarker!.title = title
            self.mUserLocationMarker!.snippet = snippet

            let m = self.mUserLocationMarker! as GMSMarker

            m.map = self.mGoogleMap // JT 18.11.05
            self.mUserLocationMarker!.isDraggable = true
            self.mUserLocationMarker!.icon = icon // JT 18.11.05

            // mUserLocationMarker.setTag(tag);  // JT 18.11.05

            var userData = Dictionary<String, String>()
            userData["tag"] = tag // JT 18.11.05
            self.mUserLocationMarker!.userData = userData

            /// builder.include(mUserLocationMarker.getPosition());  // JT 18.11.05 todo ask bruce

            boundsBuilder.includingCoordinate(self.mUserLocationMarker!.position) // JT 18.11.06

            // Update the camera view if needed.
            if self.mMatchingEngineHelper.getSpoofedLocation() != nil
            {
                // Log.i(TAG, "Leave the camera alone.");
                Swift.print("Leave the camera alone.")
                return
            }
            ///  let bounds:LatLngBounds = builder.build();
            // Log.i(TAG, "bounds.northeast="+bounds.northeast+" bounds.southwest="+bounds.southwest);

            Swift.print("boundsBuilder.northeast= \(boundsBuilder.northEast) boundsBuilder.southwest= \(boundsBuilder.southWest)")

            // If there are no cloudlets, don't use the bounds, as it will zoom in super close.
            ///  var cu: CameraUpdate
            if !(boundsBuilder.southWest == boundsBuilder.northEast)
            {
                // Log.d(TAG, "Using cloudlet boundaries");
                Swift.print("Using cloudlet boundaries")
                let padding: Int = 125 // offset from edges of the map in pixels
                /// cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                let update = GMSCameraUpdate.fit(boundsBuilder, withPadding: padding)
                self.mGoogleMap!.moveCamera(update) // JT 18.11.06

                // viewMap.camera = camera
            }
            else
            {
                // Log.d(TAG, "No cloudlets. Don't zoom in");
                Swift.print("No cloudlets. Don't zoom in")

                //  cu = CameraUpdateFactory.newLatLng(mUserLocationMarker.getPosition());

                self.mGoogleMap!.animate(toLocation: self.mUserLocationMarker!.position)
            }
            
            do
            {
                // mGoogleMap.moveCamera(cu);
            }
            catch /* ( e: Exception) */
            {
                // Log.e(TAG, "Map wasn't ready.", e);
                Swift.print("Map wasn't ready. ")
            }
        }
    }

    public func onMarkerClick(_ marker: GMSMarker) -> Bool
    {
        // Log.i(TAG, "onMarkerClick("+marker+"). Draggable="+marker.isDraggable());
        Swift.print("onMarkerClick( \(marker) Draggable= \(marker.isDraggable)")
        // marker.showInfoWindow();
        Swift.print("todo showInfoWindow") // JT 18.11.01
        return true
    }

    public func onMapClick(_ location: CLLocationCoordinate2D)
    {
        // Log.i(TAG, "onMapClick("+latLng+")");
        Swift.print("onMapClick(\(location))")
    }

    public func onInfoWindowClick(_ marker: GMSMarker)
    {
        //      if(marker.getTag().toString().equalsIgnoreCase("user"))
        let userData: [String: String] = marker.userData as! Dictionary<String, String>
        if userData["type"] == "user" // JT 18.11.01
        {
            // Log.d(TAG, "skipping mUserLocationMarker");
            Swift.print("skipping mUserLocationMarker")
            return
        }

        let cloudletName: String = userData["tag"]! // marker.getTag();  // JT 18.11.01
        let cloudlet: Cloudlet = CloudletListHolder.getSingleton().getCloudletList()[cloudletName]!
        // Log.i(TAG, "1."+cloudlet+" "+cloudlet.getCloudletName()+" "+cloudlet.getMbps());
        Swift.print("1.\(cloudlet) \(cloudlet.getCloudletName()) \(cloudlet.getMbps())")
//         let intent:Intent  =  Intent(getApplicationContext(), CloudletDetailsActivity.class);
//        intent.putExtra("CloudletName", cloudlet.getCloudletName());
//        startActivity(intent);
        Swift.print("todo Cloudlet startActivity(intent);")
        //  Log.i(TAG, "Display Detailed Cloudlet Info");
        Swift.print("Display Detailed Cloudlet Info")
    }

    public func onMapLongClick(_ latLng: CLLocationCoordinate2D)
    {
        // Log.i(TAG, "onMapLongClick("+latLng+")");
        Swift.print("onMapLongClick(\(latLng))")
        showSpoofGpsDialog(latLng)
    }

    public func onMarkerDragStart(_ marker: GMSMarker)
    {
        //  Log.i(TAG, "onMarkerDragStart("+marker+")");
        Swift.print("onMarkerDragStart(\(marker))")
    }

    public func onMarkerDrag(_: GMSMarker)
    {}

    public func onMarkerDragEnd(_ marker: GMSMarker)
    {
        // Log.i(TAG, "onMarkerDragEnd(" + marker + ")");
        Swift.print("onMarkerDragEnd( \(marker))")
        showSpoofGpsDialog(marker.position)
    }

    // MARK: -

//    todo?
//    public func onActivityResult( requestCode: Int, _ resultCode: Int, _ data: Intent) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
//        if (requestCode == MainActivity.RC_SIGN_IN) {
//            // The Task returned from this call is always completed, no need to attach a listener.
//         let  task = GoogleSignIn.getSignedInAccountFromIntent(data); //  Task<GoogleSignInAccount>
//            handleSignInResult(task);
//        }
//    }
//
//    private func handleSignInResult( _ completedTask: Task) // Task<GoogleSignInAccount>
//    {
//        do {
//            let account:GoogleSignInAccount = completedTask.getResult(ApiException.class);
//            // Signed in successfully, show authenticated UI.
//       //     signInMenuItem.setVisible(false); // JT 18.11.06 todo
//       //    signOutMenuItem.setVisible(true);  // JT 18.11.06 todo
//
//            Account.getSingleton().setGoogleSignInAccount(account);
//          //  Toast.makeText(MainActivity.this, "Sign in successful. Welcome, "+account.getDisplayName(), Toast.LENGTH_LONG).show();
//            SKToast.show(withMessage: "Sign in successful. Welcome, ")  // JT 18.11.01
//
//        } catch /* ( e: ApiException) */ {
//            // The ApiException status code indicates the detailed failure reason.
//            // Please refer to the GoogleSignInStatusCodes class reference for more information.
//           // Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
//            Swift.print("signInResult:failed code= \(e.getStatusCode())")
    ////             AlertDialog.Builder(MainActivity.this)
    ////                    .setTitle("Error")
    ////                    .setMessage("signInResult:failed code=" + e.getStatusCode())
    ////                    .setPositiveButton("OK", nil)
    ////                    .show();
//
//            let alert = UIAlertController(title: "Error", message: "signInResult:failed code=", preferredStyle: .alert)
//
//            alert.addAction(UIAlertAction(title: "OK", style: .cancel, handler: nil))
//
//            self.present(alert, animated: true) // JT 18.10.31 todo make ViewController
//
//        }
//    }

    // MARK: -

    // public func onSharedPreferenceChanged()
    // (_ sharedPreferences: SharedPreferences, _ key: String)
    func prefChanged(notification: NSNotification)
    {
        Swift.print("prefChanged \(notification)") // JT 18.11.06

        let key = "todo" // JT 18.11.06
        // Log.i(TAG, "onSharedPreferenceChanged("+key+")");
        // Swift.print("onSharedPreferenceChanged(\(key))")

        let prefKeyAllowMEX: String = prefs.string(forKey: "preference_mex_location_verification") ?? "false" // JT 18.10.31 todo
        // getResources().getString(R.string.preference_mex_location_verification);
        // let prefKeyAllowNetSwitch:String =
        // getResources().getString(R.string.preference_net_switching_allowed);
        let prefKeyAllowNetSwitch: String =
            prefs.string(forKey: "preference_net_switching_allowed") ?? "false" // JT 18.10.31 todo

        let prefKeyDownloadSize: String =
            // getResources().getString(R.string.download_size);
            prefs.string(forKey: "download_size") ?? "???" // JT 18.10.31 todo

        let prefKeyDownloadSize2: String = prefs.string(forKey: "preference_net_switching_allowed") ?? "?" // JT 18.10.31 todo

        let prefKeyNumPackets: String =
            // getResources().getString(R.string.latency_packets);
            prefs.string(forKey: "latency_packets") ?? "?"
        let prefKeyLatencyMethod: String =
            // getResources().getString(R.string.latency_method);
            prefs.string(forKey: "latency_method") ?? "?"

        let prefKeyLatencyAutoStart: String = // getResources().getString(R.string.pref_latency_autostart);
            prefs.string(forKey: "pref_latency_autostart") ?? "false"

        let prefKeyDmeHostname: String =
            // getResources().getString(R.string.dme_hostname);
            prefs.string(forKey: "dme_hostname") ?? "prefKeyDmeHostname"

        if true /* key.equals(prefKeyAllowMEX) */ // JT 18.11.01
        {
            //   let mexLocationAllowed: Bool = sharedPreferences.getBoolean(prefKeyAllowMEX, false);
            let mexLocationAllowed: Bool = prefs.bool(forKey: "prefKeyAllowMEX")

            MatchingEngine.mMexLocationAllowed = mexLocationAllowed
        }

//        if (key.equals(prefKeyAllowNetSwitch)) {
//            let netSwitchingAllowed: Bool = sharedPreferences.getBoolean(prefKeyAllowNetSwitch, false);
//            mMatchingEngineHelper.getMatchingEngine().setNetworkSwitchingEnabled(netSwitchingAllowed);
//        }

        if key.equals(prefKeyLatencyMethod)
        {
            let latencyTestMethod: String =
                // sharedPreferences.getString(getResources().getString(R.string.latency_method), defaultLatencyMethod);

                prefs.string(forKey: "latency_method") ?? "?"
            switch latencyTestMethod
            {
            case "socket":
                CloudletListHolder.getSingleton().setLatencyTestMethod(.socket)
                break

            case "ping":
                CloudletListHolder.getSingleton().setLatencyTestMethod(.ping)
                break

            default:
                break
            }
        }

        if key.equals(prefKeyLatencyAutoStart)
        {
            let latencyTestAutoStart: Bool = // sharedPreferences.getBoolean(getResources().getString(R.string.pref_latency_autostart), true);
                prefs.bool(forKey: "pref_latency_autostart")

            CloudletListHolder.getSingleton().setLatencyTestAutoStart(latencyTestAutoStart)
        }

        if key.equals(prefKeyDmeHostname)
        {
            // Log.i(TAG, "Updated mHostname="+mHostname);
            Swift.print("Updated mHostname= \(mHostname)")
            mHostname = // sharedPreferences.getString(getResources().getString(R.string.dme_hostname), "mexdemo.dme.mobiledgex.net");

                prefs.string(forKey: "dme_hostname") ?? "mexdemo.dme.mobiledgex.net" // JT 18.11.06

            mMatchingEngineHelper.setHostname(hostname: mHostname!)
            // Clear list so we don't show old cloudlets as transparent

            var list = CloudletListHolder.getSingleton().getCloudletList()
            list.removeAll() // JT 18.11.06
            getCloudlets()
        }

        // TODO: Add variables in CloudletListHolder.getSingleton() instead of setting these for every cloudlet.
        if key.equals(prefKeyDownloadSize) || key.equals(prefKeyNumPackets)
        {
            let numBytes: Int =
                // Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.download_size), "1048576"));

                Int(prefs.string(forKey: "download_size") ?? "1048576")! // JT 18.11.06

            let numPackets: Int = // Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.latency_packets), "5"));
                Int(prefs.string(forKey: "latency_packets") ?? "5")! // JT 18.11.06

            let cnt = CloudletListHolder.getSingleton().getCloudletList().count
            for i in 0 ..< cnt
            {
                let list = CloudletListHolder.getSingleton().getCloudletList()

                // let cloudlet: Cloudlet = CloudletListHolder.getSingleton().getCloudletList().valueAt(i);

                let cloudlet = Array(list)[i].value // .valueAt(i);

                cloudlet.setNumBytes(numBytes)
                cloudlet.setNumPackets(numPackets)
            }
        }
    }

    // MARK: -

    public func onResume()
    {
        //  super.onResume();
        // Log.i(TAG, "onResume() mDoLocationUpdates="+mDoLocationUpdates);
        Swift.print("onResume() mDoLocationUpdates= \(mDoLocationUpdates)")
        if mDoLocationUpdates
        {
            // startLocationUpdates();

            getLocaltionUpdates() // JT 18.11.06
        }
    }

    public func onPause()
    {
//        super.onPause();
//        stopLocationUpdates();

        Locator.stopRequest(mLocationRequest2!) // JT 18.11.06
    }

    //   todo?
//    public func onSaveInstanceState(_ savedInstanceState: Bundle) {
//        super.onSaveInstanceState(savedInstanceState);
//        savedInstanceState.putBoolean(MatchingEngine.MEX_LOCATION_PERMISSION, MatchingEngine.isMexLocationAllowed());
//    }
//
//
//    public func onRestoreInstanceState( restoreInstanceState: Bundle) {
//        super.onRestoreInstanceState(restoreInstanceState);
//        if (restoreInstanceState != nil) {
//            MatchingEngine.setMexLocationAllowed(restoreInstanceState.getBoolean(MatchingEngine.MEX_LOCATION_PERMISSION));
//        }
//    }

    /**
     * See documentation for Google's FusedLocationProviderClient for additional usage information.
     */
    private func startLocationUpdates()
    {
        // Log.i(TAG, "startLocationUpdates()");
        Swift.print("startLocationUpdates()")
        // As of Android 23, permissions can be asked for while the app is still running.
//        if (mRpUtil.getNeededPermissions(this).size() > 0) {
//          //  Log.i(TAG, "Location permission has NOT been granted");
//            Swift.print("Location permission has NOT been granted")
//
//            return;
//        }
        // Log.i(TAG, "Location permission has been granted");
        Swift.print("Location permission has been granted")

        if mGoogleMap == nil
        {
            // Log.w(TAG, "Map not ready");
            Swift.print("Map not ready")
            return
        }

        do
        {
            mGoogleMap!.isMyLocationEnabled = true // setMyLocationEnabled(true);

            let interval = 5000 // Initially, 5 second interval to get the first update quickly
            //    Log.i(TAG, "mFusedLocationClient.getLastLocation()="+mFusedLocationClient.getLastLocation()+" interval="+interval);
            Swift.print("LocationRequest")
            // Swift.print("mFusedLocationClient.getLastLocation()= \(mFusedLocationClient.getLastLocation()) interval= \(interval)")

//            mLocationRequest =  LocationRequest();
//            mLocationRequest.setSmallestDisplacement(5);
//            mLocationRequest.setInterval(interval);
//            mLocationRequest.setFastestInterval(interval);
//            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//
//            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

            getLocaltionUpdates() // JT 18.11.06

            // Log.i(TAG, "mFusedLocationClient.requestLocationUpdates() called");
            // Swift.print("mFusedLocationClient.requestLocationUpdates() called")
        }
        catch (se: MyError.SecurityException)
        {
            //  se.printStackTrace();
            // Log.i(TAG, "App should Request location permissions during onCreate().");
            Swift.print("App should Request location permissions during onCreate().")
        }
    }

    private func stopLocationUpdates()
    {
        // mFusedLocationClient.removeLocationUpdates(mLocationCallback);

        Locator.stopRequest(mLocationRequest2!) // JT 18.11.06
    }

    func getLocaltionUpdates()
    {
//        Locator.subscribePosition(accuracy: .house).onUpdate { loc in
//            print("New location received: \(loc)")
//            }.onFail { err, last in
//                print("Failed with error: \(err)")
//        }

        gpsInitialized = true

        mLocationRequest2 = Locator.subscribeSignificantLocations(onUpdate: { newLocation in
            print("New location \(newLocation)")

            if self.gpsInitialized == false
            {
                self.getCloudlets()
            }

        })
        { (err, _) -> Void in
            print("Failed with err: \(err)")
        }
    }

//    let mLocationCallback:LocationCallback =  LocationCallback()
//    {

//        func onLocationResult(_ locationResult: LocationResult) {
//           var locationList = locationResult.getLocations(); // List<Location>
//            if (locationList.size() > 0) {
//                //The last location in the list is the newest
//                let location: Location = locationList.get(locationList.size() - 1);
//                //Log.i(TAG, "onLocationResult() Location: " + location.getLatitude() + " " + location.getLongitude());
//                Swift.print("onLocationResult() Location: \(location.getLatitude() ) \(location.getLongitude()) ")
//                mLastKnownLocation = locationResult.getLastLocation();
//                if(!gpsInitialized) {
//                    getCloudlets();
//                    gpsInitialized = true;
//                }
//
//                if(mLocationRequest.getInterval() < 120000) {
//                    // Now that we got our first update, make interval longer to save battery.
//                   // Log.i(TAG, "Slowing down location request interval");
//                    Swift.print("Slowing down location request interval")
//                    mLocationRequest.setInterval(120000); // two minute interval
//                    mLocationRequest.setFastestInterval(120000);
//                }
//            }
//        }
    // };    // JT 18.11.02

    // MARK: GMSMapViewDelegate

    func mapView(_: GMSMapView, didTapAt coordinate: CLLocationCoordinate2D) // JT 18.11.01
    {
        print("You tapped at \(coordinate.latitude), \(coordinate.longitude)")
    }

    // show more place info when info marker is tapped
    func mapView(_: GMSMapView, didTapInfoWindowOf _: GMSMarker)
    {
        Swift.print("didTapInfoWindowOf") // JT 18.11.01
    }
}

////Here is a small extension for accessing keys and values in dictionary by index:
//
// extension Dictionary {
//    subscript(i:Int) -> (key:Key,value:Value) {
//        get {
//            return self[startIndex.advancedBy(i)]
//        }
//    }
//
//
//
// }
