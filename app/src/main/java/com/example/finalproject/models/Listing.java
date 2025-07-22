package com.example.finalproject.models;

public class Listing {
    private String id;
    private String userId;
    private String title;
    private String description;
    private double price;
    private String category;
    private String imageUrl;
    private String status;
    private long timestamp;
    private String location;
    private String condition; // ✅ THÊM dòng này
    private int quantity;
    private int views;

    public Listing() {}

    public Listing(String id, String userId, String title, String description, double price, String category, String imageUrl, String status, long timestamp, String location, int quantity) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.status = status;
        this.timestamp = timestamp;
        this.location = location;
        this.quantity = quantity;
    }

    // Getter và Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; } // ✅ sửa lại

    public void setPrice(double price) { this.price = price; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public String getCondition() { return condition != null ? condition : ""; }
    public void setCondition(String condition) { this.condition = condition; }

    public boolean isAvailable() {
        return status != null && status.equalsIgnoreCase("Available");
    }
    public String getCity() {
        return getLocation();
    }

}
