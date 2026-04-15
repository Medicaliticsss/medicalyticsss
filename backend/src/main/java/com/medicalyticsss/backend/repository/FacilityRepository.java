package com.medicalyticsss.backend.repository;

import com.medicalyticsss.backend.model.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    Optional<Facility> findByFacilityNameAndCity(String facilityName, String city);
}