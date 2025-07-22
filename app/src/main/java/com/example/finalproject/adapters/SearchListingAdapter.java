package com.example.finalproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.models.Listing;

import java.util.List;

public class SearchListingAdapter extends RecyclerView.Adapter<SearchListingAdapter.ViewHolder> {

    private Context context;
    private List<Listing> searchResults;
    private List<Listing> listingList;

    public SearchListingAdapter(Context context, List<Listing> searchResults) {
        this.context = context;
        this.searchResults = searchResults;
    }
    public void updateList(List<Listing> newList) {
        this.listingList = newList;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public SearchListingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchListingAdapter.ViewHolder holder, int position) {
        Listing listing = searchResults.get(position);

        holder.txtTitle.setText(listing.getTitle());
        holder.txtPrice.setText(listing.getPrice() + " đ");
        holder.txtLocation.setText(listing.getLocation());
        holder.txtQuantity.setText("Số lượng: " + listing.getQuantity());
        holder.txtStatus.setText("Trạng thái: " + listing.getStatus());
        holder.txtViews.setText("Lượt xem: " + listing.getViews());

        Glide.with(context).load(listing.getImageUrl()).into(holder.imgThumbnail);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtPrice, txtLocation, txtQuantity, txtStatus, txtViews;
        ImageView imgThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtViews = itemView.findViewById(R.id.txtViews);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
        }
    }
}
