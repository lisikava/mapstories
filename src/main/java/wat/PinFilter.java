package wat;

import org.postgresql.geometric.PGbox;

import java.util.Map;

public record PinFilter(
    PGbox bbox,
    String[] categories,
    Map<String, String> tags
) {}
