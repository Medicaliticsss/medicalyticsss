module com.example.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.net.http;
    requires java.desktop;
    requires com.google.gson;
    requires atlantafx.base;

    opens com.example.frontend to javafx.fxml, com.google.gson;
    exports com.example.frontend;
    exports com.example.frontend.models;
    opens com.example.frontend.models to com.google.gson, javafx.fxml;
}