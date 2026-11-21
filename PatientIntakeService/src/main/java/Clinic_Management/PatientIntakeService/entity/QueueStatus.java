package Clinic_Management.PatientIntakeService.entity;

public enum QueueStatus {
    PENDING_PAYMENT,
    WAITING,
    CALLED,
    IN_CONSULTATION,
    COMPLETED,
    SKIPPED,
    CANCELLED,
    EXPIRED
}