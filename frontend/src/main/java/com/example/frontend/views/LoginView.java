package com.example.frontend.views;

import com.example.frontend.models.UserSession;
import com.example.frontend.services.AuthService;
import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class LoginView {
    public static Parent getView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Medicalytics");
        titleLabel.getStyleClass().add(Styles.TITLE_2);
        titleLabel.setStyle("-fx-text-fill: #FF0055;");

        ImageView logoView = new ImageView();
        var logoUrl = LoginView.class.getResource("/images/przezroczyste.png");
        if (logoUrl != null) {
            logoView.setImage(new Image(logoUrl.toExternalForm()));
            logoView.setFitWidth(350);
            logoView.setPreserveRatio(true);
        }
        Label screenTitle = new Label("Logowanie");
        screenTitle.getStyleClass().add(Styles.TITLE_3);
        screenTitle.setStyle("-fx-text-fill: #888888;");

        TextField usernameInput = new TextField();
        usernameInput.setPromptText("Login");
        usernameInput.setMaxWidth(250);

        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Hasło");
        passwordInput.setMaxWidth(250);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add(Styles.DANGER);

        Button loginButton = new Button("Zaloguj się");
        loginButton.setMaxWidth(250);
        loginButton.getStyleClass().add(Styles.ACCENT);

        loginButton.setOnAction(e -> {
            AuthService.login(usernameInput.getText(), passwordInput.getText()).thenAccept(res -> {
                Platform.runLater(() -> {
                    if (res.equals("SUCCESS")) {
                        ViewManager.switchView(MainMenuView.getView());
                    } else {
                        errorLabel.setText(res);
                    }
                });
            });
        });
        Button registerButton = new Button("Zarejestruj się");
        registerButton.setMaxWidth(250);
        registerButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        registerButton.setOnAction(e -> ViewManager.switchView(RegisterView.getView()));

        layout.getChildren().addAll(logoView, screenTitle, usernameInput, passwordInput, loginButton, registerButton, errorLabel);        return layout;
    }
}