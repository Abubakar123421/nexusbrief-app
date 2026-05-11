package com.nexusbrief.ui;

import com.nexusbrief.model.AppData.*;
import com.nexusbrief.service.Services.*;
import com.nexusbrief.service.GoogleServices;
import com.nexusbrief.service.GoogleServices.*;
import com.nexusbrief.dao.DAOs.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// ── DashboardScreen ───────────────────────────────────────────────────────────

class DashboardScreen {

    private final DigestService           digestService   = new DigestService();
    private final DigestGeneratorService  generator       = new DigestGeneratorService();
    private final FeedbackService         feedbackService = new FeedbackService();
    private final CalendarExportService   calService      = new CalendarExportService();
    private final GoogleCalendarService   gcalService     = new GoogleCalendarService();
    private final GoogleClassroomService  gclassService   = new GoogleClassroomService();
    private final ScheduleService         scheduleService = new ScheduleService();

    DashboardScreen() { feedbackService.registerObserver(generator); }

    // Biulds the main user dashbord
    Region build() {
        User user = Session.get().getUser();
        BorderPane root = new BorderPane();
        root.setTop(AppUI.topBar("Dashboard"));
        root.setLeft(AppUI.sidebar("Dashboard"));
        root.setCenter(buildCenter(user));
        root.setStyle("-fx-background-color:" + AppUI.BG + ";");
        return root;
    }

    private Region buildCenter(User user) {
        VBox content = new VBox(18);
        content.setPadding(new Insets(28));
        Label welcome  = AppUI.titleLabel("Welcome, " + user.getName());
        Label tagline  = AppUI.subtitleLabel("Your Top 5 news digest");
        Button genBtn  = AppUI.primaryBtn("⚡ Generate New Digest");
        Button refreshBtn = AppUI.secondaryBtn("↻ Refresh");
        HBox actions   = new HBox(10, genBtn, refreshBtn);
        VBox digestBox = new VBox(12);

        if (scheduleService.checkAndTrigger(user.getUserID())) {
            generator.generate(user.getUserID());
            Label autoLbl = new Label("📬 Your scheduled digest was automatically generated.");
            autoLbl.setStyle("-fx-text-fill:#059669;-fx-font-size:12px;-fx-font-style:italic;");
            digestBox.getChildren().add(autoLbl);
        }

        VBox classroomBox = buildClassroomPreview(user.getUserID());

        loadDigest(digestBox, user.getUserID());
        genBtn.setOnAction(e -> {
            generator.generate(user.getUserID());
            loadDigest(digestBox, user.getUserID());
            AppUI.info("New digest generated successfully!");
        });
        refreshBtn.setOnAction(e -> loadDigest(digestBox, user.getUserID()));
        content.getChildren().addAll(welcome, tagline, classroomBox, actions, digestBox);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return scroll;
    }

    private VBox buildClassroomPreview(String userID) {
        VBox section = new VBox(10);
        if (!gclassService.isConnected(userID)) return section;

        Label hdr = AppUI.h2("📚 Upcoming Assignments");
        hdr.setStyle(hdr.getStyle() + "-fx-font-size:14px;");
        Label loading = new Label("Loading assignments…");
        loading.setStyle("-fx-text-fill:#64748B;-fx-font-size:12px;-fx-font-style:italic;");
        section.getChildren().addAll(hdr, loading);

        new Thread(() -> {
            List<GoogleServices.ClassroomAssignment> all = gclassService.getUpcoming(userID, 20);
            Platform.runLater(() -> {
                section.getChildren().remove(loading);
                if (all.isEmpty() || (all.size() == 1 && all.get(0).title.isEmpty())) return;
                List<GoogleServices.ClassroomAssignment> top3 = all.stream()
                    .filter(a -> a.dueDate != null && !a.dueDate.isBefore(LocalDateTime.now()))
                    .sorted(Comparator.comparing(a -> a.dueDate))
                    .limit(3)
                    .toList();
                if (top3.isEmpty()) return;
                HBox row = new HBox(12);
                row.setAlignment(Pos.TOP_LEFT);
                for (GoogleServices.ClassroomAssignment a : top3)
                    row.getChildren().add(buildAssignmentChip(a));
                section.getChildren().add(row);
            });
        }).start();

        return section;
    }

