package wat;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * Application main class.
 */
public class App {
    private App() {}

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/html", Location.CLASSPATH);
        });
        SubscriptionManager.scheduleAtLocal(12, 15, 1);
        LostAndFoundManager lostAndFoundManager = new LostAndFoundManager();
        Pin.registerPostPersistenceHook(lostAndFoundManager::matchIfLostOrFound);
        PinController pc = new PinController();
        pc.registerRoutes(app);
        SubscriptionController sc = new SubscriptionController();
        sc.registerRoutes(app);
        LostAndFoundController lostAndFoundController =
                new LostAndFoundController();
        lostAndFoundController.registerRoutes(app);
        app.start(7070);
    }
}
