package com.example.finalproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class PreviewItemActivity extends AppCompatActivity {

    ImageView imgPreview;
    TextView txtTitle, txtDescription, txtPrice, txtCategory, txtLocation, txtQuantity, txtStatus;
    Button btnBack, btnPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_item);

        imgPreview = findViewById(R.id.imgPreview);
        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtPrice = findViewById(R.id.txtPrice);
        txtCategory = findViewById(R.id.txtCategory);
        btnBack = findViewById(R.id.btnBack);
        btnPost = findViewById(R.id.btnPost);
        txtLocation = findViewById(R.id.txtLocation);
        txtQuantity = findViewById(R.id.txtQuantity);
        txtStatus = findViewById(R.id.txtStatus);

        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("title");
            String description = intent.getStringExtra("description");
            String price = intent.getStringExtra("price");
            String category = intent.getStringExtra("category");
            String imageUri = intent.getStringExtra("imageUri");
            String location = intent.getStringExtra("location");

            txtTitle.setText(title);
            txtDescription.setText(description);
            txtPrice.setText("Giá: " + price);
            txtCategory.setText("Loại: " + category);
            txtLocation.setText("Địa điểm: " + location);
            int quantity = intent.getIntExtra("quantity", 1);
            String status = intent.getStringExtra("status");

            txtQuantity.setText("Số lượng: " + quantity);
            txtStatus.setText("Trạng thái: " + status);

            if (imageUri != null && !imageUri.isEmpty()) {
                Glide.with(this).load(Uri.parse(imageUri)).into(imgPreview);
            }
        }

        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnPost.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        // Tăng view count
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String listingId = getIntent().getStringExtra("listingId");
        if (listingId != null) {
            db.collection("listings").document(listingId)
                    .update("views", FieldValue.increment(1));
        }
    }
}
