package com.example.authtest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

// Introductory splash screen shown for 3 seconds on app launch
public class SplashScreen extends AppCompatActivity {

    /*
         Shows splash screen layout for 3000 milliseconds (3 seconds)
         Uses Handler with Looper.getMainLooper() to schedule delayed action
         After 3 seconds, navigates to SignIn activity and finishes self
         Users see splash before login screen
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreen.this, SignIn.class);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}