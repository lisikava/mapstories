package wat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGpoint;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private void init() {
        Pin.create(new PGpoint(50, 50), "story", new TreeMap<>());
        Pin.create(new PGpoint(45, 45), "story", new TreeMap<>());
    }

    private void verbose(Pin pin) {
        System.out.println("Id      : " + pin.getId());
        System.out.println("Location: " + pin.getLocation());
        System.out.println("Category: " + pin.getCategory());
    }

    @Test
    public void pinRetrievalTest() {
        init();
        assertDoesNotThrow(() -> {
            var pins = Pin.retrieve(
                new PinFilter(new PGbox(60, 60, 40, 40),
                              new String[] { "story" }, Map.of())
            );
            System.out.println("Did not throw, found " + pins.size() + " pins");
            verbose(pins.get(0));
        });
    }
}
