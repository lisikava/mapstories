package wat;

import io.javalin.Javalin;
import io.javalin.http.Context;

/**
 * Controller for the endpoints specific to the lost and found pins.
 */
public class LostAndFoundController {
    /**
     * Register routes in the Javalin application.
     *
     * @param app Javalin application
     */
    public void registerRoutes(Javalin app) {
        app.get("/rejectMatch/{found_id}", this::rejectMatch);
    }

    private void rejectMatch(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("found_id"));
        LostAndFoundManager.getInstance().deleteMatchingPair(id);
    }
}
