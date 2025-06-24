package wat;
import wat.Pin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LostAndFoundMatcher {
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
    private static final String API_KEY = getApiKey(); 
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final Executor asyncExecutor = Executors.newFixedThreadPool(10);
    
    private static String getApiKey() {
        String key = dotenv.get("GEMINI_API_KEY");
        if (key == null) {
            key = System.getenv("GEMINI_API_KEY");
        }
        return key;
    }
    
    public static String preprocessPin(Pin pin) {
        StringBuilder sb = new StringBuilder();
        
        if (pin.getCategory() != null && !pin.getCategory().isEmpty()) {
            sb.append("Category: ").append(pin.getCategory()).append(". ");
        }
        
        Map<String, String> tags = pin.getTags();
        if (tags != null && !tags.isEmpty()) {
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(". ");
            }
        }
        
        return sb.toString().trim();
    }
    
    public static CompletableFuture<Boolean> match(Pin lostPin, Pin foundPin) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String lostDescription = preprocessPin(lostPin);
                String foundDescription = preprocessPin(foundPin);
                
                if (lostDescription.isEmpty() || foundDescription.isEmpty()) {
                    return false;
                }
                
                boolean isMatch = askGeminiIfItemsMatch(lostDescription, foundDescription);
                
                System.out.println("Gemini match result: " + isMatch);
                
                return isMatch;
                
            } catch (Exception e) {
                System.err.println("Error in matching pins with Gemini: " + e.getMessage());
                System.err.println("Falling back to simple matching. Make sure .env file exists with GEMINI_API_KEY");
                return simpleMatch(lostPin, foundPin);
            }
        }, asyncExecutor);
    }
    
    //  Optional synchronous wrapper for backward compatibility
    public static boolean matchSync(Pin lostPin, Pin foundPin) {
        return match(lostPin, foundPin).join(); 
    }
    
    private static boolean askGeminiIfItemsMatch(String lostDescription, String foundDescription) throws IOException, InterruptedException {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("GEMINI_API_KEY not set, using fallback matching");
            throw new IOException("API key not available");
        }
        
        String prompt = createMatchingPrompt(lostDescription, foundDescription);
        
        String payload = String.format(
            "{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}",
            escapeJson(prompt)
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GEMINI_API_URL + "?key=" + API_KEY))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            System.err.println("Gemini API error: " + response.statusCode() + " - " + response.body());
            throw new IOException("Unexpected response code: " + response.statusCode());
        }
        
        String responseBody = response.body();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        
        if (jsonResponse.has("candidates") && jsonResponse.get("candidates").isArray() &&
            jsonResponse.get("candidates").size() > 0) {
            
            JsonNode candidate = jsonResponse.get("candidates").get(0);
            if (candidate.has("content") && candidate.get("content").has("parts") &&
                candidate.get("content").get("parts").isArray() &&
                candidate.get("content").get("parts").size() > 0) {
                
                String geminiResponse = candidate.get("content").get("parts").get(0).get("text").asText();
                System.out.println("Gemini response: " + geminiResponse);
                
                return parseGeminiResponse(geminiResponse);
            }
        }
        
        throw new IOException("Unexpected response format from Gemini");
    }
    
    private static String createMatchingPrompt(String lostDescription, String foundDescription) {
        return "You are an expert at matching lost and found items. " +
               "Analyze these two item descriptions and determine if they could be the same item.\\n\\n" +
               
               "Guidelines:\\n" +
               "- Focus on key attributes: type, color, brand, material, distinctive features\\n" +
               "- Ignore minor wording differences\\n" +
               "- Consider synonyms (e.g., iPhone = smartphone, wallet = purse)\\n" +
               "- Different colors usually mean different items\\n" +
               "- Different item types usually mean different items\\n\\n" +
               
               "Examples of MATCHES:\\n" +
               "- Lost: blue Nike backpack → Found: blue Nike bag\\n" +
               "- Lost: black iPhone with case → Found: black smartphone with case\\n" +
               "- Lost: brown leather wallet → Found: brown wallet made of leather\\n\\n" +
               
               "Examples of NO MATCHES:\\n" +
               "- Lost: red Toyota keys → Found: green Honda keys\\n" +
               "- Lost: silver ring → Found: gold bracelet\\n" +
               "- Lost: orange gloves → Found: white gloves\\n\\n" +
               
               "Lost item: " + lostDescription + "\\n" +
               "Found item: " + foundDescription + "\\n\\n" +
               
               "Respond with exactly one word: either 'MATCH' if these items could be the same, or 'NO_MATCH' if they are different items.";
    }
    
    private static boolean parseGeminiResponse(String response) {
        String cleanResponse = response.trim().toUpperCase();
        
        if (cleanResponse.contains("MATCH") && !cleanResponse.contains("NO_MATCH")) {
            return true;
        }
        
        if (cleanResponse.contains("NO_MATCH") || cleanResponse.contains("NO MATCH") || 
            cleanResponse.contains("NOT A MATCH") || cleanResponse.contains("DIFFERENT")) {
            return false;
        }
        
        if (cleanResponse.contains("YES") || cleanResponse.contains("SAME") || 
            cleanResponse.contains("IDENTICAL") || cleanResponse.contains("SIMILAR")) {
            return true;
        }
        
        if (cleanResponse.contains("NO") || cleanResponse.contains("FALSE") || 
            cleanResponse.contains("DIFFERENT") || cleanResponse.contains("NOT")) {
            return false;
        }
        
        System.err.println("Unclear Gemini response, defaulting to no match: " + response);
        return false;
    }
    
    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    private static boolean simpleMatch(Pin lostPin, Pin foundPin) {
        String lostDesc = preprocessPin(lostPin).toLowerCase();
        String foundDesc = preprocessPin(foundPin).toLowerCase();
        
        String[] lostWords = lostDesc.split("\\s+");
        String[] foundWords = foundDesc.split("\\s+");
        
        int matchingWords = 0;
        int totalWords = Math.min(lostWords.length, foundWords.length);
        
        if (totalWords == 0) return false;
        
        for (String lostWord : lostWords) {
            for (String foundWord : foundWords) {
                if (lostWord.length() > 2 && foundWord.length() > 2 && 
                    (lostWord.equals(foundWord) || lostWord.contains(foundWord) || foundWord.contains(lostWord))) {
                    matchingWords++;
                    break;
                }
            }
        }
        
        double simpleScore = (double) matchingWords / totalWords;
        return simpleScore >= 0.6; 
    }
} 