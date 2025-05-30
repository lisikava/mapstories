package wat;

import java.util.Map;

public record PinFilter(
    BoundingBox bbox,
    String[] categories,
    Map<String, String> tags
) {}
