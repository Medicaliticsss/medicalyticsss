package com.example.frontend.views;

import com.example.frontend.models.UserSession;
import com.example.frontend.services.SettingsService;
import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SettingsView {

    private static final int MIN_PASSWORD_LENGTH = 4;

    public static Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30));

        Label titleLabel = new Label("Ustawienia");
        titleLabel.getStyleClass().add(Styles.TITLE_1);

        Button backButton = new Button("Wróć do Menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> ViewManager.switchView(MainMenuView.getView()));

        HBox topBar = new HBox(20, titleLabel, backButton);
        topBar.setAlignment(Pos.CENTER_LEFT);
        root.setTop(topBar);

        Tab accountTab = new Tab("Konto", createAccountTab());
        accountTab.setClosable(false);

        Tab dictionaryTab = new Tab("Słownik badań", DictionarySettingsView.getView());
        dictionaryTab.setClosable(false);

        TabPane tabPane = new TabPane(accountTab, dictionaryTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        root.setCenter(tabPane);

        return root;
    }

    private static Parent createAccountTab() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);

        VBox content = new VBox(24);
        content.setPadding(new Insets(10));
        content.setMaxWidth(560);
        content.setAlignment(Pos.TOP_LEFT);

        Label accountSectionTitle = new Label("Konto");
        accountSectionTitle.getStyleClass().add(Styles.TITLE_3);

        Label usernameLabel = new Label();
        usernameLabel.setWrapText(true);

        Label sessionLabel = new Label();
        sessionLabel.setWrapText(true);

        String localUsername = UserSession.getInstance().getUsername();
        if (localUsername != null && !localUsername.isBlank()) {
            usernameLabel.setText("Użytkownik: " + localUsername);
        } else {
            usernameLabel.setText("Użytkownik: —");
        }

        sessionLabel.setText("Status sesji: sprawdzanie...");
        sessionLabel.getStyleClass().add(Styles.TEXT_MUTED);

        SettingsService.fetchAccountInfo().thenAccept(info -> Platform.runLater(() -> {
            if (info.sessionActive()) {
                usernameLabel.setText("Użytkownik: " + info.username());
                sessionLabel.setText("Status sesji: aktywna");
                sessionLabel.setStyle("-fx-text-fill: #3DDC84;");
            } else {
                sessionLabel.setText("Status sesji: nieaktywna — " + info.message());
                sessionLabel.setStyle("-fx-text-fill: #FF6B6B;");
            }
        }));

        VBox accountSection = new VBox(10, accountSectionTitle, usernameLabel, sessionLabel);
        Separator separator = new Separator();

        Label passwordSectionTitle = new Label("Zmiana hasła");
        passwordSectionTitle.getStyleClass().add(Styles.TITLE_3);

        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Aktualne hasło");

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nowe hasło");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Powtórz nowe hasło");

        Label passwordHintLabel = new Label("Hasło musi mieć co najmniej " + MIN_PASSWORD_LENGTH + " znaki.");
        passwordHintLabel.getStyleClass().add(Styles.TEXT_MUTED);

        Label passwordStatusLabel = new Label();
        passwordStatusLabel.setWrapText(true);

        Button changePasswordButton = new Button("Zmień hasło");
        changePasswordButton.getStyleClass().add(Styles.ACCENT);
        changePasswordButton.setMaxWidth(Double.MAX_VALUE);

        GridPane passwordForm = new GridPane();
        passwordForm.setHgap(12);
        passwordForm.setVgap(12);
        passwordForm.add(new Label("Aktualne hasło:"), 0, 0);
        passwordForm.add(oldPasswordField, 1, 0);
        passwordForm.add(new Label("Nowe hasło:"), 0, 1);
        passwordForm.add(newPasswordField, 1, 1);
        passwordForm.add(new Label("Potwierdź hasło:"), 0, 2);
        passwordForm.add(confirmPasswordField, 1, 2);
        GridPane.setHgrow(oldPasswordField, Priority.ALWAYS);
        GridPane.setHgrow(newPasswordField, Priority.ALWAYS);
        GridPane.setHgrow(confirmPasswordField, Priority.ALWAYS);

        changePasswordButton.setOnAction(e -> {
            String oldPassword = oldPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            passwordStatusLabel.setStyle("");

            if (oldPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                showPasswordStatus(passwordStatusLabel, "Wypełnij wszystkie pola hasła.", false);
                return;
            }

            if (newPassword.length() < MIN_PASSWORD_LENGTH) {
                showPasswordStatus(passwordStatusLabel,
                        "Nowe hasło jest za krótkie (minimum " + MIN_PASSWORD_LENGTH + " znaki).", false);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showPasswordStatus(passwordStatusLabel, "Nowe hasła nie są identyczne.", false);
                return;
            }

            if (oldPassword.equals(newPassword)) {
                showPasswordStatus(passwordStatusLabel, "Nowe hasło musi różnić się od aktualnego.", false);
                return;
            }

            changePasswordButton.setDisable(true);
            passwordStatusLabel.setText("Zapisywanie...");

            SettingsService.changePassword(oldPassword, newPassword).thenAccept(result -> Platform.runLater(() -> {
                changePasswordButton.setDisable(false);

                if (result.toLowerCase().contains("pomyślnie")) {
                    oldPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    showPasswordStatus(passwordStatusLabel, result, true);
                } else {
                    showPasswordStatus(passwordStatusLabel, result, false);
                }
            }));
        });

        VBox passwordSection = new VBox(12,
                passwordSectionTitle,
                passwordForm,
                passwordHintLabel,
                changePasswordButton,
                passwordStatusLabel
        );

        content.getChildren().addAll(accountSection, separator, passwordSection);
        scrollPane.setContent(content);
        return scrollPane;
    }

    private static void showPasswordStatus(Label label, String message, boolean success) {
        label.setText(message);
        label.setStyle(success ? "-fx-text-fill: #3DDC84;" : "-fx-text-fill: #FF6B6B;");
    }
}
