package com.hzontal.data;

import com.google.gson.annotations.SerializedName;


public class FeedbackEntity {
    @SerializedName("name")
    public String name;

    @SerializedName("email")
    public String email;

    @SerializedName("message")
    public String message;
}