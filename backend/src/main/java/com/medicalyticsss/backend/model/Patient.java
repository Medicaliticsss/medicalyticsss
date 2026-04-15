package com.medicalyticsss.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "dim_patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_hash", nullable = false, length = 64)
    private String patientHash;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPatientHash() { return patientHash; }
    public void setPatientHash(String patientHash) { this.patientHash = patientHash; }

    public Integer getBirthYear() { return birthYear; }
    public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
}