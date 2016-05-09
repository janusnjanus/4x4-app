package net.we4x4;


import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.appindexing.Action;
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

import org.cloudinary.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**Created by JanusJanus 2016 / janusjanus@riseup.net **/

//TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USING THE APP

/** layout elemetns declaration **/

public class Upload extends AppCompatActivity implements View.OnClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    /** layout elements variables **/

    public static final int RESULT_LOAD_IMAGE = 1;
    ImageView contentToUploadPic;
    VideoView contentToUploadVid;
    String uploadedContentURL, userUploadCount, uploadPublicID, UserID, username, locationTag;
    EditText tagText;
    Button buttonPic, buttonVid, buttonUploadSubmitPic, buttonUploadSubmitVid, buttonLocation;
    Uri RealFilePath;

    /** Firebase elements variables **/

    private Firebase firebaseRefUsersUP, firebaserRefUP, firebaseRefUploadsUP, firebaseRefRatingUP, firebaseRefData;
    public String fileType;
    Firebase.AuthStateListener authStateListenerUP;

    AuthData authDataUP;

    /** GoogleMap elements variables **/

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    public static final String TAG = Upload.class.getSimpleName();
    Location currentLocation;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private LocationRequest mLocationRequest;
    double currentLatitude, currentLongitude;
    long numOfAdrs;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Firebase.setAndroidContext(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Checking Build Version

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        // initializing layout elements and other variables

        contentToUploadPic = null;
        contentToUploadVid = null;
        contentToUploadPic = (ImageView) findViewById(R.id.contentToUploadPic);
        contentToUploadVid = (VideoView) findViewById(R.id.contentToUploadVid);

        tagText = (EditText) findViewById(R.id.tagText);
        buttonUploadSubmitPic = (Button) findViewById(R.id.ButtonUploadSubmitPic);
        buttonUploadSubmitVid = (Button) findViewById(R.id.ButtonUploadSubmitVid);
        buttonLocation = (Button) findViewById(R.id.buttonLocation);

        // setting listener for Location Request Button
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLongitude ==0.0 && currentLatitude ==0.0) {
                    Toast.makeText(getApplicationContext(),
                            "Can't access current location, please enable this option in your device settings"
                            , Toast.LENGTH_SHORT).show();

                    /** Commented code below to request an intent into setting to enable location **/

//                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                    startActivity(intent);
                } else {


                    tagText.setText(currentLatitude + "-" + currentLongitude);
                    locationTag = (currentLatitude + "-" + currentLongitude);

                }
            }
        });

        // initializing layout elements and other variables

        buttonPic = (Button) findViewById(R.id.ButtonPic);
        buttonVid = (Button) findViewById(R.id.ButtonVid);

        fileType = null;

        buttonUploadSubmitPic.setOnClickListener(this);
        buttonUploadSubmitVid.setOnClickListener(this);
        buttonPic.setOnClickListener(this);
        buttonVid.setOnClickListener(this);

        // Firebase Links

        firebaserRefUP = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefUsersUP = new Firebase(getResources().getString(R.string.firebase_users));
        firebaseRefUploadsUP = new Firebase(getResources().getString(R.string.firebase_uploads));
        firebaseRefRatingUP = new Firebase(getResources().getString(R.string.firebase_rating));
        firebaseRefData = new Firebase(getResources().getString(R.string.firebase_data));

        /** firebase authentication listeners **/

        authStateListenerUP = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {

                /** if authentication present setAuthenticatedUser as authData retrieved **/

                if(authData !=null){
                setAuthenticatedUserMM(authData);
                }
                //TODO:if no authentication detected  DO SOMETHING HERE
            }
        };
        /** adding Authentication state listener **/

        firebaserRefUP.addAuthStateListener(authStateListenerUP);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    //TODO: USE BELOW METHOD TO DECLARE DEFAULT MARK ON THE MAP
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    /** layout changes and constants declaration when authentications **/

    private void setAuthenticatedUserMM(AuthData authData) {
        if (authData != null) {

            this.authDataUP = authData;
            UserID = authData.getUid();
            //usernameDisplay.setText(UserID);
            //readingData();
            extractUserInfo();
        }

    }

    /** retrieving user information from firebase **/

    public void extractUserInfo() {
        Firebase ref = new Firebase("https://wi4x4.firebaseio.com/users/" + UserID);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null) {
                    userInformation userInformation = dataSnapshot.getValue(userInformation.class);
                    username = userInformation.getUsername();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }
        });

        /** Count number of addresses logged so far in firebase to number the new location  **/

        firebaserRefUP.child("data").child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                numOfAdrs = (dataSnapshot.getChildrenCount() + 1);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    /** Different cases depedning on selected button in the layout  **/


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ButtonPic:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
                contentToUploadPic.setVisibility(View.VISIBLE);
                contentToUploadVid.setVisibility(View.GONE);
                buttonUploadSubmitPic.setVisibility(View.VISIBLE);
                buttonUploadSubmitVid.setVisibility(View.GONE);


                break;
            case R.id.ButtonVid:
                Intent galleryIntent2 = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent2, RESULT_LOAD_IMAGE);
                contentToUploadVid.setVisibility(View.VISIBLE);
                contentToUploadPic.setVisibility(View.GONE);
                buttonUploadSubmitVid.setVisibility(View.VISIBLE);
                buttonUploadSubmitPic.setVisibility(View.GONE);

                break;
            case R.id.ButtonUploadSubmitPic:
                fileType = "images";
                UploadTask uploadTask = new UploadTask();
                uploadTask.execute(String.valueOf(contentToUploadPic));
                break;

            case R.id.ButtonUploadSubmitVid:
                fileType = "videos";
                uploadTask = new UploadTask();
                uploadTask.execute(String.valueOf(contentToUploadPic));
                break;
        }
    }

    /** onActivity result to interact with user choice of content or canceling  **/


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            super.onActivityResult(requestCode, resultCode, data);
            Toast.makeText(getApplicationContext(), "Canceled ", Toast.LENGTH_SHORT).show();
            buttonUploadSubmitPic.setVisibility(View.GONE);
            buttonUploadSubmitVid.setVisibility(View.GONE);

        } else {
            if (requestCode == RESULT_LOAD_IMAGE) {
                if (resultCode == RESULT_OK) {
                    Uri selectedImgPath = data.getData();
                    contentToUploadPic.setImageURI(selectedImgPath);
                    RealFilePath = Uri.parse(getPath(selectedImgPath));
                    Toast.makeText(getApplicationContext(), " " + RealFilePath, Toast.LENGTH_SHORT).show();
                } else {
                    Uri selectedVidPath = data.getData();
                    contentToUploadVid.setVideoURI(selectedVidPath);
                    RealFilePath = Uri.parse(getPath(selectedVidPath));
                    Toast.makeText(getApplicationContext(), " " + RealFilePath, Toast.LENGTH_SHORT).show();

                }
            }
        }
    }

    /** extracting chosen conten path to be passed into the upload method  **/

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);

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

    // on Start method to activate the Map
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Upload Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://net.we4x4/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    //on pause method to stop google API

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Upload Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://net.we4x4/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
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

    //Checking permission for accessing location and requesting permission if not granted ( commented code )
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
//        }else{
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    ACCESSING_FINE_LOCATION);

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
        Toast.makeText(getApplicationContext(),
                "Location services suspended. Please reconnect."
                , Toast.LENGTH_SHORT).show();
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
        } else {

            /** notifying user of connection failure and reason **/

            Toast.makeText(getApplicationContext(),
                    "Location services connection failed with code " + connectionResult.getErrorCode()
                    , Toast.LENGTH_SHORT).show();
        }
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
//        tagText.setText(currentLatitude + "/" + currentLongitude);
//        locationTag = (currentLatitude + "/" + currentLongitude);

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    /** AsynTask to handle upload**/


    class UploadTask extends AsyncTask<String, Void, Void> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {

            /** starting progress dialog **/

            progressDialog = new ProgressDialog(Upload.this);
            progressDialog.setTitle("Upload in Progress ...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {

            /** commented code for Cloudinary Admin API to implement extra privileges  **/

            //TODO:Adding the api key and api secret provided in cloudinary dashboard
/*
ADMIN API for extra options, such as deleting content, transformations, etc.
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "we4x4",
                    "api_key", "xxxxxxxxxxx",
                    "api_secret", "xxxxxxxxxxxxxxx"));
            try{
            Map result = cloudinary.uploader().upload("" + RealFilePath, ObjectUtils.asMap(
                    "tags", UserID, "resource_type", "auto"));
                uploadedContentURL = (String) result.get("url");
                uploadPublicID = (String) result.get("public_id");
                JSONObject result = new JSONObject(cloudinary.uploader().unsignedUpload(RealFilePath, "frtkzlwz",
                        ObjectUtils.emptyMap()));
*/

            /** configuring cloud information for unsigned upload**/
            Map config = new HashMap();
            config.put("cloud_name", "we4x4");
            Cloudinary cloudinary = new Cloudinary(config);

            try {

                /** Creating Strings Array to pass Tags with the uploaded content**/

                String[] tags;
                tags = new String[3];
                tags[0] = new String("uploads");
                tags[1] = new String(UserID);
                if(locationTag !=null){
                    tags[2] = new String(locationTag);
                }

                /** configuring content information for unsigned upload with tags **/

                JSONObject result = new JSONObject(cloudinary.uploader().unsignedUpload("" + RealFilePath, "frtkzlwz",
                        ObjectUtils.asMap("tags", tags, "resource_type", "auto")));

                uploadedContentURL = (String) result.get("url");
                uploadPublicID = (String) result.get("public_id");

            } catch (IOException e) {
                e.printStackTrace();

                /** handling errors **/

                progressDialog.setMessage("Error uploading file");
                progressDialog.hide();

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            /** updating the information on firebase and recodring new information for last upload **/

            final Long[] currentRank = new Long[1];
            progressDialog.hide();
            firebaseRefUploadsUP.child(fileType).child(UserID).child(uploadPublicID).setValue(uploadedContentURL);
            firebaseRefRatingUP.child(fileType).child(UserID).child(uploadPublicID).setValue(0);
            firebaseRefData.child(fileType).child(uploadPublicID).child("author").setValue(username);
            firebaseRefData.child(fileType).child(uploadPublicID).child("author_id").setValue(UserID);
            firebaseRefData.child(fileType).child(uploadPublicID).child("rating").setValue(0);
            firebaseRefData.child(fileType).child(uploadPublicID).child("address").setValue(uploadedContentURL);
            if(locationTag != null){

                firebaseRefData.child(fileType).child(uploadPublicID).child("location").setValue(locationTag);
                firebaserRefUP.child("data").child("locations").child("location"+numOfAdrs).child("author").setValue(UserID);
                firebaserRefUP.child("data").child("locations").child("location"+numOfAdrs).child("latitude").setValue(currentLatitude);
                firebaserRefUP.child("data").child("locations").child("location"+numOfAdrs).child("longitude").setValue(currentLongitude);
                firebaserRefUP.child("locations").child("location"+numOfAdrs).setValue(locationTag);
            }else{
                firebaseRefData.child(fileType).child(uploadPublicID).child("location").setValue(0);

            }

            firebaseRefUsersUP.child(UserID).child("rank").runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData currentData) {
                    if (currentData.getValue() == null) {
                        currentData.setValue(1);
                        currentRank[0] = (Long) currentData.getValue();
                    } else {
                        currentData.setValue((Long) currentData.getValue() + 1);
                        currentRank[0] = (Long) currentData.getValue();

                    }
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {
                    if (firebaseError != null) {
                        Toast.makeText(getApplicationContext(), "Error updating your information", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Content have been uploaded", Toast.LENGTH_SHORT).show();
                        tagText.setText("");
                        locationTag = "0";

                    }
                }
            });

            super.onPostExecute(aVoid);
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /** setting up menu - action bar  **/


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
        if (this.authDataUP != null) {

            firebaserRefUP.unauth();
            //usernameDisplay.setVisibility(View.GONE);
            authDataUP = null;
            switchToMainActivity();
        }
        setAuthenticatedUserMM(null);
    }

    /** switching to different activities classes **/

    public void switchToUpload(View view) {
        Intent Upload = new Intent(this, Upload.class);
        startActivity(Upload);
    }

    public void switchToMainActivity() {
        Intent MainActivity = new Intent(this, MainActivity.class);
        startActivity(MainActivity);
    }


}