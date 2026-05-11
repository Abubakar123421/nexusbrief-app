package com.nexusbrief.ui;

import com.nexusbrief.model.AppData.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class AppUI {
    private static Stage stage;

    // Setups the main windw settings
    public static void init(Stage s) {
        stage = s;
        try { stage.getIcons().add(new Image("file:logo.png")); } catch (Exception ignored) {}
        stage.setTitle("NexusBrief");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
    }

    public static void show(Region root, String title) {
        Scene scene = new Scene(root, 1050, 720);
        try {
            java.net.URL css = AppUI.class.getResource("/dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
        stage.setScene(scene);
        stage.setTitle("NexusBrief – " + title);
        stage.show();
    }

    public static void goLogin()    { show(new AuthScreen().buildLogin(),    "Login");     stage.setResizable(false); }
    public static void goRegister() { show(new AuthScreen().buildRegister(), "Register");  stage.setResizable(false); }
    public static void goDashboard(){ show(new DashboardScreen().build(),    "Dashboard"); stage.setResizable(true); }
    public static void goHistory()  { show(new HistoryScreen().build(),      "Digest History"); stage.setResizable(true); }
    public static void goSettings() { show(new SettingsScreen().build(),     "Settings");  stage.setResizable(true); }
    public static void goAdmin()    { show(new AdminScreen().build(),        "Admin Panel"); stage.setResizable(true); }
    public static void goProfile()  { show(new ProfileScreen().build(),      "My Profile"); stage.setResizable(true); }

    public static final String BG         = "#F5F0E8";
    public static final String CARD       = "#FDFAF4";
    public static final String CARD_ALT   = "#EDE8DC";
    public static final String HOVER      = "#EDE8DC";
    public static final String BORDER     = "#1A1A1A";
    public static final String TEXT       = "#0A0A0A";
    public static final String MUTED      = "#2C2C2C";
    public static final String DIM        = "#5F5E5A";
    public static final String ACCENT     = "#0A0A0A";
    public static final String ACCENT2    = "#0A0A0A";
    public static final String ACCENT_DIM = "#2C2C2C";
    public static final String GREEN      = "#2A6B3A";
    public static final String RED        = "#A32D2D";
    public static final String YELLOW     = "#5F5E5A";
    public static final String BLUE       = "#0A0A0A";

    public static Button primaryBtn(String text) {
        Button b = new Button(text);
        String base  = "-fx-background-color:#0A0A0A;-fx-text-fill:#F5F0E8;-fx-font-size:13px;" +
                       "-fx-padding:10 20;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-cursor:hand;-fx-font-weight:600;";
        String hover = "-fx-background-color:#F5F0E8;-fx-text-fill:#0A0A0A;-fx-font-size:13px;" +
                       "-fx-padding:10 20;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-cursor:hand;-fx-font-weight:600;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    public static Button secondaryBtn(String text) {
        Button b = new Button(text);
        String base  = "-fx-background-color:transparent;-fx-text-fill:#0A0A0A;-fx-font-size:13px;" +
                       "-fx-padding:10 20;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#1A1A1A;-fx-border-width:1;-fx-cursor:hand;";
        String hover = "-fx-background-color:#0A0A0A;-fx-text-fill:#F5F0E8;-fx-font-size:13px;" +
                       "-fx-padding:10 20;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    public static Button dangerBtn(String text) {
        Button b = new Button(text);
        String base  = "-fx-background-color:transparent;-fx-text-fill:#A32D2D;" +
                       "-fx-font-size:12px;-fx-padding:7 14;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#A32D2D;-fx-border-width:1;-fx-cursor:hand;-fx-font-weight:600;";
        String hover = "-fx-background-color:#A32D2D;-fx-text-fill:#F5F0E8;" +
                       "-fx-font-size:12px;-fx-padding:7 14;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#A32D2D;-fx-border-width:1;-fx-cursor:hand;-fx-font-weight:600;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    public static Label titleLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:26px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;" +
                   "-fx-font-family:'Georgia','Times New Roman',serif;");
        return l;
    }

    public static Label subtitleLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;-fx-font-family:'Segoe UI',system;");
        return l;
    }

    public static Label h2(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;" +
                   "-fx-font-family:'Georgia','Times New Roman',serif;");
        return l;
    }

    public static Label errorLbl() {
        Label l = new Label();
        l.setStyle("-fx-text-fill:#A32D2D;-fx-font-size:13px;");
        l.setVisible(false);
        return l;
    }

    public static Label successLbl() {
        Label l = new Label();
        l.setStyle("-fx-text-fill:#2A6B3A;-fx-font-size:13px;");
        l.setVisible(false);
        return l;
    }

    public static VBox card(double pad) {
        VBox v = new VBox(10);
        v.setPadding(new Insets(pad));
        v.setStyle("-fx-background-color:#FDFAF4;-fx-background-radius:10;" +
                   "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,3);");
        return v;
    }

    public static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-padding:10 12;-fx-font-size:14px;-fx-background-radius:0;" +
                   "-fx-background-color:#F5F0E8;-fx-text-fill:#0A0A0A;" +
                   "-fx-prompt-text-fill:#5F5E5A;" +
                   "-fx-border-color:#0A0A0A;-fx-border-radius:0;-fx-border-width:1;");
        return f;
    }

    public static PasswordField passField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-padding:10 12;-fx-font-size:14px;-fx-background-radius:0;" +
                   "-fx-background-color:#F5F0E8;-fx-text-fill:#0A0A0A;" +
                   "-fx-prompt-text-fill:#5F5E5A;" +
                   "-fx-border-color:#0A0A0A;-fx-border-radius:0;-fx-border-width:1;");
        return f;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static HBox topBar(String title) {
        Label brand = new Label("NexusBrief");
        brand.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;" +
                       "-fx-font-family:'Georgia','Times New Roman',serif;");
        Label sep = new Label("—");
        sep.setStyle("-fx-font-size:13px;-fx-text-fill:#5F5E5A;");
        Label t = new Label(title.toUpperCase());
        t.setStyle("-fx-font-size:11px;-fx-text-fill:#5F5E5A;-fx-font-family:'Segoe UI',system;");
        Button logout = new Button("LOGOUT");
        logout.setGraphic(Icons.logout(15));
        logout.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        logout.setGraphicTextGap(6);
        String logBase  = "-fx-background-color:transparent;-fx-text-fill:#0A0A0A;" +
                          "-fx-padding:6 14;-fx-background-radius:0;-fx-cursor:hand;" +
                          "-fx-border-color:#1A1A1A;-fx-border-width:1;-fx-font-size:11px;-fx-font-weight:600;";
        String logHover = "-fx-background-color:#0A0A0A;-fx-text-fill:#F5F0E8;" +
                          "-fx-padding:6 14;-fx-background-radius:0;-fx-cursor:hand;" +
                          "-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-font-size:11px;-fx-font-weight:600;";
        logout.setStyle(logBase);
        logout.setOnMouseEntered(e -> logout.setStyle(logHover));
        logout.setOnMouseExited(e  -> logout.setStyle(logBase));
        logout.setOnAction(e -> { Session.get().logout(); AppUI.goLogin(); });
        HBox bar = new HBox(10, brand, sep, t, spacer(), logout);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color:#FDFAF4;-fx-border-color:#D8D3CB;-fx-border-width:0 0 1 0;");
        return bar;
    }

    public static VBox sidebar(String active) {
        VBox bar = new VBox(4);
        bar.setPadding(new Insets(20, 12, 20, 12));
        bar.setPrefWidth(200);
        bar.setStyle("-fx-background-color:#FDFAF4;-fx-border-color:#D8D3CB;-fx-border-width:0 1 0 0;");
        Label hdr = new Label("MENU");
        hdr.setStyle("-fx-font-size:10px;-fx-text-fill:#5F5E5A;-fx-font-weight:600;" +
                     "-fx-font-family:'Segoe UI',system;-fx-padding:0 0 6 8;");
        bar.getChildren().add(hdr);
        bar.getChildren().addAll(
            sidebarBtn(Icons.home(18),     "Dashboard",      active, AppUI::goDashboard),
            sidebarBtn(Icons.history(18),  "Digest History", active, AppUI::goHistory),
            sidebarBtn(Icons.settings(18), "Settings",       active, AppUI::goSettings),
            sidebarBtn(Icons.profile(18),  "My Profile",     active, AppUI::goProfile)
        );
        Region sp = new Region();
        VBox.setVgrow(sp, Priority.ALWAYS);
        bar.getChildren().add(sp);
        bar.getChildren().add(sidebarBtn(Icons.logout(18), "Logout", active, () -> {
            Session.get().logout();
            goLogin();
        }));
        return bar;
    }

    private static Button sidebarBtn(Node icon, String label, String active, Runnable action) {
        Button b = new Button("  " + label);
        b.setGraphic(icon);
        b.setContentDisplay(ContentDisplay.LEFT);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        boolean isActive = label.equals(active);
        String activeStyle = "-fx-background-color:#EDE8DC;-fx-text-fill:#0A0A0A;" +
                             "-fx-font-size:13px;-fx-padding:10 12;-fx-background-radius:0;" +
                             "-fx-cursor:hand;-fx-font-weight:600;";
        String baseStyle   = "-fx-background-color:transparent;-fx-text-fill:#5F5E5A;" +
                             "-fx-font-size:13px;-fx-padding:10 12;-fx-background-radius:0;-fx-cursor:hand;";
        String hoverStyle  = "-fx-background-color:#EDE8DC;-fx-text-fill:#0A0A0A;" +
                             "-fx-font-size:13px;-fx-padding:10 12;-fx-background-radius:0;-fx-cursor:hand;";
        b.setStyle(isActive ? activeStyle : baseStyle);
        if (!isActive) {
            b.setOnMouseEntered(e -> b.setStyle(hoverStyle));
            b.setOnMouseExited(e  -> b.setStyle(baseStyle));
        }
        b.setOnAction(e -> action.run());
        return b;
    }

    public static void info(String msg)          { alert(Alert.AlertType.INFORMATION, msg); }
    public static void error(String msg)         { alert(Alert.AlertType.ERROR, msg); }
    public static boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setHeaderText(null); a.setContentText(msg);
        styleDialogPane(a.getDialogPane());
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private static void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setHeaderText(null); a.setContentText(msg);
        styleDialogPane(a.getDialogPane());
        a.showAndWait();
    }

    public static void styleDialogPane(DialogPane dp) {
        dp.setGraphic(null);
        try {
            java.net.URL css = AppUI.class.getResource("/dark.css");
            if (css != null) dp.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
        dp.setStyle("-fx-background-color:#FDFAF4;-fx-border-color:#1A1A1A;-fx-border-width:1;" +
                    "-fx-border-radius:0;-fx-background-radius:0;");
    }
}

