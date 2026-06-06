package com.example.frontend.models;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserSession {
    private static UserSession instance;
    private String username = "";
    private String sessionStartTime = "";
    private UserSession() {}
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public String getUsername() {
        return username;
    }

    public void startSession(String username) {
        this.username = username;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        this.sessionStartTime = LocalDateTime.now().format(formatter);
    }

    public void clearSession() {
        this.username = "";
        this.sessionStartTime = "";
    }

    public VBox createFooter() {
        String text = "Zalogowano jako: " + username + " | Start sesji: " + sessionStartTime;
        Label footerLabel = new Label(text);
        footerLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 14px;");

        VBox footerContainer = new VBox(footerLabel);
        footerContainer.setAlignment(Pos.CENTER);
        footerContainer.setPadding(new Insets(20, 0, 10, 0));

        return footerContainer;
    }
}
