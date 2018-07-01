package com.eriyaz.social.model;

/**
 * Created by vikas on 27/6/18.
 */

public class Avatar {
    private String id;
    private String name;
    private String imageUrl;

    public Avatar() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
