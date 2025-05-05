package com.cookingmaster;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("recipeBean")
@SessionScoped
public class RecipeBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(RecipeBean.class.getName());
    
    private String title;
    private String description;
    private Integer cookingTime;
    private String difficulty;
    private String ingredients;
    private String instructions;
    
    @Inject
    private UserBean userBean;
    
    private List<Recipe> recipes;
    private List<Recipe> myRecipes;
    
    private boolean refreshRecipes = false;
    
    private Recipe selectedRecipe;
    
    private int recipeCount;
    
    private RecipeService recipeService;
    private List<Recipe> userRecipes;
    
    public RecipeBean() {
        this.recipeService = new RecipeService();
        this.userRecipes = new ArrayList<>();
    }
    
    public List<Recipe> getRecipes() {
        if (recipes == null) {
            loadRecipes();
        }
        return recipes;
    }
    
    public List<Recipe> getMyRecipes() {
        if (myRecipes == null) {
            loadMyRecipes();
        }
        return myRecipes;
    }
    
    private void loadRecipes() {
        recipes = new ArrayList<>();
        try {
            logger.info("Starting loadRecipes method");
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            String sql = "SELECT * FROM RECIPE ORDER BY ID DESC";
            logger.info("Executing SQL: " + sql);
            
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        Recipe recipe = new Recipe();
                        recipe.setRecipeId(rs.getLong("ID"));
                        recipe.setTitle(rs.getString("TITLE"));
                        recipe.setDescription(rs.getString("DESCRIPTION"));
                        recipe.setCookingTime(rs.getInt("COOKING_TIME"));
                        recipe.setDifficulty(rs.getString("DIFFICULTY"));
                        recipe.setIngredients(rs.getString("INGREDIENTS"));
                        recipe.setInstructions(rs.getString("INSTRUCTIONS"));
                        recipe.setUserId(rs.getLong("USER_ID"));
                        
                        recipes.add(recipe);
                        count++;
                        logger.info("Loaded recipe: " + recipe.getTitle() + " by user ID: " + recipe.getUserId());
                    }
                    logger.info("Total recipes loaded: " + count);
                }
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error loading recipes", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to load recipes: " + e.getMessage()));
        }
    }
    
    private void loadMyRecipes() {
        myRecipes = new ArrayList<>();
        try {
            logger.info("Starting loadMyRecipes method");
            if (userBean == null) {
                logger.severe("userBean is null");
                return;
            }
            if (userBean.getCurrentUser() == null) {
                logger.severe("currentUser is null");
                return;
            }
            
            int currentUserId = userBean.getCurrentUser().getId();
            String currentUsername = userBean.getCurrentUser().getUsername();
            logger.info("Current user ID: " + currentUserId);
            logger.info("Current username: " + currentUsername);
            
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM RECIPE WHERE USER_ID = ? ORDER BY ID DESC")) {
                
                stmt.setLong(1, currentUserId);
                logger.info("Executing query for user ID: " + currentUserId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        Recipe recipe = new Recipe();
                        recipe.setRecipeId(rs.getLong("ID"));
                        recipe.setTitle(rs.getString("TITLE"));
                        recipe.setDescription(rs.getString("DESCRIPTION"));
                        recipe.setCookingTime(rs.getInt("COOKING_TIME"));
                        recipe.setDifficulty(rs.getString("DIFFICULTY"));
                        recipe.setIngredients(rs.getString("INGREDIENTS"));
                        recipe.setInstructions(rs.getString("INSTRUCTIONS"));
                        recipe.setUserId(rs.getLong("USER_ID"));
                        myRecipes.add(recipe);
                        count++;
                        logger.info("Added recipe: " + recipe.getTitle() + " (ID: " + recipe.getRecipeId() + ")");
                    }
                    logger.info("Found " + count + " recipes for user " + currentUsername + " (ID: " + currentUserId + ")");
                }
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error loading user recipes", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to load your recipes: " + e.getMessage()));
        }
    }
    
    public String saveRecipe() {
        logger.info("Saving recipe: " + title);
        
        try {
            // Check if user is logged in
            if (userBean == null || userBean.getCurrentUser() == null) {
                logger.severe("User not logged in or userBean is null");
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "You must be logged in to save a recipe"));
                return "login?faces-redirect=true";
            }
            
            logger.info("User ID: " + userBean.getCurrentUser().getId());
            
            // Get DataSource using JNDI
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            logger.info("DataSource obtained successfully");
            
            try (Connection conn = ds.getConnection("APP", "APP")) {
                logger.info("Database connection established");
                
                String sql = "INSERT INTO RECIPE (TITLE, DESCRIPTION, COOKING_TIME, DIFFICULTY, USER_ID, INGREDIENTS, INSTRUCTIONS) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?)";
                
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, title);
                stmt.setString(2, description);
                
                if (cookingTime != null) {
                    stmt.setInt(3, cookingTime);
                } else {
                    stmt.setNull(3, java.sql.Types.INTEGER);
                }
                
                stmt.setString(4, difficulty);
                
                // Get current user ID
                int userId = userBean.getCurrentUser().getId();
                stmt.setInt(5, userId);
                
                // Log values before insertion
                logger.info("Ingredients value: '" + ingredients + "'");
                logger.info("Instructions value: '" + instructions + "'");
                
                stmt.setString(6, ingredients);
                stmt.setString(7, instructions);
                
                logger.info("Executing SQL: " + sql);
                int rowsAffected = stmt.executeUpdate();
                logger.info("Rows affected: " + rowsAffected);
                
                if (rowsAffected > 0) {
                    // Clear form fields after successful save
                    clearForm();
                    // Clear cached recipes to force refresh
                    recipes = null;
                    myRecipes = null;
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Recipe saved successfully!"));
                    return "home?faces-redirect=true";
                } else {
                    logger.warning("No rows affected by insert");
                    FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to save recipe"));
                    return null;
                }
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error during recipe save", e);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Database error: " + e.getMessage()));
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean isRefreshRecipes() {
        return refreshRecipes;
    }
    
    public void setRefreshRecipes(boolean refreshRecipes) {
        this.refreshRecipes = refreshRecipes;
    }
    
    public void refreshRecipes() {
        logger.info("Refreshing recipes");
        recipes = null;
        myRecipes = null;
        this.refreshRecipes = true;
    }
    
    private void clearForm() {
        this.title = null;
        this.description = null;
        this.cookingTime = null;
        this.difficulty = null;
        this.ingredients = null;
        this.instructions = null;
    }
    
    public int getRecipeCount() {
        logger.info("Getting recipe count for user");
        if (userBean == null || userBean.getCurrentUser() == null) {
            logger.severe("User not logged in or userBean is null");
            return 0;
        }
        
        int userId = userBean.getCurrentUser().getId();
        String username = userBean.getCurrentUser().getUsername();
        logger.info("Counting recipes for user: " + username + " (ID: " + userId + ")");
        
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) as count FROM RECIPE WHERE USER_ID = ?")) {
                
                stmt.setLong(1, userId);
                logger.info("Executing count query for user ID: " + userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt("count");
                        logger.info("Found " + count + " recipes for user " + username);
                        return count;
                    }
                }
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error counting user recipes", e);
        }
        return 0;
    }
    
    public Recipe getSelectedRecipe() {
        return selectedRecipe;
    }
    
    public void setSelectedRecipe(Recipe selectedRecipe) {
        this.selectedRecipe = selectedRecipe;
    }
    
    public void loadSelectedRecipe() {
        String recipeId = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("id");
        if (recipeId != null) {
            try {
                InitialContext ctx = new InitialContext();
                DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
                
                try (Connection conn = ds.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT * FROM RECIPE WHERE ID = ?")) {
                    
                    stmt.setLong(1, Long.parseLong(recipeId));
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Recipe recipe = new Recipe();
                            recipe.setRecipeId(rs.getLong("ID"));
                            recipe.setTitle(rs.getString("TITLE"));
                            recipe.setDescription(rs.getString("DESCRIPTION"));
                            recipe.setCookingTime(rs.getInt("COOKING_TIME"));
                            recipe.setDifficulty(rs.getString("DIFFICULTY"));
                            recipe.setIngredients(rs.getString("INGREDIENTS"));
                            recipe.setInstructions(rs.getString("INSTRUCTIONS"));
                            recipe.setUserId(rs.getLong("USER_ID"));
                            this.selectedRecipe = recipe;
                        }
                    }
                }
            } catch (NamingException | SQLException e) {
                logger.log(Level.SEVERE, "Error loading selected recipe", e);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to load recipe: " + e.getMessage()));
            }
        }
    }
    
    public List<String> getIngredientsList() {
        if (selectedRecipe != null && selectedRecipe.getIngredients() != null) {
            return Arrays.asList(selectedRecipe.getIngredients().split("\n"));
        }
        return new ArrayList<>();
    }
    
    public List<String> getInstructionsList() {
        if (selectedRecipe != null && selectedRecipe.getInstructions() != null) {
            return Arrays.asList(selectedRecipe.getInstructions().split("\n"));
        }
        return new ArrayList<>();
    }
    
    public String getUsername(Long userId) {
        try {
            logger.info("Fetching username for user ID: " + userId);
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            
            try (Connection conn = ds.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT USERNAME FROM USERS WHERE USER_ID = ?")) {
                
                stmt.setLong(1, userId);
                logger.info("Executing query: SELECT USERNAME FROM USERS WHERE USER_ID = " + userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String username = rs.getString("USERNAME");
                        logger.info("Found username: " + username + " for user ID: " + userId);
                        return username;
                    } else {
                        logger.warning("No username found for user ID: " + userId);
                    }
                }
            }
        } catch (NamingException | SQLException e) {
            logger.log(Level.SEVERE, "Error fetching username for user ID: " + userId, e);
        }
        return "Unknown User";
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getCookingTime() {
        return cookingTime;
    }
    
    public void setCookingTime(Integer cookingTime) {
        this.cookingTime = cookingTime;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getIngredients() {
        return ingredients;
    }
    
    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public String updateRecipe() {
        try {
            if (selectedRecipe == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "No recipe selected", null));
                return null;
            }

            if (userBean.getCurrentUser() == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "You must be logged in to update a recipe", null));
                return null;
            }

            if (selectedRecipe.getUserId() != userBean.getCurrentUser().getId()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "You can only update your own recipes", null));
                return null;
            }

            recipeService.updateRecipe(selectedRecipe);
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Recipe updated successfully", null));
            return "my-recipes?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error updating recipe: " + e.getMessage(), null));
            return null;
        }
    }

    public String deleteRecipe() {
        try {
            if (selectedRecipe == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "No recipe selected", null));
                return null;
            }

            if (userBean.getCurrentUser() == null) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "You must be logged in to delete a recipe", null));
                return null;
            }

            if (selectedRecipe.getUserId() != userBean.getCurrentUser().getId()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "You can only delete your own recipes", null));
                return null;
            }

            recipeService.deleteRecipe(selectedRecipe.getRecipeId(), (long)userBean.getCurrentUser().getId());
            
            // Clear the cached recipes to force a refresh
            myRecipes = null;
            
            // Add JavaScript to trigger refresh
            FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts()
                .add("document.dispatchEvent(new CustomEvent('recipeDeleted'));");
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Recipe deleted successfully", null));
            return "my-recipes?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error deleting recipe: " + e.getMessage(), null));
            return null;
        }
    }
    
    public List<Recipe> getUserRecipes() {
        return userRecipes;
    }
} 