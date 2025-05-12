package wat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private void init() {
        Pin.create(new Point(50, 50), "story", new TreeMap<>());
        Pin.create(new Point(45, 45), "story", new TreeMap<>());
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
            var categories = new ArrayList<String>();
            categories.add("story");
            var pins = Pin.retrieve(
                new BoundingBox(60, 60, 40, 40),
                categories
            );
            System.out.println("Found something, precisely: " + pins.size());
            verbose(pins.get(0));
        });
    }
}
