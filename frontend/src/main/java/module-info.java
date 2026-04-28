module com.example.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;
    requires atlantafx.base;
    opens com.example.frontend to javafx.fxml, com.google.gson;
    exports com.example.frontend;
}