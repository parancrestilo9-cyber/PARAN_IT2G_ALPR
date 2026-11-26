package com.scc.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class DatabaseInitializer {

    private DatabaseInitializer() {}

    public static void initialize() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS Users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role TEXT NOT NULL CHECK(role IN ('SYSTEM_ADMIN', 'SECURITY_OPERATOR', 'VEHICLE_OWNER')),
                email TEXT NOT NULL,
                contact_number TEXT,
                emergency_contact TEXT,
                active INTEGER NOT NULL DEFAULT 1,
                last_login TEXT
            );
            """;

        String createVehiclesTable = """
            CREATE TABLE IF NOT EXISTS Vehicles (
                vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                plate_number TEXT NOT NULL UNIQUE,
                owner_name TEXT NOT NULL,
                vehicle_type TEXT NOT NULL,
                access_status TEXT NOT NULL DEFAULT 'RESIDENT' CHECK(access_status IN ('VIP', 'RESIDENT', 'VISITOR', 'BLACKLISTED', 'INACTIVE')),
                registration_date TEXT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE SET NULL
            );
            """;

        String createDetectionLogsTable = """
            CREATE TABLE IF NOT EXISTS Detection_Logs (
                log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id INTEGER,
                detected_plate TEXT NOT NULL,
                confidence_score REAL NOT NULL DEFAULT 0.0,
                entry_timestamp TEXT,
                exit_timestamp TEXT,
                gate_id TEXT NOT NULL,
                validation_status TEXT NOT NULL DEFAULT 'PENDING',
                is_temporary_visitor INTEGER NOT NULL DEFAULT 0,
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (vehicle_id) REFERENCES Vehicles(vehicle_id) ON DELETE SET NULL
            );
            """;

        String createGateOverridesTable = """
            CREATE TABLE IF NOT EXISTS Gate_Overrides (
                override_id INTEGER PRIMARY KEY AUTOINCREMENT,
                log_id INTEGER,
                operator_id INTEGER NOT NULL,
                reason TEXT NOT NULL,
                override_timestamp TEXT NOT NULL,
                FOREIGN KEY (log_id) REFERENCES Detection_Logs(log_id) ON DELETE SET NULL,
                FOREIGN KEY (operator_id) REFERENCES Users(user_id) ON DELETE CASCADE
            );
            """;

        String createActivityLogsTable = """
            CREATE TABLE IF NOT EXISTS Activity_Logs (
                activity_id INTEGER PRIMARY KEY AUTOINCREMENT,
                vehicle_id INTEGER,
                operator_id INTEGER,
                event_type TEXT NOT NULL,
                plate_number TEXT,
                confidence_score REAL DEFAULT 0.0,
                gate_id TEXT,
                details TEXT,
                entry_timestamp TEXT,
                exit_timestamp TEXT,
                created_at TEXT NOT NULL,
                deleted INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (vehicle_id) REFERENCES Vehicles(vehicle_id) ON DELETE SET NULL,
                FOREIGN KEY (operator_id) REFERENCES Users(user_id) ON DELETE SET NULL
            );
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createUsersTable);
            stmt.execute(createVehiclesTable);
            stmt.execute(createDetectionLogsTable);
            stmt.execute(createGateOverridesTable);
            stmt.execute(createActivityLogsTable);

            seedDefaultUsersAndSampleData(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ALPR database tables: " + e.getMessage(), e);
        }
    }

    private static void seedDefaultUsersAndSampleData(Connection conn) throws SQLException {
        String checkUserSql = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
            checkStmt.setString(1, "admin");
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    insertDefaultUser(conn, "admin", "admin123", "SYSTEM_ADMIN",
                            "admin@alpr-system.local", "0917-000-0001", "0917-000-0099");
                    insertDefaultUser(conn, "guard1", "guard123", "SECURITY_OPERATOR",
                            "guard1@alpr-system.local", "0917-000-0002", "0917-000-0098");
                    insertDefaultUser(conn, "owner1", "owner123", "VEHICLE_OWNER",
                            "owner1@alpr-system.local", "0917-000-0003", "0917-000-0097");
                }
            }
        }

        String checkVehicleSql = "SELECT COUNT(*) FROM Vehicles";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(checkVehicleSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertVehicle = """
                    INSERT INTO Vehicles (user_id, plate_number, owner_name, vehicle_type, access_status, registration_date, active)
                    VALUES (3, 'ABC-1234', 'Juan Dela Cruz', 'Sedan', 'RESIDENT', ?, 1),
                           (NULL, 'VIP-8888', 'Facility Director', 'SUV', 'VIP', ?, 1),
                           (NULL, 'BLK-0000', 'Flagged Plate', 'Sedan', 'BLACKLISTED', ?, 1);
                    """;
                try (PreparedStatement ps = conn.prepareStatement(insertVehicle)) {
                    String now = LocalDateTime.now().toString();
                    ps.setString(1, now);
                    ps.setString(2, now);
                    ps.setString(3, now);
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void insertDefaultUser(Connection conn, String username, String rawPassword,
                                          String role, String email, String contact, String emergency) throws SQLException {
        String sql = """
            INSERT INTO Users (username, password_hash, role, email, contact_number, emergency_contact, active)
            VALUES (?, ?, ?, ?, ?, ?, 1);
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, SecurityUtil.hashPassword(rawPassword));
            ps.setString(3, role);
            ps.setString(4, email);
            ps.setString(5, contact);
            ps.setString(6, emergency);
            ps.executeUpdate();
        }
    }
}