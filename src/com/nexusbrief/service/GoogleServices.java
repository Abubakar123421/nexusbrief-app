package com.nexusbrief.service;

import com.nexusbrief.model.AppData.*;
import com.nexusbrief.dao.DAOs.*;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoogleServices {

    public static class ClassroomAssignment {
        public final String courseName;
        public final String title;
        public final LocalDateTime dueDate;
        public final String link;

        public ClassroomAssignment(String courseName, String title, LocalDateTime dueDate, String link) {
            this.courseName = courseName;
            this.title      = title;
            this.dueDate    = dueDate;
            this.link       = link;
        }
    }

    public static class GoogleCalendarService {
        public static final String CLIENT_ID     = "YOUR_GOOGLE_CLIENT_ID";
        public static final String CLIENT_SECRET = "YOUR_GOOGLE_CLIENT_SECRET";

        private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";
        private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
        private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
        private static final String CALENDAR_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
        private static final String SCOPE =
                "https://www.googleapis.com/auth/calendar.events" +
                " https://www.googleapis.com/auth/classroom.courses.readonly" +
                " https://www.googleapis.com/auth/classroom.coursework.me.readonly" +
                " https://www.googleapis.com/auth/classroom.announcements.readonly" +
                " email profile";

        private final EmailAccountDAO emailDAO  = new EmailAccountDAO();
        private final CalendarExportDAO exportDAO = new CalendarExportDAO();
        private final DigestItemDAO itemDAO     = new DigestItemDAO();
        private final HttpClient http = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10)).build();

        // Handels the google login flow
        public void startOAuth(String userID, Runnable onComplete, Runnable onError) {
            if (CLIENT_ID.startsWith("YOUR_")) { Platform.runLater(onError); return; }
            new Thread(() -> {
                try (ServerSocket server = new ServerSocket(0)) {
                    int port = server.getLocalPort();
                    String redirectUri = "http://localhost:" + port;
                    String authUrl = AUTH_URL
                            + "?client_id=" + encode(CLIENT_ID)
                            + "&redirect_uri=" + encode(redirectUri)
                            + "&response_type=code"
                            + "&scope=" + encode(SCOPE)
                            + "&access_type=offline"
                            + "&prompt=consent";
                    Desktop.getDesktop().browse(new URI(authUrl));

                    java.net.Socket conn = server.accept();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String requestLine = br.readLine();
                    String html = "<html><body style='font-family:sans-serif;text-align:center;padding:60px'>"
                            + "<h2>✅ NexusBrief connected to Google Calendar!</h2>"
                            + "<p>You can close this tab and return to the app.</p></body></html>";
                    conn.getOutputStream().write(
                            ("HTTP/1.1 200 OK\r\nContent-Type:text/html\r\n\r\n" + html).getBytes());
                    conn.close();

                    if (requestLine != null && requestLine.contains("code=")) {
                        String code = requestLine.split("code=")[1].split("[ &]")[0];
                        String[] tokens = exchangeCode(code, redirectUri);
                        if (tokens != null) { saveAccount(userID, tokens[0], tokens[1], tokens[2]); Platform.runLater(onComplete); }
                        else Platform.runLater(onError);
                    } else Platform.runLater(onError);
                } catch (Exception e) {
                    System.err.println("[GoogleOAuth] " + e.getMessage());
                    Platform.runLater(onError);
                }
            }).start();
        }

        public boolean isConnected(String userID) {
            ExternalEmailAccount acc = emailDAO.findByUser(userID);
            return acc != null && acc.isLinked() && "Gmail".equals(acc.getProvider())
                    && acc.getAccessToken() != null && !acc.getAccessToken().startsWith("TOKEN-");
        }

        public String scheduleReadingTime(String userID, DigestItem item, LocalDateTime readingTime) {
            ExternalEmailAccount acc = emailDAO.findByUser(userID);
            if (acc == null || !isConnected(userID)) return null;
            String accessToken = ensureFreshToken(acc, userID);
            if (accessToken == null) return null;
            String eventLink = createCalendarEvent(accessToken, item, readingTime);
            if (eventLink != null) {
                CalendarExport exp = new CalendarExport(Services.AuthService.uuid(), item.getItemID(),
                        item.getHeadline(), readingTime, "Google Calendar", LocalDateTime.now());
                exportDAO.insert(exp);
                itemDAO.markExported(item.getItemID());
            }
            return eventLink;
        }

        private String[] exchangeCode(String code, String redirectUri) {
            try {
                String body = "code=" + encode(code)
                        + "&client_id=" + encode(CLIENT_ID)
                        + "&client_secret=" + encode(CLIENT_SECRET)
                        + "&redirect_uri=" + encode(redirectUri)
                        + "&grant_type=authorization_code";
                JSONObject json = postForm(TOKEN_URL, body);
                if (json == null) return null;
                String accessToken  = json.optString("access_token");
                String refreshToken = json.optString("refresh_token");
                String email = fetchUserEmail(accessToken);
                return new String[]{accessToken, refreshToken, email};
            } catch (Exception e) {
                System.err.println("[GoogleOAuth] Code exchange failed: " + e.getMessage());
                return null;
            }
        }

        private String ensureFreshToken(ExternalEmailAccount acc, String userID) {
            String refresh = acc.getRefreshToken();
            if (refresh != null && !refresh.startsWith("REFRESH-")) {
                String fresh = doRefresh(refresh);
                if (fresh != null) { emailDAO.updateTokens(userID, fresh, refresh); return fresh; }
            }
            return acc.getAccessToken();
        }

        private String doRefresh(String refreshToken) {
            try {
                String body = "refresh_token=" + encode(refreshToken)
                        + "&client_id=" + encode(CLIENT_ID)
                        + "&client_secret=" + encode(CLIENT_SECRET)
                        + "&grant_type=refresh_token";
                JSONObject json = postForm(TOKEN_URL, body);
                return json == null ? null : json.optString("access_token", null);
            } catch (Exception e) {
                System.err.println("[GoogleOAuth] Refresh failed: " + e.getMessage());
                return null;
            }
        }

        private String fetchUserEmail(String accessToken) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(USERINFO_URL)).header("Authorization", "Bearer " + accessToken).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                return new JSONObject(res.body()).optString("email", "google@user");
            } catch (Exception e) { return "google@user"; }
        }

        private String createCalendarEvent(String accessToken, DigestItem item, LocalDateTime readingTime) {
            try {
                DateTimeFormatter iso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                JSONObject start = new JSONObject().put("dateTime", readingTime.format(iso)).put("timeZone", "UTC");
                JSONObject end   = new JSONObject().put("dateTime", readingTime.plusMinutes(30).format(iso)).put("timeZone", "UTC");
                String desc = (item.getSummary() == null ? "" : item.getSummary())
                        + (item.getSourceURL() != null && !item.getSourceURL().isBlank()
                           ? "\n\nRead full article: " + item.getSourceURL() : "");
                JSONObject event = new JSONObject()
                        .put("summary", "📰 Read: " + item.getHeadline())
                        .put("description", desc).put("start", start).put("end", end);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(CALENDAR_URL))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(event.toString())).build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200 || res.statusCode() == 201)
                    return new JSONObject(res.body()).optString("htmlLink", "https://calendar.google.com");
                System.err.println("[GoogleCalendar] HTTP " + res.statusCode() + ": " + res.body());
            } catch (Exception e) { System.err.println("[GoogleCalendar] Create event failed: " + e.getMessage()); }
            return null;
        }

        private void saveAccount(String userID, String accessToken, String refreshToken, String email) {
            emailDAO.delete(userID);
            emailDAO.insert(new ExternalEmailAccount(Services.AuthService.uuid(), userID, email, "Gmail",
                    accessToken, refreshToken, true, LocalDateTime.now()));
        }

        private JSONObject postForm(String url, String body) throws Exception {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new JSONObject(res.body());
        }

        private static String encode(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
    }

    public static class GoogleClassroomService {
        public static final String ERR_SCOPE   = "ERR_SCOPE";
        public static final String ERR_NETWORK = "ERR_NETWORK";

        private static final String COURSES_URL    = "https://classroom.googleapis.com/v1/courses";
        private static final String COURSEWORK_URL = "https://classroom.googleapis.com/v1/courses/%s/courseWork";

        private final EmailAccountDAO emailDAO = new EmailAccountDAO();
        private final HttpClient http = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10)).build();

        public boolean isConnected(String userID) {
            ExternalEmailAccount acc = emailDAO.findByUser(userID);
            return acc != null && acc.isLinked() && "Gmail".equals(acc.getProvider())
                    && acc.getAccessToken() != null && !acc.getAccessToken().startsWith("TOKEN-");
        }

        public List<ClassroomAssignment> getUpcoming(String userID, int limit) {
            ExternalEmailAccount acc = emailDAO.findByUser(userID);
            if (acc == null) return List.of();
            String token = ensureFreshToken(acc, userID);
            if (token == null) return List.of();

            Object coursesResult = fetchCourses(token);
            if (coursesResult instanceof String)
                return List.of(new ClassroomAssignment((String) coursesResult, "", null, null));

            @SuppressWarnings("unchecked")
            List<String[]> courses = (List<String[]>) coursesResult;
            List<ClassroomAssignment> all = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (String[] course : courses) {
                List<ClassroomAssignment> work = fetchCoursework(token, course[0], course[1]);
                for (ClassroomAssignment a : work) {
                    if (a.dueDate == null || a.dueDate.isAfter(now)) all.add(a);
                }
            }
            all.sort(Comparator.comparing(a -> a.dueDate != null ? a.dueDate : LocalDateTime.of(9999, 12, 31, 0, 0)));
            return all.size() <= limit ? all : all.subList(0, limit);
        }

        private Object fetchCourses(String token) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(COURSES_URL + "?courseStates=ACTIVE&pageSize=20"))
                        .header("Authorization", "Bearer " + token).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Classroom] fetchCourses HTTP " + res.statusCode());
                if (res.statusCode() == 403 || res.statusCode() == 401) { System.out.println("[Classroom] body: " + res.body()); return ERR_SCOPE; }
                if (res.statusCode() != 200) { System.out.println("[Classroom] body: " + res.body()); return ERR_NETWORK; }
                JSONArray courses = new JSONObject(res.body()).optJSONArray("courses");
                List<String[]> result = new ArrayList<>();
                if (courses != null) {
                    for (int i = 0; i < courses.length(); i++) {
                        JSONObject c = courses.getJSONObject(i);
                        result.add(new String[]{c.getString("id"), c.optString("name", "Course")});
                    }
                }
                System.out.println("[Classroom] Found " + result.size() + " active courses");
                return result;
            } catch (Exception e) { System.err.println("[Classroom] fetchCourses exception: " + e.getMessage()); return ERR_NETWORK; }
        }

        private List<ClassroomAssignment> fetchCoursework(String token, String courseId, String courseName) {
            List<ClassroomAssignment> result = new ArrayList<>();
            try {
                String url = String.format(COURSEWORK_URL, courseId) + "?pageSize=20";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url)).header("Authorization", "Bearer " + token).GET().build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Classroom] fetchCoursework '" + courseName + "' HTTP " + res.statusCode());
                if (res.statusCode() != 200) { System.out.println("[Classroom] body: " + res.body()); return result; }
                JSONArray items = new JSONObject(res.body()).optJSONArray("courseWork");
                if (items == null) return result;
                for (int i = 0; i < items.length(); i++) {
                    JSONObject cw = items.getJSONObject(i);
                    String title = cw.optString("title", "Untitled");
                    String link  = cw.optString("alternateLink", null);
                    LocalDateTime due = parseDueDate(cw);
                    result.add(new ClassroomAssignment(courseName, title, due, link));
                    System.out.println("[Classroom]   → " + title + " due=" + due);
                }
            } catch (Exception e) { System.err.println("[Classroom] fetchCoursework exception: " + e.getMessage()); }
            return result;
        }

        private LocalDateTime parseDueDate(JSONObject cw) {
            try {
                if (!cw.has("dueDate")) return null;
                JSONObject d = cw.getJSONObject("dueDate");
                int year = d.optInt("year", 2025), month = d.optInt("month", 1), day = d.optInt("day", 1);
                int hour = 23, min = 59;
                if (cw.has("dueTime")) {
                    JSONObject t = cw.getJSONObject("dueTime");
                    hour = t.optInt("hours", 23);
                    min  = t.optInt("minutes", 59);
                }
                return LocalDateTime.of(year, month, day, hour, min);
            } catch (Exception e) { return null; }
        }

        private String ensureFreshToken(ExternalEmailAccount acc, String userID) {
            String refresh = acc.getRefreshToken();
            if (refresh != null && !refresh.startsWith("REFRESH-")) {
                try {
                    String body = "refresh_token=" + URLEncoder.encode(refresh, StandardCharsets.UTF_8)
                            + "&client_id=" + URLEncoder.encode(GoogleCalendarService.CLIENT_ID, StandardCharsets.UTF_8)
                            + "&client_secret=" + URLEncoder.encode(GoogleCalendarService.CLIENT_SECRET, StandardCharsets.UTF_8)
                            + "&grant_type=refresh_token";
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("https://oauth2.googleapis.com/token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                    String fresh = new JSONObject(res.body()).optString("access_token", null);
                    if (fresh != null) { emailDAO.updateTokens(userID, fresh, refresh); return fresh; }
                } catch (Exception e) { System.err.println("[Classroom] Token refresh failed: " + e.getMessage()); }
            }
            return acc.getAccessToken();
        }
    }
}
