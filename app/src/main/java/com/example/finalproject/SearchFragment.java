package com.example.finalproject;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.adapters.SearchListingAdapter;
import com.example.finalproject.models.Listing;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchFragment extends Fragment {
    private EditText edtKeyword, edtMinPrice, edtMaxPrice, edtDistance;
    private Spinner spinnerCategory, spinnerCondition, spinnerSort, spinnerCity;
    private RecyclerView rvResults;
    private SearchListingAdapter adapter;
    private List<Listing> listingList = new ArrayList<>();
    private FirebaseFirestore db;
    private Handler handler = new Handler();
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        edtKeyword = view.findViewById(R.id.edtKeyword);
        edtMinPrice = view.findViewById(R.id.edtMinPrice);
        edtMaxPrice = view.findViewById(R.id.edtMaxPrice);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList("Tất cả", "TP.HCM", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Huế"));
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        spinnerCondition = view.findViewById(R.id.spinnerCondition);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        rvResults = view.findViewById(R.id.rvResults);

        adapter = new SearchListingAdapter(getContext(), listingList);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        setupDebouncedSearch();

        return view;
    }

    private void setupDebouncedSearch() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(searchRunnable);
                searchRunnable = () -> performSearch();
                handler.postDelayed(searchRunnable, 200);
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtKeyword.addTextChangedListener(textWatcher);
        edtMinPrice.addTextChangedListener(textWatcher);
        edtMaxPrice.addTextChangedListener(textWatcher);
        edtDistance.addTextChangedListener(textWatcher);
    }

    private void performSearch() {
        String keyword = edtKeyword.getText().toString().toLowerCase().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String sortOption = spinnerSort.getSelectedItem().toString();
        double minPrice = edtMinPrice.getText().toString().isEmpty() ? 0 : Double.parseDouble(edtMinPrice.getText().toString());
        double maxPrice = edtMaxPrice.getText().toString().isEmpty() ? Double.MAX_VALUE : Double.parseDouble(edtMaxPrice.getText().toString());
        String city = spinnerCity.getSelectedItem().toString();

        db.collection("listings").get().addOnSuccessListener(snapshot -> {
            listingList.clear();
            for (DocumentSnapshot doc : snapshot) {
                Listing item = doc.toObject(Listing.class);
                if (item == null || !item.isAvailable()) continue;

                boolean match = item.getTitle().toLowerCase().contains(keyword)
                        && (category.equals("Tất cả") || item.getCategory().equalsIgnoreCase(category))
                        && (condition.equals("Tất cả") || item.getCondition().equalsIgnoreCase(condition))
                        && item.getPrice() >= minPrice && item.getPrice() <= maxPrice
                        && (city.equals("Tất cả") || item.getCity().equalsIgnoreCase(city));

                if (match) listingList.add(item);
            }

            applySorting(sortOption);
        });
    }

    private void applySorting(String sortOption) {
        switch (sortOption) {
            case "Newest":
                Collections.sort(listingList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                break;
            case "Price ↑":
                Collections.sort(listingList, Comparator.comparingDouble(Listing::getPrice));
                break;
            case "Price ↓":
                Collections.sort(listingList, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
        }
        adapter.notifyDataSetChanged();
    }
}

