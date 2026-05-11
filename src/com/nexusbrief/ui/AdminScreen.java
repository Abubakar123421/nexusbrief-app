package com.nexusbrief.ui;

import com.nexusbrief.model.AppData.*;
import com.nexusbrief.service.Services.*;
import com.nexusbrief.service.GoogleServices.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// ── AdminScreen ───────────────────────────────────────────────────────────────

class AdminScreen {

    private final AdminService           adminService = new AdminService();
    private final DigestGeneratorService generator    = new DigestGeneratorService();
    private final PreferenceService      prefService  = new PreferenceService();

    // Builds the admin dashbord ui
    Region build() {
        BorderPane root = new BorderPane();
        root.setTop(buildAdminTopBar());
        root.setStyle("-fx-background-color:#F5F0E8;");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color:#F5F0E8;");
        tabs.getTabs().addAll(
            tab("Users",          Icons.profile(16),  buildUsers()),
            tab("All Digests",    Icons.history(16),  buildAllDigests()),
            tab("API Configs",    Icons.settings(16), buildApiConfigs()),
            tab("Digest Control", Icons.generate(16), buildDigestControl())
        );
        root.setCenter(tabs);
        return root;
    }

    private HBox buildAdminTopBar() {
        Label brand = new Label("NexusBrief");
        brand.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;" +
                       "-fx-font-family:'Georgia','Times New Roman',serif;");
        Label sep = new Label("/");
        sep.setStyle("-fx-font-size:13px;-fx-text-fill:#5F5E5A;");
        Label title = new Label("ADMIN PANEL");
        title.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;-fx-font-family:'Segoe UI',system;");
        Label badge = new Label("ADMIN");
        badge.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#F5F0E8;" +
                       "-fx-background-color:#0A0A0A;-fx-padding:3 9;-fx-background-radius:0;");
        Button logout = new Button("LOGOUT");
        logout.setGraphic(Icons.logout(15));
        logout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        logout.setGraphicTextGap(6);
        String logBase  = "-fx-background-color:transparent;-fx-text-fill:#0A0A0A;-fx-padding:6 14;-fx-background-radius:0;" +
                          "-fx-cursor:hand;-fx-border-color:#1A1A1A;-fx-border-width:1;-fx-font-size:11px;-fx-font-weight:600;";
        String logHover = "-fx-background-color:#0A0A0A;-fx-text-fill:#F5F0E8;-fx-padding:6 14;-fx-background-radius:0;" +
                          "-fx-cursor:hand;-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-font-size:11px;-fx-font-weight:600;";
        logout.setStyle(logBase);
        logout.setOnMouseEntered(e -> logout.setStyle(logHover));
        logout.setOnMouseExited(e  -> logout.setStyle(logBase));
        logout.setOnAction(e -> { Session.get().logout(); AppUI.goLogin(); });
        HBox bar = new HBox(10, brand, sep, title, badge, AppUI.spacer(), logout);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color:#FDFAF4;-fx-border-color:#1A1A1A;-fx-border-width:0 0 1 0;");
        return bar;
    }

    private Tab tab(String name, Node icon, Region content) {
        Tab t = new Tab();
        Label lbl = new Label("  " + name);
        lbl.setGraphic(icon);
        lbl.setContentDisplay(ContentDisplay.LEFT);
        t.setGraphic(lbl);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        t.setContent(scroll);
        return t;
    }

    private Region buildUsers() {
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("User Management"),
            AppUI.subtitleLabel("View all registered users. Generate digests or remove accounts."));
        List<User> users = adminService.getNonAdminUsers();
        TextField searchFld = AppUI.field("🔍  Search by name or email…");
        pane.getChildren().add(searchFld);
        Label statLbl = new Label("Total users: " + users.size());
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;");
        pane.getChildren().add(statLbl);
        VBox userList = new VBox(10);
        for (User u : users) {
            VBox card = AppUI.card(16);
            card.setUserData(u);
            Label nameLbl  = new Label(u.getName());
            nameLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;");
            Label emailLbl = new Label(u.getEmail());
            emailLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;");
            String since = u.getCreatedAt() == null ? "" : "Joined " + u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            Label metaLbl  = new Label(since);
            metaLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;");
            VBox info = new VBox(3, nameLbl, emailLbl, metaLbl);
            info.setAlignment(Pos.CENTER_LEFT);

            Button genBtn = AppUI.secondaryBtn("⚡ Generate Digest");
            genBtn.setOnAction(e -> { generator.generate(u.getUserID()); AppUI.info("Digest generated for " + u.getName() + "."); });

            Button editPrefBtn = AppUI.secondaryBtn("⚙️ Edit Prefs");
            editPrefBtn.setOnAction(e -> showPreferenceDialog(u));

            Button resetBtn = AppUI.secondaryBtn("🔑 Reset Password");
            resetBtn.setOnAction(e -> {
                TextInputDialog d = new TextInputDialog();
                d.setTitle("Reset Password"); d.setHeaderText("Set new password for: " + u.getName()); d.setContentText("New password:");
                AppUI.styleDialogPane(d.getDialogPane());
                d.showAndWait().ifPresent(newPass -> {
                    if (!newPass.isBlank()) { adminService.resetPassword(u.getUserID(), newPass.trim()); AppUI.info("Password updated for " + u.getName() + ".\nNew password: " + newPass.trim()); }
                });
            });

            Button deleteBtn = AppUI.dangerBtn("🗑  Remove");
            deleteBtn.setOnAction(e -> {
                if (AppUI.confirm("Permanently delete account for " + u.getName() + "?\nAll their digests and data will be removed.")) {
                    adminService.deleteUser(u.getUserID()); AppUI.goAdmin();
                }
            });

            HBox actions = new HBox(8, genBtn, editPrefBtn, resetBtn, deleteBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);
            HBox row = new HBox(16, info, AppUI.spacer(), actions);
            row.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(row);
            userList.getChildren().add(card);
        }
        if (userList.getChildren().isEmpty()) {
            Label empty = new Label("No registered users yet.");
            empty.setStyle("-fx-text-fill:#5F5E5A;-fx-font-style:italic;-fx-padding:20;");
            userList.getChildren().add(empty);
        }
        searchFld.textProperty().addListener((obs, old, q) -> {
            String query = q.trim().toLowerCase();
            for (Node node : userList.getChildren()) {
                if (node.getUserData() instanceof User) {
                    User u = (User) node.getUserData();
                    boolean match = query.isEmpty() || u.getName().toLowerCase().contains(query) || u.getEmail().toLowerCase().contains(query);
                    node.setVisible(match); node.setManaged(match);
                }
            }
        });
        pane.getChildren().add(userList);
        return pane;
    }

    private Region buildAllDigests() {
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("All Digests"),
            AppUI.subtitleLabel("Every digest generated across all users, most recent first."));
        List<Digest> all = adminService.getAllDigestsWithItems();
        if (all.isEmpty()) {
            Label empty = new Label("No digests generated yet.");
            empty.setStyle("-fx-text-fill:#5F5E5A;-fx-font-style:italic;-fx-padding:20;");
            pane.getChildren().add(empty);
            return pane;
        }
        Label count = new Label("Total: " + all.size() + " digests");
        count.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;");
        pane.getChildren().add(count);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");
        for (Digest d : all) {
            List<DigestItem> items = d.getItems();
            VBox card = AppUI.card(14);
            Label userLbl = new Label("👤  " + (d.getUserName() != null ? d.getUserName() : "Unknown"));
            userLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#5F5E5A;");
            Label dateLbl = new Label(d.getGeneratedAt().format(fmt));
            dateLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;");
            Label statusBadge = badge(d.getStatus().name(), "#0A0A0A");
            Label viewedBadge = d.isViewed() ? badge("VIEWED", "#5F5E5A") : badge("NEW", "#2A6B3A");
            Label itemCnt = new Label(items.size() + " items");
            itemCnt.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;");
            HBox header = new HBox(10, userLbl, dateLbl, statusBadge, viewedBadge, AppUI.spacer(), itemCnt);
            header.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(header);
            int shown = 0;
            for (DigestItem item : items) {
                if (shown++ >= 3) {
                    Label more = new Label("  … and " + (items.size() - 3) + " more");
                    more.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;-fx-padding:2 0 0 0;");
                    card.getChildren().add(more); break;
                }
                Label h = new Label("  #" + item.getItemRank() + "  " + item.getHeadline());
                h.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;-fx-padding:3 0 0 0;");
                h.setWrapText(true);
                card.getChildren().add(h);
            }
            pane.getChildren().add(card);
        }
        return pane;
    }

    private Region buildApiConfigs() {
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("API Configurations"),
            AppUI.subtitleLabel("Manage API keys for each news source integration."));
        List<Object[]> configs = adminService.getApiConfigs();
        if (configs.isEmpty()) {
            Label empty = new Label("No API configurations in database.");
            empty.setStyle("-fx-text-fill:#5F5E5A;-fx-font-style:italic;");
            pane.getChildren().add(empty);
            return pane;
        }
        HBox header = new HBox(16, bold("Source", 160), bold("Provider", 160), bold("API Key", 160), bold("Status", 90));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 16));
        header.setStyle("-fx-background-color:#0A0A0A;-fx-background-radius:0;");
        pane.getChildren().add(header);
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color:#FDFAF4;-fx-border-color:#1A1A1A;-fx-border-radius:0;-fx-border-width:1;");
        for (int i = 0; i < configs.size(); i++) {
            Object[] cfg  = configs.get(i);
            String configID = (String) cfg[0], provider = (String) cfg[1], apiKey = (String) cfg[2];
            boolean valid = (Boolean) cfg[3];
            String srcName = cfg[4] != null ? (String) cfg[4] : "Unknown";
            String masked = apiKey != null && apiKey.length() > 8
                ? apiKey.substring(0, 4) + "••••" + apiKey.substring(apiKey.length() - 4) : "••••••••";
            Label srcLbl = cell(srcName, 160), provLbl = cell(provider, 160), keyLbl = cell(masked, 160);
            keyLbl.setStyle(keyLbl.getStyle() + "-fx-font-family:monospace;");
            Label valLbl = new Label(valid ? "✅ Valid" : "❌ Invalid");
            valLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + (valid ? "#2A6B3A" : "#A32D2D") + ";");
            valLbl.setPrefWidth(90);
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#0A0A0A;-fx-border-color:#1A1A1A;-fx-border-radius:0;-fx-padding:4 12;-fx-font-size:11px;-fx-cursor:hand;-fx-font-weight:600;");
            editBtn.setOnAction(e -> {
                TextInputDialog d = new TextInputDialog(apiKey);
                d.setTitle("Edit API Key"); d.setHeaderText("Update key for: " + srcName); d.setContentText("New API key:");
                AppUI.styleDialogPane(d.getDialogPane());
                d.showAndWait().ifPresent(newKey -> {
                    if (!newKey.isBlank()) { adminService.updateApiKey(configID, newKey.trim()); AppUI.info("Key updated for " + srcName + "."); AppUI.goAdmin(); }
                });
            });
            HBox row = new HBox(16, srcLbl, provLbl, keyLbl, valLbl, AppUI.spacer(), editBtn);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12, 20, 12, 16));
            boolean last = (i == configs.size() - 1);
            String div = last ? "" : "-fx-border-color:" + AppUI.BORDER + ";-fx-border-width:0 0 1 0;";
            row.setStyle("-fx-background-color:#FDFAF4;" + div);
            row.setOnMouseEntered(ev -> row.setStyle("-fx-background-color:#EDE8DC;" + div));
            row.setOnMouseExited(ev  -> row.setStyle("-fx-background-color:#FDFAF4;" + div));
            table.getChildren().add(row);
        }
        pane.getChildren().add(table);
        return pane;
    }

    private Region buildDigestControl() {
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("Digest Control"),
            AppUI.subtitleLabel("Manually generate digests for any user and view their delivery schedules."));
        List<User> normalUsers = adminService.getNonAdminUsers();
        VBox genCard = AppUI.card(20);
        genCard.getChildren().add(AppUI.h2("Generate Digest for User"));
        if (normalUsers.isEmpty()) {
            genCard.getChildren().add(new Label("No regular users registered yet."));
        } else {
            ComboBox<User> userCombo = new ComboBox<>(FXCollections.observableArrayList(normalUsers));
            userCombo.setValue(normalUsers.get(0)); userCombo.setPrefWidth(240);
            Button genBtn = AppUI.primaryBtn("⚡ Generate Now");
            Label genStatus = AppUI.successLbl();
            genBtn.setOnAction(e -> {
                User target = userCombo.getValue();
                if (target != null) { generator.generate(target.getUserID()); genStatus.setText("✓ Digest generated for " + target.getName()); genStatus.setVisible(true); }
            });
            HBox genRow = new HBox(10, new Label("Select user:"), userCombo, genBtn);
            genRow.setAlignment(Pos.CENTER_LEFT);
            genCard.getChildren().addAll(genRow, genStatus);
        }
        VBox schedCard = AppUI.card(20);
        schedCard.getChildren().add(AppUI.h2("Delivery Schedules"));
        if (normalUsers.isEmpty()) {
            schedCard.getChildren().add(new Label("No users."));
        } else {
            VBox schedList = new VBox(8);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
            for (User u : normalUsers) {
                DigestSchedule s = adminService.getScheduleForUser(u.getUserID());
                Label nameLbl = new Label(u.getName());
                nameLbl.setStyle("-fx-font-weight:bold;-fx-font-size:13px;"); nameLbl.setPrefWidth(160);
                String info = s == null ? "No schedule set" :
                    s.getDeliveryTime() + "  |  Every " + s.getIntervalHours() + "h" +
                    (s.isActive() ? "" : "  [PAUSED]") +
                    (s.getNextTriggerAt() == null ? "" : "  |  Next: " + s.getNextTriggerAt().format(fmt));
                Label infoLbl = new Label(info);
                infoLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + (s != null && s.isActive() ? "#2A6B3A" : "#5F5E5A") + ";");
                HBox row = new HBox(14, nameLbl, infoLbl);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setStyle("-fx-background-color:#F5F0E8;-fx-background-radius:0;-fx-border-color:#1A1A1A;-fx-border-width:1;");
                schedList.getChildren().add(row);
            }
            schedCard.getChildren().add(schedList);
        }
        pane.getChildren().addAll(genCard, schedCard);
        return pane;
    }

    private void showPreferenceDialog(User u) {
        DigestPreference pref = prefService.getPreferences(u.getUserID());
        PrioritizationMode current = pref != null ? pref.getPrioritizationMode() : PrioritizationMode.LATEST_FIRST;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Preferences – " + u.getName());
        dialog.setHeaderText("Adjust digest settings for: " + u.getName());
        VBox content = new VBox(12);
        content.setPadding(new Insets(16)); content.setMinWidth(300);
        content.setStyle("-fx-background-color:#FDFAF4;");
        Label modeTitle = new Label("Prioritization Mode:");
        modeTitle.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#0A0A0A;-fx-font-family:'Georgia','Times New Roman',serif;");
        ToggleGroup modeGroup = new ToggleGroup();
        VBox modeOptions = new VBox(6);
        for (PrioritizationMode mode : PrioritizationMode.values()) {
            RadioButton rb = new RadioButton(mode.getDisplay());
            rb.setToggleGroup(modeGroup); rb.setUserData(mode); rb.setSelected(mode == current);
            modeOptions.getChildren().add(rb);
        }
        content.getChildren().addAll(modeTitle, modeOptions);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        AppUI.styleDialogPane(dialog.getDialogPane());
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK && modeGroup.getSelectedToggle() != null) {
                PrioritizationMode chosen = (PrioritizationMode) modeGroup.getSelectedToggle().getUserData();
                prefService.updateMode(u.getUserID(), chosen);
                AppUI.info("Updated " + u.getName() + "'s mode to: " + chosen.getDisplay());
            }
        });
    }

    private Label badge(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px;-fx-padding:2 8;-fx-background-radius:0;" +
                   "-fx-text-fill:#F5F0E8;-fx-background-color:" + color + ";" +
                   "-fx-font-family:'Segoe UI',system;-fx-font-weight:600;");
        return l;
    }

    private Label bold(String text, double width) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:#F5F0E8;-fx-font-family:'Segoe UI',system;");
        l.setPrefWidth(width); return l;
    }

    private Label cell(String text, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px;-fx-text-fill:#0A0A0A;");
        l.setPrefWidth(width); return l;
    }

    private VBox pane() {
        VBox v = new VBox(16);
        v.setPadding(new Insets(28)); v.setMaxWidth(900);
        return v;
    }
}

