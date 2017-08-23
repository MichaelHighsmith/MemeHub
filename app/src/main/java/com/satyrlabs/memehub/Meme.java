package com.satyrlabs.memehub;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by mhigh on 8/17/2017.
 */

public class Meme {


    @SerializedName("imageUrl")
    @Expose
    private String imageUrl;

    @SerializedName("points")
    @Expose
    private Integer points;

    @SerializedName("username")
    @Expose
    private String username;

    @SerializedName("pushIdd")
    @Expose
    private String pushIdd;

    @SerializedName("usernameId")
    @Expose
    private String usernameId;

    public Meme(){}

    public Meme(String imageUrl, int points, String username, String pushIdd, String usernameId){
        this.imageUrl = imageUrl;
        this.points = points;
        this.username = username;
        this.pushIdd = pushIdd;
        this.usernameId = usernameId;
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

    public String getPushIdd() {
        return pushIdd;
    }

    public void setPushIdd(String pushIdd) {
        this.pushIdd = pushIdd;
    }

    public void setUsernameId(String usernameId) {
        this.usernameId = usernameId;
    }

    public String getUsernameId() {
        return usernameId;
    }
}
