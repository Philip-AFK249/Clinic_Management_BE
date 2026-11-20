package Clinic_Management.AuthService.service;

import Clinic_Management.AuthService.dto.*;
import Clinic_Management.AuthService.entity.User;
import Clinic_Management.AuthService.entity.UserRole;
import Clinic_Management.AuthService.repository.UserRepository;
import Clinic_Management.AuthService.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RestClient intakeRestClient;
    private final RestClient clinicalRestClient;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            @Value("${services.patient-intake.base-url:http://localhost:8082/api/v1/queue}") String intakeBaseUrl,
            @Value("${services.clinical-consultation.base-url:http://localhost:8083/api/v1/clinical/encounters}") String clinicalBaseUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());

        this.intakeRestClient = RestClient.builder()
                .baseUrl(intakeBaseUrl)
                .requestFactory(factory)
                .build();
        this.clinicalRestClient = RestClient.builder()
                .baseUrl(clinicalBaseUrl)
                .requestFactory(factory)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalStateException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác");
        }

        String token = jwtProvider.generateToken(user);
        log.info("Người dùng {} ({}) đăng nhập thành công", user.getEmail(), user.getRole());

        return mapToAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse registerPatient(RegisterPatientRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email này đã được đăng ký tài khoản trong hệ thống");
        }

        User patient = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .role(UserRole.PATIENT)
                .active(true)
                .build();

        User saved = userRepository.save(patient);
        String token = jwtProvider.generateToken(saved);

        log.info("Bệnh nhân mới tự đăng ký thành công: {}", email);
        return mapToAuthResponse(saved, token);
    }


    @Transactional
    public AuthResponse createStaffAccount(CreateStaffRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email này đã được sử dụng");
        }

        String defaultPassword = "password123";

        User staff = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(defaultPassword))
                .phone(request.getPhone())
                .role(request.getRole())
                .departmentId(request.getDepartmentId())
                .departmentName(request.getDepartmentName())
                .doctorId(request.getDoctorId())
                .pharmacistId(request.getPharmacistId())
                .active(true)
                .build();

        User saved = userRepository.save(staff);
        log.info("Quản trị viên đã cấp tài khoản nhân sự mới: {} ({})", email, request.getRole());

        return mapToAuthResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public AuthResponse getProfileByToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("JWT Token không hợp lệ hoặc đã hết hạn");
        }

        Claims claims = jwtProvider.extractClaims(token);
        String email = claims.getSubject();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng"));

        return mapToAuthResponse(user, token);
    }

    @Transactional(readOnly = true)
    public List<AuthResponse> getAllPatients() {
        return userRepository.findByRoleOrderByCreatedAtDesc(UserRole.PATIENT).stream()
                .map(patient -> mapToAuthResponse(patient, null))
                .toList();
    }

    @Transactional
    public AuthResponse updatePatientByAdmin(Long patientId, UpdatePatientRequest request) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân có ID: " + patientId));

        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException("Tài khoản này không phải là Bệnh nhân");
        }

        applyPatientUpdates(patient, request);
        User updated = userRepository.save(patient);
        log.info("Admin đã cập nhật hồ sơ Bệnh nhân #{}: {}", patientId, patient.getFullName());

        return mapToAuthResponse(updated, null);
    }

    @Transactional
    public AuthResponse updateMyProfile(String token, UpdatePatientRequest request) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("Token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtProvider.extractClaims(token).getSubject();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        applyPatientUpdates(user, request);
        User updated = userRepository.save(user);
        log.info("Bệnh nhân {} đã tự cập nhật hồ sơ và thẻ BHYT", email);

        return mapToAuthResponse(updated, token);
    }

    private void applyPatientUpdates(User user, UpdatePatientRequest req) {
        user.setFullName(req.getFullName().trim());
        if (req.getMedicalHistory() != null) {
            String cleanedHistory = req.getMedicalHistory().trim();
            user.setMedicalHistory(cleanedHistory.isEmpty() ? null : cleanedHistory);
        }
        if (req.getAllergies() != null) {
            String cleanedAllergies = req.getAllergies().trim();
            user.setAllergies(cleanedAllergies.isEmpty() ? null : cleanedAllergies);
        }
        if (req.getPhone() != null) {
            String cleanedPhone = req.getPhone().trim();
            user.setPhone(cleanedPhone.isEmpty() ? null : cleanedPhone);
        }

        if (req.getIdentityCardNumber() != null) {
            String cleanedCccd = req.getIdentityCardNumber().trim();
            if (!cleanedCccd.isEmpty() && !cleanedCccd.matches("^\\d{12}$")) {
                throw new IllegalArgumentException("Số CCCD / mã định danh phải gồm đúng 12 chữ số.");
            }
            user.setIdentityCardNumber(cleanedCccd.isEmpty() ? null : cleanedCccd);
        }

        if (req.getInsuranceCode() != null) {
            String cleanedBhyt = cleanInsuranceCode(req.getInsuranceCode());
            if (cleanedBhyt != null && !cleanedBhyt.matches("^[A-Z0-9]{15}$")) {
                throw new IllegalArgumentException("Mã số thẻ BHYT phải gồm đúng 15 ký tự chữ in hoa và số.");
            }
            user.setInsuranceCode(cleanedBhyt);
        }

        if (req.getInitialHospitalCode() != null) {
            String cleanedHospital = req.getInitialHospitalCode().trim();
            user.setInitialHospitalCode(cleanedHospital.isEmpty() ? null : cleanedHospital);
        }

        if (req.getDateOfBirth() != null) {
            user.setDateOfBirth(req.getDateOfBirth());
        }
        if (req.getGender() != null) {
            String cleanedGender = req.getGender().trim();
            user.setGender(cleanedGender.isEmpty() ? null : cleanedGender);
        }
        if (req.getAddress() != null) {
            String cleanedAddress = req.getAddress().trim();
            user.setAddress(cleanedAddress.isEmpty() ? null : cleanedAddress);
        }
        if (req.getValidFrom() != null) {
            user.setValidFrom(req.getValidFrom());
        }
        if (req.getValidUntil() != null) {
            if (req.getValidFrom() != null && req.getValidUntil().isBefore(req.getValidFrom())) {
                throw new IllegalArgumentException("Ngày hết hạn thẻ BHYT không được nhỏ hơn ngày bắt đầu có giá trị.");
            }
            user.setValidUntil(req.getValidUntil());
        }
        if (user.getInsuranceCode() == null || user.getInsuranceCode().isBlank()) {
            user.setIsOcrVerified(false);
        } else if (req.getIsOcrVerified() != null) {
            user.setIsOcrVerified(req.getIsOcrVerified());
        }
    }

    private String cleanInsuranceCode(String code) {
        if (code == null) return null;
        String cleaned = code.replaceAll("\\s+", "").toUpperCase().trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private AuthResponse mapToAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .active(user.getActive())
                .departmentId(user.getDepartmentId())
                .departmentName(user.getDepartmentName())
                .doctorId(user.getDoctorId())
                .pharmacistId(user.getPharmacistId())
                .identityCardNumber(user.getIdentityCardNumber())
                .insuranceCode(user.getInsuranceCode())
                .initialHospitalCode(user.getInitialHospitalCode())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .address(user.getAddress())
                .isOcrVerified(user.getIsOcrVerified())
                .validFrom(user.getValidFrom())
                .validUntil(user.getValidUntil())
                .createdAt(user.getCreatedAt())
                .medicalHistory(user.getMedicalHistory())
                .allergies(user.getAllergies())
                .build();
    }
    @Transactional(readOnly = true)
    public List<AuthResponse> getAllStaff() {
        List<UserRole> staffRoles = List.of(
                UserRole.ADMIN,
                UserRole.DOCTOR,
                UserRole.NURSE,
                UserRole.RECEPTIONIST,
                UserRole.PHARMACIST
        );
        return userRepository.findByRoleInOrderByCreatedAtDesc(staffRoles).stream()
                .map(user -> mapToAuthResponse(user, null))
                .toList();
    }
    @Transactional
    public AuthResponse toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));

        user.setActive(!Boolean.TRUE.equals(user.getActive()));
        User updated = userRepository.save(user);
        log.info("Admin đã thay đổi trạng thái user {} sang: active={}", user.getEmail(), updated.getActive());

        return mapToAuthResponse(updated, null);
    }

    @Transactional
    public void resetStaffPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));

        user.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(user);
        log.info("Admin đã reset mật khẩu tài khoản {} về 'password123'", user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<AuthResponse> getStaffDirectory() {
        List<UserRole> staffRoles = List.of(
                UserRole.DOCTOR,
                UserRole.NURSE,
                UserRole.PHARMACIST,
                UserRole.RECEPTIONIST,
                UserRole.ADMIN
        );
        return userRepository.findByRoleInOrderByCreatedAtDesc(staffRoles).stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .map(user -> mapToAuthResponse(user, null))
                .toList();
    }

    @Transactional
    public void deletePatientByAdmin(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân có ID: " + patientId));

        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException("Chỉ được phép xóa tài khoản Bệnh nhân.");
        }

        if (hasClinicalOrQueueRecords(patient)) {
            throw new IllegalStateException(
                    "Bệnh nhân đã có hồ sơ khám bệnh, không thể xóa vĩnh viễn. Vui lòng sử dụng chức năng Khóa tài khoản."
            );
        }

        userRepository.delete(patient);
        log.info("Admin đã xóa tài khoản Bệnh nhân #{}: {}", patientId, patient.getFullName());
    }

    private boolean hasClinicalOrQueueRecords(User patient) {
        try {
            List<Object> encounters = clinicalRestClient.get()
                    .uri("/patient/{patientId}", patient.getId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (encounters != null && !encounters.isEmpty()) {
                return true;
            }

            String cccd = patient.getIdentityCardNumber();
            if (hasQueueTickets(patient.getId(), cccd)) {
                return true;
            }

            String bhyt = patient.getInsuranceCode();
            if (bhyt != null && !bhyt.isBlank() && !bhyt.equals(cccd)) {
                return hasQueueTickets(patient.getId(), bhyt);
            }

            return false;
        } catch (Exception ex) {
            log.error("Không thể kiểm tra lịch sử khám của bệnh nhân #{}: {}", patient.getId(), ex.getMessage());
            throw new IllegalStateException(
                    "Không thể xác minh lịch sử khám bệnh từ PatientIntakeService / ClinicalConsultationService. Vui lòng thử lại sau."
            );
        }
    }

    private boolean hasQueueTickets(Long patientId, String identifier) {
        List<Object> tickets = intakeRestClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/patient/{patientId}/history");
                    if (identifier != null && !identifier.isBlank()) {
                        builder.queryParam("identifier", identifier.trim());
                    }
                    return builder.build(patientId);
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return tickets != null && !tickets.isEmpty();
    }

}