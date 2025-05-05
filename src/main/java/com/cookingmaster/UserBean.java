package com.cookingmaster;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

@Named("userBean")
@SessionScoped
public class UserBean implements Serializable {
    private static final Logger logger = Logger.getLogger(UserBean.class.getName());
    private static final long serialVersionUID = 1L;
    
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private String confirmPassword;
    
    // Current logged in user
    private User currentUser;
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public UserBean() {
        logger.info("UserBean constructor called");
    }
    
    // Getters and Setters
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    // Registration method
    public String register() {
        logger.info("Register method called");
        
        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            return "register?error=password_mismatch";
        }
        
        try {
            // Get database connection
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            try (Connection conn = ds.getConnection("APP", "APP")) {
                // Check if username already exists
                String checkUserSQL = "SELECT username FROM users WHERE username = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSQL)) {
                    checkStmt.setString(1, username);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            return "register?error=username_exists";
                        }
                    }
                }
                
                // Check if email already exists
                String checkEmailSQL = "SELECT email FROM users WHERE email = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkEmailSQL)) {
                    checkStmt.setString(1, email);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            return "register?error=email_exists";
                        }
                    }
                }
                
                // Insert new user
                String insertSQL = "INSERT INTO users (first_name, last_name, email, username, password) VALUES (?, ?, ?, ?, ?)";
                int userId = 0;
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, firstName);
                    insertStmt.setString(2, lastName);
                    insertStmt.setString(3, email);
                    insertStmt.setString(4, username);
                    insertStmt.setString(5, password);
                    insertStmt.executeUpdate();
                    
                    // Get the generated user ID
                    try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            userId = generatedKeys.getInt(1);
                        }
                    }
                }
                
                // Create and set current user
                currentUser = new User(userId, firstName, lastName, email, username, password);
                
                // Clear form fields
                clearFields();
                
                // Redirect to home page
                return "home?faces-redirect=true";
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error during registration", e);
            return "register?error=database_error";
        }
    }
    
    // Login method
    public String login() {
        logger.info("Login method called for username: " + username);
        
        try {
            // Get database connection
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            try (Connection conn = ds.getConnection("APP", "APP")) {
                // First check if username exists
                String checkUserSQL = "SELECT USER_ID, first_name, last_name, email, username, password FROM users WHERE username = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSQL)) {
                    checkStmt.setString(1, username);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            // Username doesn't exist
                            FacesContext.getCurrentInstance().addMessage("loginForm:username", 
                                new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "No user found with this username"));
                            return null;
                        }
                        
                        // Username exists, now check password
                        String storedPassword = rs.getString("password");
                        if (!storedPassword.equals(password)) {
                            // Wrong password
                            FacesContext.getCurrentInstance().addMessage("loginForm:password", 
                                new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Wrong password"));
                            return null;
                        }
                        
                        // Login successful, create User object
                        currentUser = new User(
                            rs.getInt("USER_ID"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("password")
                        );
                        clearFields();
                        return "home?faces-redirect=true";
                    }
                }
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error during login", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Database error occurred. Please try again later."));
            return null;
        }
    }
    
    // Logout method
    public String logout() {
        logger.info("Logout method called");
        currentUser = null;
        clearFields();
        return "login?faces-redirect=true";
    }
    
    // Helper method to clear form fields
    private void clearFields() {
        firstName = null;
        lastName = null;
        email = null;
        username = null;
        password = null;
        confirmPassword = null;
    }
    
    // Inner User class to store user data
    public static class User implements Serializable {
        private final int id;
        private String firstName;
        private String lastName;
        private String email;
        private String username;
        private final String password;
        
        public User(int id, String firstName, String lastName, String email, String username, String password) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.username = username;
            this.password = password;
        }
        
        public int getId() {
            return id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
    }

    public String updateProfile() {
        try {
            if (currentUser == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "You must be logged in to update your profile"));
                return null;
            }

            // Get DataSource using JNDI
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            try (Connection conn = ds.getConnection()) {
                String sql = "UPDATE USERS SET FIRST_NAME = ?, LAST_NAME = ?, USERNAME = ?, EMAIL = ? WHERE USER_ID = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, currentUser.getFirstName());
                    stmt.setString(2, currentUser.getLastName());
                    stmt.setString(3, currentUser.getUsername());
                    stmt.setString(4, currentUser.getEmail());
                    stmt.setInt(5, currentUser.getId());
                    
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        // Add success message
                        FacesContext.getCurrentInstance().addMessage(null, 
                            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Profile updated successfully"));
                        
                        return "profile?faces-redirect=true";
                    } else {
                        FacesContext.getCurrentInstance().addMessage(null, 
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update profile"));
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating profile", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to update profile: " + e.getMessage()));
            return null;
        }
    }
} 