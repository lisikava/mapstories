package wat;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;


public class PinController {
    public void registerRoutes(Javalin app) {
        app.get("/pins", this::getAllPins);
        app.post("/pins", this::createPin);
    }

    private void getAllPins(Context ctx) {
        List<Pin> pins = Pin.retrieve(
                new PinFilter(
                        new BoundingBox(0, 0, 60, 60),
                        null,
                        Map.of()
                ));
        ctx.json(pins);
    }

    private void createPin(Context ctx) {
        Map<String, Object> json = ctx.bodyAsClass(Map.class);
        Map<String, Double> loc = (Map<String, Double>) json.get("location");
        double lat = loc.get("lat");
        double lon = loc.get("lon");
        String category = (String) json.get("category");
        Map<String, String> tags = (Map<String, String>) json.get("tags");
        Pin pin = Pin.create(new Point(lat, lon), category, tags);
        ctx.status(201).json(pin);
    }

}
