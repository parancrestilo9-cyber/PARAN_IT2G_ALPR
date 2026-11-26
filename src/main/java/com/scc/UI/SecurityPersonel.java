package com.scc.UI;

import com.scc.DAO.ActivityLogDao;
import com.scc.DAO.DatabaseConnection;
import com.scc.DAO.SecurityUtil;
import com.scc.MODEL.User;
import com.scc.MODEL.activityLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Scanner;

public class SecurityPersonel {
    private final User currentUser;
    private final Scanner scanner;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public SecurityPersonel(User currentUser, Scanner scanner) {
        this.currentUser = currentUser;
        this.scanner = scanner;
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n============= SECURITY PERSONNEL / GATE OPERATOR =============");
            System.out.println("  [1] CREATE - Simulate Camera Plate Scan & Entry/Exit Validation");
            System.out.println("  [2] READ   - View Live Detection Logs & Vehicle Lookup");
            System.out.println("  [3] UPDATE - Execute Manual Gate Override / Log Exit Time");
            System.out.println("  [4] DELETE - Soft-Delete Expired Visitor Detection Log");
            System.out.println("  [0] Logout");
            System.out.println("==============================================================");
            System.out.print("Select option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> simulateScan();
                case "2" -> viewDetections();
                case "3" -> manualGateOverrideOrExit();
                case "4" -> softDeleteDetection();
                case "0" -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }

    // CREATE
    private void simulateScan() {
        System.out.print("Gate ID (GATE-01-ENTRY / GATE-02-EXIT): ");
        String gate = scanner.nextLine().trim().toUpperCase();
        if (gate.isEmpty()) gate = "GATE-01-ENTRY";

        System.out.print("Detected License Plate: ");
        String plate = SecurityUtil.normalizePlate(scanner.nextLine());
        System.out.print("OCR Confidence Score (0-100): ");
        double conf = Double.parseDouble(scanner.nextLine().trim());

        Integer vehicleId = null;
        String accessStatus = "UNREGISTERED";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT vehicle_id, owner_name, access_status, active FROM Vehicles WHERE plate_number = ?;")) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    vehicleId = rs.getInt("vehicle_id");
                    accessStatus = rs.getInt("active") == 1 ? rs.getString("access_status") : "INACTIVE";
                    System.out.println("-> Matched Owner: " + rs.getString("owner_name") + " | Tag: " + accessStatus);
                }
            }
        } catch (SQLException e) {
            System.out.println("[X] Lookup error: " + e.getMessage());
            return;
        }

        String validationStatus;
        if (conf < 60.0) {
            validationStatus = "FAILED_OCR";
            System.out.println("[ALERT] FAILED OCR READ (" + conf + "%). Manual verification required.");
        } else if ("BLACKLISTED".equals(accessStatus) || "INACTIVE".equals(accessStatus)) {
            validationStatus = "DENIED_" + accessStatus;
            System.out.println("[STOP] ACCESS DENIED (" + accessStatus + ") for " + plate);
        } else if ("VIP".equals(accessStatus) || "RESIDENT".equals(accessStatus) || "VISITOR".equals(accessStatus)) {
            validationStatus = "GRANTED_" + accessStatus;
            System.out.println("[PASS] GATE OPENED - ACCESS GRANTED (" + accessStatus + ") for " + plate);
        } else {
            validationStatus = "VISITOR_UNREGISTERED";
            System.out.println("[INFO] Unregistered plate [" + plate + "] logged as visitor.");
        }

        String sql = "INSERT INTO Detection_Logs (vehicle_id, detected_plate, confidence_score, entry_timestamp, gate_id, validation_status, deleted) VALUES (?, ?, ?, ?, ?, ?, 0);";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (vehicleId != null) ps.setInt(1, vehicleId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, plate);
            ps.setDouble(3, conf);
            ps.setString(4, LocalDateTime.now().toString());
            ps.setString(5, gate);
            ps.setString(6, validationStatus);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[X] Error saving detection: " + e.getMessage());
        }

        activityLog log = new activityLog();
        log.setVehicleId(vehicleId);
        log.setOperatorId(currentUser.getUserId());
        log.setEventType(validationStatus);
        log.setPlateNumber(plate);
        log.setConfidenceScore(conf);
        log.setGateId(gate);
        log.setDetails("Camera scan at " + gate + ": " + validationStatus);
        log.setEntryTimestamp(LocalDateTime.now());
        log.setCreatedAt(LocalDateTime.now());
        activityLogDao.createLog(log);
    }

    // READ
    private void viewDetections() {
        String sql = "SELECT * FROM Detection_Logs WHERE deleted = 0 ORDER BY log_id DESC;";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            System.out.printf("\n%-6s %-12s %-8s %-15s %-22s %-20s%n",
                    "LOG_ID", "PLATE", "CONF%", "GATE", "STATUS", "ENTRY_TIME");
            System.out.println("---------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-6d %-12s %-8.1f %-15s %-22s %-20s%n",
                        rs.getInt("log_id"),
                        rs.getString("detected_plate"),
                        rs.getDouble("confidence_score"),
                        rs.getString("gate_id"),
                        rs.getString("validation_status"),
                        rs.getString("entry_timestamp"));
            }
        } catch (SQLException e) {
            System.out.println("[X] Error reading logs: " + e.getMessage());
        }
    }

    // UPDATE
    private void manualGateOverrideOrExit() {
        viewDetections();
        System.out.print("Enter Log ID to override / stamp exit: ");
        int logId = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Action ([1] Manual Gate Override  [2] Stamp Vehicle Exit): ");
        String action = scanner.nextLine().trim();

        if ("1".equals(action)) {
            System.out.print("Enter Override Reason (e.g., Muddy plate / Emergency vehicle): ");
            String reason = scanner.nextLine().trim();
            try (Connection conn = DatabaseConnection.getConnection()) {
                try (PreparedStatement ps1 = conn.prepareStatement("UPDATE Detection_Logs SET validation_status = 'OVERRIDDEN_GRANTED' WHERE log_id = ?;")) {
                    ps1.setInt(1, logId);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO Gate_Overrides (log_id, operator_id, reason, override_timestamp) VALUES (?, ?, ?, ?);")) {
                    ps2.setInt(1, logId);
                    ps2.setInt(2, currentUser.getUserId());
                    ps2.setString(3, reason);
                    ps2.setString(4, LocalDateTime.now().toString());
                    ps2.executeUpdate();
                }
                activityLog log = new activityLog();
                log.setOperatorId(currentUser.getUserId());
                log.setEventType("GATE_OVERRIDE");
                log.setDetails("Manual override on log_id=" + logId + " | Reason: " + reason);
                log.setCreatedAt(LocalDateTime.now());
                activityLogDao.createLog(log);

                System.out.println("[OK] Gate manually released & audit recorded.");
            } catch (SQLException e) {
                System.out.println("[X] Override error: " + e.getMessage());
            }
        } else if ("2".equals(action)) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE Detection_Logs SET exit_timestamp = ? WHERE log_id = ?;")) {
                ps.setString(1, LocalDateTime.now().toString());
                ps.setInt(2, logId);
                ps.executeUpdate();
                System.out.println("[OK] Exit timestamp recorded.");
            } catch (SQLException e) {
                System.out.println("[X] Exit update error: " + e.getMessage());
            }
        }
    }

    // DELETE
    private void softDeleteDetection() {
        viewDetections();
        System.out.print("Enter Log ID to soft-delete: ");
        int logId = Integer.parseInt(scanner.nextLine().trim());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE Detection_Logs SET deleted = 1 WHERE log_id = ?;")) {
            ps.setInt(1, logId);
            if (ps.executeUpdate() > 0) {
                System.out.println("[OK] Detection log soft-deleted.");
            }
        } catch (SQLException e) {
            System.out.println("[X] Delete error: " + e.getMessage());
        }
    }
}