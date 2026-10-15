package Clinic_Management.AuthService.service;

import Clinic_Management.AuthService.dto.*;
import Clinic_Management.AuthService.entity.User;
import Clinic_Management.AuthService.entity.UserRole;
import Clinic_Management.AuthService.repository.UserRepository;
import Clinic_Management.AuthService.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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
        if (req.getPhone() != null) user.setPhone(req.getPhone().trim());
        if (req.getIdentityCardNumber() != null) user.setIdentityCardNumber(req.getIdentityCardNumber().trim());
        if (req.getInsuranceCode() != null) {
            user.setInsuranceCode(cleanInsuranceCode(req.getInsuranceCode()));
        }        if (req.getInitialHospitalCode() != null) user.setInitialHospitalCode(req.getInitialHospitalCode().trim());
        if (req.getDateOfBirth() != null) user.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getAddress() != null) user.setAddress(req.getAddress().trim());
        if (req.getIsOcrVerified() != null) user.setIsOcrVerified(req.getIsOcrVerified());
        if (req.getValidFrom() != null) user.setValidFrom(req.getValidFrom());
        if (req.getValidUntil() != null) user.setValidUntil(req.getValidUntil());
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

    @Transactional
    public void deletePatientByAdmin(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân có ID: " + patientId));

        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException("Chỉ được phép xóa tài khoản Bệnh nhân.");
        }

        userRepository.delete(patient);
        log.info("Admin đã xóa tài khoản Bệnh nhân #{}: {}", patientId, patient.getFullName());
    }
}