package org.example;

public class User {
    private String login;
    private String password;
    private String username;

    private UserRole role;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getUserRole() {
        return role;
    }

    public User(String login, String password, String username, UserRole role) {
        this.login = login;
        this.password = password;
        this.username = username;
        this.role = role;
    }
}
