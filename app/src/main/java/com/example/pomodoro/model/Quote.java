package com.example.pomodoro.model;

import com.google.gson.annotations.SerializedName;

public class Quote {
    private String firebaseKey;

    @SerializedName("q")
    private String text;

    @SerializedName("a")
    private String author;

    public Quote() {
    }

    public Quote(String text, String author) {
        this.text = text;
        this.author = author;
    }

    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
}