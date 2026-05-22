package com.example.frontend.views;

import com.example.frontend.services.AuthService;
import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class RegisterView {
    public static Parent getView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Rejestracja");
        titleLabel.getStyleClass().add(Styles.TITLE_2);

        TextField userIn = new TextField();
        userIn.setPromptText("Stwórz login");
        userIn.setMaxWidth(250);

        PasswordField passIn = new PasswordField();
        passIn.setPromptText("Stwórz hasło");
        passIn.setMaxWidth(250);

        Label statusLabel = new Label();
        Button regBtn = new Button("Stwórz konto");
        regBtn.getStyleClass().add(Styles.ACCENT);

        regBtn.setOnAction(e -> {
            String user = userIn.getText();
            String pass = passIn.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                statusLabel.setText("Pola nie mogą być puste!");
                return;
            }

            // Wywołujemy nasz serwis
            AuthService.register(user, pass).thenAccept(res -> {
                Platform.runLater(() -> {
                    statusLabel.setText(res); // Wyświetli komunikat z backendu
                    if (res.contains("pomyślnie")) {
                        statusLabel.setStyle("-fx-text-fill: #00FF00;"); // Zielony kolor sukcesu
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #FF0000;"); // Czerwony kolor błędu
                    }
                });
            });
        });

        Button backBtn = new Button("Powrót");
        backBtn.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backBtn.setOnAction(e -> ViewManager.switchView(LoginView.getView()));

        layout.getChildren().addAll(titleLabel, userIn, passIn, regBtn, backBtn, statusLabel);
        return layout;
    }
}
