package wat;

import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Impure Utility class for posting updates on subscriptions.
 */
public class SubscriptionManager {
    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private static boolean routineStarted = false;

    private static final DataSource dataSource =
            DataSourceProvider.getDataSource();
    private static final String subscribeQuery = """
            insert into subscriptions(email, pattern, tz_offset)
            values(?, ?, ?)
            returning id""";
    // TODO: query that outputs count of newly updated pins for each user
    private static final String digestUpdateQuery = """
            with period as (select ? * interval '1 minute' as period),
            augmented_patterns as (
                select
                    id as sub_id,
                    pattern || jsonb_build_object('after', now() - period) as pattern
                from subscriptions, period
            ),
            search_params as (
                select
                    sub_id,
                    (pattern->>'bbox')::box as bbox,
                    pattern->'categories' as categories,
                    pattern->'tags' as tags,
                    (pattern->>'after')::timestamp as after
                from augmented_patterns
            ),
            results as (
                select
                    pins.id as pin_id,
                    search_params.sub_id
                from pins, search_params
                where
                    (
                        search_params.bbox is null or
                        search_params.bbox @> pins.location
                    ) and (
                        search_params.categories is null or
                        jsonb_typeof(search_params.categories) = 'null' or
                        exists (
                            select 1
                            from jsonb_array_elements_text(search_params.categories) super
                            where is_subcategory_of(pins.category, super)
                        )
                    ) and (
                        search_params.tags is null or
                        jsonb_typeof(search_params.tags) = 'null' or
                        not exists (
                            select 1
                            from jsonb_each_text(search_params.tags) as tag
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
                    )
            )
            select
                subscriptions.id,
                subscriptions.email,
                augmented_patterns.pattern,
                count(results.pin_id) as change_count
            from results
                join augmented_patterns on results.sub_id = augmented_patterns.sub_id
                join subscriptions on augmented_patterns.sub_id = subscriptions.id
            group by subscriptions.id, augmented_patterns.pattern;""";

    private static final String unsubscribeQuery = """
            delete from subscriptions
            where id = ?""";

    private static final String searchEndpoint =
            "http://localhost:7070/pins/search";

    private static final String unsubscribeEndpoint =
            "http://localhost:7070/unsubscribe";

    private static final String welcomeEmail = """
            Hi avid Storyteller!
            
            You have been subscribed for Mapstories updates.
            
            Yours faithfully,
            John Mapstory - Java Mail Bot
            
            Unsubscribe with %s/%d
            """;

    private static final String digestEmail = """
            Hi avid Storyteller!
            
            Your subscription to Mapstories has %d updates.
            
            Yours faithfully,
            John Mapstory - Java Mail Bot
            
            View them at %s?pattern=%s
            Unsubscribe with %s/%d
            """;

    private SubscriptionManager() {}

    /**
     * Starts the routine that tracks updates on user's subscriptions.
     * Updates are scheduled with respect to the user's timezone.
     *
     * @param hour   at which hour local time
     * @param minute at which minute
     * @param period period in minutes which must be a divisor of
     *               24[hr/day]*60[min/hr] = 1440[min/day]
     */
    public static void scheduleAtLocal(int hour, int minute, int period) {
        if (routineStarted) {
            throw new IllegalStateException(
                    "SubscriptionManager is already started");
        }
        long minutesPerDay = TimeUnit.DAYS.toMinutes(1);
        if (minutesPerDay % period != 0) {
            throw new IllegalStateException("Period must be a divisor of 1440");
        }
        scheduler.scheduleAtFixedRate(() -> {
                                          updateSubscriptions(period);
                                      },
                                      initialDelaySeconds(hour, minute, period),
                                      TimeUnit.MINUTES.toSeconds(period),
                                      TimeUnit.SECONDS
        );
        routineStarted = true;
    }

    private static long initialDelaySeconds(int hour, int minute, int period) {
        LocalTime thisMoment = LocalTime.now();
        LocalTime updateTime = LocalTime.of(hour, minute);
        long diff = Duration.between(thisMoment, updateTime).toSeconds();
        return diff % TimeUnit.MINUTES.toSeconds(period);
    }

    /**
     * Update subscriptions.
     * TODO: in perspective, enable local updates with arbitrary period
     *
     * @param period period in minutes
     */
    private static void updateSubscriptions(int period) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(digestUpdateQuery);
            pstmt.setInt(1, period);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int updates = rs.getInt("change_count");
                if (updates == 0) // nothing to inform of
                    continue;
                int id = rs.getInt("id");
                String email = rs.getString("email");
                String pattern = rs.getString("pattern");
                sendDigest(email, id, pattern, updates);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendDigest(String email,
                                   int subscriptionId,
                                   String pattern,
                                   int updates
    ) {
        EmailSender.composeEmail(email,
                                 "Your Mapstories digest",
                                 String.format(digestEmail,
                                               updates,
                                               searchEndpoint,
                                               pattern,
                                               unsubscribeEndpoint,
                                               subscriptionId
                                 )
        );
    }

    private static void sendWelcome(String email, int subscriptionId) {
        EmailSender.composeEmail(email,
                                 "Welcome to Mapstories",
                                 String.format(welcomeEmail,
                                               unsubscribeEndpoint,
                                               subscriptionId)
        );
    }

    public static void subscribe(String email,
                                 String pattern,
                                 Integer timezoneOffset
    ) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(subscribeQuery);
            pstmt.setString(1, email);
            PGobject jsonbObj = new PGobject();
            jsonbObj.setType("jsonb");
            jsonbObj.setValue(pattern);
            pstmt.setObject(2, jsonbObj);
            pstmt.setInt(3, timezoneOffset);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            sendWelcome(email, id);
        } catch (SQLException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
    }

    public static void unsubscribe(int subscriptionId) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(unsubscribeQuery);
            pstmt.setInt(1, subscriptionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
    }
}
