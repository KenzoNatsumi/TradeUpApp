package com.example.finalproject.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import android.app.AlertDialog;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.EditListingActivity;
import com.example.finalproject.R;
import com.example.finalproject.models.Listing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import java.util.Locale;

public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.MyViewHolder> {

    private final Context context;
    private final ActivityResultLauncher<Intent> editLauncher;
    private List<Listing> listingList;

    public MyListingAdapter(Context context, List<Listing> listingList, ActivityResultLauncher<Intent> editLauncher) {
        this.context = context;
        this.listingList = listingList != null ? listingList : new ArrayList<>();
        this.editLauncher = editLauncher;
    }

    public void setList(List<Listing> newList) {
        this.listingList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyListingAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing_preview, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Listing listing = listingList.get(position);

        holder.txtTitle.setText(listing.getTitle());
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        holder.txtPrice.setText("Giá: " + formatter.format(listing.getPrice()));
        holder.txtStatus.setText("Trạng thái: " + listing.getStatus());
        holder.txtQuantity.setText("Số lượng: " + listing.getQuantity());
        holder.txtViews.setText("Lượt xem: " + listing.getViews());

        // Nếu có txtTimestamp
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        holder.txtTimestamp.setText("Đăng ngày: " + sdf.format(new Date(listing.getTimestamp())));

        Glide.with(context)
                .load(listing.getImageUrl())
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgThumb);

        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditListingActivity.class);
            intent.putExtra("listingId", listing.getId());
            if (editLauncher != null) {
                editLauncher.launch(intent);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa sản phẩm này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("listings")
                                .document(listing.getId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    listingList.remove(position);
                                    notifyItemRemoved(position);
                                    Toast.makeText(context, "Đã xóa thành công", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Xóa thất bại", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }


    @Override
    public int getItemCount() {
        return listingList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtPrice, txtStatus, txtQuantity, txtViews, txtTimestamp;
        ImageView imgThumb;
        Button btnEdit, btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtViews = itemView.findViewById(R.id.txtViews);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp); // Nếu bạn thêm
            imgThumb = itemView.findViewById(R.id.imgThumbnail);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

}
