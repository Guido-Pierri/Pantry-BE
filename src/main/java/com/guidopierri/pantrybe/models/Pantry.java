package com.guidopierri.pantrybe.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity

@Table(name = "pantry")
public class Pantry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToMany(mappedBy = "pantry", fetch = FetchType.EAGER)
    private List<Item> items;

    @OneToOne
    @JsonIgnore
    @JoinColumn(name = "application_user")
    private User user;

    public Pantry() {
    }

    /*@OneToOne
    @JsonIgnore
    @JoinColumn(name = "application_user")
    private User application_user;*/
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    @Override
    public String toString() {
        return "Pantry{" +
                "id=" + id +
                ", items=" + items +
                '}';
    }
}
