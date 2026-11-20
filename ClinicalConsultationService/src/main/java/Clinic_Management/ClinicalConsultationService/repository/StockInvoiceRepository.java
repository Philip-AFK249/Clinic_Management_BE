package Clinic_Management.ClinicalConsultationService.repository;

import Clinic_Management.ClinicalConsultationService.entity.StockInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockInvoiceRepository extends JpaRepository<StockInvoice, Long> {
    @Query("SELECT DISTINCT s FROM StockInvoice s LEFT JOIN FETCH s.items i LEFT JOIN FETCH i.drug ORDER BY s.createdAt DESC")
    List<StockInvoice> findAllWithItemsOrderByCreatedAtDesc();
}