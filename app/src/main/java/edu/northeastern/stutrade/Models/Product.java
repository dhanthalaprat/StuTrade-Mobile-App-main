package edu.northeastern.stutrade.Models;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Product implements Serializable {
    private String productPrice;
    private String sellerId;
    private String productName;
    private String datePosted;
    private String productDescription;
    private String sellerName;
    private String imageUrl;

    public Product() {
    }

    public Product(String productName, String description, String price, String imageUrl, String sellerName, String sellerId, String datePosted) {
        this.productName = productName;
        this.productDescription = description;
        this.productPrice = price;
        this.imageUrl = imageUrl;
        this.sellerName = sellerName;
        this.datePosted = datePosted;
        this.sellerId = sellerId;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setSellerId(String userId) {
        this.sellerId = userId;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setProductPrice(String productPrice) {
        this.productPrice = productPrice;
    }

    public String getProductName() {
        return productName;
    }

    public void setName(String productName) {
        this.productName = productName;
    }

    public String getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(String datePosted) {
        this.datePosted = datePosted;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public Double getPriceAsDouble() {
        try {
            return Double.parseDouble(productPrice);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public Date getDatePostedAsDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);

        try {
            return sdf.parse(datePosted);
        } catch (ParseException e) {
            return new Date();
        }
    }



}