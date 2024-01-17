package com.guidopierri.pantrybe.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "item")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private long quantity;
    private String expirationDate;
    private long gtin;
    private String brand;
    private String image;
    private String category;
    @ManyToOne()
    @JsonIgnore
    @JoinColumn(name = "pantry")
    private Pantry pantry;

    public String toString() {
        return "Item(id=" + this.id + ", name=" + this.name + ", quantity=" + this.quantity + ", expirationDate=" + this.expirationDate + ", gtin=" + this.gtin + ", brand=" + this.brand + ", image=" + this.image + ", category=" + this.category + ")";
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public long getQuantity() {
        return this.quantity;
    }

    public String getExpirationDate() {
        return this.expirationDate;
    }

    public long getGtin() {
        return this.gtin;
    }

    public String getBrand() {
        return this.brand;
    }

    public String getImage() {
        return this.image;
    }

    public String getCategory() {
        return this.category;
    }

    public Pantry getPantry() {
        return this.pantry;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public void setGtin(long gtin) {
        this.gtin = gtin;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @JsonIgnore
    public void setPantry(Pantry pantry) {
        this.pantry = pantry;
    }
}
