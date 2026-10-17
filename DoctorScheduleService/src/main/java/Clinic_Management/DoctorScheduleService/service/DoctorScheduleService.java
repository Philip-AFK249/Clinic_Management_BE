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
    public List<TimeSlotResponse> getAvailableSlots(Long departmentId, LocalDate date) {
        List<DoctorShift> outpatientShifts = shiftRepository
                .findByDepartmentIdAndShiftDateAndDutyType(departmentId, date, DutyType.OUTPATIENT);

        if (outpatientShifts.isEmpty()) {
            return Collections.emptyList();
        }

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

                // CHỈ ĐẾM CÁC SLOT ĐANG Ở TRẠNG THÁI BOOKED (Slot bị hủy sẽ không tính)
                long bookedCount = slotAssignmentRepository
                        .countByDepartmentIdAndAppointmentDateAndSlotStartTimeAndStatus(
                                departmentId, date, slotStart, "BOOKED");

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
        long minPatientCount = Long.MAX_VALUE;

        for (DoctorShift shift : activeShifts) {
            Doctor doctor = shift.getDoctor();
            // CHỈ ĐẾM TẢI CỦA CÁC CA ĐANG BOOKED
            long currentAssigned = slotAssignmentRepository
                    .countByDoctorIdAndAppointmentDateAndSlotStartTimeAndStatus(
                            doctor.getId(), date, slotStartTime, "BOOKED");

            if (currentAssigned < shift.getMaxPatientsPerSlot() && currentAssigned < minPatientCount) {
                minPatientCount = currentAssigned;
                selectedDoctor = doctor;
            }
        }

        if (selectedDoctor == null) {
            throw new IllegalStateException("Khung giờ này đã kín lịch cho tất cả bác sĩ.");
        }

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
                .status("BOOKED")
                .build();
        slotAssignmentRepository.save(assignment);

        return AssignDoctorResponse.builder()
                .ticketNumber(ticketNumber)
                .doctorId(selectedDoctor.getId())
                .doctorName(selectedDoctor.getFullName())
                .roomNumber(selectedDoctor.getRoomNumber())
                .date(date)
                .slotStartTime(slotStartTime)
                .message("Phân bổ bác sĩ thành công theo cơ chế cân bằng tải.")
                .build();
    }

    /**
     * Giải phóng slot khi phiếu khám bị hủy
     */
    @Transactional
    public void releaseSlotAssignment(String ticketNumber) {
        List<SlotAssignment> assignments = slotAssignmentRepository.findByTicketNumber(ticketNumber);
        if (assignments.isEmpty()) {
            log.warn("Không tìm thấy slot_assignment cho phiếu: {}", ticketNumber);
            return;
        }
        for (SlotAssignment assignment : assignments) {
            assignment.setStatus("CANCELLED");
        }
        slotAssignmentRepository.saveAll(assignments);
        log.info("Đã hủy và giải phóng slot_assignment cho phiếu: {}", ticketNumber);
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
}