// ── SettingsScreen ────────────────────────────────────────────────────────────

class SettingsScreen {

    private final PreferenceService       prefService    = new PreferenceService();
    private final NewsSourceService       sourceService  = new NewsSourceService();
    private final EmailService            emailService   = new EmailService();
    private final ScheduleService         schedService   = new ScheduleService();
    private final GoogleCalendarService   gcalService    = new GoogleCalendarService();
    private final GoogleClassroomService  gclassService  = new GoogleClassroomService();

    private static final String[] CATEGORIES = { "Stocks", "Sports", "Technology", "Crypto", "General", "Politics", "Business", "Science" };
    private static final String[] CAT_EMOJI  = { "📈", "⚽", "💻", "🪙", "📰", "🏛️", "💼", "🔬" };
    private static final String[] CAT_COLORS = { "#16A34A", "#EA580C", "#2563EB", "#7C3AED", "#64748B", "#DC2626", "#0891B2", "#4F46E5" };
    private static final String[] CAT_DESC   = {
        "Stock market & investments", "Sports news & match scores",
        "Tech industry & gadgets",    "Cryptocurrency & blockchain",
        "Top general news stories",   "Politics & government",
        "Business & economy",         "Science & research"
    };

    Region build() {
        BorderPane root = new BorderPane();
        root.setTop(AppUI.topBar("Settings"));
        root.setStyle("-fx-background-color:#F5F0E8;");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setStyle("-fx-background-color:#F5F0E8;");
        tabs.getTabs().addAll(
            tab("Preferences",    buildPreferences()),
            tab("Prioritization", buildPrioritization()),
            tab("Sources",        buildSources()),
            tab("Email",          buildEmail()),
            tab("Connections",    buildConnections()),
            tab("Schedule",       buildSchedule()),
            tab("Classroom",      buildClassroom())
        );
        root.setLeft(AppUI.sidebar("Settings"));
        root.setCenter(tabs);
        return root;
    }

