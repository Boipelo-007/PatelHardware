package com.patelhardware.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Items {
    private int itemId;
    private String itemNumber;
    private String name;
    private String color;
    private String description;
    private BigDecimal price;
    private boolean available;
    private int stockQuantity;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public Items() {}

    // Constructor with essential fields
    public Items(String itemNumber, String name, String color, String description, BigDecimal price) {
        this.itemNumber = itemNumber;
        this.name = name;
        this.color = color;
        this.description = description;
        this.price = price;
    }

    // Full constructor
    public Items(int itemId, String itemNumber, String name, String color, String description,
                 BigDecimal price, boolean available, int stockQuantity, String category,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.itemId = itemId;
        this.itemNumber = itemNumber;
        this.name = name;
        this.color = color;
        this.description = description;
        this.price = price;
        this.available = available;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Items{" +
                "itemId=" + itemId +
                ", itemNumber='" + itemNumber + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", available=" + available +
                ", stockQuantity=" + stockQuantity +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}