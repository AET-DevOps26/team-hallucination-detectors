package de.tum.devops.vibeshield.dto;

/**
 * Request body for the forgot-password endpoint: the email to send a reset link to.
 */
public class ForgotPasswordRequest {

    private String email;

    public ForgotPasswordRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
