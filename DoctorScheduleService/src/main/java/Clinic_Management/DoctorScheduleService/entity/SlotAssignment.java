package Clinic_Management.DoctorScheduleService.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(
        name = "slot_assignments",
        indexes = {
                @Index(name = "idx_slot_dept_date_time_status", columnList = "department_id, appointment_date, slot_start_time, status"),
                @Index(name = "idx_slot_doctor_date_time_status", columnList = "doctor_id, appointment_date, slot_start_time, status"),
                @Index(name = "idx_slot_date_ticket", columnList = "appointment_date, ticket_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticketNumber; // VD: #A-102

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private Long departmentId;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime slotStartTime;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(name = "insurance_code")
    private String insuranceCode;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "BOOKED";
}