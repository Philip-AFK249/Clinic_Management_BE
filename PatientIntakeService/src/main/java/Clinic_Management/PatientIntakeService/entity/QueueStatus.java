package Clinic_Management.PatientIntakeService.entity;

public enum QueueStatus {
    PENDING_APPROVAL,
    WAITING,
    CALLED,
    IN_CONSULTATION,
    COMPLETED,
    SKIPPED,
    CANCELLED
}