    private VBox buildAssignmentChip(GoogleServices.ClassroomAssignment a) {
        VBox chip = AppUI.card(14);
        chip.setPrefWidth(220); chip.setMaxWidth(220);
        Label course = new Label(a.courseName.toUpperCase());
        course.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:#64748B;");
        Label title = new Label(a.title);
        title.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#0F172A;");
        title.setWrapText(true);
        boolean urgent = a.dueDate.isBefore(LocalDateTime.now().plusDays(2));
        String dueStr = "Due " + a.dueDate.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        Label due = new Label((urgent ? "🔴 " : "📅 ") + dueStr);
        due.setStyle("-fx-font-size:10px;-fx-text-fill:" + (urgent ? "#EA580C" : "#94A3B8") + ";");
        chip.getChildren().addAll(course, title, due);
        if (a.link != null && !a.link.isBlank()) {
            chip.setOnMouseClicked(e -> {
                try { Desktop.getDesktop().browse(new URI(a.link)); } catch (Exception ignored) {}
            });
            chip.setStyle(chip.getStyle() + "-fx-cursor:hand;");
        }
        return chip;
    }

    private void loadDigest(VBox container, String userID) {
        container.getChildren().clear();
        Digest digest = digestService.getLatest(userID);
        if (digest == null || digest.getItems().isEmpty()) {
            Label empty = new Label("No digest yet. Click \"Generate New Digest\" to start.");
            empty.setStyle("-fx-text-fill:#64748B;-fx-font-style:italic;-fx-padding:20;");
            container.getChildren().add(empty);
            return;
        }
        Label info = new Label("Generated: " +
                digest.getGeneratedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm"))
                + "   |   Status: " + digest.getStatus().name()
                + (digest.isViewed() ? "   |   VIEWED" : "   |   NEW"));
        info.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        container.getChildren().add(info);
        int rank = 1;
        for (DigestItem item : digest.getItems())
            container.getChildren().add(buildItemCard(rank++, item, userID));
    }

    private VBox buildItemCard(int rank, DigestItem item, String userID) {
        VBox card = AppUI.card(16);
        Label badge = new Label("#" + rank);
        badge.setStyle("-fx-background-color:" + AppUI.BLUE + ";-fx-text-fill:white;" +
                       "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:2 8;-fx-background-radius:10;");
        Label headline = new Label(item.getHeadline());
        headline.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0F172A;");
        headline.setWrapText(true);
        HBox header = new HBox(10, badge, headline);
        header.setAlignment(Pos.CENTER_LEFT);
        Label summary = new Label(item.getSummary());
        summary.setStyle("-fx-font-size:13px;-fx-text-fill:#475569;");
        summary.setWrapText(true);
        HBox.setHgrow(summary, Priority.ALWAYS);
        String pubDate = item.getPublishedAt() == null ? "N/A" :
                item.getPublishedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        Label meta = new Label("Published: " + pubDate + (item.isExported() ? "   ✅ Exported to Calendar" : ""));
        meta.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;");

        Button likeBtn     = smallBtn("👍 Like");
        Button dislikeBtn  = smallBtn("👎 Dislike");
        Button scheduleBtn = smallBtn("📅 Schedule Reading Time");
        Button readBtn     = smallBtn("🔗 Read Full Article");
        readBtn.setStyle(readBtn.getStyle() + "-fx-text-fill:#2563EB;-fx-font-weight:bold;");

        likeBtn.setOnAction(e -> {
            String err = feedbackService.submit(item.getItemID(), userID, FeedbackType.THUMBS_UP, "");
            if (err == null) AppUI.info("Thanks for the feedback! Future digests will improve.");
            else AppUI.error(err);
        });
        dislikeBtn.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Feedback"); d.setHeaderText("Why didn't you like this?"); d.setContentText("Reason (optional):");
            Optional<String> reason = d.showAndWait();
            String err = feedbackService.submit(item.getItemID(), userID, FeedbackType.THUMBS_DOWN, reason.orElse(""));
            if (err == null) AppUI.info("Feedback saved. We'll adjust future digests.");
            else AppUI.error(err);
        });
        scheduleBtn.setOnAction(e -> showScheduleDialog(item, userID));
        readBtn.setOnAction(e -> {
            String url = item.getSourceURL();
            if (url == null || url.isBlank()) { AppUI.error("No article URL available."); return; }
            try { Desktop.getDesktop().browse(new URI(url)); } catch (Exception ex) { AppUI.error("Could not open link:\n" + url); }
        });

        HBox btns = new HBox(8, likeBtn, dislikeBtn, scheduleBtn, readBtn);
        Region summaryRow = buildThumbRow(item.getThumbnailURL(), summary);
        card.getChildren().addAll(header, summaryRow, meta, btns);
        return card;
    }

    private Region buildThumbRow(String url, Label summary) {
        if (url == null || url.isBlank()) return summary;
        try {
            Image img = new Image(url, 120, 80, false, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(120); iv.setFitHeight(80); iv.setPreserveRatio(false);
            Rectangle clip = new Rectangle(120, 80);
            clip.setArcWidth(10); clip.setArcHeight(10);
            iv.setClip(clip);
            HBox row = new HBox(14, iv, summary);
            row.setAlignment(Pos.TOP_LEFT);
            return row;
        } catch (Exception e) { return summary; }
    }

    private void showScheduleDialog(DigestItem item, String userID) {
        Dialog<LocalDateTime> dialog = new Dialog<>();
        dialog.setTitle("Schedule Reading Time");
        dialog.setHeaderText("When do you want to read this?\n\n" + item.getHeadline());
        DatePicker datePicker = new DatePicker(LocalDate.now());
        int defaultHour = (LocalDateTime.now().getHour() + 1) % 24;
        Spinner<Integer> hourSpin = new Spinner<>(0, 23, defaultHour);
        Spinner<Integer> minSpin  = new Spinner<>(0, 59, 0);
        hourSpin.setPrefWidth(72); minSpin.setPrefWidth(72);
        boolean googleConnected = gcalService.isConnected(userID);
        Label hint = new Label(googleConnected
            ? "✅ Will export to your Google Calendar"
            : "ℹ️ Connect Google in Settings → Email to export directly. Will save .ics as fallback.");
        hint.setStyle("-fx-font-size:11px;-fx-text-fill:" + (googleConnected ? "#16A34A" : "#64748B") + ";");
        hint.setWrapText(true);
        VBox content = new VBox(10,
            new Label("Date:"), datePicker,
            new HBox(8, new Label("Time (24h):"), hourSpin, new Label(":"), minSpin), hint);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK && datePicker.getValue() != null)
                return LocalDateTime.of(datePicker.getValue(), LocalTime.of(hourSpin.getValue(), minSpin.getValue()));
            return null;
        });
        dialog.showAndWait().ifPresent(readingTime -> {
            if (googleConnected) {
                String link = gcalService.scheduleReadingTime(userID, item, readingTime);
                if (link != null) {
                    AppUI.info("Added to your Google Calendar!\n\nClick OK to open it.");
                    try { Desktop.getDesktop().browse(new URI(link)); } catch (Exception ignored) {}
                    return;
                }
                AppUI.error("Google Calendar export failed. Saving .ics as fallback.");
            }
            String path = calService.export(item.getItemID(), readingTime);
            if (path != null) AppUI.info("Reading time saved!\n\nFile: " + path + "\n\nImport it into Google/Apple/Outlook Calendar.");
            else AppUI.error("Could not save. This item may already be scheduled.");
        });
    }

    private Button smallBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:white;-fx-text-fill:#334155;-fx-font-size:12px;" +
                   "-fx-padding:6 12;-fx-background-radius:4;-fx-border-color:" + AppUI.BORDER +
                   ";-fx-border-radius:4;-fx-cursor:hand;");
        return b;
    }
}

