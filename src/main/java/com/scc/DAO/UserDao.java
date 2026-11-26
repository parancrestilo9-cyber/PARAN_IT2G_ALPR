package com.scc.DAO;

import com.scc.MODEL.User;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // CREATE
    public boolean createUser(User user) {
        String sql = """
            INSERT INTO Users (username, password_hash, role, email, contact_number, emergency_contact, active, last_login)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getContactNumber());
            ps.setString(6, user.getEmergencyContact());
            ps.setInt(7, user.isActive() ? 1 : 0);
            ps.setString(8, user.getLastLogin() != null ? user.getLastLogin().toString() : null);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setUserId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("UserDao.createUser error: " + e.getMessage());
        }
        return false;
    }

    // READ (all active or all users)
    public List<User> getAllUsers(boolean includeInactive) {
        List<User> list = new ArrayList<>();
        String sql = includeInactive
                ? "SELECT * FROM Users ORDER BY user_id ASC;"
                : "SELECT * FROM Users WHERE active = 1 ORDER BY user_id ASC;";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDao.getAllUsers error: " + e.getMessage());
        }
        return list;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDao.getUserByUsername error: " + e.getMessage());
        }
        return null;
    }

    public User getUserById(int userId) {
        String sql = "SELECT * FROM Users WHERE user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDao.getUserById error: " + e.getMessage());
        }
        return null;
    }

    // UPDATE
    public boolean updateUser(User user, boolean updatePassword) {
        String sql = updatePassword
                ? """
                  UPDATE Users
                  SET username = ?, password_hash = ?, role = ?, email = ?, contact_number = ?, emergency_contact = ?, active = ?
                  WHERE user_id = ?;
                  """
                : """
                  UPDATE Users
                  SET username = ?, role = ?, email = ?, contact_number = ?, emergency_contact = ?, active = ?
                  WHERE user_id = ?;
                  """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int idx = 1;
            ps.setString(idx++, user.getUsername().trim());
            if (updatePassword) {
                ps.setString(idx++, user.getPasswordHash());
            }
            ps.setString(idx++, user.getRole());
            ps.setString(idx++, user.getEmail());
            ps.setString(idx++, user.getContactNumber());
            ps.setString(idx++, user.getEmergencyContact());
            ps.setInt(idx++, user.isActive() ? 1 : 0);
            ps.setInt(idx, user.getUserId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDao.updateUser error: " + e.getMessage());
        }
        return false;
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE Users SET last_login = ? WHERE user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("UserDao.updateLastLogin error: " + e.getMessage());
        }
    }

    // DELETE (Soft-deactivate)
    public boolean deactivateUser(int userId) {
        String sql = "UPDATE Users SET active = 0 WHERE user_id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDao.deactivateUser error: " + e.getMessage());
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setEmail(rs.getString("email"));
        u.setContactNumber(rs.getString("contact_number"));
        u.setEmergencyContact(rs.getString("emergency_contact"));
        u.setActive(rs.getInt("active") == 1);
        String lastLoginStr = rs.getString("last_login");
        if (lastLoginStr != null && !lastLoginStr.isBlank()) {
            u.setLastLogin(LocalDateTime.parse(lastLoginStr));
        }
        return u;
    }
}