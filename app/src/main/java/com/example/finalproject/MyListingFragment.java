package com.example.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

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
import com.google.firebase.firestore.*;

import java.util.*;

public class MyListingFragment extends Fragment {

    private EditText edtKeyword, edtMinPrice, edtMaxPrice;
    private Spinner spinnerCategory, spinnerCondition, spinnerSort, spinnerCity;
    private RecyclerView rvResults;
    private MyListingAdapter adapter;
    private List<Listing> listingList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private ActivityResultLauncher<Intent> editLauncher;

    private Handler handler = new Handler();
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Ánh xạ View
        edtKeyword = view.findViewById(R.id.edtKeyword);
        edtMinPrice = view.findViewById(R.id.edtMinPrice);
        edtMaxPrice = view.findViewById(R.id.edtMaxPrice);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCondition = view.findViewById(R.id.spinnerCondition);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        rvResults = view.findViewById(R.id.rvResults);

        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyListingAdapter(getContext(), listingList, getEditLauncher());
        rvResults.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupSpinners();
        setupDebounce();
        loadMyListings();

        return view;
    }

    private ActivityResultLauncher<Intent> getEditLauncher() {
        if (editLauncher == null) {
            editLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            performSearch();
                        }
                    });
        }
        return editLauncher;
    }

    private void setupSpinners() {
        // City
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Tất cả", "Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Khác"));
        spinnerCity.setAdapter(cityAdapter);
        spinnerCity.setOnItemSelectedListener(new SimpleListener());

        // Category
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Tất cả", "Điện tử", "Thời trang", "Sách", "Khác"));
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setOnItemSelectedListener(new SimpleListener());

        // Condition
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Tất cả", "Mới", "Đã sử dụng"));
        spinnerCondition.setAdapter(conditionAdapter);
        spinnerCondition.setOnItemSelectedListener(new SimpleListener());

        // Sort
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Liên quan", "Mới nhất", "Giá ↑", "Giá ↓"));
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new SimpleListener());
    }

    private void setupDebounce() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
                searchRunnable = MyListingFragment.this::performSearch;
                handler.postDelayed(searchRunnable, 200);
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtKeyword.addTextChangedListener(watcher);
        edtMinPrice.addTextChangedListener(watcher);
        edtMaxPrice.addTextChangedListener(watcher);
    }

    private void loadMyListings() {
        db.collection("listings")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    listingList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Listing listing = doc.toObject(Listing.class);
                        if (listing != null) {
                            listing.setId(doc.getId());
                            listing.setQuantity(doc.getLong("quantity").intValue());
                            listingList.add(listing);
                        }
                    }
                    performSearch(); // gọi ngay sau khi load
                });
    }

    private void performSearch() {
        String keyword = edtKeyword.getText().toString().toLowerCase().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String sort = spinnerSort.getSelectedItem().toString();
        String city = spinnerCity.getSelectedItem().toString();

        double minPrice = edtMinPrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(edtMinPrice.getText().toString());
        double maxPrice = edtMaxPrice.getText().toString().isEmpty() ? Double.MAX_VALUE : Double.parseDouble(edtMaxPrice.getText().toString());

        List<Listing> filtered = new ArrayList<>();

        for (Listing l : listingList) {
            boolean match = l.getTitle().toLowerCase().contains(keyword)
                    && (category.equals("Tất cả") || l.getCategory().equalsIgnoreCase(category))
                    && (condition.equals("Tất cả") || l.getCondition().equalsIgnoreCase(condition))
                    && (city.equals("Tất cả") || (l.getLocation() != null && l.getLocation().equalsIgnoreCase(city)))
                    && l.getPrice() >= minPrice && l.getPrice() <= maxPrice;

            if (match) filtered.add(l);
        }

        switch (sort) {
            case "Mới nhất":
                filtered.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                break;
            case "Giá ↑":
                filtered.sort(Comparator.comparingDouble(Listing::getPrice));
                break;
            case "Giá ↓":
                filtered.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            default:
                break;
        }

        adapter.setList(filtered);
    }

    private class SimpleListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            performSearch();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {}
    }
}
