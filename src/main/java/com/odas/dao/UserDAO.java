package com.odas.dao;

import com.odas.User;
import com.odas.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for USERS table in Oracle 21c XE.
 * 
 * Purpose (for viva):
 * - Implements DAO design pattern to decouple SQL persistence logic from servlets.
 * - Uses PreparedStatements to defend against SQL Injection attacks.
 * - Handles user creation, role assignment, and credential lookup.
 */
public class UserDAO {

    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    /**
     * Finds a user by their unique username.
     * 
     * @param username username to search for
     * @return User object or null if not found
     */
    public User findByUsername(String username) {
        String sql = "SELECT ID, USERNAME, PASSWORD, ROLE FROM USERS WHERE USERNAME = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by username: " + username, e);
        }
        return null;
    }

    /**
     * Finds a user by their primary key ID.
     * 
     * @param id user ID
     * @return User object or null if not found
     */
    public User findById(int id) {
        String sql = "SELECT ID, USERNAME, PASSWORD, ROLE FROM USERS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding user by id: " + id, e);
        }
        return null;
    }

    /**
     * Checks if a username is already taken.
     * 
     * @param username username to check
     * @return true if exists, false otherwise
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM USERS WHERE LOWER(USERNAME) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking username existence: " + username, e);
        }
        return false;
    }

    /**
     * Creates a new user record and returns the generated user ID.
     * 
     * @param user user details to insert
     * @return generated ID or -1 on failure
     */
    public int createUser(User user) {
        String sql = "INSERT INTO USERS (USERNAME, PASSWORD, ROLE) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID"})) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys != null && generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        user.setId(generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating user: " + user.getUsername(), e);
        }
        return -1;
    }

    /**
     * Retrieves all registered users.
     * 
     * @return List of Users
     */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT ID, USERNAME, PASSWORD, ROLE FROM USERS ORDER BY ID";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all users", e);
        }
        return list;
    }

    /**
     * Updates password for a given user ID.
     * 
     * @param userId user ID
     * @param newHashedPassword newly computed SHA-256 password hash
     * @return true if updated successfully, false otherwise
     */
    public boolean updatePassword(int userId, String newHashedPassword) {
        String sql = "UPDATE USERS SET PASSWORD = ? WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newHashedPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating password for userId: " + userId, e);
        }
        return false;
    }

    /**
     * Deletes a user by ID.
     * 
     * @param id user ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM USERS WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting user: " + id, e);
        }
        return false;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("ID"));
        u.setUsername(rs.getString("USERNAME"));
        u.setPassword(rs.getString("PASSWORD"));
        u.setRole(rs.getString("ROLE"));
        return u;
    }
}
