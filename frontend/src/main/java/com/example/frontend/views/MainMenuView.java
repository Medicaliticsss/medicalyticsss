package com.example.frontend.views;

import com.example.frontend.services.AuthService;
import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class MainMenuView {
    public static Parent getView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(40));

        Label titleLabel = new Label("Główne Menu");
        titleLabel.getStyleClass().add(Styles.TITLE_1);

        Button logoutBtn = new Button("Wyloguj");
        logoutBtn.getStyleClass().addAll(Styles.BUTTON_OUTLINED, Styles.DANGER);
        logoutBtn.setOnAction(e -> AuthService.logout().thenAccept(ok ->
                Platform.runLater(() -> ViewManager.switchView(LoginView.getView()))
        ));

        StackPane top = new StackPane(titleLabel, logoutBtn);
        StackPane.setAlignment(logoutBtn, Pos.CENTER_RIGHT);
        root.setTop(top);

        HBox cards = new HBox(20);
        cards.setAlignment(Pos.CENTER);

        Button filesBtn = createMenuCard("Pliki");
        filesBtn.setOnAction(e -> ViewManager.switchView(DashboardView.getView()));

        Button reportsBtn = createMenuCard("Raporty");
        reportsBtn.setOnAction(e -> ViewManager.switchView(ReportView.getView()));

        Button settingsBtn = createMenuCard("Ustawienia");
        settingsBtn.setOnAction(e -> ViewManager.switchView(SettingsView.getView()));

        cards.getChildren().addAll(filesBtn, reportsBtn, settingsBtn);
        root.setCenter(cards);

        return root;
    }

    private static Button createMenuCard(String text) {
        Button card = new Button(text);
        card.setPrefSize(300, 300);
        card.getStyleClass().addAll(Styles.ELEVATED_2, Styles.TITLE_2);
        return card;
    }
}
