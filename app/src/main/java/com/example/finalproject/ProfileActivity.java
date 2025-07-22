package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    ImageView imgAvatar;
    TextView tvName, tvBio, tvContact, tvRating;
    Button btnEdit;

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String uid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        imgAvatar = findViewById(R.id.imgAvatar);
        tvName = findViewById(R.id.tvName);
        tvBio = findViewById(R.id.tvBio);
        tvContact = findViewById(R.id.tvContact);
        tvRating = findViewById(R.id.tvRating);
        btnEdit = findViewById(R.id.btnEdit);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        uid = mAuth.getCurrentUser().getUid();

        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, EditProfileActivity.class);
            startActivity(i);
        });

        loadProfile();
    }

    private void loadProfile() {
        db.collection("profiles").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvName.setText(doc.getString("fullName"));
                        tvBio.setText(doc.getString("bio"));
                        tvContact.setText(doc.getString("contact"));
                        double rating = doc.getDouble("rating") != null ? doc.getDouble("rating") : 0.0;
                        tvRating.setText("Đánh giá: " + String.format("%.1f", rating) + " ★");

                        String avatarUri = doc.getString("avatarUri");
                        if (avatarUri != null && !avatarUri.isEmpty()) {
                            Glide.with(this).load(avatarUri).into(imgAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Không thể tải hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
