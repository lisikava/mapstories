package wat;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class LostAndFoundController {
    public void registerRoutes(Javalin app) {
        app.get("/rejectMatch/{found_id}", this::rejectMatch);
    }

    private void rejectMatch(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("found_id"));
        LostAndFoundManager.getInstance().deleteMatchingPair(id);
    }
}
