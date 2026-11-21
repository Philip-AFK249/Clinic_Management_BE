package Clinic_Management.DoctorScheduleService.service;

import Clinic_Management.DoctorScheduleService.dto.*;
import Clinic_Management.DoctorScheduleService.entity.*;
import Clinic_Management.DoctorScheduleService.repository.DepartmentRepository;
import Clinic_Management.DoctorScheduleService.repository.DoctorRepository;
import Clinic_Management.DoctorScheduleService.repository.DoctorShiftRepository;
import Clinic_Management.DoctorScheduleService.repository.SlotAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorScheduleService {

    private final DoctorShiftRepository shiftRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final SlotAssignmentRepository slotAssignmentRepository;

    @Transactional
    public DoctorShift registerShift(CreateShiftRequest request) {
        if (shiftRepository.existsByDoctorIdAndShiftDateAndSession(
                request.getDoctorId(), request.getShiftDate(), request.getSession())) {
            throw new IllegalStateException("Bác sĩ này đã có lịch trực trong buổi này.");
        }

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Bác sĩ có ID: " + request.getDoctorId()));

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Chuyên khoa có ID: " + request.getDepartmentId()));

        String resolvedNurseName = request.getNurseName();
        Long resolvedNurseId = request.getNurseId();

        if (resolvedNurseName == null || resolvedNurseName.isBlank()) {
            resolvedNurseName = switch (department.getId().intValue()) {
                case 1 -> "ĐD. Trịnh Thu Hằng";
                case 2 -> "ĐD. Đỗ Phương Mai";
                case 3 -> "ĐD. Nguyễn Thảo Ly";
                default -> "ĐD. Phụ tá Lâm sàng";
            };
        }

        DoctorShift shift = DoctorShift.builder()
                .doctor(doctor)
                .department(department)
                .shiftDate(request.getShiftDate())
                .session(request.getSession())
                .dutyType(request.getDutyType())
                .maxPatientsPerSlot(4)
                .nurseId(resolvedNurseId)
                .nurseName(resolvedNurseName)
                .build();

        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Doctor> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartmentIdAndActiveTrue(departmentId);
    }

    @Transactional(readOnly = true)
    public List<DoctorShiftResponse> getShiftsByDate(Long departmentId, LocalDate date) {
        return shiftRepository.findByDepartmentIdAndShiftDate(departmentId, date).stream()
                .map(this::mapToShiftResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorShiftResponse> getWeeklyShifts(Long departmentId, LocalDate startDate, LocalDate endDate) {
        return shiftRepository.findByDepartmentIdAndShiftDateBetweenOrderByShiftDateAsc(departmentId, startDate, endDate).stream()
                .map(this::mapToShiftResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean validateRosterCompliance(Long departmentId, LocalDate date, ShiftSession session) {
        long outpatientCount = shiftRepository.countByDepartmentIdAndShiftDateAndSessionAndDutyType(
                departmentId, date, session, DutyType.OUTPATIENT);
        long inpatientCount = shiftRepository.countByDepartmentIdAndShiftDateAndSessionAndDutyType(
                departmentId, date, session, DutyType.INPATIENT);

        return outpatientCount >= 1 && inpatientCount >= 1;
    }

    private DoctorShiftResponse mapToShiftResponse(DoctorShift shift) {
        return DoctorShiftResponse.builder()
                .id(shift.getId())
                .doctor(DoctorShiftResponse.DoctorReference.builder()
                        .id(shift.getDoctor().getId())
                        .fullName(shift.getDoctor().getFullName())
                        .title(shift.getDoctor().getTitle())
                        .roomNumber(shift.getDoctor().getRoomNumber())
                        .build())
                .departmentId(shift.getDepartment().getId())
                .departmentName(shift.getDepartment().getName())
                .shiftDate(shift.getShiftDate())
                .session(shift.getSession())
                .dutyType(shift.getDutyType())
                .maxPatientsPerSlot(shift.getMaxPatientsPerSlot())
                .roomNumber(shift.getDoctor().getRoomNumber())
                .nurseId(shift.getNurseId())
                .nurseName(shift.getNurseName() != null ? shift.getNurseName() : switch (shift.getDepartment().getId().intValue()) {
                    case 1 -> "ĐD. Trịnh Thu Hằng";
                    case 2 -> "ĐD. Đỗ Phương Mai";
                    case 3 -> "ĐD. Nguyễn Thảo Ly";
                    default -> "ĐD. Phụ tá";
                })
                .build();
    }
    @Transactional
    public DoctorShift updateShift(Long shiftId, UpdateShiftRequest request) {
        DoctorShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca trực có ID: " + shiftId));

        if (request.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Bác sĩ có ID: " + request.getDoctorId()));
            shift.setDoctor(doctor);
        }

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Chuyên khoa có ID: " + request.getDepartmentId()));
            shift.setDepartment(department);
        }

        if (request.getShiftDate() != null) {
            shift.setShiftDate(request.getShiftDate());
        }

        if (request.getSession() != null) {
            shift.setSession(request.getSession());
        }

        if (request.getDutyType() != null) {
            shift.setDutyType(request.getDutyType());
        }

        if (request.getMaxPatientsPerSlot() != null && request.getMaxPatientsPerSlot() > 0) {
            shift.setMaxPatientsPerSlot(request.getMaxPatientsPerSlot());
        }

        if (request.getNurseId() != null) {
            shift.setNurseId(request.getNurseId());
        }

        if (request.getNurseName() != null && !request.getNurseName().isBlank()) {
            shift.setNurseName(request.getNurseName());
        }

        log.info("Admin đã cập nhật ca trực #{} cho BS {}", shiftId, shift.getDoctor().getFullName());
        return shiftRepository.save(shift);
    }

    @Transactional
    public void deleteShift(Long shiftId) {
        DoctorShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca trực có ID: " + shiftId));
        shiftRepository.delete(shift);
        log.info("Admin đã xóa ca trực #{} của BS {}", shiftId, shift.getDoctor().getFullName());
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(Long departmentId, LocalDate date) {
        List<DoctorShift> outpatientShifts = shiftRepository
                .findByDepartmentIdAndShiftDateAndDutyType(departmentId, date, DutyType.OUTPATIENT);

        if (outpatientShifts.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        List<TimeSlotResponse> slots = new ArrayList<>();

        for (ShiftSession session : ShiftSession.values()) {
            List<DoctorShift> shiftsInSession = outpatientShifts.stream()
                    .filter(s -> s.getSession() == session)
                    .toList();

            if (shiftsInSession.isEmpty()) continue;

            LocalTime current = session.getStartTime();
            while (current.isBefore(session.getEndTime())) {
                LocalTime slotStart = current;
                LocalTime slotEnd = current.plusHours(1);

                int totalCapacity = shiftsInSession.stream()
                        .mapToInt(DoctorShift::getMaxPatientsPerSlot)
                        .sum();

                long bookedCount = slotAssignmentRepository
                        .countActiveByDepartmentAndSlot(departmentId, date, slotStart, now);

                int availableCapacity = Math.max(0, totalCapacity - (int) bookedCount);

                slots.add(TimeSlotResponse.builder()
                        .startTime(slotStart)
                        .endTime(slotEnd)
                        .totalCapacity(totalCapacity)
                        .bookedCount((int) bookedCount)
                        .availableCapacity(availableCapacity)
                        .isAvailable(availableCapacity > 0)
                        .build());

                current = slotEnd;
            }
        }

        return slots;
    }

    @Transactional
    public AssignDoctorResponse assignDoctorToSlot(AssignDoctorRequest request) {
        Long departmentId = request.getDepartmentId();
        LocalDate date = request.getAppointmentDate();
        LocalTime slotStartTime = request.getSlotStartTime();
        String ticketNumber = request.getTicketNumber();
        LocalDateTime now = LocalDateTime.now();

        ShiftSession session = slotStartTime.isBefore(LocalTime.of(12, 0)) ? ShiftSession.MORNING : ShiftSession.AFTERNOON;

        List<DoctorShift> activeShifts = shiftRepository
                .findByDepartmentIdAndShiftDateAndDutyType(departmentId, date, DutyType.OUTPATIENT)
                .stream()
                .filter(s -> s.getSession() == session)
                .toList();

        if (activeShifts.isEmpty()) {
            throw new IllegalStateException("Không có bác sĩ trực phòng khám trong khung giờ này.");
        }

        Doctor selectedDoctor = null;

        if (request.getPreferredDoctorId() != null) {
            for (DoctorShift shift : activeShifts) {
                if (shift.getDoctor().getId().equals(request.getPreferredDoctorId())) {
                    long currentAssigned = slotAssignmentRepository
                            .countActiveByDoctorAndSlot(shift.getDoctor().getId(), date, slotStartTime, now);
                    if (currentAssigned < shift.getMaxPatientsPerSlot()) {
                        selectedDoctor = shift.getDoctor();
                        log.info("Phân bổ theo chỉ định của bệnh nhân: Bác sĩ ID {}", selectedDoctor.getId());
                        break;
                    } else {
                        log.warn("Bác sĩ chỉ định #{} đã đầy slot, tự động chuyển sang cân bằng tải Least-Busy.", request.getPreferredDoctorId());
                    }
                }
            }
        }

        if (selectedDoctor == null) {
            long minPatientCount = Long.MAX_VALUE;
            for (DoctorShift shift : activeShifts) {
                Doctor doctor = shift.getDoctor();
                long currentAssigned = slotAssignmentRepository
                        .countActiveByDoctorAndSlot(doctor.getId(), date, slotStartTime, now);

                if (currentAssigned < shift.getMaxPatientsPerSlot() && currentAssigned < minPatientCount) {
                    minPatientCount = currentAssigned;
                    selectedDoctor = doctor;
                }
            }
        }

        if (selectedDoctor == null) {
            throw new IllegalStateException("Khung giờ này đã kín lịch cho tất cả bác sĩ.");
        }

        boolean isTemporary = Boolean.TRUE.equals(request.getTemporary());
        LocalDateTime expiresAt = isTemporary
                ? (request.getExpiresAt() != null ? request.getExpiresAt() : now.plusMinutes(3))
                : null;

        SlotAssignment assignment = SlotAssignment.builder()
                .ticketNumber(ticketNumber)
                .doctor(selectedDoctor)
                .departmentId(departmentId)
                .appointmentDate(date)
                .slotStartTime(slotStartTime)
                .patientName(request.getPatientName())
                .patientPhone(request.getPatientPhone())
                .insuranceCode(request.getInsuranceCode())
                .chiefComplaint(request.getChiefComplaint())
                .status(isTemporary ? "TEMPORARY" : "BOOKED")
                .expiresAt(expiresAt)
                .build();
        slotAssignmentRepository.save(assignment);

        return AssignDoctorResponse.builder()
                .ticketNumber(ticketNumber)
                .doctorId(selectedDoctor.getId())
                .doctorName(selectedDoctor.getFullName())
                .roomNumber(selectedDoctor.getRoomNumber())
                .date(date)
                .slotStartTime(slotStartTime)
                .message(isTemporary ? "Giữ chỗ tạm thời thành công." : "Phân bổ bác sĩ thành công.")
                .build();
    }

    @Transactional
    public void confirmSlotAssignment(String ticketNumber) {
        List<SlotAssignment> assignments = slotAssignmentRepository.findByTicketNumber(ticketNumber);
        if (assignments.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy slot giữ chỗ cho phiếu: " + ticketNumber);
        }
        LocalDateTime now = LocalDateTime.now();
        for (SlotAssignment assignment : assignments) {
            if ("TEMPORARY".equals(assignment.getStatus())
                    && assignment.getExpiresAt() != null
                    && !assignment.getExpiresAt().isAfter(now)) {
                assignment.setStatus("CANCELLED");
                assignment.setExpiresAt(null);
                slotAssignmentRepository.save(assignment);
                throw new IllegalStateException("Suất giữ chỗ tạm thời đã hết hạn 3 phút và đã bị hủy.");
            }
            assignment.setStatus("BOOKED");
            assignment.setExpiresAt(null);
        }
        slotAssignmentRepository.saveAll(assignments);
        log.info("Đã xác nhận chính thức slot_assignment (TEMPORARY -> BOOKED) cho phiếu: {}", ticketNumber);
    }

    @Transactional
    public void releaseSlotAssignment(String ticketNumber) {
        List<SlotAssignment> assignments = slotAssignmentRepository.findByTicketNumber(ticketNumber);
        if (assignments.isEmpty()) {
            log.warn("Không tìm thấy slot_assignment cho phiếu: {}", ticketNumber);
            return;
        }
        for (SlotAssignment assignment : assignments) {
            assignment.setStatus("CANCELLED");
            assignment.setExpiresAt(null);
        }
        slotAssignmentRepository.saveAll(assignments);
        log.info("Đã hủy và giải phóng slot_assignment cho phiếu: {}", ticketNumber);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 10000)
    @Transactional
    public void cleanupExpiredTemporarySlots() {
        int released = slotAssignmentRepository.releaseExpiredTemporaryAssignments(LocalDateTime.now());
        if (released > 0) {
            log.info("Đã tự động giải phóng {} suất giữ chỗ tạm thời quá hạn 3 phút.", released);
        }
    }

}