package net.we4x4;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.MutableData;
import com.firebase.client.Transaction;
import com.firebase.client.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import net.we4x4.models.uploadedContentModels;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**Created by JanusJanus 2016 / janusjanus@riseup.net **/

//TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USING THE APP

/** layout elemetns declaration **/


public class Members  extends RegisterSection implements SwipeRefreshLayout.OnRefreshListener {

    SwipeRefreshLayout swipeLayout;

    /** layout elements variables **/

    TextView usernameDisplay, uploadedFilesValue, rankValue;
    public Button buttonUpload, buttonChat,buttonMyUploads, buttonMyUploadedVid, buttonMyInfo,
            buttonGPS;
    ListView listViewRow;

    String UserID,Username,uploadList, topContRefImg;
    public int intUploadCountValue, intUploadCountValuePic,
            intUploadCountValueVid, intUploadCountValuetotal;
    public int ratingNew;
    public long maxRatingPic,maxRatingVid;
    public Long maxRating;
    public String author,author_id;

    /** firebase links & variables **/

    Firebase firebaseRefUsersMM, firebaseDataRef, firebaserRefMM, firebaseRefUploadsMM;
    Firebase.AuthStateListener authStateListenerMM;
    AuthData authDataMM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.members);

        /** Declaring & Setting up the SwipeRefresh Layout**/

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        /** firebase links **/
        firebaserRefMM = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefUsersMM = new Firebase(getResources().getString(R. string.firebase_users));
        firebaseRefUploadsMM = new Firebase(getResources().getString(R. string.firebase_uploads));
        firebaseDataRef = new Firebase(getResources().getString(R. string.firebaseData));


        UserID = null;
        Username= null;
        uploadList = null;
        intUploadCountValue = 0;
        maxRating = null;

        /** layout elements setters **/

        usernameDisplay = (TextView) findViewById(R.id.textViewUsername);
        rankValue = (TextView) findViewById(R.id.rankValue);
        uploadedFilesValue = (TextView) findViewById(R.id. uploadedFilesValue);
        buttonChat = (Button) findViewById(R.id.buttonChat);
        buttonUpload = (Button) findViewById(R.id.buttonUpload);
        buttonMyUploads = (Button)findViewById(R.id.buttonMyUploads);
        buttonMyUploadedVid = (Button)findViewById(R.id.buttonMyUploadedVid);
        buttonMyInfo = (Button)findViewById(R.id.buttonMyInfo);
        listViewRow = (ListView) findViewById(R.id.listViewRow);
        buttonGPS = (Button) findViewById(R.id.buttonGPS);

        /** firebase authentication listeners **/

        authStateListenerMM = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {

                /** if authentication present setAuthenticatedUser as authData retrieved **/

                if(authData !=null){
                    setAuthenticatedUserMM(authData);
                }else{
                    /**if no authentication detected null user info, and clear layout elements
                     related to logged in user;**/

                    usernameDisplay.setText("Please Login");
                }
            }
        };
        /** adding Authentication state listener **/

        firebaserRefMM.addAuthStateListener(authStateListenerMM);

    }

    /** layout changes and constants declaration when authentications **/

    private void setAuthenticatedUserMM(AuthData authData){
        if(authData != null) {

            this.authDataMM = authData;
            UserID = authData.getUid();
            readingData();
            uploadCountMM();
            rankCountMM();
            new JsonTask().execute("http://res.cloudinary.com/we4x4/image/list/uploads.json");

        }

    }

    /** retrieving user information from firebase **/

    public void readingData() {
        Firebase ref = new Firebase("https://wi4x4.firebaseio.com/users/" + UserID);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userInformation userInformation = dataSnapshot.getValue(userInformation.class);
                String username = userInformation.getUsername();
                Long rank = userInformation.getRank();
                rankValue.setText(rank.toString());
                usernameDisplay.setText(username);
            }
            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }
        });
    }

    /** Calculating number of uploaded content by the logged in user  **/

    public void rankCountMM(){

        /** choosing top rating of content (Images) **/

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
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

        /** choosing top rating of content (Videos) **/

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

                        /** Updating Rank based on top ratings of contents  **/
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

        firebaseRefUsersMM.child(UserID).child("rank").runTransaction(new Transaction.Handler() {
            final Long[] currentRank = new Long[1];


            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                Long updatedRank = (Long) currentData.getValue() + maxRatingVid + maxRatingPic;
                currentData.setValue(updatedRank);
                rankValue.setText(updatedRank.toString());
                return null;
            }

            @Override
            public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {

            }
        });
    }
    /** Retrieving number of uploaded content by user  **/

    //Images;

    public void uploadCountMM(){
        Firebase refUploadCountImg = new Firebase("https://wi4x4.firebaseio.com/uploads/images/"+UserID);
        refUploadCountImg.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot !=null) {
                    long uploadCountImg = snapshot.getChildrenCount();
                    intUploadCountValuePic = (int) uploadCountImg;
                }else{
                    uploadedFilesValue.setText("0");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }
        });

        //Videos;

        Firebase refUploadCountVid = new Firebase("https://wi4x4.firebaseio.com/uploads/videos/"+UserID);
        refUploadCountVid.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(snapshot !=null) {
                    long uploadCountValueVid = snapshot.getChildrenCount();
                    intUploadCountValueVid = (int) uploadCountValueVid;
                    uploadedFilesValue.setText(Integer.toString(intUploadCountValueVid + intUploadCountValuePic));
                }else{
                    uploadedFilesValue.setText("0");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }

        });
    }

    /** Reaction when refreshed/ when swiped **/

    @Override
    public void onRefresh() {
        // TODO Auto-generated method stub
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                swipeLayout.setRefreshing(false);
            }
        }, 5000);
    }

    /** AsyncTask to extract information of top content **/

    public class JsonTask extends AsyncTask<String, String, List<uploadedContentModels> > {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<uploadedContentModels> doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {

                /** establishing a connection to retrieve Json**/

                URL url = new URL(params[0]); //Json task file address is passed in the method
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();

                /** creating Buffer to read Json provided by Cloudinary **/

                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuffer buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                String finalJson = buffer.toString();

                /** After converting the buffer to string, JSONObject & JSONArray created **/

                JSONObject parentObject = new JSONObject(finalJson);
                JSONArray parentArray = parentObject.getJSONArray("resources");

                /** Creating a List for the extracted  information **/

                List<uploadedContentModels> upImgList = new ArrayList<>();

                for(int i=0; i<parentArray.length(); i++) {
                    JSONObject finalObject = parentArray.getJSONObject(i);

                    /** setter for the Model used to extract data from Json**/

                    uploadedContentModels upImgModels = new uploadedContentModels();

                    /** setters for the created Model above elements **/

                    upImgModels.setPublic_id(finalObject.getString("public_id"));
                    upImgModels.setVersion(finalObject.getString("version"));
                    topContRefImg = finalObject.getString("public_id");
                    upImgModels.setFormat(finalObject.getString("format"));

                    /** constructing content address on cloudinary from the elements above **/

                    upImgModels.setAddress("http://res.cloudinary.com/we4x4/image/"
                            + "upload/v" + finalObject.getString("version") + "/"
                            + finalObject.getString("public_id") + "." +
                            finalObject.getString("format"));

                    upImgList.add(upImgModels);
                }
                return upImgList;
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(final List<uploadedContentModels> result) {
            super.onPostExecute(result);
            if(result !=null) {

                /** setting up an adapter for the view **/

                upImgAdapter adapter = new upImgAdapter(getApplicationContext(), R.layout.row_members, result);
                listViewRow.setAdapter(adapter);
                setListViewHeightBasedOnChildren(listViewRow);

            }else{
                Toast.makeText(getApplicationContext(), "No content uploaded / Connection Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** setting up ArrayAdapter**/

    public class upImgAdapter extends ArrayAdapter {

        public List<uploadedContentModels> upImgModelsList;
        private int resource;
        private LayoutInflater inflater;

        public upImgAdapter(Context context, int resource,
                            List<uploadedContentModels> objects) {
            super(context, resource, objects);
            upImgModelsList = objects;
            this.resource = resource;
            inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        }

        /** getting the view ref. to be implemented in the listview & declaring layout elements **/

        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            if(convertView == null){
                convertView = inflater.inflate(resource, null);
            }

            /** passing the image to the UIL **/

            ImageView imageViewDisplayMemebers;
            imageViewDisplayMemebers = (ImageView)convertView.findViewById(R.id.imageViewDisplayMemebers);

            ImageLoader.getInstance().displayImage(upImgModelsList.get(position).getAddress(), imageViewDisplayMemebers);

            /** Extract image information, author, and rating **/

            final String imgToBeRatedRef = upImgModelsList.get(position).getPublic_id();

            final TextView authorName = (TextView) convertView.findViewById(R.id.authorName);

            firebaseDataRef.child("images").child(imgToBeRatedRef).child("author").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    authorName.setText(dataSnapshot.getValue().toString());
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

            final TextView currentRatingValue = (TextView) convertView.findViewById(R.id.currentRatingValue);

            firebaseDataRef.child("images").child(imgToBeRatedRef).child("rating").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    currentRatingValue.setText(dataSnapshot.getValue().toString());
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

            /** Setting up the rating bar, and set a listener for rating to be submitted **/

            final RatingBar ratingBar;
            ratingBar = (RatingBar)convertView.findViewById(R.id.ratingBar);
            ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {

                    ratingNew = (int)ratingBar.getRating();
                }
            });

            /** Setting up rating submit button, and submitting rating to firebase onClick **/
            Button buttonRatingSubm;
            buttonRatingSubm = (Button)convertView.findViewById(R.id.buttonRatingSubm);
            buttonRatingSubm.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v){
                    firebaseDataRef.child("images").child(imgToBeRatedRef).child("author").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            author = dataSnapshot.getValue().toString();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });

                    firebaseDataRef.child("images").child(imgToBeRatedRef).child("author_id").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            author_id = dataSnapshot.getValue().toString();
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });

                    /** Checking if the user voted before **/

                        firebaseDataRef.child("images").child(imgToBeRatedRef).child(UserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(final DataSnapshot dataSnapshot) {
                          boolean userVotStat = dataSnapshot.exists();
                            //if user voted before display the following message;
                            if(userVotStat){
                                Toast.makeText(getApplicationContext(),
                                        "Sorry, you can't vote more than once"
                                        , Toast.LENGTH_SHORT).show();
                            }else{
                                //if user did not vote before execute Transaction/ save rating

                                /** Rating is saved in two different location on firebase
                                 * One; under the image specific folder with all other information
                                 * such as author, authur_id, etc
                                 * Two; under the Rating only folder for faster rating value retrieval**/

                            //Saving submitted rating to the specific image folder
                                firebaseDataRef.child("images").child(imgToBeRatedRef).child("rating").runTransaction(new Transaction.Handler() {

                                    @Override
                                    public Transaction.Result doTransaction(MutableData currentData) {
                                        if(currentData.getValue() == null){
                                            currentData.setValue(ratingNew);
                                            firebaseRefUsersMM.child(author_id).child("rank").runTransaction(new Transaction.Handler() {
                                                @Override
                                                public Transaction.Result doTransaction(MutableData mutableData) {
                                                    mutableData.setValue((Long) mutableData.getValue() + 1);

                                                    return Transaction.success(mutableData);
                                                }

                                                @Override
                                                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {

                                                }
                                            });
                                            //Saving submitted rating to the Rating folder
                                            firebaserRefMM.child("rating").child("images").child(author_id).child(imgToBeRatedRef).runTransaction(new Transaction.Handler() {
                                                @Override
                                                public Transaction.Result doTransaction(MutableData mutableData) {
                                                    mutableData.setValue((Long) mutableData.getValue() + ratingNew);

                                                    return Transaction.success(mutableData);
                                                }

                                                @Override
                                                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {

                                                }
                                            });


                                        }else {
                                            currentData.setValue((Long) currentData.getValue() + ratingNew);

                                            firebaseDataRef.child("images").child(imgToBeRatedRef).child(UserID).setValue(ratingNew);
                                            firebaserRefMM.child("rating").child("images").child(author_id).child(imgToBeRatedRef).setValue((Long) currentData.getValue() + ratingNew);

                                            firebaseRefUsersMM.child(author_id).child("rank").runTransaction(new Transaction.Handler() {
                                                @Override
                                                public Transaction.Result doTransaction(MutableData mutableData) {
                                                    mutableData.setValue((Long) mutableData.getValue() + 1);

                                                    return Transaction.success(mutableData);
                                                }

                                                @Override
                                                public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {

                                                }
                                            });

                                        }
                                        return Transaction.success(currentData);
                                    }

                                    @Override
                                    public void onComplete(FirebaseError firebaseError, boolean b, DataSnapshot dataSnapshot) {

                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });

                }
            });

                return convertView;
        }
    }

    /** Configure the layout to be compatible with the posted images **/

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    /** Actions whe user log out; null user Info. and clear layout elements related to user **/

    private void logout() {
        if (this.authDataMM != null) {

            firebaserRefMM.unauth();
            usernameDisplay.setVisibility(View.GONE);
            authDataMM = null;
            switchToMainActivity();
        }
        setAuthenticatedUserMM(null);
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

    /** switching to different activities classes **/

    public void switchToMyInfo(View view){
        Intent MyInformation = new Intent(this, MyInformation.class);
        startActivity(MyInformation);
    }
    public void switchToMyUploadedVideos(View view){
        Intent MyUploadedVideos = new Intent(this, MyUploadedVideos.class);
        startActivity(MyUploadedVideos);
    }
    public void switchToMyUploadedImages(View view){
        Intent MyUploadedImages = new Intent(this, MyUploadedImages.class);
        startActivity(MyUploadedImages);
    }
    public void switchToVideoGallery(View view){
        Intent videoGallery = new Intent(this, videoGallery.class);
        startActivity(videoGallery);
    }
    public void switchTochatsection(View view){
        Intent chatSection = new Intent(this, chatSection.class);
        startActivity(chatSection);
    }

    public void switchToUpload(View view){
        Intent Upload = new Intent(this, Upload.class);
        startActivity(Upload);
    }
    public void switchToGPSlocations(View view){
        Intent GPSlocations = new Intent(this, GPSlocations.class);
        startActivity(GPSlocations);
    }

    public void switchToMainActivity(){
        Intent MainActivity = new Intent(this, MainActivity.class);
        startActivity(MainActivity);
    }
}