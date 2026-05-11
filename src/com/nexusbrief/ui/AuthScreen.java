package com.nexusbrief.ui;

import com.nexusbrief.model.AppData.*;
import com.nexusbrief.service.Services.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

class AuthScreen {

    private static final String CARD_WHITE = "#FDFAF4";
    private static final String TEXT_DARK  = "#0A0A0A";
    private static final String FIELD_BG   = "#F5F0E8";
    private static final String FIELD_BDR  = "#0A0A0A";

    private final AuthService authService = new AuthService();

    // Creates the logn screen view
    Region buildLogin() {
        VBox card = new VBox(11);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(420); card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(28, 44, 28, 44));
        card.setStyle("-fx-background-color:" + CARD_WHITE + ";" +
                      "-fx-background-radius:12;-fx-border-color:transparent;" +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");

        VBox brandBlock = centeredBrand();
        Region div = divider();

        Label welcome = new Label("Welcome back");
        welcome.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;" +
                         "-fx-font-family:'Georgia','Times New Roman',serif;");
        welcome.setMaxWidth(Double.MAX_VALUE); welcome.setAlignment(Pos.CENTER);
        Label sub = new Label("Sign in to your NexusBrief account");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;");
        sub.setMaxWidth(Double.MAX_VALUE); sub.setAlignment(Pos.CENTER);

        TextField emailFld     = lightField("you@example.com");
        PasswordField passFld  = lightPassField("••••••••");
        Label errLbl           = AppUI.errorLbl();

        Button loginBtn = inkBtn("SIGN IN");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink toReg = new Hyperlink("Don't have an account? Register");
        toReg.setStyle("-fx-text-fill:#2C2C2C;-fx-border-color:transparent;-fx-font-size:12px;-fx-underline:true;");
        HBox linkRow = new HBox(toReg);
        linkRow.setAlignment(Pos.CENTER);

        loginBtn.setOnAction(e -> {
            errLbl.setVisible(false);
            User user = authService.login(emailFld.getText(), passFld.getText());
            if (user == null) { errLbl.setText("Invalid email or password."); errLbl.setVisible(true); }
            else { Session.get().setUser(user); if (user.isAdmin()) AppUI.goAdmin(); else AppUI.goDashboard(); }
        });
        passFld.setOnAction(e -> loginBtn.fire());
        toReg.setOnAction(e -> AppUI.goRegister());

        card.getChildren().addAll(brandBlock, div, welcome, sub,
            fieldLabel("Email address"), emailFld,
            fieldLabel("Password"), passFld,
            errLbl, loginBtn, linkRow);

