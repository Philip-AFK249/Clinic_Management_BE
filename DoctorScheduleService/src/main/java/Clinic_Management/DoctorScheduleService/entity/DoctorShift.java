package Clinic_Management.DoctorScheduleService.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "doctor_shifts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"doctor_id", "shift_date", "session"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DutyType dutyType;

    // Maximum number of patients that can be scheduled in a single time slot for this shift
    @Builder.Default
    @Column(nullable = false)
    private Integer maxPatientsPerSlot = 4;

    @Column(name = "nurse_id")
    private Long nurseId;

    @Column(name = "nurse_name")
    private String nurseName;
}