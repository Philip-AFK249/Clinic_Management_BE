package Clinic_Management.ClinicalConsultationService.repository;

import Clinic_Management.ClinicalConsultationService.entity.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {
    Optional<Drug> findByCode(String code);
    List<Drug> findByActiveTrueOrderByNameAsc();
    @Query("""
        SELECT d FROM Drug d 
        WHERE d.active = true 
          AND d.stockQuantity > 0 
          AND (d.expiryDate IS NULL OR d.expiryDate > :currentDate)
        ORDER BY d.name ASC
    """)
    List<Drug> findAvailableDrugsForPrescription(LocalDate currentDate);
}