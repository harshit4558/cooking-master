package com.cookingmaster;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class RecipeService {
    
    private Connection getConnection() throws SQLException {
        try {
            InitialContext ctx = new InitialContext();
            DataSource ds = (DataSource) ctx.lookup("jdbc/cookingmaster");
            return ds.getConnection("APP", "APP");
        } catch (NamingException e) {
            throw new SQLException("Error looking up database connection", e);
        }
    }

    public void updateRecipe(Recipe recipe) {
        String sql = "UPDATE recipe SET title = ?, description = ?, cooking_time = ?, difficulty = ?, ingredients = ?, instructions = ? WHERE id = ? AND user_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, recipe.getTitle());
            pstmt.setString(2, recipe.getDescription());
            pstmt.setInt(3, recipe.getCookingTime());
            pstmt.setString(4, recipe.getDifficulty());
            pstmt.setString(5, recipe.getIngredients());
            pstmt.setString(6, recipe.getInstructions());
            pstmt.setLong(7, recipe.getRecipeId());
            pstmt.setLong(8, recipe.getUserId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating recipe: " + e.getMessage());
        }
    }

    public void deleteRecipe(Long recipeId, Long userId) {
        String sql = "DELETE FROM recipe WHERE id = ? AND user_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, recipeId);
            pstmt.setLong(2, userId);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new RuntimeException("Recipe not found or you don't have permission to delete it");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting recipe: " + e.getMessage());
        }
    }
} 