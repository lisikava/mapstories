package wat;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/html", Location.CLASSPATH);
        });
        app.get("/", ctx -> { ctx.redirect("main.html"); });
        app.start(7070);
    }
}
