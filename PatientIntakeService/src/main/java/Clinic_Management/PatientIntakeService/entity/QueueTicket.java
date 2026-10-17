package Clinic_Management.PatientIntakeService.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "queue_tickets",
        indexes = {
                @Index(name = "idx_ticket_date_dept_status", columnList = "appointment_date, department_id, status"),
                @Index(name = "idx_ticket_doctor_queue", columnList = "appointment_date, doctor_id, status, slot_start_time"),
                @Index(name = "idx_ticket_number", columnList = "ticket_number")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_queue_ticket_date_number",
                        columnNames = {"appointment_date", "ticket_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, length = 20)
    private String ticketNumber; // VD: #A-101, #B-102

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "slot_start_time", nullable = false)
    private LocalTime slotStartTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false)
    private TriagePriority priorityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_source", nullable = false)
    private IntakeSource intakeSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private QueueStatus status = QueueStatus.WAITING;

    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @CreationTimestamp
    @Column(name = "check_in_time", updatable = false)
    private LocalDateTime checkInTime;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "blood_pressure", length = 20)
    private String bloodPressure;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "temperature", length = 10)
    private String temperature;

    @Column(name = "spo2")
    private Integer spo2;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column(name = "height_cm")
    private Double heightCm;

    @Column(name = "nurse_notes", columnDefinition = "TEXT")
    private String nurseNotes;
}