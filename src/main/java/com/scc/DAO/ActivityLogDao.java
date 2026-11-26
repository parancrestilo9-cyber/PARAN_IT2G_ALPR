package com.scc.DAO;

import com.scc.MODEL.activityLog;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // CREATE
    public boolean createLog(activityLog log) {
        String sql = """
            INSERT INTO Activity_Logs
            (vehicle_id, operator_id, event_type, plate_number, confidence_score, gate_id, details, entry_timestamp, exit_timestamp, created_at, deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0);
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (log.getVehicleId() != null) ps.setInt(1, log.getVehicleId()); else ps.setNull(1, Types.INTEGER);
            if (log.getOperatorId() != null) ps.setInt(2, log.getOperatorId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, log.getEventType());
            ps.setString(4, log.getPlateNumber());
            ps.setDouble(5, log.getConfidenceScore());
            ps.setString(6, log.getGateId());
            ps.setString(7, log.getDetails());
            ps.setString(8, log.getEntryTimestamp() != null ? log.getEntryTimestamp().toString() : null);
            ps.setString(9, log.getExitTimestamp() != null ? log.getExitTimestamp().toString() : null);
            ps.setString(10, log.getCreatedAt() != null ? log.getCreatedAt().toString() : LocalDateTime.now().toString());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        log.setLogId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("ActivityLogDao.createLog error: " + e.getMessage());
        }
        return false;
    }

    // READ
    public List<activityLog> getAllLogs(String searchKeyword, boolean includeDeleted) {
        List<activityLog> logs = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM Activity_Logs WHERE 1=1 ");
        if (!includeDeleted) {
            sql.append("AND deleted = 0 ");
        }
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            sql.append("AND (UPPER(plate_number) LIKE ? OR UPPER(event_type) LIKE ? OR UPPER(details) LIKE ?) ");
        }
        sql.append("ORDER BY activity_id DESC;");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            if (searchKeyword != null && !searchKeyword.isBlank()) {
                String pattern = "%" + searchKeyword.trim().toUpperCase() + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                ps.setString(3, pattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ActivityLogDao.getAllLogs error: " + e.getMessage());
        }
        return logs;
    }

    public List<activityLog> getLogsByPlate(String plateNumber) {
        List<activityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM Activity_Logs WHERE deleted = 0 AND UPPER(plate_number) LIKE ? ORDER BY activity_id DESC;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + plateNumber.trim().toUpperCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("ActivityLogDao.getLogsByPlate error: " + e.getMessage());
        }
        return logs;
    }

    // UPDATE
    public boolean updateLog(activityLog log) {
        String sql = """
            UPDATE Activity_Logs
            SET event_type = ?, plate_number = ?, confidence_score = ?, gate_id = ?, details = ?, exit_timestamp = ?
            WHERE activity_id = ?;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, log.getEventType());
            ps.setString(2, log.getPlateNumber());
            ps.setDouble(3, log.getConfidenceScore());
            ps.setString(4, log.getGateId());
            ps.setString(5, log.getDetails());
            ps.setString(6, log.getExitTimestamp() != null ? log.getExitTimestamp().toString() : null);
            ps.setInt(7, log.getLogId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ActivityLogDao.updateLog error: " + e.getMessage());
        }
        return false;
    }

    // DELETE (Soft delete)
    public boolean softDeleteLog(int logId) {
        String sql = "UPDATE Activity_Logs SET deleted = 1 WHERE activity_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, logId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ActivityLogDao.softDeleteLog error: " + e.getMessage());
        }
        return false;
    }

    private activityLog mapRow(ResultSet rs) throws SQLException {
        activityLog log = new activityLog();
        log.setLogId(rs.getInt("activity_id"));
        int vId = rs.getInt("vehicle_id");
        log.setVehicleId(rs.wasNull() ? null : vId);
        int oId = rs.getInt("operator_id");
        log.setOperatorId(rs.wasNull() ? null : oId);
        log.setEventType(rs.getString("event_type"));
        log.setPlateNumber(rs.getString("plate_number"));
        log.setConfidenceScore(rs.getDouble("confidence_score"));
        log.setGateId(rs.getString("gate_id"));
        log.setDetails(rs.getString("details"));

        String entryStr = rs.getString("entry_timestamp");
        if (entryStr != null && !entryStr.isBlank()) {
            log.setEntryTimestamp(LocalDateTime.parse(entryStr));
        }
        String exitStr = rs.getString("exit_timestamp");
        if (exitStr != null && !exitStr.isBlank()) {
            log.setExitTimestamp(LocalDateTime.parse(exitStr));
        }
        String createdStr = rs.getString("created_at");
        if (createdStr != null && !createdStr.isBlank()) {
            log.setCreatedAt(LocalDateTime.parse(createdStr));
        }
        log.setDeleted(rs.getInt("deleted") == 1);
        return log;
    }
}