// ── Session ──────────────────────────────────────────────────────────────────

class Session {
    private static Session instance;
    private User currentUser;

    private Session() {}

    static synchronized Session get() {
        if (instance == null) instance = new Session();
        return instance;
    }

    User getUser()       { return currentUser; }
    void setUser(User u) { this.currentUser = u; }
    boolean isLoggedIn() { return currentUser != null; }
    void logout()        { currentUser = null; }
}

// ── Icons ─────────────────────────────────────────────────────────────────────

final class Icons {
    static final String RED   = "#A32D2D";
    static final String GREEN = "#2A6B3A";

    private Icons() {}

    static StackPane home(double d)     { return icon("M3 10.5L12 3L21 10.5V21H15V15H9V21H3V10.5Z", "#0A0A0A", d, false); }
    static StackPane history(double d)  { return multiIcon(d, "#0A0A0A", "M12 3a9 9 0 1 0 9 9A9 9 0 0 0 12 3Z", "M12 7v5l3 3", "M3 3L6.5 6.5M3 9H1M3 3V9"); }
    static StackPane settings(double d) { return icon("M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM12 2v2M12 20v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M2 12h2M20 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42", "#0A0A0A", d, false); }
    static StackPane profile(double d)  { return icon("M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8ZM4 20c0-4 3.58-7 8-7s8 3 8 7", "#0A0A0A", d, false); }
    static StackPane admin(double d)    { return icon("M12 2L3 6v6c0 5.25 3.75 10.15 9 11.25C17.25 22.15 21 17.25 21 12V6L12 2ZM9 12l2 2 4-4", "#0A0A0A", d, false); }
    static StackPane logout(double d)   { return icon("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9", RED, d, false); }
    static StackPane generate(double d) { return icon("M13 2L3 14h9l-1 8 10-12h-9l1-8Z", "#0A0A0A", d, false); }
    static StackPane calendar(double d) { return icon("M3 4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4ZM16 2v4M8 2v4M3 10h18M14 16l2 2 4-4", RED, d, false); }
    static StackPane thumbsUp(double d) { return icon("M7 22V11M2 13v7a2 2 0 0 0 2 2h11.17a2 2 0 0 0 1.98-1.71l1.17-8A2 2 0 0 0 16.34 9H13V5a2 2 0 0 0-2-2 1 1 0 0 0-1 1v1L7 11", GREEN, d, false); }
    static StackPane thumbsDown(double d){ return icon("M17 2v11M22 11V4a2 2 0 0 0-2-2H8.83a2 2 0 0 0-1.98 1.71l-1.17 8A2 2 0 0 0 7.66 15H11v4a2 2 0 0 0 2 2 1 1 0 0 0 1-1v-1l3-6", RED, d, false); }
    static StackPane login(double d)    { return icon("M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3", "#0A0A0A", d, false); }
    static StackPane news(double d)     { return icon("M3 3a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v18a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V3ZM7 8h10M7 12h10M7 16h6", "#0A0A0A", d, false); }

