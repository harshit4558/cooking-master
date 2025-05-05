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

import com.cookingmaster.service.OpenRouterService;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("chefBean")
@SessionScoped
public class ChefBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(ChefBean.class.getName());
    
    @Inject
    private OpenRouterService openRouterService;
    
    @Inject
    private UserBean userBean;
    
    private String prompt;
    private boolean isLoading;
    
    public String getPrompt() {
        return prompt;
    }
    
    public void setPrompt(String prompt) {
        logger.info("Setting prompt: " + prompt);
        this.prompt = prompt;
    }
    
    public boolean isLoading() {
        logger.info("Checking loading state: " + isLoading);
        return isLoading;
    }
    
    public void setLoading(boolean loading) {
        logger.info("Setting loading state to: " + loading);
        this.isLoading = loading;
    }
    
    public String generateRecipe() {
        logger.info("generateRecipe method called with prompt: " + prompt);
        if (prompt != null && !prompt.trim().isEmpty()) {
            try {
                String recipe = openRouterService.generateRecipe(prompt);
                logger.info("Recipe generated successfully: " + recipe);
                
                // Parse the recipe and save to database
                long recipeId = saveRecipeToDatabase(recipe);
                logger.info("Recipe saved with ID: " + recipeId);
                
                // Clear the prompt after successful generation
                prompt = null;
                
                // Return to home page
                return "/home.xhtml?faces-redirect=true";
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error generating recipe", e);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to generate recipe. Please try again."));
                return null;
            }
        }
        return null;
    }
    
    private long saveRecipeToDatabase(String recipe) throws NamingException, SQLException {
        // Check if user is logged in
        if (userBean == null || userBean.getCurrentUser() == null) {
            throw new IllegalStateException("User must be logged in to save a recipe");
        }
        
        // Parse the recipe string
        String[] lines = recipe.split("\n");
        String title = "";
        String description = "";
        String difficulty = "";
        int cookingTime = 0;
        StringBuilder ingredients = new StringBuilder();
        StringBuilder instructions = new StringBuilder();
        
        boolean inIngredients = false;
        boolean inInstructions = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue; // Skip empty lines
            
            if (line.startsWith("Title:")) {
                title = line.substring(6).trim();
            } else if (line.startsWith("Description:")) {
                description = line.substring(12).trim();
            } else if (line.startsWith("Difficulty:")) {
                difficulty = line.substring(11).trim();
            } else if (line.startsWith("Time:")) {
                String timeStr = line.substring(5).trim();
                cookingTime = Integer.parseInt(timeStr.replaceAll("[^0-9]", ""));
            } else if (line.equals("Ingredients:")) {
                inIngredients = true;
                inInstructions = false;
            } else if (line.equals("Instructions:")) {
                inIngredients = false;
                inInstructions = true;
            } else if (inIngredients && line.startsWith("-")) {
                if (ingredients.length() > 0) ingredients.append("\n");
                ingredients.append(line.substring(1).trim());
            } else if (inInstructions) {
                if (instructions.length() > 0) instructions.append("\n");
                instructions.append(line);
            }
        }
        
        logger.info("Parsed recipe details:");
        logger.info("Title: " + title);
        logger.info("Description: " + description);
        logger.info("Difficulty: " + difficulty);
        logger.info("Cooking Time: " + cookingTime);
        logger.info("Ingredients: " + ingredients.toString());
        logger.info("Instructions: " + instructions.toString());
        
        // Get database connection
        InitialContext ctx = new InitialContext();
        DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
        
        try (Connection conn = ds.getConnection("APP", "APP")) {
            String sql = "INSERT INTO RECIPE (TITLE, DESCRIPTION, COOKING_TIME, DIFFICULTY, USER_ID, INGREDIENTS, INSTRUCTIONS) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, title);
                stmt.setString(2, description);
                stmt.setInt(3, cookingTime);
                stmt.setString(4, difficulty);
                stmt.setInt(5, userBean.getCurrentUser().getId());
                stmt.setString(6, ingredients.toString());
                stmt.setString(7, instructions.toString());
                
                int rowsAffected = stmt.executeUpdate();
                logger.info("Recipe saved to database. Rows affected: " + rowsAffected);
                
                // Get the generated recipe ID
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long recipeId = generatedKeys.getLong(1);
                        logger.info("Generated recipe ID: " + recipeId);
                        return recipeId;
                    } else {
                        throw new SQLException("Creating recipe failed, no ID obtained.");
                    }
                }
            }
        }
    }
} 