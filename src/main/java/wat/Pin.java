package wat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.postgresql.geometric.PGpoint;
import org.postgresql.util.PGobject;

public class Pin {

    private static final DataSource dataSource =
        DataSourceProvider.getDataSource();

    private static final String createQuery =
        """
        insert into pins(location, category, tags)
        values (?, ?, ?)
        returning id, create_time""";

    public static Pin create(
        final Point location,
        final String category,
        final Map<String, String> tags
    ) {
        Integer id = null;
        Timestamp createTime = null;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(createQuery);
            pstmt.setObject(1, new PGpoint(location.toString()));
            pstmt.setString(2, category);
            PGobject hstorePGobject = new PGobject();
            hstorePGobject.setType("hstore");
            hstorePGobject.setValue(mapToHStore(tags));
            pstmt.setObject(3, hstorePGobject);
            ResultSet resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                id = resultSet.getInt(1);
                createTime = resultSet.getTimestamp(2);
                if (resultSet.next()) {
                    throw new SQLException("Insert returned multiple rows");
                }
            } else {
                throw new SQLException("Insert returned no rows");
            }
        } catch (SQLException e) {
            // TODO: exception handling
            throw new RuntimeException(e);
        }
        return new Pin(id, location, category, tags, createTime, createTime);
    }

    private static String mapToHStore(Map<String, String> map) {
        return map
            .entrySet()
            .stream()
            .map(tag ->
                String.format("%1s=>\"%2s\"", tag.getKey(), tag.getValue())
            )
            .collect(Collectors.joining(", "));
    }

    private final Integer id;
    private Point location;
    private String category;
    private Map<String, String> tags;
    private final Timestamp createTime;
    private Timestamp updateTime;

    private Pin(
        final Integer id,
        final Point location,
        final String category,
        final Map<String, String> tags,
        final Timestamp createTime,
        final Timestamp updateTime
    ) {
        this.id = id;
        this.location = location;
        this.category = category;
        this.tags = tags;
        this.createTime = createTime;
        this.updateTime = updateTime;
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

    public Timestamp getCreateTime() {
        return createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
