package wat;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;


public class PinController {
    public void registerRoutes(Javalin app) {
        app.get("/pins", this::getAllPins);
//        app.post("/pins", this::createPin);
    }

    private void getAllPins(Context ctx) {
        List<Pin> pins = Pin.retrieve(
                new PinFilter(
                        new BoundingBox(0,
                                        0,
                                        60,
                                        60
                        ),
                        null,
                        Map.of()
                ));
        ctx.json(pins);
    }


}
