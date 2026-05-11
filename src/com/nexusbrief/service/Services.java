package com.nexusbrief.service;

import com.nexusbrief.model.AppData.*;

import com.nexusbrief.dao.DAOs.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader; // feeds XML string into parser without writing to temp file
import java.net.URI;
import java.net.http.HttpClient; // FOR hhtp calls to rss feeds  N GNEWS API
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat; // date n time libs
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONArray; // External JSON libo for parsing GNews API responses.
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Services {

    public static class AuthService {
        private final UserDAO userDAO = new UserDAO();
        private final PreferenceDAO prefDAO = new PreferenceDAO(); // -> instance for user and pref of user

        public String register(String name, String email, String password) {
            if (name == null || name.isBlank())
                return "Name is required";
            if (!email.matches(".+@.+\\..+"))
                return "Invalid email format";
            if (password.length() < 6)
                return "Password must be 6+ characters";
            if (userDAO.findByEmail(email) != null)
                return "Email already registered";

            User u = new User(uuid(), name.trim(), email.trim().toLowerCase(),
                    password, true, LocalDateTime.now(), false);
            if (!userDAO.insert(u)) // using DB layer to insert/update the user
                return "Database error";

            DigestPreference pref = new DigestPreference(uuid(), u.getUserID(),
                    PrioritizationMode.LATEST_FIRST, LocalTime.of(8, 0), LocalDateTime.now());
            prefDAO.insert(pref); // -> inserts default preferences for user
            return null;
        }

        public User login(String email, String password) {
            if (email == null || password == null)
                return null;
            User u = userDAO.findByEmail(email.trim().toLowerCase());
            if (u == null || !password.equals(u.getPasswordHash()))
                return null;
            return u;
        }

        public static String uuid() {
            return UUID.randomUUID().toString();
        }
    }

    public static class PreferenceService {
        private final PreferenceDAO dao = new PreferenceDAO();

        public DigestPreference getPreferences(String userID) {
            DigestPreference p = dao.findByUser(userID);
            if (p != null)
                p.setCategories(dao.getCategoriesByPref(p.getPreferenceID()));
            return p;
        }

        public String saveCategories(String userID, List<PreferenceCategory> cats) {
            if (cats == null || cats.isEmpty())
                return "Preferences not found";
            boolean anyActive = false;
            for (PreferenceCategory c : cats) {
                if (c.getWeight() > 0) {
                    anyActive = true;
                    break;
                }
            }
            if (!anyActive)
                return "Enable at least one category with weight above 0";
            DigestPreference p = dao.findByUser(userID);
            if (p == null)
                return "Preferences not found";
            dao.deleteCategories(p.getPreferenceID());
            for (PreferenceCategory c : cats) {
                c.setCategoryID(AuthService.uuid());
                c.setPreferenceID(p.getPreferenceID());
                dao.insertCategory(c);
            }
            return null;
        }

        public boolean updateMode(String userID, PrioritizationMode mode) {
            return dao.updateMode(userID, mode);
        }

        public PrioritizationMode getMode(String userID) {
            DigestPreference p = dao.findByUser(userID);
            return p == null ? PrioritizationMode.LATEST_FIRST : p.getPrioritizationMode();
        }
    }

    public static class NewsSourceService {
        private final NewsSourceDAO dao = new NewsSourceDAO();

        public List<NewsSource> getAll() {
            return dao.findAll();
        }

        public List<NewsSource> getUserSources(String u) {
            return dao.findByUser(u);
        }

        public String saveSources(String userID, List<NewsSource> selected) {
            if (selected == null || selected.isEmpty())
                return "Select at least one source";
            dao.clearUserSources(userID);
            for (NewsSource s : selected)
                dao.link(userID, s.getSourceID());
            return null;
        }

        public boolean addSource(String userID, String sourceID) {
            return dao.link(userID, sourceID);
        }

        public boolean removeSource(String userID, String sourceID) {
            return dao.unlink(userID, sourceID);
        }
    }

    public static class EmailService {
        private final EmailAccountDAO emailDAO = new EmailAccountDAO();
        private final ConnectedSourceDAO connDAO = new ConnectedSourceDAO();

        public String linkEmail(String userID, String emailAddr, String provider) {
            if (!emailAddr.matches(".+@.+\\..+"))
                return "Invalid email address";
            emailDAO.delete(userID);
            ExternalEmailAccount a = new ExternalEmailAccount(
                    AuthService.uuid(), userID, emailAddr.trim().toLowerCase(), provider,
                    "TOKEN-" + AuthService.uuid(), "REFRESH-" + AuthService.uuid(),
                    true, LocalDateTime.now());
            return emailDAO.insert(a) ? null : "Could not save email";
        }

        public ExternalEmailAccount getLinkedEmail(String userID) {
            return emailDAO.findByUser(userID);
        }

        public boolean disconnectEmail(String userID) {
            return emailDAO.updateLinked(userID, false);
        }

        public List<ConnectedSource> getConnections(String userID) {
            return connDAO.findByUser(userID);
        }

        public boolean addConnection(String userID, String sourceID) {
            ConnectedSource c = new ConnectedSource(AuthService.uuid(), userID, sourceID, "ACTIVE",
                    LocalDateTime.now());
            return connDAO.insert(c);
        }

        public boolean setConnectionStatus(String connID, String status) {
            return connDAO.updateStatus(connID, status);
        }

        public boolean removeConnection(String connID) {
            return connDAO.delete(connID);
        }
    }

    public static class ScheduleService {
        private final ScheduleDAO dao = new ScheduleDAO();

        public DigestSchedule getSchedule(String userID) {
            return dao.findByUser(userID);
        }

        public String save(String userID, LocalTime time, int intervalHours) {
            if (time == null)
                return "Delivery time required";
            if (intervalHours < 1)
                intervalHours = 1;
            LocalDateTime next = LocalDateTime.now().withHour(time.getHour()).withMinute(time.getMinute())
                    .withSecond(0).withNano(0);
            if (next.isBefore(LocalDateTime.now()))
                next = next.plusDays(1);
            DigestSchedule existing = dao.findByUser(userID);
            if (existing == null) {
                DigestSchedule s = new DigestSchedule(AuthService.uuid(), userID, time, intervalHours, next, true);
                return dao.insert(s) ? null : "Could not save schedule";
            } else {
                existing.setDeliveryTime(time);
                existing.setIntervalHours(intervalHours);
                existing.setNextTriggerAt(next);
                existing.setActive(true);
                return dao.update(existing) ? null : "Could not update schedule";
            }
        }

        public boolean checkAndTrigger(String userID) {
            DigestSchedule s = dao.findByUser(userID);
            if (s == null || !s.isActive() || s.getNextTriggerAt() == null)
                return false;

            if (LocalDateTime.now().isAfter(s.getNextTriggerAt())) {
                s.setNextTriggerAt(LocalDateTime.now().plusHours(s.getIntervalHours()));
                dao.update(s);
                return true;
            }
            return false;
        }
    }

    public static class DigestService {
        private final DigestDAO digestDAO = new DigestDAO();
        private final DigestItemDAO itemDAO = new DigestItemDAO();

        public Digest getLatest(String userID) {
            Digest d = digestDAO.getLatest(userID);
            if (d != null) {
                d.setItems(itemDAO.getByDigest(d.getDigestID()));
                digestDAO.markViewed(d.getDigestID());
            }
            return d;
        }

        public List<Digest> getHistory(String userID) {
            List<Digest> list = digestDAO.getHistory(userID);
            for (Digest d : list)
                d.setItems(itemDAO.getByDigest(d.getDigestID()));
            return list;
        }
    }

    public static class FeedbackService {
        private final FeedbackDAO dao = new FeedbackDAO();
        private final List<DigestObserver> observers = new ArrayList<>();

        public void registerObserver(DigestObserver o) {
            if (!observers.contains(o))
                observers.add(o);
        }

        public String submit(String itemID, String userID, FeedbackType rating, String reason) {
            if (itemID == null || userID == null)
                return "Missing item or user";
            if (rating == null)
                return "Please choose a rating";
            Feedback fb = new Feedback(AuthService.uuid(), itemID, userID, rating,
                    reason == null ? "" : reason.trim(), LocalDateTime.now());
            if (!dao.save(fb))
                return "Could not save feedback";
            for (DigestObserver o : observers)
                o.onFeedbackSubmitted(userID, itemID, rating.name());
            return null;
        }

        public Feedback getForItem(String itemID, String userID) {
            return dao.findByItemUser(itemID, userID);
        }
    }

    public static class AdminService {
        private final UserDAO userDAO = new UserDAO();
        private final DigestDAO digestDAO = new DigestDAO();
        private final DigestItemDAO itemDAO = new DigestItemDAO();
        private final ApiConfigDAO apiDAO = new ApiConfigDAO();
        private final ScheduleDAO schedDAO = new ScheduleDAO();

        // ^ declare all abve instanccces

        public List<User> getNonAdminUsers() {
            List<User> result = new ArrayList<>();
            for (User u : userDAO.findAll())
                if (!u.isAdmin())
                    result.add(u);
            return result;
        }

        public boolean resetPassword(String userID, String newPassword) {
            return userDAO.updatePassword(userID, newPassword);
        }

        public boolean deleteUser(String userID) {
            return userDAO.delete(userID);
        }

        public List<Digest> getAllDigestsWithItems() {
            List<Digest> all = digestDAO.getAll();
            for (Digest d : all)
                d.getItems().addAll(itemDAO.getByDigest(d.getDigestID()));
            return all;
        }

        public List<Object[]> getApiConfigs() {
            return apiDAO.getAllWithSource();
        }

        public boolean updateApiKey(String configID, String newKey) {
            return apiDAO.updateKey(configID, newKey);
        }

        public DigestSchedule getScheduleForUser(String userID) {
            return schedDAO.findByUser(userID);
        }
    }

    public static class CalendarExportService {
        private final CalendarExportDAO exportDAO = new CalendarExportDAO();
        private final DigestItemDAO itemDAO = new DigestItemDAO();

        public String export(String itemID, LocalDateTime eventDate) {
            DigestItem item = itemDAO.findByID(itemID);
            if (item == null)
                return null;

            // tmrw scheduled date by defualt
            if (eventDate == null)
                eventDate = LocalDateTime.now().plusDays(1);
            //
            //
            CalendarExport exp = new CalendarExport(AuthService.uuid(), itemID,
                    item.getHeadline(), eventDate, "iCal", LocalDateTime.now());
            exportDAO.insert(exp);
            // Records export in DB, marks item exported
            // -~_~_~_~_~_~_~_~_~_~_~_~_~~!!!!!!!!!!!!!!!
            itemDAO.markExported(itemID);
            String filename = "NexusBrief_" + AuthService.uuid().substring(0, 8) + ".ics";
            try (FileWriter fw = new FileWriter(filename)) {
                fw.write(buildICS(item, eventDate));
            } catch (IOException e) {
                return null;
            }
            return filename;
        }

        private String buildICS(DigestItem item, LocalDateTime date) {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
            return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//NexusBrief//EN\r\n" +
                    "BEGIN:VEVENT\r\nUID:" + AuthService.uuid() + "@nexusbrief\r\n" +
                    "DTSTAMP:" + LocalDateTime.now().format(f) + "\r\n" +
                    "DTSTART:" + date.format(f) + "\r\n" +
                    "DTEND:" + date.plusHours(1).format(f) + "\r\n" +
                    "SUMMARY:" + esc(item.getHeadline()) + "\r\n" +
                    "DESCRIPTION:" + esc(item.getSummary()) + "\r\n" +
                    "URL:" + item.getSourceURL() + "\r\n" +
                    "END:VEVENT\r\nEND:VCALENDAR\r\n";
        }

        private String esc(String s) {
            return s == null ? "" : s.replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
        }
    }

    public static class RssNewsService {

        private static final Map<String, String> FEEDS = new LinkedHashMap<>();
        static {
            FEEDS.put("bbc news", "https://feeds.bbci.co.uk/news/world/rss.xml");
            FEEDS.put("bbc", "https://feeds.bbci.co.uk/news/world/rss.xml");
            FEEDS.put("cnn", "http://rss.cnn.com/rss/edition.rss");
            FEEDS.put("the guardian", "https://www.theguardian.com/world/rss");
            FEEDS.put("guardian", "https://www.theguardian.com/world/rss");
            FEEDS.put("al jazeera", "https://www.aljazeera.com/xml/rss/all.xml");
            FEEDS.put("npr", "https://feeds.npr.org/1001/rss.xml");
            FEEDS.put("abc news", "https://abcnews.go.com/abcnews/topstories");
            FEEDS.put("fox news", "https://moxie.foxnews.com/google-publisher/latest.xml");
            FEEDS.put("new york times", "https://rss.nytimes.com/services/xml/rss/nyt/World.xml");
            FEEDS.put("sky news", "https://feeds.skynews.com/feeds/rss/world.xml");
            FEEDS.put("bbc sport", "https://feeds.bbci.co.uk/sport/rss.xml");
            FEEDS.put("espn", "https://www.espn.com/espn/rss/news");
            FEEDS.put("sky sports", "https://www.skysports.com/rss/12040");
            FEEDS.put("cbs sports", "https://www.cbssports.com/rss/headlines/");
            FEEDS.put("nyt sports", "https://rss.nytimes.com/services/xml/rss/nyt/Sports.xml");
            FEEDS.put("techcrunch", "https://techcrunch.com/feed/");
            FEEDS.put("the verge", "https://www.theverge.com/rss/index.xml");
            FEEDS.put("verge", "https://www.theverge.com/rss/index.xml");
            FEEDS.put("ars technica", "https://feeds.arstechnica.com/arstechnica/index");
            FEEDS.put("wired", "https://www.wired.com/feed/rss");
            FEEDS.put("engadget", "https://www.engadget.com/rss.xml");
            FEEDS.put("zdnet", "https://www.zdnet.com/news/rss.xml");
            FEEDS.put("cnet", "https://www.cnet.com/rss/news/");
            FEEDS.put("mit technology", "https://www.technologyreview.com/feed/");
            FEEDS.put("technology review", "https://www.technologyreview.com/feed/");
            FEEDS.put("9to5mac", "https://9to5mac.com/feed/");
            FEEDS.put("android authority", "https://www.androidauthority.com/feed/");
            FEEDS.put("coindesk", "https://www.coindesk.com/arc/outboundfeeds/rss/");
            FEEDS.put("cointelegraph", "https://cointelegraph.com/rss");
            FEEDS.put("coin telegraph", "https://cointelegraph.com/rss");
            FEEDS.put("decrypt", "https://decrypt.co/feed");
            FEEDS.put("the block", "https://www.theblock.co/rss.xml");
            FEEDS.put("cryptonews", "https://cryptonews.com/news/feed/");
            FEEDS.put("marketwatch", "https://feeds.marketwatch.com/marketwatch/topstories/");
            FEEDS.put("market watch", "https://feeds.marketwatch.com/marketwatch/topstories/");
            FEEDS.put("yahoo finance", "https://finance.yahoo.com/rss/");
            FEEDS.put("cnbc", "https://www.cnbc.com/id/100003114/device/rss/rss.html");
            FEEDS.put("fortune", "https://fortune.com/feed/");
            FEEDS.put("bloomberg", "https://feeds.bloomberg.com/markets/news.rss");
            FEEDS.put("business insider", "https://www.businessinsider.com/rss");
            FEEDS.put("the economist", "https://www.economist.com/the-world-this-week/rss.xml");
            FEEDS.put("financial times", "https://www.ft.com/rss/home");
            FEEDS.put("investing.com", "https://www.investing.com/rss/news.rss");
            FEEDS.put("seeking alpha", "https://seekingalpha.com/feed.xml");
            FEEDS.put("science daily", "https://www.sciencedaily.com/rss/all.xml");
            FEEDS.put("sciencedaily", "https://www.sciencedaily.com/rss/all.xml");
            FEEDS.put("nasa", "https://www.nasa.gov/news-release/feed/");
            FEEDS.put("new scientist", "https://www.newscientist.com/feed/home/");
            FEEDS.put("scientific american", "https://rss.sciam.com/ScientificAmerican-Global");
            FEEDS.put("phys.org", "https://phys.org/rss-feed/");
            FEEDS.put("nature", "https://www.nature.com/nature.rss");
            FEEDS.put("space.com", "https://www.space.com/feeds/all");
            FEEDS.put("live science", "https://www.livescience.com/feeds/all");
            FEEDS.put("politico", "https://www.politico.com/rss/politicopicks.xml");
            FEEDS.put("the hill", "https://thehill.com/feed/");
            FEEDS.put("hill", "https://thehill.com/feed/");
            FEEDS.put("axios", "https://api.axios.com/feed/");
            FEEDS.put("vox", "https://www.vox.com/rss/index.xml");
            FEEDS.put("npr politics", "https://feeds.npr.org/1014/rss.xml");
        }

        private static final HttpClient HTTP = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build();

        public List<DigestItem> fetch(NewsSource src, int maxItems) {
            String feedUrl = resolveUrl(src);
            if (feedUrl == null)
                return Collections.emptyList();
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(feedUrl))
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", "NexusBrief/1.0 RSS Reader")
                        .GET().build();
                HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    System.err.println("[RSS] HTTP " + res.statusCode() + " – " + src.getName());
                    return Collections.emptyList();
                }
                List<DigestItem> items = parseXml(res.body(), src, maxItems);
                if (!items.isEmpty())
                    System.out.println("[RSS] " + items.size() + " articles ← " + src.getName());
                return items;
            } catch (java.net.http.HttpTimeoutException e) {
                System.err.println("[RSS] Timeout – " + src.getName());
            } catch (Exception e) {
                System.err.println("[RSS] " + src.getName() + ": " + e.getMessage());
            }
            return Collections.emptyList();
        }

        private String resolveUrl(NewsSource src) {
            String name = src.getName() == null ? "" : src.getName().toLowerCase().trim();
            if (FEEDS.containsKey(name))
                return FEEDS.get(name);
            for (Map.Entry<String, String> e : FEEDS.entrySet()) {
                if (name.contains(e.getKey()) || e.getKey().contains(name))
                    return e.getValue();
            }
            String ep = src.getApiEndpoint();
            if (ep != null
                    && (ep.contains("/rss") || ep.contains("/feed") || ep.endsWith(".xml") || ep.contains("atom")))
                return ep;
            return null;
        }

        private List<DigestItem> parseXml(String xml, NewsSource src, int maxItems) {
            List<DigestItem> items = new ArrayList<>();
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                dbf.setExpandEntityReferences(false);
                DocumentBuilder db = dbf.newDocumentBuilder();
                db.setErrorHandler(null);
                Document doc = db.parse(new InputSource(new StringReader(xml)));
                doc.getDocumentElement().normalize();

                NodeList nodes = doc.getElementsByTagName("item");
                boolean isAtom = nodes.getLength() == 0;
                if (isAtom)
                    nodes = doc.getElementsByTagName("entry");

                int limit = Math.min(nodes.getLength(), maxItems);
                for (int i = 0; i < limit; i++) {
                    Element el = (Element) nodes.item(i);
                    String title = text(el, "title");
                    String rawDesc = isAtom ? text(el, "summary") : text(el, "description");
                    String link = isAtom ? attr(el, "link", "href") : text(el, "link");
                    String pubDate = isAtom ? coalesce(text(el, "published"), text(el, "updated"))
                            : coalesce(text(el, "pubDate"), text(el, "updated"));
                    if (title == null || title.isBlank())
                        continue;

                    String thumb = extractThumbnail(el, rawDesc);
                    String desc = rawDesc;
                    if (desc != null)
                        desc = desc.replaceAll("<[^>]+>", "").trim();
                    if (desc != null && desc.length() > 350)
                        desc = desc.substring(0, 347) + "…";

                    DigestItem item = new DigestItem();
                    item.setSourceID(src.getSourceID());
                    item.setHeadline(title.trim());
                    item.setSummary(desc != null && !desc.isBlank() ? desc : "(No description)");
                    item.setSourceURL(link != null ? link.trim() : "");
                    item.setThumbnailURL(thumb);
                    item.setPublishedAt(parseDate(pubDate));
                    item.setItemRank(i + 1);
                    item.setExported(false);
                    items.add(item);
                }
            } catch (Exception e) {
                System.err.println("[RSS] XML parse error – " + src.getName() + ": " + e.getMessage());
            }
            return items;
        }

        private String extractThumbnail(Element el, String descHtml) {
            String url = attrAnyNs(el, "thumbnail", "url");
            if (url != null)
                return url;
            NodeList contents = el.getElementsByTagName("*");
            for (int i = 0; i < contents.getLength(); i++) {
                Element n = (Element) contents.item(i);
                String local = n.getLocalName() == null ? n.getNodeName() : n.getLocalName();
                if (local == null)
                    continue;
                local = local.toLowerCase();
                if (local.equals("content") || local.endsWith(":content")) {
                    String u = n.getAttribute("url");
                    String medium = n.getAttribute("medium");
                    String type = n.getAttribute("type");
                    if (u != null && !u.isBlank()
                            && ("image".equalsIgnoreCase(medium)
                                    || (type != null && type.toLowerCase().startsWith("image"))
                                    || u.matches("(?i).+\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$"))) {
                        return u;
                    }
                }
            }
            NodeList encs = el.getElementsByTagName("enclosure");
            for (int i = 0; i < encs.getLength(); i++) {
                Element en = (Element) encs.item(i);
                String type = en.getAttribute("type");
                String u = en.getAttribute("url");
                if (u != null && !u.isBlank()
                        && (type == null || type.isBlank() || type.toLowerCase().startsWith("image"))) {
                    return u;
                }
            }
            if (descHtml != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(descHtml);
                if (m.find())
                    return m.group(1);
            }
            return null;
        }

        private String attrAnyNs(Element el, String localName, String attr) {
            NodeList all = el.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Element n = (Element) all.item(i);
                String local = n.getLocalName() == null ? n.getNodeName() : n.getLocalName();
                if (local == null)
                    continue;
                if (local.equalsIgnoreCase(localName) || local.toLowerCase().endsWith(":" + localName.toLowerCase())) {
                    String v = n.getAttribute(attr);
                    if (v != null && !v.isBlank())
                        return v;
                }
            }
            return null;
        }

        private String text(Element parent, String tag) {
            NodeList nl = parent.getElementsByTagName(tag);
            return nl.getLength() == 0 ? null : nl.item(0).getTextContent();
        }

        private String attr(Element parent, String tag, String attrName) {
            NodeList nl = parent.getElementsByTagName(tag);
            if (nl.getLength() == 0)
                return null;
            String val = ((Element) nl.item(0)).getAttribute(attrName);
            return val.isBlank() ? null : val;
        }

        private String coalesce(String a, String b) {
            return (a != null && !a.isBlank()) ? a : b;
        }

        private LocalDateTime parseDate(String raw) {
            if (raw == null || raw.isBlank())
                return LocalDateTime.now();
            raw = raw.trim();
            try {
                java.util.Date d = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(raw);
                return d.toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            } catch (Exception ignore) {
            }
            try {
                return OffsetDateTime.parse(raw).toLocalDateTime();
            } catch (Exception ignore) {
            }
            try {
                return LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ignore) {
            }
            return LocalDateTime.now();
        }
    }

    public static class DigestGeneratorService implements DigestObserver {
        private final DigestDAO digestDAO = new DigestDAO();
        private final DigestItemDAO itemDAO = new DigestItemDAO();
        private final NewsSourceDAO sourceDAO = new NewsSourceDAO();
        private final PreferenceService prefService = new PreferenceService();
        // GNEWS API key
        private static final String API_TOKEN = "67c44558e959aec8caa392ec595fa6b8"; // API KEY FOR GNEWS
        private static final String BASE_URL = "https://gnews.io/api/v4/top-headlines";

        private static final String[] FALLBACK_HEADLINES = {
                "Markets rally on strong earnings", "Tech giant unveils new AI model",
                "Championship final ends in overtime", "Crypto prices surge to new highs",
                "Central bank signals rate cut", "Major sports trade shakes league",
                "Startup raises record funding", "Scientists find cancer treatment",
                "Global summit reaches agreement", "New regulations target social media"
        };
        private static final String[] FALLBACK_SUMMARIES = {
                "Investors showed strong confidence as companies exceeded quarterly expectations, sending indexes near record highs.",
                "The company's latest model outperforms benchmarks by a significant margin across multiple industry standards.",
                "A last-second play decided the championship in what commentators call one of the most exciting finals ever.",
                "Digital asset markets posted notable gains as institutional adoption accelerates amid favorable regulation.",
                "The central bank chair indicated economic conditions may warrant a policy adjustment in coming months."
        };

        // Builds the custom news digst
        public Digest generate(String userID) {
            List<NewsSource> sources = sourceDAO.findByUser(userID);
            if (sources.isEmpty())
                sources = sourceDAO.findAll();

            DigestPreference pref = prefService.getPreferences(userID);
            if (pref != null && !pref.getCategories().isEmpty()) {
                Set<String> enabled = new LinkedHashSet<>();
                for (PreferenceCategory c : pref.getCategories()) {
                    if (c.getWeight() > 0)
                        enabled.add(c.getCategoryName().toLowerCase());
                }
                if (!enabled.isEmpty()) {
                    List<NewsSource> filtered = new ArrayList<>();
                    for (NewsSource s : sources) {
                        if (s.getCategory() != null && enabled.contains(s.getCategory().toLowerCase()))
                            filtered.add(s);
                    }
                    if (!filtered.isEmpty())
                        sources = filtered;
                }
            }

            List<DigestItem> pool = buildPool(sources);
            PrioritizationMode mode = prefService.getMode(userID);
            PrioritizationStrategy strategy = StrategyFactory.create(mode);
            List<DigestItem> top5 = strategy.rank(pool, 5);

            Digest digest = new Digest(AuthService.uuid(), userID, LocalDateTime.now(), DigestStatus.GENERATED, false);
            digestDAO.insert(digest);

            int rank = 1;
            for (DigestItem item : top5) {
                item.setItemID(AuthService.uuid());
                item.setDigestID(digest.getDigestID());
                item.setItemRank(rank++);
                itemDAO.insert(item);
                digest.addItem(item);
            }
            System.out.println(
                    "[DigestGenerator] Created digest with " + top5.size() + " items using: " + strategy.getName());
            return digest;
        }

        private List<DigestItem> buildPool(List<NewsSource> sources) {
            List<DigestItem> pool = new ArrayList<>();
            RssNewsService rss = new RssNewsService(); // RSS INSTACE
            Set<String> needGNews = new LinkedHashSet<>();

            for (NewsSource src : sources) {
                List<DigestItem> rssItems = rss.fetch(src, 3);
                if (!rssItems.isEmpty()) { // FINDS PRIORTIES FROM RSS INSTANCE
                    pool.addAll(rssItems);
                    continue;
                }
                String cat = src.getCategory() == null ? "general" : src.getCategory().trim().toLowerCase();
                needGNews.add(cat);
            }
            // GNEWS API
            if (!needGNews.isEmpty()) {
                HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
                for (String category : needGNews) {
                    try {
                        String url = buildGNewsUrl(category);
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build();
                        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                        if (res.statusCode() != 200)
                            continue;
                        JSONArray articles = new JSONObject(res.body()).optJSONArray("articles");
                        if (articles == null)
                            continue;
                        String srcId = "";
                        for (NewsSource s : sources) {
                            String sCat = s.getCategory() == null ? "general" : s.getCategory();
                            if (category.equalsIgnoreCase(sCat)) {
                                srcId = s.getSourceID();
                                break;
                            }
                        }
                        for (int i = 0; i < articles.length(); i++) {
                            JSONObject art = articles.getJSONObject(i);
                            DigestItem item = new DigestItem();
                            item.setSourceID(srcId);
                            item.setHeadline(art.optString("title", "No title"));
                            item.setSummary(art.optString("description", "No description available."));
                            item.setSourceURL(art.optString("url", ""));
                            item.setThumbnailURL(art.optString("image", null));
                            item.setPublishedAt(parseDate(art.optString("publishedAt", null)));
                            item.setItemRank(i + 1);
                            item.setExported(false);
                            pool.add(item);
                        }
                        System.out.println("[GNews] Fallback fetched for category: " + category);
                    } catch (Exception e) {
                        System.err.println("[GNews] " + category + ": " + e.getMessage());
                    }
                }
            }

            if (pool.isEmpty()) {
                System.err.println("[DigestGenerator] No live articles – using mock fallback.");
                return buildFallbackPool(sources);
            }
            return pool;
        }

        private String buildGNewsUrl(String category) {
            String gnewsCat, extraQuery = "";
            switch (category) {
                case "stocks":
                    gnewsCat = "business";
                    extraQuery = "&q=stock+market";
                    break;
                case "sports":
                    gnewsCat = "sports";
                    break;
                case "technology":
                    gnewsCat = "technology";
                    break;
                case "crypto":
                    gnewsCat = "business";
                    extraQuery = "&q=cryptocurrency+bitcoin";
                    break;
                case "politics":
                    gnewsCat = "world";
                    extraQuery = "&q=politics+government";
                    break;
                case "business":
                    gnewsCat = "business";
                    break;
                case "science":
                    gnewsCat = "science";
                    break;
                default:
                    gnewsCat = "general";
                    break;
            }
            return BASE_URL + "?category=" + gnewsCat + "&lang=en&max=10&token=" + API_TOKEN + extraQuery;
        }

        private LocalDateTime parseDate(String iso) {
            if (iso == null || iso.isBlank())
                return LocalDateTime.now();
            try {
                return OffsetDateTime.parse(iso).toLocalDateTime();
            } catch (Exception e1) {
            }
            try {
                return LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
            }
            return LocalDateTime.now();
        }

        private List<DigestItem> buildFallbackPool(List<NewsSource> sources) {
            List<DigestItem> pool = new ArrayList<>();
            Random rand = new Random();
            for (NewsSource src : sources) {
                int count = 2 + rand.nextInt(3);
                for (int i = 0; i < count; i++) {
                    DigestItem item = new DigestItem();
                    item.setSourceID(src.getSourceID());
                    item.setHeadline(FALLBACK_HEADLINES[rand.nextInt(FALLBACK_HEADLINES.length)]);
                    item.setSummary(FALLBACK_SUMMARIES[rand.nextInt(FALLBACK_SUMMARIES.length)]);
                    item.setSourceURL(src.getApiEndpoint() != null ? src.getApiEndpoint() : "");
                    item.setPublishedAt(LocalDateTime.now().minusHours(rand.nextInt(48)));
                    item.setItemRank(i + 1);
                    item.setExported(false);
                    pool.add(item);
                }
            }
            return pool;
        }

        @Override
        public void onFeedbackSubmitted(String userID, String itemID, String rating) {
            System.out.println("[Observer] Feedback received – future digests will adjust weights for user " + userID);
        }
    }
}
