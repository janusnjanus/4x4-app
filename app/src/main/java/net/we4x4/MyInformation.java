package net.we4x4;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**Created by JanusJanus 2016 / janusjanus@riseup.net **/

//TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USING THE APP

public class MyInformation extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    /** Layout elemnts **/
    SwipeRefreshLayout swipeLayout;

    String UserID;
   public String Username;
   public String Email;
    public long uploadCountImg,uploadCountValueVid;

    public int intUploadCountValuePic, intUploadCountValueVid, intMaxRatingImg,
            intMaxRatingVid, intMaxRating, intUploadCountValuetotal;
    public Long maxRatingPic,maxRatingVid;

    EditText editTextPassChEntryOld, editTextPassChEntryNew, editTextEmailChEntryOld
            , editTextEmailChEntryNew, editTextPasswordConfirm;

    Button buttonPasswordlCh,buttonEmailCh,buttonPassReset;

    TextView textViewUsername,textViewEmailAddress, textViewUploadedContentValue, textViewRank
            , textViewUploadedVidValue, textViewUploadedImgValue;

    /** Firebase elements **/
    Firebase firebaseRefUsersMI,firebaserRefMI,firebaseRefUploadsMI;
    Firebase.AuthStateListener authStateListenerMI;
    AuthData authDataMI;

    ProgressDialog progressDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_information);

        /** Declaring & Setting up the SwipeRefresh Layout**/

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        /** Setting up Layout Elements **/

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Authenticating in Progress ...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        textViewUsername = (TextView)findViewById(R.id.textViewUsername);
        textViewEmailAddress = (TextView)findViewById(R.id.textViewEmailAddress);
        textViewUploadedContentValue =(TextView)findViewById(R.id.textViewUploadedContentValue);
        editTextPassChEntryOld = (EditText)findViewById(R.id.editTextPassChEntryOld);
        editTextPassChEntryNew =(EditText)findViewById(R.id.editTextPassChEntryNew);
        editTextEmailChEntryOld = (EditText)findViewById(R.id.editTextEmailChEntryOld);
        editTextEmailChEntryNew = (EditText) findViewById(R.id.editTextEmailChEntryNew);
        editTextPasswordConfirm = (EditText)findViewById(R.id.editTextPasswordConfirm);
        textViewRank = (TextView)findViewById(R.id.textViewRank);
        textViewUploadedVidValue =(TextView)findViewById(R.id.textViewUploadedVidValue);
        textViewUploadedImgValue = (TextView)findViewById(R.id.textViewUploadedImgValue);
        buttonPassReset = (Button) findViewById(R.id.buttonPassReset);

        /** setting up reset password button **/
        buttonPassReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                Firebase ref = new Firebase("https://wi4x4.firebaseio.com");
                ref.resetPassword(textViewEmailAddress.toString(), new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        // password reset email sent
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                "Check your Email for instructions"
                                , Toast.LENGTH_LONG).show();
                        logout();
                    }
                    @Override
                    public void onError(FirebaseError firebaseError) {
                        // error encountered
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                "Error, could not send an password reset email, please contact Admin."
                                , Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        /** setting up password change button **/
        buttonPasswordlCh = (Button)findViewById(R.id.buttonPasswordlCh);
        buttonPasswordlCh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                String emailAddress = textViewEmailAddress.getText().toString();
                String oldPass = editTextPassChEntryOld.getText().toString();
                String newPass = editTextPassChEntryNew.getText().toString();
                Firebase ref = new Firebase("https://wi4x4.firebaseio.com");
                ref.changePassword(emailAddress, oldPass, newPass, new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        // password changed
                        Toast.makeText(getApplicationContext(),
                                "New password is " + editTextPassChEntryNew.getText().toString()
                                , Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                        logout();
                    }
                    @Override
                    public void onError(FirebaseError firebaseError) {
                        // error encountered
                        Toast.makeText(getApplicationContext(),
                                "Error, could not change password, please try again"
                                , Toast.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }
                });
            }
        });

        /** setting up Email change button **/
        buttonEmailCh = (Button)findViewById(R.id.buttonEmailCh);
        buttonEmailCh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                Firebase ref = new Firebase("https://wi4x4.firebaseio.com");
                ref.changeEmail(editTextEmailChEntryOld.getText().toString(), editTextPasswordConfirm.getText().toString(),
                        editTextEmailChEntryNew.getText().toString(), new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        // email changed
                        firebaseRefUsersMI.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                firebaseRefUsersMI.child(UserID.toString()).child("email").setValue(editTextEmailChEntryNew.getText().toString());
                                Toast.makeText(getApplicationContext(),
                                        "New email is " + editTextEmailChEntryNew.getText().toString()
                                        , Toast.LENGTH_LONG).show();
                                editTextEmailChEntryOld.setText("");
                                editTextEmailChEntryNew.setText("");
                                editTextPasswordConfirm.setText("");
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                        Toast.makeText(getApplicationContext(),
                                                "Error, Please contact Admin to update your Information"
                                                , Toast.LENGTH_LONG).show();
                                        progressDialog.dismiss();
                            }
                        });
                    }

                    @Override
                    public void onError(FirebaseError firebaseError) {
                        // error encountered
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                "Error, could not change email, please try again"
                                , Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        /** firebase links shortcuts **/

        firebaserRefMI = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefUsersMI = new Firebase(getResources().getString(R. string.firebase_users));
        firebaseRefUploadsMI = new Firebase(getResources().getString(R. string.firebase_uploads));

        /** firebase Authentication listener  **/

        authStateListenerMI = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if(authData !=null){
                    setAuthenticatedUserMI(authData);
                }else{
                    textViewUsername.setText("Please Login");
                }
            }
        };

        /** adding Authentication state listener **/

        firebaserRefMI.addAuthStateListener(authStateListenerMI);

    }
    /** When user authenticated update layout & request user Info. **/

    private void setAuthenticatedUserMI(AuthData authData){
        if(authData != null) {

            this.authDataMI = authData;
            UserID = authData.getUid();
            getUserInfo();
            textViewUsername.setText(UserID);
            getUserInfo();
            uploadCount();
            ratingCountImg();
            ratingCountVid();
            rankCountMI();
        }

    }
    /** extracting authenticated user Ifno. **/
    //TODO: firebase link should be changed in case of using a different account

    public void getUserInfo(){
        Firebase refUserInfo = new Firebase("https://wi4x4.firebaseio.com/users/" + UserID);
        refUserInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userInformation userInformation = dataSnapshot.getValue(userInformation.class);
                textViewUsername.setText(userInformation.getUsername());
                textViewEmailAddress.setText(userInformation.getEmail());
                textViewRank.setText(userInformation.getRank().toString());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    /**  calculating uploaded content number**/

    //TODO: firebase link should be changed in case of using a different account
    public void uploadCount(){
        Firebase refUploadCountImg = new Firebase("https://wi4x4.firebaseio.com/uploads/images/"+UserID);

        // IMAGES
        refUploadCountImg.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot !=null) {
                    uploadCountImg = snapshot.getChildrenCount();
                    intUploadCountValuePic = (int) uploadCountImg;
                }else{
                    textViewUploadedContentValue.setText("0");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                    firebaseError.getMessage();
            }
        });

        // VIDEOS
        //TODO: firebase link should be changed in case of using a different account

        Firebase refUploadCountVid = new Firebase("https://wi4x4.firebaseio.com/uploads/videos/"+UserID);
        refUploadCountVid.addValueEventListener(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot !=null) {
                    uploadCountValueVid = snapshot.getChildrenCount();
                    intUploadCountValueVid = (int) uploadCountValueVid;
                    textViewUploadedContentValue.setText(Integer.toString(intUploadCountValueVid + intUploadCountValuePic));
                }else{
                    textViewUploadedContentValue.setText("0");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }

        });

        // SUM OF IMAGES & VIDEOS UPLOADED CONTENT
        intUploadCountValuetotal = intUploadCountValuePic + intUploadCountValueVid;
    }

    /** getting top rating value of images **/
    public void ratingCountImg(){

        //TODO: firebase link should be changed in case of using a different account

        Firebase refRankCountImg = new Firebase("https://wi4x4.firebaseio.com/rating/images/"+UserID);
        refRankCountImg.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Long, Long> ratingImg = (HashMap<Long, Long>) snapshot.getValue();
                if (ratingImg != null) {
                    Map<Long, Long> ratings = ratingImg;
                    Collection<Long> imgBulkValues = ratings.values();
                    ArrayList<Long> values = new ArrayList<>();
                    values.addAll(imgBulkValues);
                    maxRatingPic = Collections.max(values);
                    intMaxRatingImg = maxRatingPic.intValue();
                    textViewUploadedImgValue.setText(maxRatingPic.toString());
                } else {
                    textViewUploadedImgValue.setText("0");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    /** getting top rating value of videos **/

    public void ratingCountVid(){
        //TODO: firebase link should be changed in case of using a different account

        Firebase refRankCountVid = new Firebase("https://wi4x4.firebaseio.com/rating/videos/"+UserID);
        refRankCountVid.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Long, Long> ratingVid = (HashMap<Long, Long>) snapshot.getValue();
                if (ratingVid != null) {
                    Map<Long, Long> ratings = ratingVid;
                    Collection<Long> vidBulkValues = ratings.values();
                    ArrayList<Long>  values = new ArrayList<>();
                    values.addAll(vidBulkValues);
                    maxRatingVid = Collections.max(values);
                    intMaxRatingVid = maxRatingPic.intValue();
                    textViewUploadedVidValue.setText(maxRatingVid.toString());
                } else {
                    textViewUploadedVidValue.setText("0");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }

    /** calculating rank  **/
    public void rankCountMI() {
        //TODO: firebase link should be changed in case of using a different account

        // IMAGES
        Firebase refUploadCountImg = new Firebase("https://wi4x4.firebaseio.com/uploads/images/" + UserID);
        refUploadCountImg.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot != null) {
                    uploadCountImg = snapshot.getChildrenCount();
                    intUploadCountValuePic = (int) uploadCountImg;
                } else {


                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }
        });

        //TODO: firebase link should be changed in case of using a different account

        Firebase refUploadCountVid = new Firebase("https://wi4x4.firebaseio.com/uploads/videos/" + UserID);
        refUploadCountVid.addValueEventListener(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot != null) {
                    uploadCountValueVid = snapshot.getChildrenCount();
                    intUploadCountValueVid = (int) uploadCountValueVid;


                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error reading data on firebase"
                            , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }

        });

        // IMAGES
        intUploadCountValuetotal = intUploadCountValuePic + intUploadCountValueVid;
        Firebase refRankCountImg = new Firebase("https://wi4x4.firebaseio.com/rating/images/"+UserID);
        refRankCountImg.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Long, Long> ratingImg = (HashMap<Long, Long>) snapshot.getValue();
                if (ratingImg != null) {
                    Map<Long, Long> ratings = ratingImg;
                    Collection<Long> imgBulkValues = ratings.values();
                    ArrayList<Long> values = new ArrayList<>();
                    values.addAll(imgBulkValues);
                    maxRatingPic = Collections.max(values);
                    intMaxRatingImg = maxRatingPic.intValue();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error reading data on firebase"
                            , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

        //TODO: firebase link should be changed in case of using a different account

        // VIDEOS
        Firebase refRankCountVid = new Firebase("https://wi4x4.firebaseio.com/rating/videos/"+UserID);
        refRankCountVid.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Long, Long> ratingVid = (HashMap<Long, Long>) snapshot.getValue();
                if (ratingVid != null) {
                    Map<Long, Long> ratings = ratingVid;
                    Collection<Long> vidBulkValues = ratings.values();
                    ArrayList<Long>  values = new ArrayList<>();
                    values.addAll(vidBulkValues);
                    maxRatingVid = Collections.max(values);
                    intMaxRatingVid = maxRatingPic.intValue();

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error reading data on firebase"
                            , Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    /** Actions whe user log out; null user Info. and clear layout elements related to user **/

    private void logout() {
        if (this.authDataMI != null) {

            firebaserRefMI.unauth();
            textViewUsername.setVisibility(View.GONE);
            authDataMI = null;
            switchToMainActivity();
        }
        setAuthenticatedUserMI(null);
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

    /** switching to different activities classes **/

    public void switchToMyUploadedVideos(View view){
        Intent MyUploadedVideos = new Intent(this, net.we4x4.MyUploadedVideos.class);
        startActivity(MyUploadedVideos);
    }
    public void switchToMyUploadedImages(View view){
        Intent MyUploadedImages = new Intent(this, net.we4x4.MyUploadedImages.class);
        startActivity(MyUploadedImages);
    }
    public void switchTochatsection(View view){
        Intent chatSection = new Intent(this, net.we4x4.chatSection.class);
        startActivity(chatSection);
    }

    public void switchToUpload(View view){
        Intent Upload = new Intent(this, net.we4x4.Upload.class);
        startActivity(Upload);
    }

    public void switchToMainActivity(){
        Intent MainActivity = new Intent(this, net.we4x4.MainActivity.class);
        startActivity(MainActivity);
    }

    @Override
    public void onRefresh() {
        // TODO Auto-generated method stub
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                swipeLayout.setRefreshing(false);
            }
        }, 5000);
    }
}
