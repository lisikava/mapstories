package wat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.geometric.PGpoint;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Representation of the Pin in project backend. Encapsulates CRUD operations.
 */
public class Pin {

    private static final DataSource dataSource =
            DataSourceProvider.getDataSource();

    private static final String createQuery = """
            insert into pins(location, category, tags)
            values (?, ?, hstore(?, ?))
            returning id, create_time, update_time""";

    private static final String retrieveQuery = """
            with pattern as (select
                ? as pattern
            ),
            params as (
                select
                    (pattern->>'bbox')::box as bbox,
                    pattern->'categories' as categories,
                    pattern->'tags' as tags,
                    (pattern->>'after')::timestamp as after
                from pattern
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
                (
                    params.bbox is null or
                    params.bbox @> pins.location
                ) and (
                    params.categories is null or
                    jsonb_typeof(params.categories) = 'null' or
                    exists (
                        select 1
                        from jsonb_array_elements_text(params.categories) super
                        where is_subcategory_of(pins.category, super)
                    )
                ) and (
                    params.tags is null or
                    jsonb_typeof(params.tags) = 'null' or
                    not exists (
                        select 1
                        from jsonb_each_text(params.tags) as tag
                        where not (
                            exist(pins.tags, tag.key) and (
                                tag.value is null or
                                pins.tags->tag.key = tag.value
                            )
                        )
                    )
                ) and (
                    after is null or
                    update_time > after
                )""";

    private static final String updateQuery = """
            update pins
            set
                location = coalesce(?, location),
                category = coalesce(?, category),
                tags = coalesce(hstore(?, ?), tags)
            where
                id = ?
            returning create_time, update_time""";

    private static final String deleteQuery = """
            delete from pins
            where
                id = ?""";

    private static final Set<Consumer<Pin>> postPersistenceHooks =
            new HashSet<>();

    private static void executePostPersistenceHooks(Pin pin) {
        System.out.println("POST PERSISTENCE HOOKS EXECUTING");
        for (var hook : postPersistenceHooks)
            hook.accept(pin);
    }

    /**
     * Register the action to the executed upon successful persistence of the
     * pin.
     *
     * @param hook action to be executed
     */
    public static void registerPostPersistenceHook(Consumer<Pin> hook) {
        postPersistenceHooks.add(hook);
    }

    /**
     * Create a pin.
     *
     * @param location pin geographical location
     * @param category pin category
     * @param tags     pin tags
     * @return pin representation
     */
    public static Pin create(final PGpoint location,
                             final String category,
                             final Map<String, String> tags
    ) {
        Integer id = null;
        Timestamp createTime = null;
        Timestamp updateTime = null;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(createQuery);
            pstmt.setObject(1, location);
            pstmt.setString(2, category);
            pstmt.setArray(3,
                           conn.createArrayOf("text", tags.keySet().toArray())
            );
            pstmt.setArray(4,
                           conn.createArrayOf("text", tags.values().toArray())
            );
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            id = rs.getInt(1);
            createTime = rs.getTimestamp(2);
            updateTime = rs.getTimestamp(3);
        } catch (SQLException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
        Pin retVal =
                new Pin(id, location, category, tags, createTime, updateTime);
        executePostPersistenceHooks(retVal);
        return retVal;
    }

    /**
     * Retrieve pins matching the pattern.
     *
     * @param pattern pattern
     * @return list of matched pins
     */
    public static List<Pin> retrieve(String pattern) {
        List<Pin> foundPins = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(retrieveQuery);
            ObjectMapper objectMapper = new ObjectMapper();
            PGobject jsonbObj = new PGobject();
            jsonbObj.setType("jsonb");
            jsonbObj.setValue(pattern);
            pstmt.setObject(1, jsonbObj);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                foundPins.add(new Pin(rs.getInt(1),
                                      rs.getObject(2, PGpoint.class),
                                      rs.getString(3),
                                      objectMapper.readValue(rs.getString(4),
                                                             TreeMap.class
                                      ),
                                      rs.getTimestamp(5),
                                      rs.getTimestamp(6)
                ));
            }
        } catch (SQLException | JsonProcessingException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
        return foundPins;
    }

    /**
     * Update an existing pin.
     *
     * @param id       pin identifier
     * @param location pin geographical location
     * @param category pin category
     * @param tags     pin tags
     * @return updated pin representation
     */
    public static Pin update(Integer id,
                             PGpoint location,
                             String category,
                             Map<String, String> tags
    ) {
        Timestamp createTime, updateTime;
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(updateQuery);
            pstmt.setObject(1, location);
            pstmt.setString(2, category);
            pstmt.setArray(3,
                           conn.createArrayOf("text", tags.keySet().toArray())
            );
            pstmt.setArray(4,
                           conn.createArrayOf("text", tags.values().toArray())
            );
            pstmt.setInt(5, id);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            createTime = rs.getTimestamp(1);
            updateTime = rs.getTimestamp(2);
        } catch (SQLException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
        Pin retVal =
                new Pin(id, location, category, tags, createTime, updateTime);
        executePostPersistenceHooks(retVal);
        return retVal;
    }

    /**
     * Delete a pin.
     *
     * @param id pin identifier
     */
    public static void delete(Integer id) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(deleteQuery);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
    }

    private final Integer id;
    private final PGpoint location;
    private final String category;
    private final Map<String, String> tags;
    private final Timestamp createTime;
    private final Timestamp updateTime;

    private Pin(final Integer id,
                final PGpoint location,
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

    public PGpoint getLocation() {
        return location;
    }

    public String getCategory() {
        return category;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public Map<String, String> getTags() {
        return new TreeMap<>(tags);
    }
}
