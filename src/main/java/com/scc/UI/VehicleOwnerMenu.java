package com.scc.UI;

import com.scc.DAO.DatabaseConnection;
import com.scc.DAO.SecurityUtil;
import com.scc.DAO.UserDao;
import com.scc.MODEL.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class VehicleOwnerMenu {
    private final User currentUser;
    private final Scanner scanner;
    private final UserDao userDao = new UserDao();

    public VehicleOwnerMenu(User currentUser, Scanner scanner) {
        this.currentUser = currentUser;
        this.scanner = scanner;
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n============= REGISTERED VEHICLE OWNER MENU =============");
            System.out.println("  [1] CREATE - Register My Vehicle");
            System.out.println("  [2] READ   - View My Vehicles & Entry/Exit History");
            System.out.println("  [3] UPDATE - Edit My Contact / Emergency Contact Info");
            System.out.println("  [4] DELETE - Deactivate My Vehicle Record");
            System.out.println("  [0] Logout");
            System.out.println("=========================================================");
            System.out.print("Select option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> registerMyVehicle();
                case "2" -> viewMyVehiclesAndLogs();
                case "3" -> updateMyContactInfo();
                case "4" -> deactivateMyVehicle();
                case "0" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    // CREATE
    private void registerMyVehicle() {
        System.out.print("Enter Plate Number: ");
        String plate = SecurityUtil.normalizePlate(scanner.nextLine());
        System.out.print("Enter Vehicle Type (Sedan/SUV/Motorcycle): ");
        String type = scanner.nextLine().trim();

        String sql = "INSERT INTO Vehicles (user_id, plate_number, owner_name, vehicle_type, access_status, registration_date, active) VALUES (?, ?, ?, ?, 'RESIDENT', ?, 1);";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getUserId());
            ps.setString(2, plate);
            ps.setString(3, currentUser.getUsername());
            ps.setString(4, type);
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
            System.out.println("[OK] Vehicle " + plate + " registered under your account.");
        } catch (SQLException e) {
            System.out.println("[X] Registration failed: " + e.getMessage());
        }
    }

    // READ
    private void viewMyVehiclesAndLogs() {
        System.out.println("\n--- MY REGISTERED VEHICLES ---");
        String sql = "SELECT * FROM Vehicles WHERE user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                System.out.printf("%-5s %-12s %-12s %-12s %-10s%n", "ID", "PLATE", "TYPE", "STATUS", "STATE");
                while (rs.next()) {
                    System.out.printf("%-5d %-12s %-12s %-12s %-10s%n",
                            rs.getInt("vehicle_id"),
                            rs.getString("plate_number"),
                            rs.getString("vehicle_type"),
                            rs.getString("access_status"),
                            rs.getInt("active") == 1 ? "ACTIVE" : "INACTIVE");
                }
            }
        } catch (SQLException e) {
            System.out.println("[X] Error loading vehicles: " + e.getMessage());
        }

        System.out.println("\n--- MY VEHICLE ACCESS HISTORY ---");
        String logSql = """
            SELECT d.log_id, d.detected_plate, d.gate_id, d.validation_status, d.entry_timestamp, d.exit_timestamp
            FROM Detection_Logs d
            JOIN Vehicles v ON d.vehicle_id = v.vehicle_id
            WHERE v.user_id = ? AND d.deleted = 0
            ORDER BY d.log_id DESC;
            """;
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(logSql)) {
            ps.setInt(1, currentUser.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("Log #%d | %s | %s | %s | Entry: %s | Exit: %s%n",
                            rs.getInt("log_id"),
                            rs.getString("detected_plate"),
                            rs.getString("gate_id"),
                            rs.getString("validation_status"),
                            rs.getString("entry_timestamp"),
                            rs.getString("exit_timestamp"));
                }
            }
        } catch (SQLException e) {
            System.out.println("[X] Error loading logs: " + e.getMessage());
        }
    }

    // UPDATE
    private void updateMyContactInfo() {
        System.out.print("New Email (" + currentUser.getEmail() + "): ");
        String email = scanner.nextLine().trim();
        if (!email.isEmpty()) currentUser.setEmail(email);

        System.out.print("New Contact Number (" + currentUser.getContactNumber() + "): ");
        String contact = scanner.nextLine().trim();
        if (!contact.isEmpty()) currentUser.setContactNumber(contact);

        System.out.print("New Emergency Contact (" + currentUser.getEmergencyContact() + "): ");
        String emergency = scanner.nextLine().trim();
        if (!emergency.isEmpty()) currentUser.setEmergencyContact(emergency);

        if (userDao.updateUser(currentUser, false)) {
            System.out.println("[OK] Personal & emergency contact updated.");
        } else {
            System.out.println("[X] Update failed.");
        }
    }

    // DELETE
    private void deactivateMyVehicle() {
        viewMyVehiclesAndLogs();
        System.out.print("Enter your Vehicle ID to deactivate: ");
        int vid = Integer.parseInt(scanner.nextLine().trim());
        String sql = "UPDATE Vehicles SET active = 0, access_status = 'INACTIVE' WHERE vehicle_id = ? AND user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vid);
            ps.setInt(2, currentUser.getUserId());
            if (ps.executeUpdate() > 0) {
                System.out.println("[OK] Vehicle deactivated.");
            } else {
                System.out.println("[X] Vehicle ID not found under your account.");
            }
        } catch (SQLException e) {
            System.out.println("[X] Deactivation error: " + e.getMessage());
        }
    }
}