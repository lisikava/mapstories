package wat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.postgresql.geometric.PGpoint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PinController {
    public void registerRoutes(Javalin app) {
        app.get("/pins", this::getAllPins);
        app.post("/pins", this::createPin);
        app.delete("/pins/{id}", this::deletePin);
        app.put("/pins/{id}", this::updatePin);
        app.get("/pins/search", this::advancedSearch);
    }

    private void getAllPins(Context ctx) {
        String bbox = ctx.queryParam("bbox");
        StringBuilder pattern = new StringBuilder("{");
        if (bbox != null && !bbox.trim().isEmpty()) {
            bbox = parseBbox(bbox);
            pattern.append(String.format("\"bbox\": \"%s\"", bbox));
        }
        List<Pin> pins = Pin.retrieve(pattern.append("}").toString());
        ctx.json(pins);
    }

    private void createPin(Context ctx) {
        Map<String, Object> json = ctx.bodyAsClass(Map.class);
        Map<String, Double> loc = (Map<String, Double>) json.get("location");
        double lat = loc.get("lat");
        double lon = loc.get("lon");
        String category = (String) json.get("category");
        Map<String, String> tags = (Map<String, String>) json.get("tags");
        Pin pin = Pin.create(new PGpoint(lat, lon), category, tags);
        ctx.status(201).json(pin);
    }

    private void deletePin(Context ctx) {
        Integer id = Integer.parseInt(ctx.pathParam("id"));
        Pin.delete(id);
    }

    private void updatePin(Context ctx) {
        Integer id = Integer.parseInt(ctx.pathParam("id"));
        Map<String, Object> json = ctx.bodyAsClass(Map.class);
        Map<String, Double> loc = (Map<String, Double>) json.get("location");
        double lat = loc.get("lat");
        double lon = loc.get("lon");
        String category = (String) json.get("category");
        Map<String, String> tags = (Map<String, String>) json.get("tags");
        Pin.update(id, new PGpoint(lat, lon), category, tags);
    }

    private void advancedSearch(Context ctx) {
        String categories = ctx.queryParam("categories");
        String jsonCategories = null;
        if (categories != null && !categories.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            List<String> categoriesList = Arrays.asList(categories.split(","));
            try {
                jsonCategories = mapper.writeValueAsString(categoriesList);
            } catch (JsonProcessingException e) {
                System.out.println(e);
            }
        }
        String bbox = ctx.queryParam("bbox");
        Map<String, List<String>> rawTags = ctx.queryParamMap();
        Map<String, String> tags = new HashMap<>();
        for (String key : rawTags.keySet()) {
            if (key.startsWith("tags[")) {
                String tagKey = key.substring(5, key.length() - 1);
                tags.put(tagKey, ctx.queryParam(key).isEmpty() ? null : ctx.queryParam(key));

            }
        }
        String jsonTags = null;
        if (!tags.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                jsonTags = mapper.writeValueAsString(tags);

            } catch (JsonProcessingException e) {
                System.out.println(e);
            }
        }
        String after = ctx.queryParam("after");
        StringBuilder pattern = new StringBuilder("{");
        if (bbox != null && !bbox.trim().isEmpty()) {
            bbox = parseBbox(bbox);
            pattern.append(String.format("\"bbox\": \"%s\",", bbox));
        }
        if (after != null && !after.trim().isEmpty())
            pattern.append(String.format("\"after\": \"%s\",", after));
        if (jsonCategories != null && !jsonCategories.isEmpty())
            pattern.append(String.format("\"categories\": %s,", jsonCategories));
        if (jsonTags != null && !jsonTags.trim().isEmpty()) {
            pattern.append(String.format("\"tags\": %s", jsonTags));
        }
        if (pattern.charAt(pattern.length() - 1) == ',') pattern.deleteCharAt(pattern.length() - 1);
        pattern.append("}");
        List<Pin> pins = Pin.retrieve(pattern.toString());
        ctx.json(pins);
    }
    private String parseBbox(String bbox) {
        bbox = bbox.trim();
        bbox = bbox.replaceAll("[\\s,]+", ",");
        return bbox;
    }
}
