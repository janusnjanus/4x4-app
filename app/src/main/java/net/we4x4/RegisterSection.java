package net.we4x4;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Map;

/**Created by JanusJanus 2016 / janusjanus@riseup.net **/

/** Based on Firebase login-demo privided by Firebase on
 * https://github.com/firebase/firebase-login-demo-android **/

//TODO; SETTING UP CLOUDINARY & FIREBASE ACCOUNTS SHOULD BE DONE FIRST BEFORE USING THE APP

public class RegisterSection extends AppCompatActivity{

    /** Layout elements **/
    EditText usernameReg, emailReg, passwordReg;
    Button buttonReg, buttonMemReg, buttonSwitchToLogin, buttonLGIN, buttonSwitchToRegister, buttonPassReset, buttonSubmitPassReset;
    TextView statReg;

    /**Firebase links and elements **/
    private Firebase firebaseRefReg, firebaseRefUserReg;
    private Firebase.AuthStateListener firebaseAuthListnerReg;
    private ProgressDialog createUserProg;
    private ProgressDialog authProg;
    private AuthData authDataReg;


    String StEmailReg, StUsernameReg, StPasswordReg;
    String userID;

    @Override
    public void onCreate (Bundle savedInstanceState){
        Firebase.setAndroidContext(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.regsiter_section);


        // Declaring elements from layout
        usernameReg = (EditText) findViewById(R.id. usernameReg);
        emailReg = (EditText) findViewById(R.id. emailReg);
        passwordReg = (EditText) findViewById(R.id. passwordReg);

        statReg = (TextView)findViewById(R.id.statReg);

        /** setting up pssword reset button**/
        buttonSubmitPassReset = (Button)findViewById(R.id.buttonSubmitPassReset);
        buttonSubmitPassReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authProg.show();
                /** getting user email from the field and sending a request to firebase**/
                StEmailReg = emailReg.getText().toString();
                Firebase ref = new Firebase("https://wi4x4.firebaseio.com");
                ref.resetPassword(StEmailReg, new Firebase.ResultHandler() {
                    @Override
                    public void onSuccess() {
                        // password reset email sent the following should take place
                        //changing layout organisation
                        Toast.makeText(getApplicationContext(),
                                "Check your Email for instructions",
                                Toast.LENGTH_LONG).show();
                        emailReg.setVisibility(View.VISIBLE);
                        emailReg.setText("");
                        passwordReg.setVisibility(View.VISIBLE);
                        buttonSwitchToRegister.setVisibility(View.VISIBLE);
                        buttonPassReset.setVisibility(View.VISIBLE);
                        buttonLGIN.setVisibility(View.VISIBLE);
                        authProg.dismiss();
                    }
                    @Override
                    public void onError(FirebaseError firebaseError) {
                        // error encountered
                        authProg.dismiss();
                        Toast.makeText(getApplicationContext(),
                                "Error, could not send an Eamil to reset your password, please contact Admin.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        /** setting up password reset button to reorganise layout**/
        buttonPassReset = (Button) findViewById(R.id.buttonPassReset);
        buttonPassReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSubmitPassReset.setVisibility(View.VISIBLE);
                buttonLGIN.setVisibility(View.GONE);
                buttonSwitchToRegister.setVisibility(View.GONE);
                passwordReg.setVisibility(View.GONE);
                buttonPassReset.setVisibility(View.GONE);
            }
        });
        /** Setting up user registeration button and executing the method**/
        buttonReg = (Button)findViewById(R.id.buttonReg);
        buttonReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                regNewUser();
            }
        });
        /** Setting up member button, desplayed after logging in **/
        buttonMemReg = (Button)findViewById(R.id.buttonMemReg);
        buttonMemReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToMembers();

            }
        });

        /** Setting up LOGIN button for existing user to submit email & password **/
        buttonLGIN = (Button) findViewById(R.id. buttonLGIN);
        buttonLGIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                buttonMemReg.setVisibility(View.GONE);
                login();

            }
        });
        /** setting up login button to reorganise layout **/
        buttonSwitchToLogin = (Button) findViewById(R.id.buttonSwitchToLogin);
        buttonSwitchToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonReg.setVisibility(View.GONE);
                usernameReg.setVisibility(View.GONE);
                buttonLGIN.setVisibility(View.VISIBLE);
                buttonSwitchToLogin.setVisibility(View.GONE);
                buttonSwitchToRegister.setVisibility(View.VISIBLE);
                buttonPassReset.setVisibility(View.VISIBLE);
            }
        });
        /** setting up registeration button to reorganise layout **/
        buttonSwitchToRegister = (Button)findViewById(R.id.buttonSwitchToRegister);
        buttonSwitchToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSwitchToLogin.setVisibility(View.VISIBLE);
                buttonReg.setVisibility(View.VISIBLE);
                usernameReg.setVisibility(View.VISIBLE);
                buttonSwitchToRegister.setVisibility(View.GONE);
                buttonLGIN.setVisibility(View.GONE);
                buttonPassReset.setVisibility(View.GONE);
            }
        });

        /** firebase links **/
        firebaseRefReg = new Firebase(getResources().getString(R.string.firebase_url));
        firebaseRefUserReg = new Firebase(getResources().getString(R.string.firebase_users));

        createUserProg = new ProgressDialog(this);
        createUserProg.setTitle("Loading");
        createUserProg.setMessage("Creating Account");
        createUserProg.setCancelable(false);

        authProg = new ProgressDialog(this);
        authProg.setTitle("Loading");
        authProg.setMessage("Authenticating");
        authProg.setCancelable(false);

        /** firebase authentication listeners **/

        firebaseAuthListnerReg = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                setAuthenticatedUser(authData);
            }
        };
        /** adding Authentication state listener **/

        firebaseRefReg.addAuthStateListener(firebaseAuthListnerReg);

    }

    /** When user authenticated update layout **/

    private void setAuthenticatedUser(AuthData authData){
        if(authData !=null){
            emailReg.setVisibility(View.GONE);
            usernameReg.setVisibility(View.GONE);
            passwordReg.setVisibility(View.GONE);
            buttonReg.setVisibility(View.GONE);
            buttonSwitchToLogin.setVisibility(View.GONE);
            buttonMemReg.setVisibility(View.VISIBLE);
            statReg.setVisibility(View.VISIBLE);
            buttonLGIN.setVisibility(View.GONE);
            buttonSwitchToRegister.setVisibility(View.GONE);
            statReg.setText(StEmailReg);
            buttonPassReset.setVisibility(View.GONE);
            this.authDataReg =authData;
        }
    }

    /** Requesting  user method**/
    public void regNewUser(){

        StUsernameReg = usernameReg.getText().toString();
        StEmailReg = emailReg.getText().toString();
        StPasswordReg = passwordReg.getText().toString();
        createUserProg.show();

        /** Creating a new user - submitting entered email & password **/
        firebaseRefReg.createUser(StEmailReg, StPasswordReg, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                statReg.setVisibility(View.VISIBLE);
                statReg.setText("Successfully created an account with  " + StEmailReg);

                /** Authenticating user after reg.**/
                firebaseRefReg.authWithPassword(StEmailReg, StPasswordReg, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {

                        /** Upon authenticating - new folder created in firebase with the following info.**/
                        createUserProg.setMessage("Please wait authentication ");
                        firebaseRefUserReg.child(authData.getUid()).child("email").setValue(StEmailReg);
                        firebaseRefUserReg.child(authData.getUid()).child("username").setValue(StUsernameReg);
                        firebaseRefUserReg.child(authData.getUid()).child("rank").setValue(1);
                        createUserProg.hide();
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        createUserProg.setMessage("Error authenticating user !");
                        createUserProg.hide();

                    }
                });

                /** reorganising layout after reg.  **/
                emailReg.setVisibility(View.GONE);
                usernameReg.setVisibility(View.GONE);
                passwordReg.setVisibility(View.GONE);
                buttonReg.setVisibility(View.GONE);
                buttonSwitchToLogin.setVisibility(View.GONE);
                buttonMemReg.setVisibility(View.VISIBLE);
                buttonPassReset.setVisibility(View.GONE);

            }
            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
                createUserProg.setMessage("Sorry, Unsuccessful registration, try again !");
                createUserProg.hide();

            }
        });
    }
    /** logging in method **/
    public void login(){
        StEmailReg = emailReg.getText().toString();
        StPasswordReg = passwordReg.getText().toString();
        authProg.show();
        firebaseRefReg.authWithPassword(StEmailReg, StPasswordReg, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
                authProg.hide();
                //firebaseRefUserReg.child(authData.getUid()).child("email").setValue(StEmailReg);
                //firebaseRefUserReg.child(authData.getUid()).child("username").setValue(StUsernameReg);
                createUserProg.hide();
                emailReg.setVisibility(View.GONE);
                usernameReg.setVisibility(View.GONE);
                passwordReg.setVisibility(View.GONE);
                buttonReg.setVisibility(View.GONE);
                buttonSwitchToRegister.setVisibility(View.GONE);
                buttonLGIN.setVisibility(View.GONE);
                buttonMemReg.setVisibility(View.VISIBLE);
                buttonSubmitPassReset.setVisibility(View.GONE);

            }

            @Override
            public void onAuthenticationError(FirebaseError firebaseError) {
                createUserProg.setMessage("Error authenticating user !");
                authProg.hide();

            }
        });

    }

    /** Logging out method - mainly reorganising layout, and clearing fields - getting auth. to null **/

    private void logout() {
            /* logout of Firebase */
        if(authDataReg !=null) {
            firebaseRefReg.unauth();
            emailReg.setVisibility(View.VISIBLE);
            emailReg.setText("");
            passwordReg.setVisibility(View.VISIBLE);
            passwordReg.setText("");
            usernameReg.setText("");
            usernameReg.setVisibility(View.GONE);
            buttonReg.setVisibility(View.GONE);
            buttonMemReg.setVisibility(View.GONE);
            buttonLGIN.setVisibility(View.VISIBLE);
            buttonSwitchToRegister.setVisibility(View.VISIBLE);
            buttonSwitchToLogin.setVisibility(View.GONE);
            statReg.setVisibility(View.GONE);
            // switchToMainActivity();
        }
        setAuthenticatedUser(null);
    }

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


    public void switchToMainActivity(){
        Intent MainActivity = new Intent(this, MainActivity.class);
        startActivity(MainActivity);
    }

    public void switchToMembers() {
        Intent Members = new Intent(this, Members.class);
        startActivity(Members);
    }


}
