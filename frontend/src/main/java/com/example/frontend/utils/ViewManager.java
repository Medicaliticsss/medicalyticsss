package com.example.frontend.utils;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ViewManager {
    private static Scene mainScene;
    private static Stage primaryStage;

    public static void setMainScene(Scene scene) {
        mainScene = scene;
    }
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    public static void switchView(Parent view) {
        mainScene.setRoot(view);
    }
}