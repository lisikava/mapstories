package wat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.postgresql.geometric.PGpoint;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Pin {

    private static final DataSource dataSource = DataSourceProvider.getDataSource();

    private static final String createQuery = """
            insert into pins(location, category, tags)
            values (?, ?, hstore(?, ?))
            returning id, create_time, update_time""";

    private static final String retrieveQuery = """
            with params as (select
                ? as bbox,
                ? as categories,
                ? as keys,
                ? as values
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
                pins.removed = false and (
                    params.bbox is null or
                    params.bbox @> pins.location
                ) and (
                    params.categories is null or
                    exists (
                        select 1
                        from unnest(params.categories) super
                        where is_subcategory_of(pins.category, super)
                    )
                ) and (
                    params.keys is null or
                    cardinality(params.keys) = 0 or
                    not exists (
                        select 1
                        from unnest(keys, values) as tag(key, value)
                        where not (
                            exist(pins.tags, tag.key) and (
                                tag.value is null or
                                pins.tags->tag.key = tag.value
                            )
                        )
                    )
                )""";

    private static final String updateQuery = """
            update pins
            set
                location = coalesce(?, location),
                category = coalesce(?, category),
                tags = coalesce(hstore(?, ?), tags)
            where
                id = ? and
                removed = false
            returning create_time, update_time""";

    private static final String deleteQuery = """
            update pins
            set
                removed = true
            where
                id = ? and
                removed = false""";

    public static Pin create(final Point location,
                             final String category,
                             final Map<String, String> tags
    ) {
        Integer id = null;
        Timestamp createTime = null;
        Timestamp updateTime = null;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(createQuery);
            pstmt.setObject(1, Point.asPGpoint(location));
            pstmt.setString(2, category);
            pstmt.setArray(3,
                           conn.createArrayOf("text", tags.keySet().toArray())
            );
            pstmt.setArray(4,
                           conn.createArrayOf("text", tags.values().toArray())
            );
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt(1);
                createTime = rs.getTimestamp(2);
                updateTime = rs.getTimestamp(3);
                if (rs.next()) {
                    throw new SQLException("Insert returned multiple rows");
                }
            } else {
                throw new SQLException("Insert returned no rows");
            }
        } catch (SQLException e) {
            // TODO: exception handling
            throw new RuntimeException(e);
        }
        return new Pin(id,
                       location,
                       category,
                       tags,
                       createTime,
                       updateTime,
                       false
        );
    }

    public static List<Pin> retrieve(PinFilter filter) {
        List<Pin> foundPins = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(retrieveQuery);
            if (filter.bbox() != null) {
                pstmt.setObject(1, BoundingBox.asPGbox(filter.bbox()));
            } else {
                pstmt.setNull(1, Types.OTHER);
            }
            pstmt.setArray(2, conn.createArrayOf("text", filter.categories()));
            pstmt.setArray(3,
                           conn.createArrayOf("text",
                                              filter.tags().keySet().toArray()
                           )
            );
            pstmt.setArray(4,
                           conn.createArrayOf("text",
                                              filter.tags().values().toArray()
                           )
            );
            ResultSet rs = pstmt.executeQuery();
            JSONParser parser = new JSONParser();
            while (rs.next()) {
                var tags = new TreeMap<String, String>();
                try {
                    JSONObject obj = (JSONObject) parser.parse(rs.getString(4));
                    tags = new TreeMap<>();
                    for (Object key : obj.keySet()) {
                        tags.put((String) key, (String) obj.get(key));
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                foundPins.add(new Pin(rs.getInt(1),
                                      new Point(rs.getObject(2, PGpoint.class)),
                                      rs.getString(3),
                                      tags,
                                      rs.getTimestamp(5),
                                      rs.getTimestamp(6),
                                      false
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return foundPins;
    }

    public static Pin update(Integer id,
                             Point location,
                             String category,
                             Map<String, String> tags
    ) {
        Timestamp createTime, updateTime;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setObject(1, Point.asPGpoint(location));
            pstmt.setString(2, category);
            pstmt.setArray(3,
                           conn.createArrayOf("text", tags.keySet().toArray())
            );
            pstmt.setArray(4,
                           conn.createArrayOf("text", tags.values().toArray())
            );
            pstmt.setInt(5, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                createTime = rs.getTimestamp(1);
                updateTime = rs.getTimestamp(2);
                if (rs.next()) {
                    throw new SQLException("Update returned multiple rows");
                }
            } else {
                throw new SQLException("Update returned no rows");
            }
        } catch (SQLException e) {
            // TODO: handling
            throw new RuntimeException(e);
        }
        return new Pin(id,
                       location,
                       category,
                       tags,
                       createTime,
                       updateTime,
                       false
        );
    }

    public static void delete(Integer id) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(deleteQuery);
            pstmt.setInt(1, id);
            pstmt.execute();
            assert (pstmt.getUpdateCount() == 1);
        } catch (SQLException e) {
            // TODO: handling
            throw new RuntimeException(e);
        }
    }

    private final Integer id;
    private Point location;
    private String category;
    private Map<String, String> tags;
    private final Timestamp createTime;
    private Timestamp updateTime;
    private boolean removed;

    private Pin(final Integer id,
                final Point location,
                final String category,
                final Map<String, String> tags,
                final Timestamp createTime,
                final Timestamp updateTime,
                final boolean removed
    ) {
        this.id = id;
        this.location = location;
        this.category = category;
        this.tags = tags;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.removed = removed;
    }

    public boolean isRemoved() {
        return removed;
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