    private Tab tab(String name, Region content) {
        Tab t = new Tab(name);
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        t.setContent(scroll);
        return t;
    }

    private Region buildPreferences() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("Digest Preferences"),
            AppUI.subtitleLabel("Choose categories and set their importance (weight 0–2)."));
        DigestPreference existing = prefService.getPreferences(user.getUserID());
        List<PreferenceCategory> existingCats = existing == null ? new ArrayList<>() : existing.getCategories();
        Map<String, Float> savedWeights = new LinkedHashMap<>();
        for (PreferenceCategory c : existingCats) savedWeights.put(c.getCategoryName(), c.getWeight());

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:#FDFAF4;-fx-background-radius:10;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,3);");
        List<CheckBox> boxes = new ArrayList<>();
        List<Slider> sliders = new ArrayList<>();

        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i], color = CAT_COLORS[i];
            StackPane badge = Icons.catBadge(i, color);
            CheckBox cb = new CheckBox();
            float savedW = savedWeights.getOrDefault(cat, -1f);
            cb.setSelected(savedW > 0); cb.setStyle("-fx-cursor:hand;");
            Label catName = new Label(cat);
            catName.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;-fx-font-family:'Georgia','Times New Roman',serif;");
            Label catDesc = new Label(CAT_DESC[i]);
            catDesc.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;");
            VBox nameBox = new VBox(2, catName, catDesc); nameBox.setAlignment(Pos.CENTER_LEFT);
            catName.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));
            catDesc.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));
            catName.setStyle(catName.getStyle() + "-fx-cursor:hand;");
            HBox labelSection = new HBox(10, cb, nameBox); labelSection.setAlignment(Pos.CENTER_LEFT); labelSection.setPrefWidth(240);
            Slider sl = new Slider(0, 2, 1);
            sl.setShowTickLabels(true); sl.setShowTickMarks(true); sl.setMajorTickUnit(1); sl.setSnapToTicks(true);
            HBox.setHgrow(sl, Priority.ALWAYS);
            if (savedW >= 0) sl.setValue(savedW);
            Label wt = new Label(String.format("%.0f", sl.getValue()));
            wt.setMinWidth(28); wt.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
            sl.valueProperty().addListener((o, x, n) -> wt.setText(String.format("%.0f", n.doubleValue())));
            HBox row = new HBox(14, badge, labelSection, sl, wt);
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(13, 20, 13, 16));
            boolean isLast = (i == CATEGORIES.length - 1);
            String divider = isLast ? "" : "-fx-border-color:#D8D3CB;-fx-border-width:0 0 1 0;";
            row.setStyle("-fx-background-color:#FDFAF4;" + divider);
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:" + color + "12;" + divider));
            row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:#FDFAF4;" + divider));
            card.getChildren().add(row);
            boxes.add(cb); sliders.add(sl);
        }

        Label status = AppUI.successLbl();
        Button save = AppUI.primaryBtn("Save Preferences");
        save.setOnAction(e -> {
            List<PreferenceCategory> all = new ArrayList<>();
            for (int i = 0; i < boxes.size(); i++) {
                PreferenceCategory c = new PreferenceCategory();
                c.setCategoryName(CATEGORIES[i]);
                c.setWeight(boxes.get(i).isSelected() ? (float) sliders.get(i).getValue() : 0f);
                all.add(c);
            }
            String err = prefService.saveCategories(user.getUserID(), all);
            if (err != null) AppUI.error(err);
            else { status.setText("✓ Preferences saved"); status.setVisible(true); }
        });
        pane.getChildren().addAll(card, status, save);
        return pane;
    }

    private Region buildPrioritization() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("Prioritization Mode"),
            AppUI.subtitleLabel("Choose how your Top 5 digest items are ranked."));
        PrioritizationMode current = prefService.getMode(user.getUserID());
        ToggleGroup group = new ToggleGroup();
        VBox card = AppUI.card(20);
        for (PrioritizationMode mode : PrioritizationMode.values()) {
            RadioButton rb = new RadioButton(mode.getDisplay());
            rb.setToggleGroup(group); rb.setUserData(mode); rb.setSelected(mode == current);
            rb.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;-fx-font-family:'Playfair Display', 'Georgia', serif;");
            String desc = switch (mode) {
                case LATEST_FIRST  -> "Shows newest articles first by publication date.";
                case MOST_RELEVANT -> "Ranks articles by how well they match your preferences.";
                case MOST_POPULAR  -> "Highlights most shared and viewed stories.";
            };
            Label d = new Label(desc);
            d.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;-fx-padding:0 0 8 24;");
            card.getChildren().addAll(rb, d);
        }
        Label status = AppUI.successLbl();
        Button save = AppUI.primaryBtn("Save Mode");
        save.setOnAction(e -> {
            PrioritizationMode chosen = (PrioritizationMode) group.getSelectedToggle().getUserData();
            if (prefService.updateMode(user.getUserID(), chosen)) { status.setText("✓ Saved: " + chosen.getDisplay()); status.setVisible(true); }
            else AppUI.error("Could not save.");
        });
        pane.getChildren().addAll(card, status, save);
        return pane;
    }

    private Region buildSources() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("News Sources"),
            AppUI.subtitleLabel("Choose which sources your digest pulls from."));
        List<NewsSource> all = sourceService.getAll();
        List<NewsSource> userSrcs = sourceService.getUserSources(user.getUserID());
        Set<String> selected = new LinkedHashSet<>();
        for (NewsSource s : userSrcs) selected.add(s.getSourceID());

        TextField searchFld = AppUI.field("Search by name, category, or URL…");
        searchFld.setMaxWidth(Double.MAX_VALUE);
        HBox searchRow = new HBox(6, new Label("🔍"), searchFld);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchFld, Priority.ALWAYS);
        pane.getChildren().add(searchRow);

        Map<String, List<NewsSource>> byCategory = new LinkedHashMap<>();
        for (NewsSource s : all) byCategory.computeIfAbsent(s.getCategory(), k -> new ArrayList<>()).add(s);

        List<CheckBox> boxes = new ArrayList<>();
        List<Object[]> sectionMeta = new ArrayList<>();

        for (Map.Entry<String, List<NewsSource>> entry : byCategory.entrySet()) {
            String cat = entry.getKey();
            int catIdx = catIndex(cat);
            String color = catIdx >= 0 ? CAT_COLORS[catIdx] : "#64748B";
            StackPane catIconNode = Icons.catIcon(catIdx >= 0 ? catIdx : 4, 16, color);
            Label catLabel = new Label(cat.toUpperCase());
            catLabel.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + color + ";-fx-font-family:'Segoe UI',system;");
            Region catSpacer = new Region(); HBox.setHgrow(catSpacer, Priority.ALWAYS);
            Label countLbl = new Label(entry.getValue().size() + " source" + (entry.getValue().size() == 1 ? "" : "s"));
            countLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;");
            HBox catHeader = new HBox(6, catIconNode, catLabel, catSpacer, countLbl);
            catHeader.setAlignment(Pos.CENTER_LEFT); catHeader.setPadding(new Insets(0, 0, 6, 0));
            VBox catCard = new VBox(0);
            catCard.setStyle("-fx-background-color:#FDFAF4;-fx-background-radius:10;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,3);");
            List<NewsSource> srcs = entry.getValue();
            List<Object[]> rowMeta = new ArrayList<>();
            for (int i = 0; i < srcs.size(); i++) {
                NewsSource src = srcs.get(i);
                Node iconNode = buildSourceIcon(src.getApiEndpoint(), cat);
                CheckBox cb = new CheckBox(); cb.setUserData(src.getSourceID());
                cb.setSelected(selected.contains(src.getSourceID())); cb.setStyle("-fx-cursor:hand;");
                Label srcName = new Label(src.getName());
                srcName.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;-fx-cursor:hand;");
                srcName.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));
                Label srcUrl = new Label(src.getApiEndpoint());
                srcUrl.setStyle("-fx-font-size:10px;-fx-text-fill:#5F5E5A;"); srcUrl.setMaxWidth(340);
                VBox srcInfo = new VBox(2, srcName, srcUrl); srcInfo.setAlignment(Pos.CENTER_LEFT);
                HBox row = new HBox(12, iconNode, cb, srcInfo);
                row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(12, 20, 12, 16));
                boolean isLast = (i == srcs.size() - 1);
                String divider = isLast ? "" : "-fx-border-color:#D8D3CB;-fx-border-width:0 0 1 0;";
                row.setStyle("-fx-background-color:#FDFAF4;" + divider);
                row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#EDE8DC;" + divider));
                row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:#FDFAF4;" + divider));
                catCard.getChildren().add(row); boxes.add(cb); rowMeta.add(new Object[]{row, src});
            }
            VBox section = new VBox(8, catHeader, catCard);
            pane.getChildren().add(section);
            sectionMeta.add(new Object[]{section, rowMeta, catCard, countLbl});
        }

        searchFld.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal.trim().toLowerCase();
            for (Object[] sm : sectionMeta) {
                VBox section = (VBox) sm[0];
                @SuppressWarnings("unchecked") List<Object[]> rows = (List<Object[]>) sm[1];
                Label count = (Label) sm[3];
                int visible = 0;
                for (Object[] rm : rows) {
                    HBox row = (HBox) rm[0]; NewsSource src = (NewsSource) rm[1];
                    boolean match = q.isEmpty() || src.getName().toLowerCase().contains(q)
                        || (src.getCategory() != null && src.getCategory().toLowerCase().contains(q))
                        || (src.getApiEndpoint() != null && src.getApiEndpoint().toLowerCase().contains(q));
                    row.setVisible(match); row.setManaged(match);
                    if (match) visible++;
                }
                count.setText(visible + " source" + (visible == 1 ? "" : "s"));
                section.setVisible(visible > 0); section.setManaged(visible > 0);
            }
        });

        Label status = AppUI.successLbl();
        Button save = AppUI.primaryBtn("Save Sources");
        save.setOnAction(e -> {
            List<NewsSource> picked = new ArrayList<>();
            for (CheckBox cb : boxes) {
                if (cb.isSelected()) {
                    String id = (String) cb.getUserData();
                    for (NewsSource s : all) { if (s.getSourceID().equals(id)) { picked.add(s); break; } }
                }
            }
            String err = sourceService.saveSources(user.getUserID(), picked);
            if (err != null) AppUI.error(err);
            else { status.setText("✓ " + picked.size() + " source(s) saved"); status.setVisible(true); }
        });
        pane.getChildren().addAll(status, save);
        return pane;
    }

    private Region buildEmail() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("Email & Google Calendar"),
            AppUI.subtitleLabel("Connect your Google account to export reading schedules directly to Google Calendar."));
        VBox statusCard = AppUI.card(16);
        Label currentStatus = new Label();
        refreshEmailStatus(currentStatus, user.getUserID());
        statusCard.getChildren().addAll(AppUI.h2("Current Status"), currentStatus);
        VBox googleCard = AppUI.card(20);
        googleCard.getChildren().add(AppUI.h2("Connect Google Account"));
        boolean credsMissing = GoogleCalendarService.CLIENT_ID.startsWith("YOUR_");
        if (credsMissing) {
            Label warn = new Label("⚠️  Google credentials not yet configured.\n\nTo enable Google Calendar:\n" +
                "1. Go to console.cloud.google.com\n2. Enable the Google Calendar API\n" +
                "3. Create OAuth 2.0 credentials (Desktop app)\n4. Paste Client ID + Secret into GoogleServices.java");
            warn.setWrapText(true);
            warn.setStyle("-fx-text-fill:#2C2C2C;-fx-background-color:#EDE8DC;-fx-padding:12;" +
                          "-fx-background-radius:0;-fx-border-color:#1A1A1A;-fx-border-width:1;-fx-font-size:13px;");
            googleCard.getChildren().add(warn);
        } else {
            Label oauthHint = new Label("Click the button below. Your browser will open for Google sign-in.\n" +
                "After approval, return here — your account will be connected automatically.");
            oauthHint.setWrapText(true); oauthHint.setStyle("-fx-text-fill:#5F5E5A;-fx-font-size:13px;");
            Button oauthBtn = AppUI.primaryBtn("Connect Google Account via OAuth");
            Label oauthStatus = new Label(); oauthStatus.setWrapText(true);
            oauthBtn.setOnAction(e -> {
                oauthBtn.setDisable(true);
                oauthStatus.setText("Browser opened — sign in with Google, then return here...");
                oauthStatus.setStyle("-fx-text-fill:#5F5E5A;");
                gcalService.startOAuth(user.getUserID(),
                    () -> { oauthBtn.setDisable(false); oauthStatus.setText("Google account connected successfully."); oauthStatus.setStyle("-fx-text-fill:#2A6B3A;-fx-font-weight:bold;"); refreshEmailStatus(currentStatus, user.getUserID()); },
                    () -> { oauthBtn.setDisable(false); oauthStatus.setText("Connection failed. Check your credentials and try again."); oauthStatus.setStyle("-fx-text-fill:#A32D2D;"); }
                );
            });
            googleCard.getChildren().addAll(oauthHint, oauthBtn, oauthStatus);
        }
        VBox otherCard = AppUI.card(20);
        otherCard.getChildren().add(AppUI.h2("Other Providers / Manual Link"));
        ComboBox<String> providerBox = new ComboBox<>();
        providerBox.getItems().addAll("Outlook", "Yahoo", "Apple Mail"); providerBox.setValue("Outlook");
        TextField emailFld = AppUI.field("your@email.com");
        Button linkBtn = AppUI.primaryBtn("Link Email");
        Button disconnectBtn = AppUI.dangerBtn("Disconnect Current Account");
        linkBtn.setOnAction(e -> {
            String err = emailService.linkEmail(user.getUserID(), emailFld.getText(), providerBox.getValue());
            if (err != null) AppUI.error(err);
            else { AppUI.info("Email linked: " + emailFld.getText()); refreshEmailStatus(currentStatus, user.getUserID()); }
        });
        disconnectBtn.setOnAction(e -> {
            if (AppUI.confirm("Disconnect current email/Google account?")) { emailService.disconnectEmail(user.getUserID()); refreshEmailStatus(currentStatus, user.getUserID()); }
        });
        otherCard.getChildren().addAll(new Label("Provider:"), providerBox, new Label("Email Address:"), emailFld, new HBox(10, linkBtn, disconnectBtn));
        pane.getChildren().addAll(statusCard, googleCard, otherCard);
        return pane;
    }

    private void refreshEmailStatus(Label label, String userID) {
        ExternalEmailAccount acc = emailService.getLinkedEmail(userID);
        if (acc == null) { label.setText("No account connected."); label.setStyle("-fx-text-fill:#5F5E5A;-fx-font-style:italic;"); }
        else {
            String since = acc.getLinkedAt() == null ? "" : "  |  Linked: " + acc.getLinkedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            boolean isGoogle = "Gmail".equals(acc.getProvider()) && acc.getAccessToken() != null && !acc.getAccessToken().startsWith("TOKEN-");
            String type = isGoogle ? "Google Calendar OAuth" : acc.getProvider();
            label.setText(acc.getEmailAddress() + "  (" + type + ")" + since + "  |  " + (acc.isLinked() ? "ACTIVE" : "DISCONNECTED"));
            label.setStyle("-fx-text-fill:" + (acc.isLinked() ? "#2A6B3A" : "#A32D2D") + ";-fx-font-weight:500;");
        }
    }

    private Region buildConnections() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("Connected Sources"),
            AppUI.subtitleLabel("Manage your active news source integrations."));
        VBox listCard = AppUI.card(16);
        listCard.getChildren().add(AppUI.h2("Active Connections"));
        VBox listBox = new VBox(8);
        listCard.getChildren().add(listBox);
        refreshConnections(listBox, user.getUserID());
        VBox addCard = AppUI.card(16);
        addCard.getChildren().add(AppUI.h2("Add Connection"));
        List<NewsSource> allSrc = sourceService.getAll();
        ComboBox<NewsSource> srcCombo = new ComboBox<>(FXCollections.observableArrayList(allSrc));
        if (!allSrc.isEmpty()) srcCombo.setValue(allSrc.get(0));
        Button addBtn = AppUI.primaryBtn("Add");
        addBtn.setOnAction(e -> {
            if (srcCombo.getValue() != null) { emailService.addConnection(user.getUserID(), srcCombo.getValue().getSourceID()); refreshConnections(listBox, user.getUserID()); }
        });
        HBox addRow = new HBox(10, srcCombo, addBtn); addRow.setAlignment(Pos.CENTER_LEFT);
        addCard.getChildren().add(addRow);
        pane.getChildren().addAll(listCard, addCard);
        return pane;
    }

    private void refreshConnections(VBox container, String userID) {
        container.getChildren().clear();
        List<ConnectedSource> list = emailService.getConnections(userID);
        if (list.isEmpty()) {
            Label empty = new Label("No connections yet."); empty.setStyle("-fx-text-fill:#5F5E5A;-fx-font-style:italic;");
            container.getChildren().add(empty); return;
        }
        for (ConnectedSource c : list) {
            HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color:#F5F0E8;-fx-background-radius:0;-fx-border-color:#1A1A1A;-fx-border-width:1;");
            boolean active = "ACTIVE".equalsIgnoreCase(c.getStatus());
            Label name = new Label(c.getSourceName() == null ? c.getSourceID() : c.getSourceName());
            name.setStyle("-fx-font-weight:bold;-fx-font-size:13px;"); name.setPrefWidth(160);
            Label statusBadge = new Label(c.getStatus());
            statusBadge.setStyle("-fx-font-size:11px;-fx-padding:2 8;-fx-background-radius:0;-fx-text-fill:#F5F0E8;-fx-background-color:" + (active ? "#2A6B3A" : "#A32D2D") + ";");
            Button toggleBtn = AppUI.secondaryBtn(active ? "Disconnect" : "Reconnect");
            toggleBtn.setOnAction(e -> { emailService.setConnectionStatus(c.getConnectionID(), active ? "DISCONNECTED" : "ACTIVE"); refreshConnections(container, userID); });
            Button delBtn = AppUI.dangerBtn("Remove");
            delBtn.setOnAction(e -> { if (AppUI.confirm("Remove this connection permanently?")) { emailService.removeConnection(c.getConnectionID()); refreshConnections(container, userID); } });
            row.getChildren().addAll(name, statusBadge, AppUI.spacer(), toggleBtn, delBtn);
            container.getChildren().add(row);
        }
    }

    private Region buildSchedule() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().addAll(AppUI.titleLabel("Delivery Schedule"),
            AppUI.subtitleLabel("Set when your digest is delivered. Minimum interval: 1 hour."));
        DigestSchedule existing = schedService.getSchedule(user.getUserID());
        int existHour = existing == null ? 8  : existing.getDeliveryTime().getHour();
        int existMin  = existing == null ? 0  : existing.getDeliveryTime().getMinute();
        int existInt  = existing == null ? 24 : existing.getIntervalHours();
        VBox card = AppUI.card(24);
        Label currStatus = new Label(existing == null ? "No schedule set yet." :
            "Current: " + existing.getDeliveryTime() + " every " + existing.getIntervalHours() + " hour(s)" +
            (existing.getNextTriggerAt() == null ? "" : " | Next: " + existing.getNextTriggerAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))));
        currStatus.setStyle("-fx-text-fill:#5F5E5A;-fx-font-size:13px;"); currStatus.setWrapText(true);
        Spinner<Integer> hourSpin = new Spinner<>(0, 23, existHour); hourSpin.setPrefWidth(80);
        Spinner<Integer> minSpin  = new Spinner<>(0, 59, existMin);  minSpin.setPrefWidth(80);
        HBox timeRow = new HBox(8, new Label("Time (24h):"), hourSpin, new Label(":"), minSpin);
        timeRow.setAlignment(Pos.CENTER_LEFT);
        ComboBox<String> intervalBox = new ComboBox<>();
        intervalBox.getItems().addAll("Every 1 hour", "Every 6 hours", "Every 12 hours", "Every 24 hours");
        intervalBox.setValue("Every " + existInt + " hour" + (existInt == 1 ? "" : "s"));
        HBox intRow = new HBox(10, new Label("Repeat:"), intervalBox); intRow.setAlignment(Pos.CENTER_LEFT);
        Label status = AppUI.successLbl();
        Button save = AppUI.primaryBtn("Save Schedule");
        save.setOnAction(e -> {
            LocalTime time = LocalTime.of(hourSpin.getValue(), minSpin.getValue());
            String sel = intervalBox.getValue();
            int interval = sel.contains("1 hour") ? 1 : sel.contains("6") ? 6 : sel.contains("12") ? 12 : 24;
            String err = schedService.save(user.getUserID(), time, interval);
            if (err != null) AppUI.error(err);
            else {
                DigestSchedule updated = schedService.getSchedule(user.getUserID());
                currStatus.setText("Current: " + updated.getDeliveryTime() + " every " + updated.getIntervalHours() + " hour(s) | Next: " +
                    (updated.getNextTriggerAt() == null ? "-" : updated.getNextTriggerAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))));
                status.setText("✓ Schedule saved"); status.setVisible(true);
            }
        });
        card.getChildren().addAll(AppUI.h2("Current Schedule"), currStatus, new Label(), AppUI.h2("Update Schedule"), timeRow, intRow, status);
        pane.getChildren().addAll(card, save);
        return pane;
    }

    private int catIndex(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i].equalsIgnoreCase(cat)) return i;
        return -1;
    }

    private Node buildSourceIcon(String endpoint, String category) {
        int idx = catIndex(category);
        String emoji = idx >= 0 ? CAT_EMOJI[idx] : "📡";
        String color = idx >= 0 ? CAT_COLORS[idx] : "#64748B";
        Label fallback = new Label(emoji);
        fallback.setMinSize(40, 40); fallback.setMaxSize(40, 40); fallback.setAlignment(Pos.CENTER);
        fallback.setStyle("-fx-background-color:" + color + "18;-fx-background-radius:0;-fx-font-size:18px;");
        StackPane container = new StackPane(fallback);
        container.setMinSize(40, 40); container.setMaxSize(40, 40);
        String domain = extractDomain(endpoint);
        if (!domain.isEmpty()) {
            try {
                Image img = new Image("https://www.google.com/s2/favicons?domain=" + domain + "&sz=32", 32, 32, true, true, true);
                ImageView iv = new ImageView(img); iv.setFitWidth(32); iv.setFitHeight(32);
                img.progressProperty().addListener((obs, old, progress) -> {
                    if (progress.doubleValue() >= 1.0 && !img.isError()) {
                        Platform.runLater(() -> { container.setStyle("-fx-background-color:#EDE8DC;-fx-background-radius:0;"); container.getChildren().setAll(iv); });
                    }
                });
            } catch (Exception ignored) {}
        }
        return container;
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) return "";
        try { return new java.net.URL(url).getHost(); } catch (Exception e) { return ""; }
    }

    private Region buildClassroom() {
        User user = Session.get().getUser();
        VBox pane = pane();
        pane.getChildren().add(AppUI.h2("📚 Google Classroom"));

        if (!gclassService.isConnected(user.getUserID())) {
            Label msg = new Label("Connect your Google account in the Email tab to view upcoming Classroom assignments.");
            msg.setStyle("-fx-text-fill:#64748B;-fx-font-style:italic;");
            msg.setWrapText(true);
            pane.getChildren().add(msg);
            return pane;
        }

        Label loading = new Label("⏳  Loading assignments...");
        loading.setStyle("-fx-text-fill:#64748B;-fx-font-style:italic;");
        pane.getChildren().add(loading);

        new Thread(() -> {
            List<ClassroomAssignment> list = gclassService.getUpcoming(user.getUserID(), 10);
            Platform.runLater(() -> {
                pane.getChildren().remove(loading);
                if (list.isEmpty()) {
                    Label empty = new Label("🎉  No upcoming assignments found.");
                    empty.setStyle("-fx-text-fill:#16A34A;-fx-font-size:13px;");
                    pane.getChildren().add(empty);
                    return;
                }
                if (list.size() == 1 && list.get(0).title.isEmpty()) {
                    String code = list.get(0).courseName;
                    String txt = GoogleClassroomService.ERR_SCOPE.equals(code)
                        ? "⚠️  Classroom access was not granted. Re-connect Google and approve the Classroom permission."
                        : "⚠️  Could not reach Google Classroom. Check your internet connection.";
                    Label err = new Label(txt);
                    err.setStyle("-fx-text-fill:#DC2626;-fx-font-size:12px;");
                    err.setWrapText(true);
                    pane.getChildren().add(err);
                    return;
                }
                for (ClassroomAssignment a : list)
                    pane.getChildren().add(buildAssignmentCard(a));
            });
        }).start();

        return pane;
    }

    private VBox buildAssignmentCard(ClassroomAssignment a) {
        VBox card = AppUI.card(12);
        Label course = new Label(a.courseName.toUpperCase());
        course.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#64748B;");
        Label title = new Label(a.title);
        title.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#0F172A;");
        title.setWrapText(true);

        boolean overdue = a.dueDate != null && a.dueDate.isBefore(LocalDateTime.now());
        boolean urgent  = a.dueDate != null && !overdue && a.dueDate.isBefore(LocalDateTime.now().plusDays(2));
        String dueText  = a.dueDate == null ? "No due date"
                : "Due: " + a.dueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm"));
        Label due = new Label(overdue ? "⚠️  OVERDUE — " + dueText : (urgent ? "🔴  " : "📅  ") + dueText);
        due.setStyle("-fx-font-size:11px;-fx-text-fill:" + (overdue ? "#DC2626" : urgent ? "#EA580C" : "#94A3B8") + ";");

        card.getChildren().addAll(course, title, due);
        if (a.link != null && !a.link.isBlank()) {
            Button open = AppUI.secondaryBtn("🔗  Open in Classroom");
            open.setOnAction(e -> {
                try { java.awt.Desktop.getDesktop().browse(new java.net.URI(a.link)); }
                catch (Exception ex) { AppUI.error("Could not open link."); }
            });
            card.getChildren().add(open);
        }
        return card;
    }

    private VBox pane() {
        VBox v = new VBox(16); v.setPadding(new Insets(28)); v.setMaxWidth(720); return v;
    }
}
