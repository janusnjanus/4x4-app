package net.we4x4;

/** this class Provided by firebase - GitHub * @author greg **/

import com.firebase.client.Firebase;

public class ChatApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}