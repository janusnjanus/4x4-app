package net.we4x4;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**Created by JanusJanus 2016 / janusjanus@riseup.net **/

//TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USING THE APP

/** layout elements declaration **/

public class GPSlocations extends AppCompatActivity implements View.OnClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


    String UserID, locationTag;
    EditText editTextCurrentLocation;
    Button buttonListLocations, buttonLogLocation, buttonSaveLoc;
    ListView listViewLocations;
    private ArrayAdapter<Long> listAdapter ;
    ArrayList<Long> addressList = new ArrayList<Long>();

    long numOfAdrs;

    /** GoogleMap elements variables **/

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = Upload.class.getSimpleName();
    Location currentLocation;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;
    double currentLatitude;
    double currentLongitude;
    private GoogleApiClient client;

    /** Firebase elements variables **/

    private Firebase firebaseRefUsersLC, firebaserRefLC;
    Firebase.AuthStateListener authStateListenerLC;
    AuthData authDataLC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.gps_locations);

        // initializing layout elements and other variables

        buttonListLocations = (Button) findViewById(R.id.buttonListLocations);
        buttonListLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                listAddresses();

            }
        });

        /** Initialising log location button and check of Lang. Lat. available **/


        buttonLogLocation = (Button) findViewById(R.id.buttonLogLocation);
        buttonLogLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLongitude ==0.0 && currentLatitude ==0.0) {
                    Toast.makeText(getApplicationContext(),
                            "Can't access current location, please enable this option in your device settings"
                            , Toast.LENGTH_SHORT).show();

                    //TODO; activate the code below to add option of switching to setting and activate location listener
//                    ActivityCompat.requestPermissions(Upload.this,
//                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                            ACCESSING_FINE_LOCATION);
//                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    startActivity(intent);
                } else {

                    editTextCurrentLocation.setText(locationTag);

                }
            }
        });

        /** Initialising save location button and check # of saved addresses to attach correct address to the new location **/

        buttonSaveLoc = (Button)findViewById(R.id.buttonSaveLoc);
        buttonSaveLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /** extracting # of addresses from Firebase **/

                firebaserRefLC.child("data").child("locations").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        numOfAdrs = (dataSnapshot.getChildrenCount() + 1);
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });

                /** saving new location to firebase in different locations **/

                firebaserRefLC.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        firebaserRefLC.child("data").child("locations").child("location"+numOfAdrs).child("author").setValue(UserID);
                        firebaserRefLC.child("data").child("locations").child("location"+numOfAdrs).child("latitude").setValue(currentLatitude);
                        firebaserRefLC.child("data").child("locations").child("location"+numOfAdrs).child("longitude").setValue(currentLongitude);
                        firebaserRefLC.child("locations").child("location"+numOfAdrs).setValue(locationTag);

//                        firebaserRefLC.child("locations").child("location"+numOfAdrs).setValue(curr);

                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {

                    }
                });
            }
        });

        /** initialising other layout elements **/

        editTextCurrentLocation = (EditText) findViewById(R.id.editTextCurrentLocation);
        listViewLocations = (ListView) findViewById(R.id.listViewLocations);

        /** setting up ArrayList to save extracted address to be passed to the listView **/

        ArrayList<Long>  addresses = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapLocations);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        /** Check build version to see if permission granted and google map compatibility **/


        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }


        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        /** Firebase Links **/


        firebaserRefLC = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefUsersLC = new Firebase(getResources().getString(R.string.firebase_users));

        /** firebase authentication listeners **/

        authStateListenerLC = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                //if(authData !=null){
                setAuthenticatedUserLC(authData);

                /** if authentication present setAuthenticatedUser as authData retrieved **/

                //}
                //TODO:if no authentication detected  DO SOMETHING HERE, uncomment if statement

            }
        };
        /** adding Authentication state listener **/

        firebaserRefLC.addAuthStateListener(authStateListenerLC);

    }

    /** layout changes and constants declaration when authentications **/

    private void setAuthenticatedUserLC(AuthData authData) {
        if (authData != null) {

            this.authDataLC = authData;
            UserID = authData.getUid();

        }

    }

    /** listAddress method to extract all saved addresses from Firebase  **/

    private void listAddresses() {
        Firebase ref = new Firebase("https://wi4x4.firebaseio.com/locations");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                    Map<Long, Long> allADR = (HashMap<Long, Long>) snapshot.getValue();
                    if (allADR != null) {
                        Collection<Long> BlkADR = allADR.values();
                        addressList.addAll(BlkADR);
//                        Log.i("myTag", addressList.toString());

                    }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        /** setting up the list view with list adapter **/


        listAdapter = new ArrayAdapter<Long>(this, R.layout.simplerow, addressList);
        listViewLocations.setAdapter(listAdapter);

    }

    //TODO: envoke this method if you want the market for the default location to be marked
        private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }


    @Override
    protected void onResume() {
        super.onResume();
//        setUpMapIfNeeded();

        // connecting to google API to initiate the Map

        mGoogleApiClient.connect();
    }

    //on pause method to stop google API and location listener

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    //Checking permission for accessing location and requesting permission if not granted ( commented code )

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(getApplicationContext(),
//                    "Can't access current location, please enable this option in your device settings"
//                    , Toast.LENGTH_SHORT).show();

            //TODO: uncomment code above to inform user of current settings and request changes
            return;
        }

        /** Location listener declaration and retrieving current location **/

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        currentLocation = location;
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }
    }

    /** notifying user of connection suspension **/

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }

            /** notifying user of connection failure and reason **/

        } else {
             Toast.makeText(getApplicationContext(),
                     "Location services connection failed with code " + connectionResult.getErrorCode()
                     , Toast.LENGTH_SHORT).show();
        }
    }

    //onMapReady method to plot default location
    //TODO:Change default location

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    /** handeling new location and passing Longitude and Latitude **/

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");
        mMap.addMarker(options);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//        editTextCurrentLocation.setText(currentLatitude + "-" + currentLongitude);
        locationTag= (currentLatitude + "-" + currentLongitude);

    }

    /** setting up menu - action bar  **/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                switchToMainActivity();
                return true;

            case R.id.action_about:
                Toast.makeText(getApplicationContext(),
                        "wi4x4 v 1.0 developed by A.A./JanusJanus@riseup.net"
                        , Toast.LENGTH_LONG).show();

            case R.id.action_logout:
                logout();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    /** Actions whe user log out; null user Info.  **/

    private void logout() {
        if (this.authDataLC != null) {

            firebaserRefLC.unauth();
            authDataLC = null;
            switchToMainActivity();
        }
        setAuthenticatedUserLC(null);
    }

    /** switching to different activities classes **/


    public void switchToMainActivity() {
        Intent MainActivity = new Intent(this, MainActivity.class);
        startActivity(MainActivity);
    }
}