    static StackPane catBadge(int n, String color) {
        StackPane sp = new StackPane(catIcon(n, 22.0, color));
        sp.setMinSize(44.0, 44.0); sp.setMaxSize(44.0, 44.0);
        sp.setStyle("-fx-background-color:" + color + "18;-fx-background-radius:0;");
        return sp;
    }

    static StackPane catIcon(int n, double d, String color) {
        return switch (n) {
            case 0 -> multiIcon(d, color, "M3 18L9 11L13 15L21 7", "M17 7h4v4");
            case 1 -> icon("M22 12h-4l-3 9L9 3l-3 9H2", color, d, false);
            case 2 -> multiIcon(d, color, "M20 3H4a1 1 0 0 0-1 1v12a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V4a1 1 0 0 0-1-1Z", "M8 21h8M12 17v4");
            case 3 -> multiIcon(d, color, "M12 2v20", "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6");
            case 4 -> multiIcon(d, color, "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8L14 2Z", "M14 2v6h6", "M16 13H8M16 17H8M10 9H8");
            case 5 -> multiIcon(d, color, "M3 22h18", "M3 10h18", "M12 2L3 10M12 2L21 10", "M5 22V10M9 22V10M15 22V10M19 22V10");
            case 6 -> multiIcon(d, color, "M20 7H4a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2Z", "M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2");
            case 7 -> multiIcon(d, color, "M9 2v7.5L4.5 17a2.5 2.5 0 0 0 2.2 3.7h10.6a2.5 2.5 0 0 0 2.2-3.7L15 9.5V2", "M9 2h6", "M6.5 17h11");
            default -> multiIcon(d, color, "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8L14 2Z", "M14 2v6h6", "M16 13H8M16 17H8M10 9H8");
        };
    }