// ── HistoryScreen ─────────────────────────────────────────────────────────────

class HistoryScreen {

    private final DigestService digestService = new DigestService();
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

    Region build() {
        User user = Session.get().getUser();
        BorderPane root = new BorderPane();
        root.setTop(AppUI.topBar("Digest History"));
        root.setStyle("-fx-background-color:" + AppUI.BG + ";");

        VBox content = new VBox(16);
        content.setPadding(new Insets(28));
        content.setMaxWidth(820);

        Label title    = AppUI.titleLabel("Digest History");
        Label subtitle = AppUI.subtitleLabel("All digests generated for your account, most recent first.");
        TextField searchFld = AppUI.field("Search by date (e.g. Apr 22) or status (e.g. GENERATED)…");
        List<Digest> history = digestService.getHistory(user.getUserID());
        VBox list = new VBox(14);

        if (history.isEmpty()) {
            Label empty = new Label("No digests yet. Generate one from the dashboard.");
            empty.setStyle("-fx-text-fill:#64748B;-fx-font-style:italic;-fx-padding:20;");
            list.getChildren().add(empty);
        } else {
            List<VBox> cards = new ArrayList<>();
            for (Digest d : history) {
                VBox card = buildDigestCard(d);
                card.setUserData(d);
                cards.add(card);
                list.getChildren().add(card);
            }
            searchFld.textProperty().addListener((obs, oldValue, newValue) -> {
                String query = newValue.trim().toLowerCase();
                for (VBox card : cards) {
                    Digest d = (Digest) card.getUserData();
                    boolean matches = query.isEmpty()
                            || d.getGeneratedAt().format(fmt).toLowerCase().contains(query)
                            || d.getStatus().name().toLowerCase().contains(query);
                    card.setVisible(matches); card.setManaged(matches);
                }
            });
        }

        content.getChildren().addAll(title, subtitle, searchFld, list);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        root.setLeft(AppUI.sidebar("Digest History"));
        root.setCenter(scroll);
        return root;
    }

