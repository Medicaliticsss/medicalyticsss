module com.example.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson;

    opens com.example.frontend to javafx.fxml, com.google.gson;
    exports com.example.frontend;
}