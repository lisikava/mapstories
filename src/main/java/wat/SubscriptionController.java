package wat;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class SubscriptionController {
    public void registerRoutes(Javalin app) {
        app.post("/subscribe", this::subscribe);
        app.get("/unsubscribe/{id}", this::unsubscribe);
    }

    private void subscribe(Context ctx) {
        Map<String, Object> json = ctx.bodyAsClass(Map.class);
        String email = json.get("email").toString();
        String pattern = json.get("pattern").toString();
        int tz_offset = Integer.parseInt(json.get("tz_offset").toString());
        SubscriptionManager.subscribe(email, pattern, tz_offset);
        ctx.status(201);
    }

    private void unsubscribe(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        SubscriptionManager.unsubscribe(id);
    }
}
