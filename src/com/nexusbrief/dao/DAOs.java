package com.nexusbrief.dao;

import com.nexusbrief.model.AppData.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class DAOs {

    // USER DAO
    public static class UserDAO extends BaseDAO {
        // mapping database user table to User object [ read from Db columns to java
        // vars]
        private User map(ResultSet rs) throws SQLException {
            return new User(rs.getString("userID"), rs.getString("name"), rs.getString("email"),
                    rs.getString("passwordHash"), rs.getBoolean("isVerified"),
                    toDate(rs.getTimestamp("createdAt")), rs.getBoolean("isAdmin"));
        }

        // Savs a new user
        public boolean insert(User u) {
            return exec("INSERT INTO users VALUES(?,?,?,?,?,?,?)",
                    u.getUserID(), u.getName(), u.getEmail(),
                    u.getPasswordHash(), u.isVerified(), u.getCreatedAt(), u.isAdmin());
        }

        // it will fetch user by email from DB
        public User findByEmail(String email) {
            return queryOne("SELECT * FROM users WHERE email=?", this::map, email);
        }

        public User findByID(String id) {
            return queryOne("SELECT * FROM users WHERE userID=?", this::map, id);
        }

        public List<User> findAll() {
            return queryMany("SELECT * FROM users ORDER BY createdAt DESC", this::map);
        }

        public boolean update(User u) {
            return exec("UPDATE users SET name=?,email=?,passwordHash=? WHERE userID=?",
                    u.getName(), u.getEmail(), u.getPasswordHash(), u.getUserID());
        }

        public boolean updatePassword(String userID, String newPassword) {
            return exec("UPDATE users SET passwordHash=? WHERE userID=?", newPassword, userID);
        }

        public boolean delete(String id) {
            return exec("DELETE FROM users WHERE userID=?", id);
        }
    }

    public static class DigestDAO extends BaseDAO {
        private Digest map(ResultSet rs) throws SQLException {
            DigestStatus status = DigestStatus.valueOf(rs.getString("status").toUpperCase());
            return new Digest(rs.getString("digestID"), rs.getString("userID"),
                    toDate(rs.getTimestamp("generatedAt")), status, rs.getBoolean("isViewed"));
        }

        // [Insert]
        public boolean insert(Digest d) {
            return exec("INSERT INTO digests VALUES(?,?,?,?,?)",
                    d.getDigestID(), d.getUserID(), d.getGeneratedAt(), d.getStatus().name(), d.isViewed());
        }

        // query fro getting latest genrtead digests for a user
        public Digest getLatest(String userID) {
            return queryOne("SELECT TOP 1 * FROM digests WHERE userID=? ORDER BY generatedAt DESC", this::map, userID);
        }

        // list /arr of all digests for a user
        public List<Digest> getHistory(String userID) {
            return queryMany("SELECT * FROM digests WHERE userID=? ORDER BY generatedAt DESC", this::map, userID);
        }

        public boolean markViewed(String digestID) {
            return exec("UPDATE digests SET isViewed=1 WHERE digestID=?", digestID);
        }

        public boolean delete(String digestID) {
            return exec("DELETE FROM digests WHERE digestID=?", digestID);
        }

        public List<Digest> getAll() {
            return queryMany(
                    "SELECT d.*, u.name AS userName FROM digests d LEFT JOIN users u ON d.userID=u.userID ORDER BY d.generatedAt DESC",
                    rs -> {
                        Digest d = map(rs);
                        d.setUserName(rs.getString("userName"));
                        return d;
                    });
        }
    }

    public static class DigestItemDAO extends BaseDAO {
        private DigestItem map(ResultSet rs) throws SQLException {
            DigestItem item = new DigestItem(rs.getString("itemID"), rs.getString("digestID"),
                    rs.getString("sourceID"), rs.getString("headline"),
                    rs.getString("summary"), rs.getString("sourceURL"),
                    toDate(rs.getTimestamp("publishedAt")), rs.getInt("itemRank"), rs.getBoolean("isExported"));
            item.setThumbnailURL(rs.getString("thumbnailURL"));
            return item;
        }

        public boolean insert(DigestItem i) {
            return exec(
                    "INSERT INTO digest_items(itemID,digestID,sourceID,headline,summary,sourceURL,thumbnailURL,publishedAt,itemRank,isExported) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    i.getItemID(), i.getDigestID(), i.getSourceID(), i.getHeadline(),
                    i.getSummary(), i.getSourceURL(), i.getThumbnailURL(), i.getPublishedAt(), i.getItemRank(),
                    i.isExported());
        }

        public List<DigestItem> getByDigest(String digestID) {
            return queryMany("SELECT * FROM digest_items WHERE digestID=? ORDER BY itemRank", this::map, digestID);
        }

        public DigestItem findByID(String id) {
            return queryOne("SELECT * FROM digest_items WHERE itemID=?", this::map, id);
        }

        public boolean markExported(String itemID) {
            return exec("UPDATE digest_items SET isExported=1 WHERE itemID=?", itemID);
        }
    }

    public static class PreferenceDAO extends BaseDAO {
        private DigestPreference mapPref(ResultSet rs) throws SQLException {
            return new DigestPreference(rs.getString("preferenceID"), rs.getString("userID"),
                    PrioritizationMode.valueOf(rs.getString("prioritizationMode")),
                    rs.getTime("deliveryTime").toLocalTime(), toDate(rs.getTimestamp("updatedAt")));
        }

        private PreferenceCategory mapCat(ResultSet rs) throws SQLException {
            return new PreferenceCategory(rs.getString("categoryID"), rs.getString("preferenceID"),
                    rs.getString("categoryName"), rs.getFloat("weight"));
        }

        public boolean insert(DigestPreference p) {
            return exec("INSERT INTO digest_preferences VALUES(?,?,?,?,?)",
                    p.getPreferenceID(), p.getUserID(), p.getPrioritizationMode().name(),
                    p.getDeliveryTime(), p.getUpdatedAt());
        }

        public DigestPreference findByUser(String userID) {
            return queryOne("SELECT * FROM digest_preferences WHERE userID=?", this::mapPref, userID);
        }

        public boolean updateMode(String userID, PrioritizationMode mode) {
            return exec("UPDATE digest_preferences SET prioritizationMode=?,updatedAt=GETDATE() WHERE userID=?",
                    mode.name(), userID);
        }

        public boolean insertCategory(PreferenceCategory c) {
            return exec("INSERT INTO preference_categories VALUES(?,?,?,?)",
                    c.getCategoryID(), c.getPreferenceID(), c.getCategoryName(), c.getWeight());
        }

        public List<PreferenceCategory> getCategoriesByPref(String prefID) {
            return queryMany("SELECT * FROM preference_categories WHERE preferenceID=?", this::mapCat, prefID);
        }

        public boolean deleteCategories(String prefID) {
            return exec("DELETE FROM preference_categories WHERE preferenceID=?", prefID);
        }
    }

    public static class NewsSourceDAO extends BaseDAO {
        private NewsSource map(ResultSet rs) throws SQLException {
            return new NewsSource(rs.getString("sourceID"), rs.getString("name"),
                    rs.getString("category"), rs.getString("apiEndpoint"), rs.getBoolean("isAvailable"));
        }

        public List<NewsSource> findAll() {
            return queryMany("SELECT * FROM news_sources ORDER BY category,name", this::map);
        }

        public NewsSource findByID(String id) {
            return queryOne("SELECT * FROM news_sources WHERE sourceID=?", this::map, id);
        }

        public List<NewsSource> findByUser(String userID) {
            return queryMany(
                    "SELECT ns.* FROM news_sources ns INNER JOIN user_sources us ON ns.sourceID=us.sourceID WHERE us.userID=?",
                    this::map, userID);
        }

        public boolean link(String userID, String sourceID) {
            return exec("INSERT INTO user_sources(userID,sourceID) VALUES(?,?)", userID, sourceID);
        }

        public boolean unlink(String userID, String sourceID) {
            return exec("DELETE FROM user_sources WHERE userID=? AND sourceID=?", userID, sourceID);
        }

        public boolean clearUserSources(String userID) {
            return exec("DELETE FROM user_sources WHERE userID=?", userID);
        }
    }

    public static class ScheduleDAO extends BaseDAO {
        private DigestSchedule map(ResultSet rs) throws SQLException {
            return new DigestSchedule(rs.getString("scheduleID"), rs.getString("userID"),
                    rs.getTime("deliveryTime").toLocalTime(), rs.getInt("intervalHours"),
                    toDate(rs.getTimestamp("nextTriggerAt")), rs.getBoolean("isActive"));
        }

        public boolean insert(DigestSchedule s) {
            return exec("INSERT INTO digest_schedules VALUES(?,?,?,?,?,?)",
                    s.getScheduleID(), s.getUserID(), s.getDeliveryTime(),
                    s.getIntervalHours(), s.getNextTriggerAt(), s.isActive());
        }

        public DigestSchedule findByUser(String userID) {
            return queryOne("SELECT * FROM digest_schedules WHERE userID=?", this::map, userID);
        }

        public boolean update(DigestSchedule s) {
            return exec(
                    "UPDATE digest_schedules SET deliveryTime=?,intervalHours=?,nextTriggerAt=?,isActive=? WHERE userID=?",
                    s.getDeliveryTime(), s.getIntervalHours(), s.getNextTriggerAt(), s.isActive(), s.getUserID());
        }
    }

    public static class EmailAccountDAO extends BaseDAO {
        private ExternalEmailAccount map(ResultSet rs) throws SQLException {
            return new ExternalEmailAccount(rs.getString("emailID"), rs.getString("userID"),
                    rs.getString("emailAddress"), rs.getString("provider"),
                    rs.getString("accessToken"), rs.getString("refreshToken"),
                    rs.getBoolean("isLinked"), toDate(rs.getTimestamp("linkedAt")));
        }

        public boolean insert(ExternalEmailAccount a) {
            return exec("INSERT INTO external_email_accounts VALUES(?,?,?,?,?,?,?,?)",
                    a.getEmailID(), a.getUserID(), a.getEmailAddress(), a.getProvider(),
                    a.getAccessToken(), a.getRefreshToken(), a.isLinked(), a.getLinkedAt());
        }

        public ExternalEmailAccount findByUser(String userID) {
            return queryOne("SELECT * FROM external_email_accounts WHERE userID=?", this::map, userID);
        }

        public boolean updateLinked(String userID, boolean linked) {
            return exec("UPDATE external_email_accounts SET isLinked=? WHERE userID=?", linked, userID);
        }

        public boolean updateTokens(String userID, String accessToken, String refreshToken) {
            return exec("UPDATE external_email_accounts SET accessToken=?,refreshToken=? WHERE userID=?",
                    accessToken, refreshToken, userID);
        }

        public boolean delete(String userID) {
            return exec("DELETE FROM external_email_accounts WHERE userID=?", userID);
        }
    }

    public static class ConnectedSourceDAO extends BaseDAO {
        public boolean insert(ConnectedSource c) {
            return exec("INSERT INTO connected_sources VALUES(?,?,?,?,?)",
                    c.getConnectionID(), c.getUserID(), c.getSourceID(), c.getStatus(), c.getConnectedAt());
        }

        public List<ConnectedSource> findByUser(String userID) {
            return queryMany(
                    "SELECT cs.*, ns.name AS sourceName FROM connected_sources cs LEFT JOIN news_sources ns ON cs.sourceID=ns.sourceID WHERE cs.userID=? ORDER BY cs.connectedAt DESC",
                    rs -> {
                        ConnectedSource c = new ConnectedSource(rs.getString("connectionID"), rs.getString("userID"),
                                rs.getString("sourceID"), rs.getString("status"),
                                toDate(rs.getTimestamp("connectedAt")));
                        c.setSourceName(rs.getString("sourceName"));
                        return c;
                    }, userID);
        }

        public boolean updateStatus(String connectionID, String status) {
            return exec("UPDATE connected_sources SET status=? WHERE connectionID=?", status, connectionID);
        }

        public boolean delete(String connectionID) {
            return exec("DELETE FROM connected_sources WHERE connectionID=?", connectionID);
        }
    }

    public static class FeedbackDAO extends BaseDAO {
        private Feedback map(ResultSet rs) throws SQLException {
            return new Feedback(rs.getString("feedbackID"), rs.getString("itemID"),
                    rs.getString("userID"), FeedbackType.valueOf(rs.getString("rating")),
                    rs.getString("reason"), toDate(rs.getTimestamp("submittedAt")));
        }

        public boolean save(Feedback f) {
            exec("DELETE FROM feedbacks WHERE itemID=? AND userID=?", f.getItemID(), f.getUserID());
            return exec("INSERT INTO feedbacks VALUES(?,?,?,?,?,?)",
                    f.getFeedbackID(), f.getItemID(), f.getUserID(),
                    f.getRating().name(), f.getReason(), f.getSubmittedAt());
        }

        public List<Feedback> findByUser(String userID) {
            return queryMany("SELECT * FROM feedbacks WHERE userID=? ORDER BY submittedAt DESC", this::map, userID);
        }

        public Feedback findByItemUser(String itemID, String userID) {
            return queryOne("SELECT * FROM feedbacks WHERE itemID=? AND userID=?", this::map, itemID, userID);
        }
    }

    public static class CalendarExportDAO extends BaseDAO {
        private CalendarExport map(ResultSet rs) throws SQLException {
            return new CalendarExport(rs.getString("exportID"), rs.getString("itemID"),
                    rs.getString("eventTitle"), toDate(rs.getTimestamp("eventDate")),
                    rs.getString("calendarProvider"), toDate(rs.getTimestamp("exportedAt")));
        }

        public boolean insert(CalendarExport e) {
            return exec("INSERT INTO calendar_exports VALUES(?,?,?,?,?,?)",
                    e.getExportID(), e.getItemID(), e.getEventTitle(),
                    e.getEventDate(), e.getCalendarProvider(), e.getExportedAt());
        }

        public CalendarExport findByItem(String itemID) {
            return queryOne("SELECT * FROM calendar_exports WHERE itemID=?", this::map, itemID);
        }

        public List<CalendarExport> findByUser(String userID) {
            return queryMany("SELECT ce.* FROM calendar_exports ce " +
                    "INNER JOIN digest_items di ON ce.itemID=di.itemID " +
                    "INNER JOIN digests d ON di.digestID=d.digestID WHERE d.userID=?", this::map, userID);
        }
    }

    public static class ApiConfigDAO extends BaseDAO {
        public List<Object[]> getAllWithSource() {
            return queryMany(
                    "SELECT ac.*,ns.name FROM api_configurations ac LEFT JOIN news_sources ns ON ac.sourceID=ns.sourceID",
                    rs -> new Object[] { rs.getString("configID"), rs.getString("providerName"),
                            rs.getString("apiKey"), rs.getBoolean("isValid"), rs.getString("name") });
        }

        public boolean updateKey(String configID, String apiKey) {
            return exec("UPDATE api_configurations SET apiKey=?,lastValidatedAt=GETDATE() WHERE configID=?", apiKey,
                    configID);
        }
    }
}
