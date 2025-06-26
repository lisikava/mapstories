package wat;

import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Singleton for properly persisting states of Lost and Found pins as well as
 * finding their matching counterparts.
 */
public class LostAndFoundManager {
    private static final DataSource ds = DataSourceProvider.getDataSource();
    private static LostAndFoundManager instance;

    // TODO: query matching lost and found pins
    private static final String matchingAgainstLostQuery = """
            with found as (select ? as found)
            select id, tags->'description' as description
            from
                pins left join lost_and_found_matches m
                on pins.id = m.lost
            where
                m.lost is null and
                pins.category = 'lost' and
                defined(pins.tags, 'description') and
                defined(pins.tags, 'email') and
                (
                    found is null or
                    update_time > (
                        select update_time
                        from pins
                        where id = found
                    )
                )
            """;

    private static final String matchingAgainstFoundQuery = """
            with lost as (select ? as lost)
            select id, tags->'description' as description
            from
                pins left join lost_and_found_matches m
                on pins.id = m.found
            where
                m.found is null and
                pins.category = 'found' and
                defined(pins.tags, 'description') and
                defined(pins.tags, 'email') and
                (
                    lost is null or
                    update_time > (
                        select update_time
                        from pins
                        where id = lost
                    )
                )
            """;

    private static final String registerMatchQuery = """
            insert into lost_and_found_matches (lost, found)
            values (?, ?)
            """;

    private static final String unregisterMatchQuery = """
            delete from lost_and_found_matches
            where found = ?
            returning lost;
            """;

    // TODO: actual backend logic
    private static final String viewPinEndpoint =
            "http://localhost:7070/pins";

    private static final String rejectMatchEndpoint =
            "http://localhost:7070/rejectmatch";

    private static final String matchingPairEmail = """
            Hi avid Storyteller!
            
            Your Found pin has been matched through our system!
            
            View the Lost pin and its creator's contact here:
            %s/%d
            
            If you confirm the match, please proceed with returning the item.
            If you think there has been a mistake, please unmatch your pin here:
            %s/%d
            
            Yours faithfully,
            John Mapstory - Java Mail Bot
            """;

    public static LostAndFoundManager getInstance() {
        if (instance == null) {
            instance = new LostAndFoundManager();
        }
        return instance;
    }

    public void matchIfLostOrFound(Pin pin) {
        System.out.println("MATCHING IF LOST OR FOUND");
        if (pin.getCategory().equals("lost") || pin.getCategory()
                .equals("found")) {
            matchAgainstCounterparts(pin);
//            CompletableFuture.runAsync(() -> {matchAgainstCounterparts(pin);});
        }
    }

    private void matchAgainstCounterparts(Pin pin) {
        if (pin.getCategory().equals("lost")) {
            matchAgainstFound(pin);
        } else {
            matchAgainstLost(pin);
        }
    }

    private void matchAgainstLost(Pin pin) {
        matchAgainstLost(pin, null);
    }

    private void matchAgainstLost(Pin pin, Integer foundId) {
        System.out.println("MATCHING AGAINST LOST");
        String foundDesc = pin.getTags().get("description");

        Integer lostId = null;

        boolean match = false;
        try (Connection conn = ds.getConnection()) {
            PreparedStatement pstmt =
                    conn.prepareStatement(matchingAgainstLostQuery);
            if(foundId != null) {
                pstmt.setInt(1, foundId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String lostDesc = rs.getString("description");
                try {
                    match = LostAndFoundMatcher.descriptionsMatch(lostDesc,
                                                                  foundDesc
                    ).get();
                    System.out.println("MATCHING result:" + match);
                } catch (InterruptedException | ExecutionException e) {
                    continue;
                }
                if (match) {
                    lostId = rs.getInt("id");
                    break;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (match) {
            foundId = pin.getId();
            String foundEmail = pin.getTags().get("email");
            createMatchingPair(lostId, foundId, foundEmail);
        }
    }

    private void matchAgainstFound(Pin pin) {
        matchAgainstFound(pin, null);
    }

    private void matchAgainstFound(Pin pin, Integer lostId) {
        String lostDesc = pin.getTags().get("description");

        Integer foundId = null;
        String foundEmail = null;

        boolean match = false;
        try (Connection conn = ds.getConnection()) {
            PreparedStatement pstmt =
                    conn.prepareStatement(matchingAgainstFoundQuery);
            if(lostId != null) {
                pstmt.setInt(1, lostId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String foundDesc = rs.getString("description");
                try {
                    match = LostAndFoundMatcher.descriptionsMatch(lostDesc,
                                                                  foundDesc
                    ).get();
                } catch (InterruptedException | ExecutionException e) {
                    continue;
                }
                if (match) {
                    foundId = rs.getInt("id");
                    foundEmail = rs.getString("email");
                    break;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (match) {
            lostId = pin.getId();
            createMatchingPair(lostId, foundId, foundEmail);
        }
    }

    private void createMatchingPair(int lostId,
                                    int foundId,
                                    String foundEmail
    ) {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement pstmt = conn.prepareStatement(registerMatchQuery);
            pstmt.setInt(1, lostId);
            pstmt.setInt(2, foundId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        EmailSender.composeEmail(
                foundEmail,
                "Potential match for the item you found",
                String.format(matchingPairEmail,
                              viewPinEndpoint,
                              foundId,
                              rejectMatchEndpoint,
                              lostId));
    }

    // TODO: parameters according to the relational model
    public void deleteMatchingPair(Integer foundId) {
//        Integer lostId = null;
        try (Connection conn = ds.getConnection()) {
            PreparedStatement pstmt =
                    conn.prepareStatement(unregisterMatchQuery);
            pstmt.setInt(1, foundId);
            ResultSet rs = pstmt.executeQuery();
//            lostId = rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
//        matchAgainstLost();
    }
}
