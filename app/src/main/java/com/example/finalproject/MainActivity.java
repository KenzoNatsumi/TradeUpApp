package com.example.finalproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    ImageView imgAvatar;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgAvatar = findViewById(R.id.imgAvatar);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadAvatar();

        imgAvatar.setOnClickListener(v -> showPopupMenu());
    }

    private void showPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(this, imgAvatar);
        popupMenu.getMenuInflater().inflate(R.menu.menu_avatar_popup, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.menu_edit_profile) {
                startActivityForResult(new Intent(this, EditProfileActivity.class), 1001);
                return true;
            } else if (id == R.id.menu_add_listing) {
                startActivity(new Intent(this, AddListingActivity.class));
                return true;
            } else if (id == R.id.menu_my_listings) {
                startActivity(new Intent(this, MyListingActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                mAuth.signOut();
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void loadAvatar() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("profiles").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String avatarUrl = doc.getString("avatarUri");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(avatarUrl)
                                .circleCrop()
                                .error(R.drawable.ic_default_avatar)
                                .into(imgAvatar);
                    } else {
                        imgAvatar.setImageResource(R.drawable.ic_default_avatar);
                    }
                })
                .addOnFailureListener(e -> {
                    imgAvatar.setImageResource(R.drawable.ic_default_avatar);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            loadAvatar();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAvatar();
    }
}
