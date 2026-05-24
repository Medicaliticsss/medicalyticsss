package com.example.frontend.models;

public class FileItem {
    public Long id;
    public String fileName;
    public String status;
    public String uploadTime;
    public String content;

    public ProcessingError processingError;

    // Nadpisujemy metodę toString, aby ListView na dashboardzie ładnie wyświetlało nazwy
    @Override
    public String toString() {
        return fileName + " [" + status + "]";
    }
}