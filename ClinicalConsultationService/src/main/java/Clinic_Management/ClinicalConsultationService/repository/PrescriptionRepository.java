package Clinic_Management.ClinicalConsultationService.repository;

import Clinic_Management.ClinicalConsultationService.entity.Prescription;
import Clinic_Management.ClinicalConsultationService.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @Query("SELECT p FROM Prescription p JOIN FETCH p.items WHERE p.status = :status ORDER BY p.createdAt ASC")
    List<Prescription> findQueueByStatus(@Param("status") PrescriptionStatus status);

    @Query("SELECT p FROM Prescription p JOIN FETCH p.items WHERE p.status = :status ORDER BY p.dispensedAt DESC, p.createdAt DESC")
    List<Prescription> findHistoryByStatus(@Param("status") PrescriptionStatus status);

    @Query("SELECT p FROM Prescription p JOIN FETCH p.items WHERE p.id = :id")
    Optional<Prescription> findByIdWithItems(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT p FROM Prescription p
        LEFT JOIN FETCH p.items i
        LEFT JOIN FETCH i.drug
        WHERE p.encounterId IN :encounterIds
    """)
    List<Prescription> findByEncounterIdInWithItems(@Param("encounterIds") List<Long> encounterIds);
}