package com.satyrlabs.memehub;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by mhigh on 8/17/2017.
 */

public class Meme {

    @SerializedName("title")
    @Expose
    private String title;

    @SerializedName("imageUrl")
    @Expose
    private String imageUrl;

    @SerializedName("points")
    @Expose
    private Integer points;

    @SerializedName("username")
    @Expose
    private String username;

    public Meme(){}

    public Meme(String title, String imageUrl, int points, String username){
        this.title = title;
        this.imageUrl = imageUrl;
        this.points = points;
        this.username = username;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
