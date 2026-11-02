package Clinic_Management.PatientIntakeService.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleAssignDoctorRequest {
    private Long departmentId;
    private LocalDate appointmentDate;
    private LocalTime slotStartTime;
    private String ticketNumber;
    private String patientName;
    private Long preferredDoctorId;
    private String patientPhone;
    private String insuranceCode;
    private String chiefComplaint;
}