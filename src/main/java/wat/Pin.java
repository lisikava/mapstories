package wat;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.postgresql.geometric.PGpoint;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;

public class Pin {

    private static final DataSource dataSource =
        DataSourceProvider.getDataSource();

    private static final String createQuery =
        """
        insert into pins(location, category, tags)
        values (?, ?, ?)
        returning id, create_time""";

    private static final String retrieveQuery =
        """
        with params as (select
            ? as bbox,
            ? as categories
        )
        select
            pins.id,
            pins.location,
            pins.category,
            hstore_to_json(pins.tags)::text as tags,
            pins.create_time,
            pins.update_time
        from pins, params
        where
            (params.bbox is null or params.bbox @> pins.location) and
            (params.categories is null or pins.category = any(params.categories));
        """;

    public static Pin create(
        final Point location,
        final String category,
        final Map<String, String> tags
    ) {
        Integer id = null;
        Timestamp createTime = null;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(createQuery);
            pstmt.setObject(1, location.asPGpoint());
            pstmt.setString(2, category);
            pstmt.setObject(3, makeHStore(tags));
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

    public static List<Pin> retrieve(
        BoundingBox bbox,
        Collection<String> categories
    ) {
        List<Pin> foundPins = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(retrieveQuery);
            pstmt.setObject(1, bbox.asPGbox());
            pstmt.setArray(2, conn.createArrayOf("text", categories.toArray()));
            JSONParser parser = new JSONParser();
            try (ResultSet resultSet = pstmt.executeQuery()) {
                while (resultSet.next()) {
                    Integer id = resultSet.getInt(1);
                    Point location = new Point(
                        resultSet.getObject(2, PGpoint.class)
                    );
                    String category = resultSet.getString(3);
                    Map<String, String> tags = null;
                    try {
                        JSONObject obj = (JSONObject) parser.parse(
                            resultSet.getString(4)
                        );
                        tags = new TreeMap<>();
                        for (Object key : obj.keySet()) {
                            tags.put((String) key, (String) obj.get(key));
                        }
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    Timestamp createTime = resultSet.getTimestamp(5);
                    Timestamp updateTime = resultSet.getTimestamp(6);

                    foundPins.add(
                        new Pin(
                            id,
                            location,
                            category,
                            tags,
                            createTime,
                            updateTime
                        )
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return foundPins;
    }

    private static PGobject makeHStore(Map<String, String> map)
        throws SQLException {
        PGobject obj = new PGobject();
        obj.setType("hstore");
        obj.setValue(
            map
                .entrySet()
                .stream()
                .map(tag ->
                    String.format("%1s=>\"%2s\"", tag.getKey(), tag.getValue())
                )
                .collect(Collectors.joining(", "))
        );
        return obj;
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
