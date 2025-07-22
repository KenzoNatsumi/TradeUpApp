package com.example.finalproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditListingActivity extends AppCompatActivity {

    private EditText edtTitle, edtDescription, edtPrice;
    private Spinner spinnerCategory, spinnerStatus;
    private ImageView imgProduct;
    private Button btnUpdate, btnDelete, btnChangeImage, btnIncreaseQty, btnDecreaseQty;
    private TextView txtQuantity;

    private Uri selectedImageUri = null;
    private String currentImageUrl = "";
    private String listingId;

    private FirebaseFirestore db;

    private int quantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_listing);

        // Ánh xạ View
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtPrice = findViewById(R.id.edtPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imgProduct = findViewById(R.id.imgProduct);
        btnChangeImage = findViewById(R.id.btnChangeImage);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);

        db = FirebaseFirestore.getInstance();
        listingId = getIntent().getStringExtra("listingId");

        if (listingId == null || listingId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadListing();

        btnChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            launcher.launch(intent);
        });

        btnUpdate.setOnClickListener(v -> updateListing());

        btnDelete.setOnClickListener(v -> {
            db.collection("listings").document(listingId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã xoá sản phẩm", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xoá sản phẩm", Toast.LENGTH_SHORT).show());
        });
        txtQuantity = findViewById(R.id.txtQuantity);
        btnIncreaseQty = findViewById(R.id.btnIncreaseQty);
        btnDecreaseQty = findViewById(R.id.btnDecreaseQty);

        btnIncreaseQty.setOnClickListener(v -> {
            quantity++;
            txtQuantity.setText(String.valueOf(quantity));
        });

        btnDecreaseQty.setOnClickListener(v -> {
            if (quantity > 0) {
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

    private void loadListing() {
        db.collection("listings").document(listingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        edtTitle.setText(doc.getString("title"));
                        edtDescription.setText(doc.getString("description"));
                        edtPrice.setText(doc.getString("price"));

                        Long qty = doc.getLong("quantity");
                        if (qty != null) {
                            quantity = qty.intValue();
                            txtQuantity.setText(String.valueOf(quantity));
                        }

                        currentImageUrl = doc.getString("imageUrl");

                        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                                R.array.categories_array, android.R.layout.simple_spinner_item);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategory.setAdapter(adapter);

                        String category = doc.getString("category");
                        if (category != null) {
                            int pos = adapter.getPosition(category);
                            spinnerCategory.setSelection(pos);
                        }

                        String status = doc.getString("status");
                        if (status != null) {
                            ArrayAdapter<CharSequence> statusAdapter = (ArrayAdapter<CharSequence>) spinnerStatus.getAdapter();
                            int pos = statusAdapter.getPosition(status);
                            spinnerStatus.setSelection(pos);
                        }

                        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                            Glide.with(this).load(currentImageUrl).into(imgProduct);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show());
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imgProduct);
                }
            }
    );

    private void updateListing() {
        String title = edtTitle.getText().toString().trim();
        String desc = edtDescription.getText().toString().trim();
        String price = edtPrice.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        try {
            quantity = Integer.parseInt(txtQuantity.getText().toString());
        } catch (NumberFormatException e) {
            quantity = 1;
        }

        if (title.isEmpty() || desc.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", desc);
        data.put("price", price);
        data.put("category", category);
        data.put("quantity", quantity);
        String selectedStatus = spinnerStatus.getSelectedItem().toString();
        String finalStatus = quantity == 0 ? "Sold" : selectedStatus;
        data.put("status", finalStatus);

        if (selectedImageUri != null) {
            MediaManager.get().upload(selectedImageUri)
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            data.put("imageUrl", imageUrl);
                            finishUpdate(data);
                        }

                        @Override public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(EditListingActivity.this, "Lỗi khi upload ảnh", Toast.LENGTH_SHORT).show();
                        }

                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onStart(String requestId) {}
                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } else {
            data.put("imageUrl", currentImageUrl);
            finishUpdate(data);
        }
    }

    private void finishUpdate(Map<String, Object> data) {
        db.collection("listings").document(listingId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
    }
}
