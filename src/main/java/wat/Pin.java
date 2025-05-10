package wat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import javax.sql.DataSource;

public class Pin {

    private static final DataSource dataSource =
        DataSourceProvider.getDataSource();

    // TODO: prepare an insert query returning a value
    private static final String createQuery = "";

    public static Pin create(
        final Point location,
        final String category,
        final Map<String, String> tags
    ) {
        Integer id = null;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(createQuery);
            // TODO: set prepared statement parameters
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                id = resultSet.getInt(1);
                if (resultSet.next()) {
                    throw new SQLException("Insert returned multiple rows");
                }
            } else {
                throw new SQLException("Insert returned no rows");
            }
        } catch (SQLException e) {
            // TODO: exception handling
            // rethrow with a different type?
        }
        return new Pin(id, location, category, tags);
    }

    private final Integer id;
    private Point location;
    private String category;

    private Map<String, String> tags;

    private Pin(
        final Integer id,
        final Point location,
        final String category,
        final Map<String, String> tags
    ) {
        this.id = id;
        this.location = location;
        this.category = category;
        this.tags = tags;
    }

    public Integer getId() {
        return id;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(final Point location) {
        this.location = location;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public Map<String, String> getTags() {
        return new TreeMap<String, String>(tags);
    }

    public void setTags(final Map<String, String> tags) {
        this.tags = new TreeMap<String, String>(tags);
    }
}