    private static StackPane icon(String path, String colorHex, double d, boolean filled) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        Color color = Color.web(colorHex);
        if (filled) {
            svg.setFill(color); svg.setStroke(Color.TRANSPARENT);
        } else {
            svg.setFill(Color.TRANSPARENT); svg.setStroke(color);
            svg.setStrokeWidth(2.0);
            svg.setStrokeLineCap(StrokeLineCap.ROUND);
            svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }
        return scaled(svg, d);
    }

    static StackPane multiIcon(double d, String colorHex, String... paths) {
        StackPane sp = new StackPane();
        Color color = Color.web(colorHex);
        for (String path : paths) {
            SVGPath svg = new SVGPath();
            svg.setContent(path);
            svg.setFill((Paint) Color.TRANSPARENT); svg.setStroke((Paint) color);
            svg.setStrokeWidth(2.0);
            svg.setStrokeLineCap(StrokeLineCap.ROUND);
            svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
            sp.getChildren().add(svg);
        }
        double scale = d / 24.0;
        sp.getTransforms().add(new Scale(scale, scale));
        sp.setPrefSize(d, d);
        return sp;
    }

    private static StackPane scaled(SVGPath svg, double d) {
        StackPane sp = new StackPane(svg);
        double scale = d / 24.0;
        sp.getTransforms().add(new Scale(scale, scale));
        sp.setPrefSize(d, d);
        return sp;
    }
}
