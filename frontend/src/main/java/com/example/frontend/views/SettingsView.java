package com.example.frontend.views;

import com.example.frontend.utils.ViewManager;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsView {
    public static Parent getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label label = new Label("USTAWIENIA SYSTEMU");
        label.getStyleClass().add(Styles.TITLE_1);

        Button backButton = new Button("Wróć do Menu");
        backButton.getStyleClass().add(Styles.BUTTON_OUTLINED);
        backButton.setOnAction(e -> ViewManager.switchView(MainMenuView.getView()));

        root.getChildren().addAll(label, backButton);
        return root;
    }
}