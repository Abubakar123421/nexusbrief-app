package com.nexusbrief;

import com.nexusbrief.dao.Database;
import com.nexusbrief.ui.AppUI;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    // Initalizes the main app windw
    @Override
    public void start(Stage stage) {
        Database.init();
        Database.initAdminUser();
        AppUI.init(stage);
        AppUI.goLogin();
    }

    @Override
    public void stop() {
        Database.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
