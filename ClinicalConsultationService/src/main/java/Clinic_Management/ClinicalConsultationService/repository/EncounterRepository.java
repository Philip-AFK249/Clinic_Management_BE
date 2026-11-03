package Clinic_Management.ClinicalConsultationService.repository;

import Clinic_Management.ClinicalConsultationService.entity.Encounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EncounterRepository extends JpaRepository<Encounter, Long> {
    Optional<Encounter> findByTicketNumber(String ticketNumber);

    @Query("SELECT e FROM Encounter e JOIN FETCH e.diagnoses WHERE e.patientId = :patientId ORDER BY e.encounterDate DESC")
    List<Encounter> findPatientHistory(@Param("patientId") Long patientId);

    @Query("""
        SELECT e FROM Encounter e 
        LEFT JOIN FETCH e.diagnoses 
        WHERE e.doctorId = :doctorId 
        ORDER BY e.encounterDate DESC, e.createdAt DESC
    """)
    List<Encounter> findDoctorEncounters(@Param("doctorId") Long doctorId);
}