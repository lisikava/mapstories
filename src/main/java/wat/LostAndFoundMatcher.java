package wat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for determining whether two strings match.
 */
public class LostAndFoundMatcher {

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final Properties properties = loadProperties();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String promptTemplate = """
            You are an expert at matching lost and found items. Analyze these two item descriptions and determine if they could be the same item.
            
            Guidelines:
            - Focus on key attributes: type, color, brand, material, distinctive features;
            - Ignore minor wording differences;
            - Consider synonyms (e.g., iPhone = smartphone, wallet = purse);
            - Different colors usually mean different items;
            - Different item types usually mean different items.
            
            Examples of MATCHES:
            - Lost: blue Nike backpack → Found: blue Nike bag
            - Lost: black iPhone with case → Found: black smartphone with case
            - Lost: brown leather wallet → Found: brown wallet made of leather
            
            Examples of NO MATCHES:
            - Lost: red Toyota keys → Found: green Honda keys
            - Lost: silver ring → Found: gold bracelet
            - Lost: orange gloves → Found: white gloves
            
            Lost item: "%s"
            Found item: "%s"
            
            Respond with exactly one word: either 'MATCH' if these items could be the same, or 'NO_MATCH' if they are different items.""";

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("gemini.properties");
             InputStream secretsStream = Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream("gemini-secrets.properties")) {
            properties.load(stream);
            properties.load(secretsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    public static CompletableFuture<Boolean> descriptionsMatch(String lost,
                                                               String found
    ) {
        if (properties.containsKey("api_key")) {
            String prompt = String.format(promptTemplate, lost, found);
            try {
                return askGeminiIfItemsMatch(prompt);
            } catch (RuntimeException e) {
                // TODO: logging
            }
        }
        // fallback
        return CompletableFuture.completedFuture(simpleMatch(lost, found));
    }

    private static CompletableFuture<Boolean> askGeminiIfItemsMatch(String prompt) {
        // create request body as root
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");
        ObjectNode content0 = contents.addObject();
        ArrayNode parts = content0.putArray("parts");
        ObjectNode part0 = parts.addObject();
        part0.put("text", prompt);

        // serialize
        String payload = null;
        try {
            payload = objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        properties.getProperty("api_url") +
                                "?key=" +
                                properties.getProperty("api_key")))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(LostAndFoundMatcher::parseGeminiResponse);
    }

    private static boolean parseGeminiResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Unexpected response code: " + response.statusCode());
        }
        String cleanResponse = null;
        try {
            cleanResponse = objectMapper.readTree(response.body())
                    .get("candidates")
                    .get(0)
                    .get("content")
                    .get("parts")
                    .get(0)
                    .get("text")
                    .asText()
                    .trim()
                    .toUpperCase();
        } catch (JsonProcessingException | NullPointerException e) {
            throw new RuntimeException(e);
        }
        return cleanResponse.equals("MATCH"); // otherwise unreliable
    }

    private static boolean simpleMatch(String lostDesc, String foundDesc) {
        String intoWordsRegex = "[\\p{Punct}\\s]+";
        Set<String> lostWords =
                new HashSet<>(Arrays.stream(lostDesc.split(intoWordsRegex))
                                      .filter(str -> str.length() > 2)
                                      .toList());
        Set<String> foundWords =
                new HashSet<>(Arrays.stream(foundDesc.split(intoWordsRegex))
                                      .filter(str -> str.length() > 2)
                                      .toList());
        int lostWordsCount = lostWords.size();
        int foundWordsCount = foundWords.size();

        Set<String> intersection = new HashSet<>(lostWords);
        intersection.retainAll(foundWords);
        int intersectingWords = intersection.size();

        double simpleScore =
                ((double) intersectingWords / lostWordsCount + (double) intersectingWords / foundWordsCount) / 2;
        return simpleScore >= 0.6;
    }

    private LostAndFoundMatcher() {}
} 