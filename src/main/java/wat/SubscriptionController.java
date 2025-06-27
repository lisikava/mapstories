package wat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubscriptionController {

    private record EmailRequest(String email, Integer tz_offset) {}

    public void registerRoutes(Javalin app) {
        app.post("/subscribe", this::subscribe);
        app.get("/unsubscribe/{id}", this::unsubscribe);
    }

    private void subscribe(Context ctx) {
        EmailRequest request = ctx.bodyAsClass(EmailRequest.class);
        String email = request.email;
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
                tags.put(tagKey,
                         ctx.queryParam(key).isEmpty() ?
                         null :
                         ctx.queryParam(key)
                );

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
            bbox = bbox.trim();
            bbox = bbox.replaceAll("[\\s,]+", ",");
            pattern.append(String.format("\"bbox\": \"%s\",", bbox));
        }
        if (after != null && !after.trim().isEmpty())
            pattern.append(String.format("\"after\": \"%s\",", after));
        if (jsonCategories != null && !jsonCategories.isEmpty())
            pattern.append(String.format("\"categories\": %s,",
                                         jsonCategories
            ));
        if (jsonTags != null && !jsonTags.trim().isEmpty()) {
            pattern.append(String.format("\"tags\": %s", jsonTags));
        }
        if (pattern.charAt(pattern.length() - 1) == ',')
            pattern.deleteCharAt(pattern.length() - 1);
        pattern.append("}");
        int tz_offset = 120;
        SubscriptionManager.subscribe(email, pattern.toString(), tz_offset);
        ctx.status(201);
    }

    private void unsubscribe(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        SubscriptionManager.unsubscribe(id);
    }
}