    private VBox buildDigestCard(Digest digest) {
        VBox card = AppUI.card(18);
        Label date = new Label(digest.getGeneratedAt().format(fmt));
        date.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0F172A;");
        Label statusBadge = badge(digest.getStatus().name(), AppUI.BLUE);
        Label viewedBadge = digest.isViewed() ? badge("VIEWED", "#64748B") : badge("NEW", AppUI.GREEN);
        Label count = new Label(digest.getItems().size() + " items");
        count.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        HBox header = new HBox(10, date, statusBadge, viewedBadge, AppUI.spacer(), count);
        header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);
        int rank = 1;
        for (DigestItem item : digest.getItems()) {
            VBox row = new VBox(4);
            row.setPadding(new Insets(10, 0, 10, 0));
            row.setStyle("-fx-border-color:" + AppUI.BORDER + ";-fx-border-width:1 0 0 0;");
            Label headline = new Label("#" + rank + "  " + item.getHeadline());
            headline.setStyle("-fx-font-size:14px;-fx-font-weight:500;-fx-text-fill:#1E293B;");
            headline.setWrapText(true);
            Label summary = new Label(item.getSummary());
            summary.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
            summary.setWrapText(true);
            row.getChildren().addAll(headline, summary);
            card.getChildren().add(row);
            rank++;
        }
        return card;
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px;-fx-padding:2 10;-fx-background-radius:10;" +
                   "-fx-text-fill:white;-fx-background-color:" + color + ";");
        return l;
    }
}

// ── ProfileScreen ─────────────────────────────────────────────────────────────

class ProfileScreen {

    private final UserDAO userDAO = new UserDAO();

    Region build() {
        User user = Session.get().getUser();
        BorderPane root = new BorderPane();
        root.setTop(AppUI.topBar("My Profile"));
        root.setStyle("-fx-background-color:#F5F0E8;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(28));
        content.setMaxWidth(540);

        Label title    = AppUI.titleLabel("My Profile");
        Label subtitle = AppUI.subtitleLabel("Update your name or email address.");
        VBox card = AppUI.card(24);
        card.getChildren().add(AppUI.h2("Account Information"));

        String joined = "";
        if (user.getCreatedAt() != null)
            joined = "Member since: " + user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        Label joinedLbl = new Label(joined);
        joinedLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;");

        TextField nameFld  = AppUI.field("Your name");  nameFld.setText(user.getName());
        TextField emailFld = AppUI.field("you@example.com"); emailFld.setText(user.getEmail());
        Label errLbl = AppUI.errorLbl();
        Label okLbl  = AppUI.successLbl();

        Button saveBtn = AppUI.primaryBtn("Save Changes");
        saveBtn.setGraphic(Icons.profile(16));
        saveBtn.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        saveBtn.setGraphicTextGap(8);
        saveBtn.setMaxWidth(Double.MAX_VALUE);

        saveBtn.setOnAction(e -> {
            errLbl.setVisible(false); okLbl.setVisible(false);
            String newName  = nameFld.getText().trim();
            String newEmail = emailFld.getText().trim().toLowerCase();
            if (newName.isEmpty()) { errLbl.setText("Name cannot be empty."); errLbl.setVisible(true); return; }
            if (!newEmail.matches(".+@.+\\..+")) { errLbl.setText("Invalid email format."); errLbl.setVisible(true); return; }
            User other = userDAO.findByEmail(newEmail);
            if (other != null && !other.getUserID().equals(user.getUserID())) {
                errLbl.setText("That email is already used by another account."); errLbl.setVisible(true); return;
            }
            user.setName(newName); user.setEmail(newEmail);
            if (userDAO.update(user)) { okLbl.setText("✓ Profile updated successfully!"); okLbl.setVisible(true); }
            else { errLbl.setText("Could not save changes. Please try again."); errLbl.setVisible(true); }
        });

        Label nameLbl  = fieldLabel("Full Name");
        Label emailLbl = fieldLabel("Email Address");
        card.getChildren().addAll(joinedLbl, nameLbl, nameFld, emailLbl, emailFld, errLbl, okLbl, saveBtn);
        content.getChildren().addAll(title, subtitle, card);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        root.setLeft(AppUI.sidebar("My Profile"));
        root.setCenter(scroll);
        return root;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:#5F5E5A;-fx-font-family:'Segoe UI',system;");
        VBox.setMargin(l, new Insets(8, 0, 0, 0));
        return l;
    }
}
