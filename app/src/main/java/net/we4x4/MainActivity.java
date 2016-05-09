package net.we4x4;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

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


public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    SwipeRefreshLayout swipeLayout;

    TextView usernameDisplay, authorName,registerLink,currentRatingValue,
            textViewTopRatedCont,contentAuthor, currentRating, textViewSprt,textViewLatestUps;
    ListView listViewContent;
    public Button buttonUserDash;
    public VideoView vid;
    public static String vid_url = "http://res.cloudinary.com/we4x4/video/upload/v1460868673/shortVid_gnokgj.mp4";
    public static Uri video;

    public String UserID, username, topContRefImg, topVidRef;
//    int ratingValueImgs, ratingValueImg;
    public Long vidMaxRating;

    /** firebase links & variables **/

    Firebase firebaseRefUaers, firebaseRefData,firebaserRefMain;
    Firebase.AuthStateListener authStateListenerMain;
    AuthData authDataMain;


//    Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        setContentView(R.layout.activity_main);

        /** Declaring & Setting up the SwipeRefresh Layout**/

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        /** Setting up Main VidPlayer**/

        UserID = null;
        vid = (VideoView) findViewById(R.id.VidClip);
        video = Uri.parse(vid_url);
        vid.setMediaController(new MediaController(this));
        video = Uri.parse(vid_url);
        vid.setVideoURI(video);

        /** Universal Image Loader **/

        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        ImageLoader.getInstance().init(config);

        /** Setting up Layout Elements **/
        currentRating = (TextView)findViewById(R.id.currentRating);
        contentAuthor = (TextView)findViewById(R.id.contentAuthor);
        registerLink = (TextView) findViewById(R.id.registerLink);
        usernameDisplay = (TextView) findViewById(R.id.textViewUsername);
        currentRatingValue = (TextView)findViewById(R.id.currentRatingValue);
        authorName = (TextView) findViewById(R.id.authorName);
        textViewTopRatedCont = (TextView) findViewById(R.id.textViewTopRatedCont);
        listViewContent = (ListView)findViewById(R.id.listViewContent);
        textViewLatestUps = (TextView)findViewById(R.id.textViewLatestUps);
        textViewSprt = (TextView)findViewById(R.id.textViewSprt);

        /** Logged in user Dashboard button **/
        buttonUserDash = (Button) findViewById(R.id.buttonUserDash);
        buttonUserDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToMembers();
            }
        });

        /** firebase links shortcuts **/

        firebaserRefMain = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefData = new Firebase(getResources().getString(R.string.firebaseData));
        firebaseRefUaers = new Firebase(getResources().getString(R.string.firebase_users));

        if (savedInstanceState == null) {
            Firebase.setAndroidContext(this);
        }

        /** firebase Authentication listener  **/

        authStateListenerMain = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if(authData != null){

                    /** if authentication present setAuthenticatedUser as authData retrieved **/

                    setAuthenticatedUserMM(authData);

                }else{
                    /**if no authentication detected null user info, and clear layout elements
                     related to logged in user;**/

                    usernameDisplay.setText("Please login");
                    //null users info
                    UserID = null;
                    currentRatingValue.setVisibility(View.GONE);
                    authorName.setVisibility(View.GONE);
                    contentAuthor.setVisibility(View.GONE);
                    currentRating.setVisibility(View.GONE);
                    textViewSprt.setVisibility(View.GONE);
                }
            }
        };

        /** adding Authentication state listener **/
        firebaserRefMain.addAuthStateListener(authStateListenerMain);
        /** By the end of onCreate execute requestTopImgs method to retrieve members posts **/
        requestTopImgs();
    }

    /** When user authenticated update layout & request user Info. **/
    private void setAuthenticatedUserMM(AuthData authData) {
        if(authData != null) {

            this.authDataMain = authData;
            UserID = authData.getUid();
            extractUserInfo();
            registerLink.setVisibility(View.GONE);
            extractUserTopVidInfo();
        }else{
            vid.setVideoURI(video);
            vid.requestFocus();
            vid.seekTo(100);
        }
    }

    /** extracting authenticated user Ifno. **/

    public void extractUserInfo() {
        Firebase ref = new Firebase("https://wi4x4.firebaseio.com/users/" + UserID);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot !=null) {
                    userInformation userInformation = dataSnapshot.getValue(userInformation.class);
                    username = userInformation.getUsername();
                }
                if (username != null) {
                    usernameDisplay.setText(username);
                    buttonUserDash.setVisibility(View.VISIBLE);
                    extractUserTopVidInfo();

                }else{
                    usernameDisplay.setText("Please log in");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }
        });
    }

    /** Requesting top images of members if user not logged in or the user uploaded images **/
    public void requestTopImgs(){
        if(authDataMain != null) {

            /** Execute Json Reader provided by Cloudinary by the address of the cloud created for the app **/
        //TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USEING THE APP

            new MainActivity.JsonTask().execute("http://res.cloudinary.com/we4x4/image/list/" + UserID + ".json");
        }else{
            new MainActivity.JsonTask().execute("http://res.cloudinary.com/we4x4/image/list/uploads.json");
        }
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

                upImgAdapter adapter = new upImgAdapter(getApplicationContext(), R.layout.row_main, result);
                listViewContent.setAdapter(adapter);
                setListViewHeightBasedOnChildren(listViewContent);

            }else{
                Toast.makeText(getApplicationContext(), "No content uploaded / Connection Error", Toast.LENGTH_SHORT).show();
                textViewLatestUps.setVisibility(View.GONE);
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

            ImageView imageViewDisplay;
            imageViewDisplay = (ImageView)convertView.findViewById(R.id.imageViewDisplayMain);

            ImageLoader.getInstance().displayImage(upImgModelsList.get(position).getAddress(), imageViewDisplay);

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

    /** Extract users Top rated Vid **/

    public void extractUserTopVidInfo(){
        final Firebase ref = new Firebase("https://wi4x4.firebaseio.com/rating/videos/" + UserID);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Map<Long, Long> ratings = (HashMap<Long, Long>) snapshot.getValue();
                if (ratings != null) {
                    Collection<Long> BulkValues = ratings.values();
                    ArrayList<Long> values = new ArrayList<>();
                    values.addAll(BulkValues);
                    vidMaxRating = Collections.max(values);
                    currentRatingValue.setText(vidMaxRating.toString());
                    authorName.setText(username);
                    textViewTopRatedCont.setVisibility(View.VISIBLE);

                    Query queryRef = ref.orderByValue().equalTo(vidMaxRating);

                    queryRef.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot snapshot, String s) {
                            topVidRef = String.valueOf(snapshot.getKey());
                            Firebase refTopCont = new Firebase("https://wi4x4.firebaseio.com/uploads/videos/" + UserID + "/" + topVidRef);
                            refTopCont.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {

                                    vid_url = String.valueOf(snapshot.getValue());

                                }

                                @Override
                                public void onCancelled(FirebaseError firebaseError) {

                                }
                            });

                            /** above is ref to top content key, should be used to get address of the content **/
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                firebaseError.getMessage();
            }

        });

        // feed layout elements with the user info and the top rated vid //
        authorName.setVisibility(View.VISIBLE);
        contentAuthor.setVisibility(View.VISIBLE);
        currentRatingValue.setVisibility(View.VISIBLE);
        currentRating.setVisibility(View.VISIBLE);
        textViewSprt.setVisibility(View.VISIBLE);
        video = Uri.parse(vid_url);
        vid.setVideoURI(video);
        vid.requestFocus();
        vid.seekTo(100);

    }

    /** Actions whe user log out; null user Info. and clear layout elements related to user **/
    private void logout() {

        firebaserRefMain.unauth();
        usernameDisplay.setVisibility(View.GONE);
        authDataMain = null;
        registerLink.setVisibility(View.VISIBLE);
        switchToMainActivity();
        // Resetting main vid address to default "welcome vid."
        vid_url = "http://res.cloudinary.com/we4x4/video/upload/v1460868673/shortVid_gnokgj.mp4";
        setAuthenticatedUserMM(null);
    }

    /** switching to different activities classes **/

    public void switchToRegisterSection(View v) {
        Intent RegisterSection = new Intent(this, RegisterSection.class);
        startActivity(RegisterSection);
    }

    public void switchToMainActivity(){
        Intent MainActivity = new Intent(this, MainActivity.class);
        startActivity(MainActivity);
    }

    public void switchToMembers() {
        Intent Members = new Intent(this, Members.class);
        startActivity(Members);
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
                        "wi4x4 v 1.0 developed by A.A./JanusJanus@riseup.net",
                        Toast.LENGTH_LONG).show();

            case R.id.action_logout:
                logout();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

}
