package com.example.finalproject;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MyListingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listing);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new MyListingFragment())
                .commit();
    }
}
