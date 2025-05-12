package wat;

import org.postgresql.geometric.PGpoint;

/**
 * Point on a map. The point is given by its geodetic coordinates with double
 * resolution in WGS84 coordinate system.
 *
 * @param lat latitude
 * @param lon longitude
 */
public record Point(double lat, double lon) {
    @Override
    public String toString() {
        return String.format("(%1f, %2f)", lat, lon);
    }

    public Point(PGpoint point) {
        this(point.x, point.y);
    }

    public PGpoint asPGpoint() {
        return new PGpoint(lat, lon);
    }
}
