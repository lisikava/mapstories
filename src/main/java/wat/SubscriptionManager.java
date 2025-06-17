package wat;

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
    // TODO: query that outputs pin counts for each user in their timezone
    private static final String updateSubscriptionsQuery = """
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
     * TODO: enable local updates with arbitrary period
     *
     * @param period period in minutes
     */
    private static void updateSubscriptions(int period) {
        LocalTime thisMoment = LocalTime.now();
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt =
                    conn.prepareStatement(updateSubscriptionsQuery);
            conn.prepareStatement(updateSubscriptionsQuery);
//            LocalTime currentTime =
//                    LocalTime.of(thisMoment.getHour(), thisMoment.getMinute());
            pstmt.setInt(1, period);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int updates = rs.getInt("updates");
                if (updates == 0) continue;
                String email = rs.getString("email");
                String pattern = rs.getString("pattern");
                composeEmail(email, pattern, updates);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void composeEmail(String email,
                                     String pattern,
                                     int updates
    ) {
        // TODO: angus-mail to send emails
    }

    public static void subscribe(String email,
                                 String pattern,
                                 Integer timezoneOffset
    ) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(subscribeQuery);
            pstmt.setString(1, email);
            pstmt.setString(2, pattern);
            pstmt.setInt(3, timezoneOffset);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
//            Integer id = rs.getInt(1); // TODO: welcome email
        } catch (SQLException e) {
            // TODO: exception handling in controllers
            throw new RuntimeException(e);
        }
    }
}
