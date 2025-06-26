package wat;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/html", Location.CLASSPATH);
        });
        // LostAndFoundManager lostAndFoundManager = new LostAndFoundManager();
        PinController pc = new PinController();
        pc.registerRoutes(app);
        app.start(7070);
    }
}
