package edu.northeastern.stutrade.Models;

public class User {

    public String username;
    private String email;

    private String name;

    public User(String name, String email) {
        this.username = name.replaceAll(" ", "");
        this.email = email;
        this.name = name;
    }
}
