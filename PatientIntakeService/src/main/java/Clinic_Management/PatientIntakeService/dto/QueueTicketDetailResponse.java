package Clinic_Management.PatientIntakeService.dto;

import Clinic_Management.PatientIntakeService.entity.IntakeSource;
import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.TriagePriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueTicketDetailResponse {
    // queue ticket's information
    private String ticketNumber;
    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private String doctorName;
    private String roomNumber;
    private LocalDate appointmentDate;
    private LocalTime slotStartTime;
    private TriagePriority priorityLevel;
    private IntakeSource intakeSource;
    private QueueStatus status;
    private String chiefComplaint;
    private LocalDateTime checkInTime;
    private LocalDateTime calledAt;

    //identity patient's information
    private Long patientId;
    private String patientFullName;
    private String identityCardNumber;
    private String insuranceCode;
    private String initialHospitalCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String address;
    private Boolean isOcrVerified;

    private String bloodPressure;
    private Integer heartRate;
    private String temperature;
    private Integer spo2;
    private Double weightKg;
    private Double heightCm;
    private String nurseNotes;
}