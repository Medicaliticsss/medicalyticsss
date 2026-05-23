package com.example.frontend.models;

public class FileItem {
    public Long id;
    public String fileName;
    public String status;
    public String uploadTime;

    // Nadpisujemy metodę toString, aby ListView na dashboardzie ładnie wyświetlało nazwy
    @Override
    public String toString() {
        return fileName + " [" + status + "]";
    }
}