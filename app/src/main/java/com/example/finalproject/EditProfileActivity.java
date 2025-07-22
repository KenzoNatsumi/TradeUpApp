package com.example.finalproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;


import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.EmailAuthProvider;

public class EditProfileActivity extends AppCompatActivity {

    private EditText edtDisplayName, edtBio, edtContact;
    private ImageView imgAvatar;
    private Button btnChooseImage, btnSave, btnDelete;
    private Uri selectedImageUri = null;

    private FirebaseFirestore db;
    private String userId;
    private ActivityResultLauncher<Intent> googleReauthLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Lấy userId
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // 2. Init Cloudinary nếu chưa có
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

        // 3. Ánh xạ view
        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtBio = findViewById(R.id.edtBio);
        edtContact = findViewById(R.id.edtContact);
        imgAvatar = findViewById(R.id.imgAvatar);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnDelete = findViewById(R.id.btnDeleteAccount);
        btnDelete.setOnClickListener(view -> confirmDeleteAccount());

        // 4. Load dữ liệu cũ
        loadUserData();

        // 5. Chọn ảnh
        btnChooseImage.setOnClickListener(view -> pickImageFromGallery());

        // 6. Lưu hồ sơ
        btnSave.setOnClickListener(view -> saveUserProfile());

        googleReauthLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                                reauthenticateAndDelete(credential);
                            }
                        } catch (ApiException e) {
                            Toast.makeText(this, "Đăng nhập lại thất bại", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
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
                    Glide.with(this).load(selectedImageUri).into(imgAvatar);
                }
            }
    );

    private void loadUserData() {
        db.collection("profiles").document(userId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                edtDisplayName.setText(snapshot.getString("fullName"));
                edtBio.setText(snapshot.getString("bio"));
                edtContact.setText(snapshot.getString("contact"));

                String avatarUri = snapshot.getString("avatarUri");
                if (avatarUri != null && !avatarUri.isEmpty()) {
                    Glide.with(this).load(avatarUri).into(imgAvatar);
                }
            }
        });
    }

    private void saveUserProfile() {
        String displayName = edtDisplayName.getText().toString().trim();
        String bio = edtBio.getText().toString().trim();
        String contact = edtContact.getText().toString().trim();

        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("fullName", displayName);
        userProfile.put("bio", bio);
        userProfile.put("contact", contact);
        userProfile.put("rating", 5.0);

        if (selectedImageUri != null) {
            MediaManager.get().upload(selectedImageUri)
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String imageUrl = (String) resultData.get("secure_url");
                            userProfile.put("avatarUri", imageUrl);
                            saveToFirestore(userProfile);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(EditProfileActivity.this, "Lỗi khi upload: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        }

                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}
                        @Override public void onStart(String requestId) {}
                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } else {
            saveToFirestore(userProfile);
        }
    }

    private void saveToFirestore(Map<String, Object> userProfile) {
        db.collection("profiles").document(userId).set(userProfile)
                .addOnSuccessListener(task -> {
                    Toast.makeText(this, "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
    }

    private void confirmDeleteAccount() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Xoá tài khoản")
                .setMessage("Bạn có chắc muốn xoá tài khoản? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xoá", (dialog, which) -> deleteAccount())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deleteAccount() {
        String providerId = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getProviderData()
                .get(1) // index 1 là provider chính
                .getProviderId();

        if (providerId.equals("google.com")) {
            // Nếu là Google Sign-In
            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);

                FirebaseAuth.getInstance().getCurrentUser()
                        .reauthenticate(credential)
                        .addOnSuccessListener(unused -> {
                            proceedToDeleteAccount(FirebaseAuth.getInstance(), userId);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Xác thực lại thất bại", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Không tìm thấy tài khoản Google. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            }
        } else {
            // Nếu dùng Email/Password → yêu cầu nhập lại mật khẩu
            EditText input = new EditText(this);
            input.setHint("Nhập lại mật khẩu");

            new AlertDialog.Builder(this)
                    .setTitle("Xác thực tài khoản")
                    .setMessage("Vui lòng nhập lại mật khẩu để xác thực xoá tài khoản")
                    .setView(input)
                    .setPositiveButton("Xác nhận", (dialog, which) -> {
                        String password = input.getText().toString().trim();
                        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

                        FirebaseAuth.getInstance().getCurrentUser()
                                .reauthenticate(credential)
                                .addOnSuccessListener(unused -> {
                                    proceedToDeleteAccount(FirebaseAuth.getInstance(), userId);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Xác thực thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .setNegativeButton("Huỷ", null)
                    .show();
        }
    }
    private void proceedToDeleteAccount(FirebaseAuth auth, String userId) {
        FirebaseFirestore.getInstance().collection("profiles").document(userId).delete()
                .addOnSuccessListener(unused -> {
                    auth.getCurrentUser().delete()
                            .addOnSuccessListener(unused2 -> {
                                Toast.makeText(this, "Tài khoản đã được xoá", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Xoá Auth thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Xoá hồ sơ thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    private void reauthenticateAndDelete(AuthCredential credential) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        auth.getCurrentUser().reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    String userId = auth.getCurrentUser().getUid();

                    // Xoá Firestore
                    db.collection("profiles").document(userId).delete();

                    // Xoá Auth
                    auth.getCurrentUser().delete().addOnSuccessListener(task -> {
                        Toast.makeText(this, "Tài khoản đã bị xoá!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi khi xoá tài khoản", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Xác thực lại thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    private void promptReauthenticateWithPassword() {
        EditText input = new EditText(this);
        input.setHint("Nhập lại mật khẩu");

        new AlertDialog.Builder(this)
                .setTitle("Xác thực lại")
                .setMessage("Nhập lại mật khẩu của bạn để xác thực.")
                .setView(input)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String password = input.getText().toString().trim();
                    if (password.isEmpty()) {
                        Toast.makeText(this, "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    AuthCredential credential = EmailAuthProvider.getCredential(email, password);

                    FirebaseAuth.getInstance().getCurrentUser()
                            .reauthenticate(credential)
                            .addOnSuccessListener(unused -> {
                                proceedToDeleteAccount(FirebaseAuth.getInstance(), userId);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Xác thực thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
