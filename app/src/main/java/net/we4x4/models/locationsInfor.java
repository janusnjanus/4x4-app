package net.we4x4.models;

/**
 * Created by PK on 16-05-05.
 */
public class locationsInfor {
    String author;
    long latitude;
    long longitude;

    private locationsInfor(){

    }

    locationsInfor( String author, long latitude, long longitude){
        this.author = author;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getAuthor() {
        return author;
    }

    public long getLatitude() {
        return latitude;
    }

    public long getLongitude() {
        return longitude;
    }
}
