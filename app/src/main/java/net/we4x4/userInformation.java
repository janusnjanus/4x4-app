package net.we4x4;


public class userInformation {
    private String email;
    private String username;
    private Long rank;

    private userInformation(){

    }
    userInformation(String username, String email, Long rank){
        this.username = username;
        this.email = email;
        this.rank = rank;

    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }
    public Long getRank() {
        return rank;
    }
}