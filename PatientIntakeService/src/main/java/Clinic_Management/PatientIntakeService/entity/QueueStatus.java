package Clinic_Management.PatientIntakeService.entity;

public enum QueueStatus {
    PENDING_CONFIRMATION,
    WAITING,
    CALLED,
    IN_CONSULTATION,
    COMPLETED,
    SKIPPED,
    CANCELLED,
    REJECTED
}