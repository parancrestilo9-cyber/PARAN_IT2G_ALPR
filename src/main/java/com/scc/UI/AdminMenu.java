package com.scc.UI;

import com.scc.DAO.ActivityLogDao;
import com.scc.DAO.DatabaseConnection;
import com.scc.DAO.SecurityUtil;
import com.scc.DAO.UserDao;
import com.scc.MODEL.User;
import com.scc.MODEL.activityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {
    private final User currentUser;
    private final Scanner scanner;
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public AdminMenu(User currentUser, Scanner scanner) {
        this.currentUser = currentUser;
        this.scanner = scanner;
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n================ SYSTEM ADMINISTRATOR MENU ================");
            System.out.println("  [1] Manage Users (CRUD)");
            System.out.println("  [2] Manage Vehicles (CRUD)");
            System.out.println("  [3] Manage Activity & Audit Logs (CRUD)");
            System.out.println("  [4] Generate Daily Parking Occupancy & Peak Flow Report");
            System.out.println("  [0] Logout");
            System.out.println("===========================================================");
            System.out.print("Choose option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> manageUsersCrud();
                case "2" -> manageVehiclesCrud();
                case "3" -> new ActivityLogviewer(currentUser, scanner).showMenu();
                case "4" -> generateDailyReport();
                case "0" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("[!] Invalid choice.");
            }
        }
    }

    // ==================== USERS CRUD ====================
    private void manageUsersCrud() {
        while (true) {
            System.out.println("\n--- USERS MANAGEMENT (CRUD) ---");
            System.out.println("  [1] CREATE - Add New User");
            System.out.println("  [2] READ   - View All Users");
            System.out.println("  [3] UPDATE - Edit User Details / Role");
            System.out.println("  [4] DELETE - Deactivate User");
            System.out.println("  [0] Back");
            System.out.print("Select: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createUser();
                case "2" -> readUsers();
                case "3" -> updateUser();
                case "4" -> deleteUser();
                case "0" -> { return; }
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private void createUser() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        System.out.print("Role (SYSTEM_ADMIN / SECURITY_OPERATOR / VEHICLE_OWNER): ");
        String role = scanner.nextLine().trim().toUpperCase();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Contact Number: ");
        String contact = scanner.nextLine().trim();
        System.out.print("Emergency Contact: ");
        String emergency = scanner.nextLine().trim();

        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(SecurityUtil.hashPassword(password));
        u.setRole(role);
        u.setEmail(email);
        u.setContactNumber(contact);
        u.setEmergencyContact(emergency);
        u.setActive(true);

        if (userDao.createUser(u)) {
            logAudit("USER_CREATED", "Created user: " + username + " (" + role + ")");
            System.out.println("[OK] User created successfully.");
        } else {
            System.out.println("[X] Failed to create user.");
        }
    }

    private void readUsers() {
        List<User> users = userDao.getAllUsers(true);
        System.out.printf("\n%-5s %-15s %-18s %-25s %-10s%n", "ID", "USERNAME", "ROLE", "EMAIL", "STATUS");
        System.out.println("---------------------------------------------------------------------------");
        for (User u : users) {
            System.out.printf("%-5d %-15s %-18s %-25s %-10s%n",
                    u.getUserId(), u.getUsername(), u.getRole(), u.getEmail(),
                    u.isActive() ? "ACTIVE" : "INACTIVE");
        }
    }

    private void updateUser() {
        readUsers();
        System.out.print("Enter User ID to update: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        User u = userDao.getUserById(id);
        if (u == null) {
            System.out.println("[X] User not found.");
            return;
        }
        System.out.print("New Email (" + u.getEmail() + "): ");
        String email = scanner.nextLine().trim();
        if (!email.isEmpty()) u.setEmail(email);

        System.out.print("New Contact (" + u.getContactNumber() + "): ");
        String contact = scanner.nextLine().trim();
        if (!contact.isEmpty()) u.setContactNumber(contact);

        System.out.print("New Role (" + u.getRole() + "): ");
        String role = scanner.nextLine().trim().toUpperCase();
        if (!role.isEmpty()) u.setRole(role);

        System.out.print("New Password (leave blank to keep current): ");
        String pass = scanner.nextLine().trim();
        boolean changePass = !pass.isEmpty();
        if (changePass) u.setPasswordHash(SecurityUtil.hashPassword(pass));

        if (userDao.updateUser(u, changePass)) {
            logAudit("USER_UPDATED", "Updated user_id=" + id);
            System.out.println("[OK] User updated.");
        } else {
            System.out.println("[X] Update failed.");
        }
    }

    private void deleteUser() {
        readUsers();
        System.out.print("Enter User ID to deactivate: ");
        int id = Integer.parseInt(scanner.nextLine().trim());
        if (userDao.deactivateUser(id)) {
            logAudit("USER_DEACTIVATED", "Deactivated user_id=" + id);
            System.out.println("[OK] User deactivated.");
        } else {
            System.out.println("[X] Deactivation failed.");
        }
    }

    // ==================== VEHICLES CRUD ====================
    private void manageVehiclesCrud() {
        while (true) {
            System.out.println("\n--- VEHICLES MANAGEMENT (CRUD) ---");
            System.out.println("  [1] CREATE - Register New Vehicle");
            System.out.println("  [2] READ   - View / Search Vehicles");
            System.out.println("  [3] UPDATE - Change Access Tag (VIP/RESIDENT/VISITOR/BLACKLISTED)");
            System.out.println("  [4] DELETE - Archive / Deactivate Vehicle");
            System.out.println("  [0] Back");
            System.out.print("Select: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createVehicle();
                case "2" -> readVehicles();
                case "3" -> updateVehicle();
                case "4" -> deleteVehicle();
                case "0" -> { return; }
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    private void createVehicle() {
        System.out.print("Plate Number (e.g., ABC-1234): ");
        String plate = SecurityUtil.normalizePlate(scanner.nextLine());
        System.out.print("Owner Full Name: ");
        String owner = scanner.nextLine().trim();
        System.out.print("Vehicle Type (Sedan/SUV/Van/Motorcycle): ");
        String type = scanner.nextLine().trim();
        System.out.print("Access Status (VIP / RESIDENT / VISITOR / BLACKLISTED): ");
        String status = scanner.nextLine().trim().toUpperCase();
        System.out.print("Linked Owner User ID (or press Enter for none): ");
        String uidStr = scanner.nextLine().trim();

        String sql = "INSERT INTO Vehicles (user_id, plate_number, owner_name, vehicle_type, access_status, registration_date, active) VALUES (?, ?, ?, ?, ?, ?, 1);";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (uidStr.isEmpty()) ps.setNull(1, Types.INTEGER); else ps.setInt(1, Integer.parseInt(uidStr));
            ps.setString(2, plate);
            ps.setString(3, owner);
            ps.setString(4, type);
            ps.setString(5, status);
            ps.setString(6, LocalDateTime.now().toString());
            ps.executeUpdate();
            logAudit("VEHICLE_CREATED", "Registered vehicle " + plate + " [" + status + "]");
            System.out.println("[OK] Vehicle registered.");
        } catch (SQLException e) {
            System.out.println("[X] Error registering vehicle: " + e.getMessage());
        }
    }

    private void readVehicles() {
        System.out.print("Search by Plate or Owner (leave blank for all): ");
        String keyword = scanner.nextLine().trim().toUpperCase();
        String sql = "SELECT * FROM Vehicles WHERE UPPER(plate_number) LIKE ? OR UPPER(owner_name) LIKE ? ORDER BY vehicle_id ASC;";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("\n%-5s %-12s %-20s %-12s %-14s %-10s%n", "ID", "PLATE", "OWNER", "TYPE", "ACCESS_TAG", "STATE");
                System.out.println("-------------------------------------------------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-5d %-12s %-20s %-12s %-14s %-10s%n",
                            rs.getInt("vehicle_id"),
                            rs.getString("plate_number"),
                            rs.getString("owner_name"),
                            rs.getString("vehicle_type"),
                            rs.getString("access_status"),
                            rs.getInt("active") == 1 ? "ACTIVE" : "ARCHIVED");
                }
            }
        } catch (SQLException e) {
            System.out.println("[X] Error reading vehicles: " + e.getMessage());
        }
    }

    private void updateVehicle() {
        readVehicles();
        System.out.print("Enter Vehicle ID to update: ");
        int vid = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("New Access Status (VIP / RESIDENT / VISITOR / BLACKLISTED): ");
        String status = scanner.nextLine().trim().toUpperCase();

        String sql = "UPDATE Vehicles SET access_status = ? WHERE vehicle_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, vid);
            if (ps.executeUpdate() > 0) {
                logAudit("VEHICLE_UPDATED", "Updated vehicle_id=" + vid + " to " + status);
                System.out.println("[OK] Vehicle status updated.");
            }
        } catch (SQLException e) {
            System.out.println("[X] Error updating vehicle: " + e.getMessage());
        }
    }

    private void deleteVehicle() {
        readVehicles();
        System.out.print("Enter Vehicle ID to archive/deactivate: ");
        int vid = Integer.parseInt(scanner.nextLine().trim());
        String sql = "UPDATE Vehicles SET active = 0, access_status = 'INACTIVE' WHERE vehicle_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vid);
            if (ps.executeUpdate() > 0) {
                logAudit("VEHICLE_ARCHIVED", "Archived vehicle_id=" + vid);
                System.out.println("[OK] Vehicle archived.");
            }
        } catch (SQLException e) {
            System.out.println("[X] Error archiving vehicle: " + e.getMessage());
        }
    }

    // ==================== REPORT ====================
    private void generateDailyReport() {
        int totalCapacity = 100;
        int activeVehicles = 0;
        int totalScans = 0;
        int overrides = 0;

        try (Connection conn = DatabaseConnection.getConnection(); Statement st = conn.createStatement()) {
            try (ResultSet r1 = st.executeQuery("SELECT COUNT(*) FROM Vehicles WHERE active = 1")) {
                if (r1.next()) activeVehicles = r1.getInt(1);
            }
            try (ResultSet r2 = st.executeQuery("SELECT COUNT(*) FROM Detection_Logs WHERE deleted = 0")) {
                if (r2.next()) totalScans = r2.getInt(1);
            }
            try (ResultSet r3 = st.executeQuery("SELECT COUNT(*) FROM Gate_Overrides")) {
                if (r3.next()) overrides = r3.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("[X] Report error: " + e.getMessage());
            return;
        }

        System.out.println("\n=======================================================");
        System.out.println("     DAILY PARKING OCCUPANCY & PEAK FLOW REPORT");
        System.out.println("=======================================================");
        System.out.println("  Report Timestamp         : " + LocalDateTime.now());
        System.out.println("  Registered Active Plates : " + activeVehicles);
        System.out.println("  Total Gate Detections    : " + totalScans);
        System.out.println("  Manual Gate Overrides    : " + overrides);
        System.out.println("  Remaining Slot Capacity  : " + Math.max(0, totalCapacity - totalScans) + " / " + totalCapacity);
        System.out.println("=======================================================");
    }

    private void logAudit(String eventType, String details) {
        activityLog log = new activityLog();
        log.setOperatorId(currentUser.getUserId());
        log.setEventType(eventType);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        activityLogDao.createLog(log);
    }
}