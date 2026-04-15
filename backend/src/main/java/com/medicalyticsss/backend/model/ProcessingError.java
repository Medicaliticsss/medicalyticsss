package com.medicalyticsss.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "processing_errors")
public class ProcessingError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacja: Wiele błędów należy do jednego pliku (files_history)
    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private FileHistory fileHistory;

    @Column(name = "error_row_number")
    private Integer errorRowNumber;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_line_data", columnDefinition = "TEXT")
    private String rawLineData;

    // Gettery i Settery
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FileHistory getFileHistory() { return fileHistory; }
    public void setFileHistory(FileHistory fileHistory) { this.fileHistory = fileHistory; }

    public Integer getErrorRowNumber() { return errorRowNumber; }
    public void setErrorRowNumber(Integer errorRowNumber) { this.errorRowNumber = errorRowNumber; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getRawLineData() { return rawLineData; }
    public void setRawLineData(String rawLineData) { this.rawLineData = rawLineData; }
}