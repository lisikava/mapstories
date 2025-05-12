package wat;

import org.postgresql.geometric.PGbox;

public record BoundingBox(
    double south,
    double west,
    double north,
    double east
) {
    public BoundingBox(PGbox box) {
        this(box.point[0].x, box.point[0].y, box.point[1].x, box.point[1].y);
    }

    public static PGbox asPGbox(BoundingBox bbox) {
        return (bbox == null)
            ? null
            : new PGbox(bbox.south, bbox.west, bbox.north, bbox.east);
    }
}
