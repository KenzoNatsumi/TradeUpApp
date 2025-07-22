package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.adapters.MyListingAdapter;
import com.example.finalproject.models.Listing;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyListingFragment extends Fragment {

    private RecyclerView recyclerView;
    private MyListingAdapter adapter;
    private List<Listing> listingList;
    private FirebaseFirestore db;
    private String currentUserId;

    private ActivityResultLauncher<Intent> editLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_listings, container, false);

        recyclerView = view.findViewById(R.id.recyclerMyListings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listingList = new ArrayList<>();

        // Đăng ký launcher để reload khi sửa xong
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadMyListings();
                    }
                });

        adapter = new MyListingAdapter(getContext(), listingList, editLauncher);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadMyListings();

        return view;
    }

    private void loadMyListings() {
        db.collection("listings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listingList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Listing listing = doc.toObject(Listing.class);
                        listing.setId(doc.getId());
                        listing.setQuantity(doc.getLong("quantity").intValue());
                        listingList.add(listing);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show());
    }
}
