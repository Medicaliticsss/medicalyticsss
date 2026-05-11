package com.example.frontend;

import com.example.frontend.services.AuthService;
import com.example.frontend.utils.ViewManager;
import com.example.frontend.views.LoginView;
import com.example.frontend.views.MainMenuView;
import atlantafx.base.theme.Dracula;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Ustawienie stylu wizualnego
        Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());

        // 2. Przygotowanie głównego okna i sceny
        // Tworzymy pusty kontener na start, ViewManager będzie nim zarządzał
        Scene mainScene = new Scene(new VBox(), 1200, 800);
        ViewManager.setMainScene(mainScene);
        ViewManager.setPrimaryStage(stage);

        stage.setScene(mainScene);
        stage.setTitle("Medicalytics");
        stage.setMaximized(true);

        // 3. Logika startowa: Sprawdzamy sesję
        AuthService.checkSession().thenAccept(isLoggedIn -> {
            Platform.runLater(() -> {
                if (isLoggedIn) {
                    // Jeśli zalogowany, idź do menu
                    ViewManager.switchView(MainMenuView.getView());
                } else {
                    // Jeśli nie, pokaż logowanie
                    ViewManager.switchView(LoginView.getView());
                }
                stage.show();
            });
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}