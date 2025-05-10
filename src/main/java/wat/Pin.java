package wat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.TreeMap;
import javax.sql.DataSource;

public class Pin {

    private static final DataSource dataSource =
        DataSourceProvider.getDataSource();

    private static final String createQuery = """
        insert into pins(location, category, tags)
        values (?, ?, ?)
        returning id, create_time""";

    private static String mapToHStore(Map<String, String> map) {
        return map.entrySet().stream()
            .map(tag -> String.format("%1s=>\"%2s\"", tag.getKey(), tag.getValue()))
            .collect(Collectors.joining(", "));
    }

    public static Pin create(
        final Point location,
        final String category,
        final Map<String, String> tags
    ) {
        Integer id = null;
        Timestamp createTime = null;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(createQuery);
            pstmt.setString(1, location.toString());
            pstmt.setString(2, category);
            pstmt.setString(3, mapToHStore(tags));
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
            // rethrow with a different type?
        }
        return new Pin(id, location, category, tags, createTime, createTime);
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
