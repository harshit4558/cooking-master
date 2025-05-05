package com.cookingmaster.service;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class OpenRouterService {
    private static final Logger logger = Logger.getLogger(OpenRouterService.class.getName());
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String API_KEY = "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";
    private static final String MODEL = "meta-llama/llama-4-scout:free";
    
    // Add this for testing
    private static boolean simulateApiFailure = false;
    
    public static void setSimulateApiFailure(boolean value) {
        simulateApiFailure = value;
    }

    private Client createClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };

            // Create SSL context with the trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create client with SSL context
            return ClientBuilder.newBuilder()
                .sslContext(sslContext)
                .hostnameVerifier((hostname, session) -> true)
                .build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating SSL client", e);
            return ClientBuilder.newClient();
        }
    }

    public String generateRecipe(String prompt) {
        logger.info("Starting recipe generation with prompt: " + prompt);
        
        // Add this for testing
        if (simulateApiFailure) {
            logger.info("Simulating API failure for testing");
            throw new RuntimeException("Connection refused: API is down");
        }
        
        try {
            Client client = createClient();
            
            // Create the request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            
            // Add system message to guide the AI
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a recipe generator. You must follow these rules strictly:\n" +
                "1. Choose EXACTLY ONE difficulty level: Easy, Medium, or Hard\n" +
                "2. Include ONLY these sections in this exact order\n" +
                "3. Do not add any tips, notes, or additional sections\n" +
                "4. Format your response EXACTLY as follows:\n\n" +
                "Title: [Recipe Name]\n" +
                "Description: \n" +
                "Difficulty: [Choose only one: Easy/Medium/Hard]\n" +
                "Time: [Number of minutes only]\n" +
                "Ingredients:\n" +
                "- [Ingredient 1 with measurement]\n" +
                "- [Ingredient 2 with measurement]\n" +
                "Instructions:\n" +
                "1. [Step 1]\n" +
                "2. [Step 2]");
            
            // Add user message with the prompt
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            
            // Add both messages to the request
            requestBody.put("messages", new Object[]{systemMessage, userMessage});

            String requestJson = requestBody.toString();
            logger.info("Sending request to OpenRouter API: " + requestJson);

            // Make the API request
            Response response = client.target(API_URL)
                .request(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + API_KEY)
                .post(Entity.json(requestJson));

            String responseBody = response.readEntity(String.class);
            logger.info("Received response from API with status: " + response.getStatus());
            logger.info("Response body: " + responseBody);

            if (response.getStatus() == 200) {
                JSONObject jsonResponse = new JSONObject(responseBody);
                String content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");
                logger.info("Successfully extracted recipe content: " + content);
                return content;
            } else {
                logger.log(Level.SEVERE, "API request failed with status: " + response.getStatus() + ", Response: " + responseBody);
                return "Error generating recipe. Please try again.";
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error calling OpenRouter API", e);
            return "Error generating recipe. Please try again.";
        }
    }
} 