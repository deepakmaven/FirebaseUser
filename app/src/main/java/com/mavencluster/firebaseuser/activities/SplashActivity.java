package com.mavencluster.firebaseuser.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.auth.FirebaseAuth;
import com.mavencluster.firebaseuser.R;
import io.fabric.sdk.android.Fabric;

/**
 * Splash activity to show brand logo and check user logged In or not..
 */
public class SplashActivity extends AppCompatActivity {

    private Handler handler;
    private Runnable runnable;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_splash);
        mAuth = FirebaseAuth.getInstance();
        handler = new Handler();
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                /*check user is logged in or not*/
                if (mAuth.getCurrentUser() != null) {
                    sentToUserListActivity();
                } else {
                    /*sent to login screen..*/
                    sentToLoginScreen();
                }

            }
        }, 2500);
    }

    /**
     * This method navigate user to user list activity
     */
    private void sentToUserListActivity() {
        Intent intent = new Intent(SplashActivity.this, UserListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Method to sent user on login screen
     */
    private void sentToLoginScreen() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.removeCallbacks(runnable);
    }
}