        return withFloatingProps(card);
    }

    Region buildRegister() {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(420); card.setMaxWidth(420);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPadding(new Insets(16, 44, 16, 44));
        card.setStyle("-fx-background-color:" + CARD_WHITE + ";" +
                      "-fx-background-radius:12;-fx-border-color:transparent;" +
                      "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");

        VBox brandBlock = centeredBrand();
        Region div = divider();

        Label title = new Label("Create Account");
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#0A0A0A;" +
                       "-fx-font-family:'Georgia','Times New Roman',serif;");
        title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
        Label sub = new Label("Start your personalized digest");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;");
        sub.setMaxWidth(Double.MAX_VALUE); sub.setAlignment(Pos.CENTER);

        TextField nameFld      = lightField("Full name");
        TextField emailFld     = lightField("Email address");
        PasswordField passFld  = lightPassField("Password (6+ chars)");
        PasswordField confFld  = lightPassField("Confirm password");
        Label errLbl           = AppUI.errorLbl();
        Label okLbl            = AppUI.successLbl();

        Button regBtn = inkBtn("CREATE ACCOUNT");
        regBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink toLogin = new Hyperlink("Already have an account? Sign in");
        toLogin.setStyle("-fx-text-fill:#2C2C2C;-fx-border-color:transparent;-fx-font-size:12px;-fx-underline:true;");
        HBox linkRow = new HBox(toLogin);
        linkRow.setAlignment(Pos.CENTER);

        regBtn.setOnAction(e -> {
            errLbl.setVisible(false); okLbl.setVisible(false);
            if (!passFld.getText().equals(confFld.getText())) {
                errLbl.setText("Passwords do not match."); errLbl.setVisible(true); return;
            }
            String err = authService.register(nameFld.getText(), emailFld.getText(), passFld.getText());
            if (err != null) { errLbl.setText(err); errLbl.setVisible(true); }
            else {
                okLbl.setText("✓ Account created! Redirecting..."); okLbl.setVisible(true);
                new Thread(() -> {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(AppUI::goLogin);
                }).start();
            }
        });
        toLogin.setOnAction(e -> AppUI.goLogin());

        card.getChildren().addAll(brandBlock, div, title, sub,
            fieldLabel("Name"), nameFld,
            fieldLabel("Email"), emailFld,
            fieldLabel("Password"), passFld,
            fieldLabel("Confirm password"), confFld,
            errLbl, okLbl, regBtn, linkRow);

        return withFloatingProps(card);
    }

    private Region divider() {
        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color:#1A1A1A;");
        VBox.setMargin(div, new Insets(4, -44, 4, -44));
        return div;
    }

    private StackPane withFloatingProps(VBox card) {
        Canvas waves = new Canvas(1050, 720);
        drawWaves(waves.getGraphicsContext2D(), 1050, 720);

        AnchorPane overlay = new AnchorPane();
        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);

        ImageView newspaper = prop("src/login icons/newspaper.png", 120, -15);
        AnchorPane.setTopAnchor(newspaper, 70.0); AnchorPane.setLeftAnchor(newspaper, 60.0);
        animate(newspaper, 10, 3.0);

        ImageView wifi = prop("src/login icons/wifi.png", 95, 20);
        AnchorPane.setBottomAnchor(wifi, 80.0); AnchorPane.setLeftAnchor(wifi, 80.0);
        animate(wifi, 8, 3.6);

        ImageView flash = prop("src/login icons/yellow flash.png", 75, 22);
        AnchorPane.setTopAnchor(flash, 55.0); AnchorPane.setRightAnchor(flash, 70.0);
        animate(flash, 12, 2.4);

        ImageView search = prop("src/login icons/search.png", 110, -12);
        AnchorPane.setBottomAnchor(search, 60.0); AnchorPane.setRightAnchor(search, 60.0);
        animate(search, 9, 2.9);

        ImageView trending = prop("src/login icons/trending.png", 160, -5);
        AnchorPane.setBottomAnchor(trending, 32.0); AnchorPane.setLeftAnchor(trending, 400.0);
        animate(trending, 6, 2.7);

        overlay.getChildren().addAll(newspaper, wifi, flash, search, trending);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color:#F5F0E8;");
        StackPane.setAlignment(card, Pos.CENTER);
        root.getChildren().addAll(waves, overlay, card);
        return root;
    }

    private void drawWaves(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.web("#C4BFB5")); gc.setLineWidth(1.2);
        double waveH = 18, waveLen = 80;
        for (double y = 60; y < h; y += 28) {
            gc.beginPath(); gc.moveTo(0, y);
            for (double x = 0; x < w; x += waveLen)
                gc.bezierCurveTo(x + waveLen * 0.25, y - waveH, x + waveLen * 0.75, y + waveH, x + waveLen, y);
            gc.stroke();
        }
    }

    private TextField lightField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color:" + FIELD_BG + ";-fx-border-color:" + FIELD_BDR + ";" +
                   "-fx-border-radius:0;-fx-background-radius:0;-fx-padding:11 14;" +
                   "-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";-fx-prompt-text-fill:#5F5E5A;");
        return f;
    }

    private PasswordField lightPassField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color:" + FIELD_BG + ";-fx-border-color:" + FIELD_BDR + ";" +
                   "-fx-border-radius:0;-fx-background-radius:0;-fx-padding:11 14;" +
                   "-fx-font-size:13px;-fx-text-fill:" + TEXT_DARK + ";-fx-prompt-text-fill:#5F5E5A;");
        return f;
    }

    private Button inkBtn(String text) {
        Button b = new Button(text);
        String base  = "-fx-background-color:#0A0A0A;-fx-text-fill:#F5F0E8;-fx-font-size:13px;-fx-font-weight:600;" +
                       "-fx-padding:13 22;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-cursor:hand;";
        String hover = "-fx-background-color:#F5F0E8;-fx-text-fill:#0A0A0A;-fx-font-size:13px;-fx-font-weight:600;" +
                       "-fx-padding:13 22;-fx-background-radius:0;-fx-border-radius:0;" +
                       "-fx-border-color:#0A0A0A;-fx-border-width:1;-fx-cursor:hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(hover));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    private static Font serifFont(double size) {
        try {
            java.io.File f = new java.io.File("src/fonts/PlayfairDisplay-Bold.ttf");
            if (f.exists()) { Font loaded = Font.loadFont(f.toURI().toString(), size); if (loaded != null) return loaded; }
        } catch (Exception ignored) {}
        return Font.font("Palatino Linotype", javafx.scene.text.FontWeight.BOLD, size);
    }

    private VBox centeredBrand() {
        ImageView logoView = new ImageView();
        try {
            java.io.File f = new java.io.File("login logo.png");
            if (!f.exists()) f = new java.io.File("logo.png");
            if (f.exists()) {
                Image img = new Image(f.toURI().toString(), 80, 80, true, true);
                logoView.setImage(img); logoView.setFitHeight(80); logoView.setFitWidth(80); logoView.setPreserveRatio(true);
            }
        } catch (Exception ignored) {}

        Label name = new Label("NexusBrief");
        name.setFont(serifFont(36)); name.setStyle("-fx-text-fill:#0A0A0A;");
        Label tagline = new Label("Your Daily News Digest");
        tagline.setStyle("-fx-font-size:12px;-fx-text-fill:#5F5E5A;" +
                         "-fx-font-family:'Georgia','Times New Roman',serif;-fx-font-style:italic;");

        VBox block = new VBox(6, logoView, name, tagline);
        block.setAlignment(Pos.CENTER);
        block.setPadding(new Insets(8, 0, 8, 0));
        return block;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-font-size:11px;-fx-font-weight:600;-fx-text-fill:#5F5E5A;-fx-font-family:'Segoe UI',system;");
        VBox.setMargin(l, new Insets(4, 0, 0, 0));
        return l;
    }

    private ImageView prop(String path, double width, double rotate) {
        try {
            ImageView iv = new ImageView(new Image(new java.io.File(path).toURI().toString(), width, width, true, true));
            iv.setFitWidth(width); iv.setPreserveRatio(true); iv.setRotate(rotate); iv.setOpacity(0.5);
            return iv;
        } catch (Exception e) { return new ImageView(); }
    }

    private void animate(ImageView iv, double distance, double seconds) {
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,           new KeyValue(iv.translateYProperty(), 0)),
            new KeyFrame(Duration.seconds(seconds), new KeyValue(iv.translateYProperty(), -distance)));
        t.setAutoReverse(true); t.setCycleCount(Timeline.INDEFINITE); t.play();
    }
}
