package net.we4x4;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**Created by JanusJanus 2016 / janusjanus@riseup.net **/

//TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USING THE APP

public class MyUploadedImages extends AppCompatActivity {

    /** Layout elements **/
    String UserID, fileType;

    EditText editTextShare;
    ListView listView, listViewUpload;
    Button buttonListImages;
    private ProgressDialog dialog;

    /** Firebase links & elements**/
    Firebase firebaseRefUsersMU, firebaserRefMU, firebaseRefUploadsMU;
    Firebase.AuthStateListener authStateListenerMU;
    AuthData authDataMU;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_upload);


        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .build();
        ImageLoader.getInstance().init(config);

        /**Firebase links **/
        firebaserRefMU = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefUsersMU = new Firebase(getResources().getString(R.string.firebase_users));

        /** Layput elements**/
        dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage("Loading. Please wait...");

        listViewUpload = (ListView)findViewById(R.id.listView);

        editTextShare = (EditText)findViewById(R.id.editTextShare);
        listView = (ListView)findViewById(R.id.listView);

        /** Listing videos addresses Button to execute JsonTask**/
        buttonListImages = (Button) findViewById(R.id.buttonListImages);
        buttonListImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /** Requesting a list of all uploaded video from Cloudinary with the Tag UserID **/

                new JsonTask().execute("http://res.cloudinary.com/we4x4/image/list/" + UserID + ".json");
                fileType = "image";
                dialog.show();
            }
        });

        /** firebase authentication listeners **/

        authStateListenerMU = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                if (authData != null) {
                    setAuthenticatedUserMU(authData);
                } else {

                }
            }
        };
        /** adding Authentication state listener **/

        firebaserRefMU.addAuthStateListener(authStateListenerMU);
    }
    /** Extracting user Ifno. from the authData **/

    private void setAuthenticatedUserMU(AuthData authData) {
        if (authData != null) {

            this.authDataMU = authData;
            UserID = authData.getUid();
        }
    }

    /** AsyncTask to extract uploaded images Info. **/

    public class JsonTask extends AsyncTask<String, String, List<uploadedContentModels> > {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected List<uploadedContentModels> doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                /** establishing a connection to retrieve Json**/

                URL url = new URL(params[0]);
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
                    upImgModels.setFormat(finalObject.getString("format"));

                    /** constructing content address on cloudinary from the elements above **/

                    upImgModels.setAddress("http://res.cloudinary.com/we4x4/" + fileType
                            + "/upload/v" + finalObject.getString("version") + "/"
                            + finalObject.getString("public_id") + "." +
                            finalObject.getString("format"));


//                     imagesAddress = ("http://res.cloudinary.com/we4x4/" + fileType +
//                            "/upload/" + upImgModels.setVersion(finalObject.getString("version")) +
//                            "/" + upImgModels.setPublic_id(finalObject.getString("public_id")) +
//                            "." + upImgModels.setFormat(finalObject.getString("format")) + "\n");

                    upImgList.add(upImgModels);
                }
                return upImgList;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
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
            dialog.dismiss();
            if(result !=null) {

                /** setting up an adapter for the view **/

                upImgAdapter adapter = new upImgAdapter(getApplicationContext(), R.layout.row, result);
                listView.setAdapter(adapter);

                /** Setting a listener for the list view to choose a vid address **/

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                        uploadedContentModels upImgModels = result.get(position);
                        String choice = result.get(position).getAddress();

                        /** passing the chosen vid address to the editText field in order for the user to be
                         * to copy the link**/

                        editTextShare.setText(choice);
                    }
                });


            }else{
                Toast.makeText(getApplicationContext(), "No content uploaded / Connection Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**Setting up ArrayAdapter for the view with the created ModelsList**/

    public class upImgAdapter extends ArrayAdapter{

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
        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            if(convertView == null){
                convertView = inflater.inflate(resource, null);
            }

            /** setting up the image from the row layout source **/
            ImageView imageViewDisplay;
            imageViewDisplay = (ImageView)convertView.findViewById(R.id.imageViewDisplay);

            ImageLoader.getInstance().displayImage(upImgModelsList.get(position).getAddress(), imageViewDisplay);

            return convertView;
        }
    }
    /** Actions whe user log out; null user Info. and clear layout elements related to user **/

    private void logout() {
        if (this.authDataMU != null) {

            firebaserRefMU.unauth();
            authDataMU = null;
            switchToMainActivity();
        }
        setAuthenticatedUserMU(null);
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
    public void switchToMainActivity(){
        Intent MainActivity = new Intent(this, MainActivity.class);
        startActivity(MainActivity);
    }
}

