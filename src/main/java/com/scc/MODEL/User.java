package com.scc.MODEL;

import java.time.LocalDateTime;

public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String role; // SYSTEM_ADMIN, SECURITY_OPERATOR, VEHICLE_OWNER
    private String email;
    private String contactNumber;
    private String emergencyContact;
    private boolean active;
    private LocalDateTime lastLogin;

    public User() {}

    public User(int userId, String username, String passwordHash, String role, String email,
                String contactNumber, String emergencyContact, boolean active, LocalDateTime lastLogin) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.email = email;
        this.contactNumber = contactNumber;
        this.emergencyContact = emergencyContact;
        this.active = active;
        this.lastLogin = lastLogin;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    @Override
    public String toString() {
        return userId + " - " + username + " (" + role + ")";
    }
}