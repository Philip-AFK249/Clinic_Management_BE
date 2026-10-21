package Clinic_Management.PatientIntakeService.dto;
import Clinic_Management.PatientIntakeService.entity.TriagePriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class IntakeResponse {
    private String ticketNumber;
    private Long patientId;
    private String patientName;
    private String insuranceCode;
    private Long departmentId;
    private String departmentName;
    private Long doctorId;
    private String doctorName;
    private String roomNumber;
    private LocalDate appointmentDate;
    private LocalTime slotStartTime;
    private TriagePriority priorityLevel;
    private LocalDateTime checkInTime;
    private String message;
    private Long secondaryDepartmentId;
    private String secondaryDepartmentName;
    private String multiSpecialtyNote;
}