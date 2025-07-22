package com.example.finalproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.*;

public class AddListingActivity extends AppCompatActivity {

    private EditText edtTitle, edtDescription, edtPrice;
    private Spinner spinnerCategory, spinnerStatus;
    private ImageView imgPreview;
    private Button btnSelectImage, btnPost, btnPreview, btnIncreaseQty, btnDecreaseQty;
    private Uri selectedImageUri = null;

    private TextView txtQuantity;

    private FusedLocationProviderClient fusedLocationClient;
    private String currentAddress = "";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_listing);

        // Init Cloudinary nếu cần
        try {
            if (MediaManager.get() == null || MediaManager.get().getCloudinary() == null) {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dl106kyha");
                config.put("api_key", "389738897636243");
                config.put("api_secret", "xVbzsqrpkJ7YHMQtQuiH8HP-3us");
                MediaManager.init(this.getApplicationContext(), config);
            }
        } catch (Exception e) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dl106kyha");
            config.put("api_key", "389738897636243");
            config.put("api_secret", "xVbzsqrpkJ7YHMQtQuiH8HP-3us");
            MediaManager.init(this.getApplicationContext(), config);
        }

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtPrice = findViewById(R.id.edtPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imgPreview = findViewById(R.id.imgPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnPost = findViewById(R.id.btnPostListing);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentLocation();

        btnSelectImage.setOnClickListener(v -> pickImageFromGallery());
        btnPost.setOnClickListener(v -> uploadAndSaveListing());

        // Spinner category
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.categories_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
        btnPreview = findViewById(R.id.btnPreview);

        btnPreview.setOnClickListener(v -> {
            String title = edtTitle.getText().toString().trim();
            String desc = edtDescription.getText().toString().trim();
            String price = edtPrice.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            try {
                quantity = Integer.parseInt(txtQuantity.getText().toString());
            } catch (NumberFormatException e) {
                quantity = 1;
            }

            String status = spinnerStatus.getSelectedItem().toString();
            if (quantity == 0) {
                status = "Sold";
            }

            if (title.isEmpty() || desc.isEmpty() || price.isEmpty() || selectedImageUri == null) {
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin trước khi xem trước", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, PreviewItemActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("description", desc);
            intent.putExtra("price", price);
            intent.putExtra("category", category);
            intent.putExtra("imageUri", selectedImageUri.toString());
            intent.putExtra("location", currentAddress);
            intent.putExtra("quantity", quantity);
            intent.putExtra("status", status);

            startActivity(intent);
        });

        txtQuantity = findViewById(R.id.txtQuantity);
        btnIncreaseQty = findViewById(R.id.btnIncreaseQty);
        btnDecreaseQty = findViewById(R.id.btnDecreaseQty);

        btnIncreaseQty.setOnClickListener(v -> {
            quantity++;
            txtQuantity.setText(String.valueOf(quantity));
        });

        btnDecreaseQty.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                txtQuantity.setText(String.valueOf(quantity));
            }
        });

        spinnerStatus = findViewById(R.id.spinnerStatus);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);


    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imgPreview);
                }
            }
    );

    private void uploadAndSaveListing() {
        String title = edtTitle.getText().toString().trim();
        String desc = edtDescription.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        try {
            quantity = Integer.parseInt(txtQuantity.getText().toString());
        } catch (NumberFormatException e) {
            quantity = 1;
        }

        if (title.isEmpty() || desc.isEmpty() || price.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPost.setEnabled(false);
        MediaManager.get().upload(selectedImageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveToFirestore(title, desc, price, category, imageUrl);
                    }

                    @Override public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(AddListingActivity.this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show();
                        btnPost.setEnabled(true);
                    }

                    @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override public void onStart(String requestId) {}
                    @Override public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirestore(String title, String desc, String price, String category, String imageUrl) {
        Map<String, Object> listing = new HashMap<>();
        listing.put("userId", userId);
        listing.put("title", title);
        listing.put("description", desc);
        listing.put("price", price);
        listing.put("category", category);
        listing.put("imageUrl", imageUrl);
        listing.put("timestamp", System.currentTimeMillis());
        listing.put("location", currentAddress);
        String status = spinnerStatus.getSelectedItem().toString();
        if (quantity == 0) {
            status = "Sold";
        }

        listing.put("quantity", quantity);
        listing.put("status", status);
        listing.put("views", 0);

        db.collection("listings").add(listing)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Đăng sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi đăng sản phẩm", Toast.LENGTH_SHORT).show();
                    btnPost.setEnabled(true);
                });
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        currentAddress = addresses.get(0).getAddressLine(0);
                    }
                } catch (IOException e) {
                    currentAddress = "";
                }
            }
        });
    }
}
