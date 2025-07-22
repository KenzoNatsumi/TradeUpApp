package com.example.finalproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.EditListingActivity;
import com.example.finalproject.R;
import com.example.finalproject.models.Listing;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.ViewHolder> {

    private final Context context;
    private final List<Listing> listingList;
    private final ActivityResultLauncher<Intent> editLauncher;

    public MyListingAdapter(Context context, List<Listing> listingList, ActivityResultLauncher<Intent> editLauncher) {
        this.context = context;
        this.listingList = listingList;
        this.editLauncher = editLauncher;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listing listing = listingList.get(position);
        holder.txtTitle.setText(listing.getTitle());
        holder.txtPrice.setText(listing.getPrice() + " đ");
        holder.txtLocation.setText(listing.getLocation());
        holder.txtQuantity.setText("Số lượng: " + listing.getQuantity());
        holder.txtStatus.setText("Trạng thái: " + listing.getStatus());

        Glide.with(context)
                .load(listing.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgThumbnail);

        holder.btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(context, EditListingActivity.class);
            i.putExtra("listingId", listing.getId());
            editLauncher.launch(i);
        });

        holder.btnDelete.setOnClickListener(v -> {
            FirebaseFirestore.getInstance()
                    .collection("listings")
                    .document(listing.getId())
                    .delete()
                    .addOnSuccessListener(unused -> {
                        listingList.remove(position);
                        notifyItemRemoved(position);
                    });
        });
        holder.txtViews.setText("Lượt xem: " + listing.getViews());
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtPrice, txtLocation, txtStatus, txtQuantity, txtViews;
        ImageView imgThumbnail;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            txtViews = itemView.findViewById(R.id.txtViews);
        }
    }
}
