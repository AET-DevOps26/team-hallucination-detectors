package de.tum.devops.vibeshield.dto;

/**
 * Request body for the reset-password endpoint: the reset token and the new password.
 */
public class ResetPasswordRequest {

    private String token;
    private String newPassword;

    public ResetPasswordRequest() {
    }

    public String getToken() {
        return token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
