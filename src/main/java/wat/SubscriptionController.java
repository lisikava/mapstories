package wat;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class SubscriptionController {
    public void registerRoutes(Javalin app) {
        app.delete("/subscriptions/{id}", this::deleteSubscription);
    }

    private void deleteSubscription(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        SubscriptionManager.unsubscribe(id);
    }
}
