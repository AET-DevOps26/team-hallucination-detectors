package de.tum.devops.vibeshield.dto;

/**
 * Request body for the registration endpoint: the new user's email and password.
 */
public class RegisterRequest {

    private String email;
    private String password;

    public RegisterRequest() {
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}