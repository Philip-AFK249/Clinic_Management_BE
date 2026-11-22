package Clinic_Management.AuthService.config;
import Clinic_Management.AuthService.entity.User;
import Clinic_Management.AuthService.entity.UserRole;
import Clinic_Management.AuthService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("admin@smartclinic.vn")) {
            log.info(">>> [DataInitializer] Tài khoản hệ thống đã tồn tại, bỏ qua seed data.");
            return;
        }

        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        List<User> seedUsers = List.of(

                User.builder()
                        .email("admin@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Quản trị viên Hệ thống")
                        .phone("0901000001")
                        .role(UserRole.ADMIN)
                        .active(true)
                        .build(),


                User.builder()
                        .email("tuan.tran@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("PGS. TS. BS. Trần Minh Tuấn")
                        .phone("0902000001")
                        .role(UserRole.DOCTOR)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .doctorId(1L)
                        .active(true)
                        .build(),
                // ID 3 -> Doctor ID 2 (Khoa 1 - Nội tổng quát & Tim mạch)
                User.builder()
                        .email("dung.nguyen@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("BS. CKI. Nguyễn Văn Dũng")
                        .phone("0902000002")
                        .role(UserRole.DOCTOR)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .doctorId(2L)
                        .active(true)
                        .build(),
                // ID 4 -> Doctor ID 3 (Khoa 1 - Nội tổng quát & Tim mạch)
                User.builder()
                        .email("bao.pham@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("ThS. BS. Phạm Quốc Bảo")
                        .phone("0902000003")
                        .role(UserRole.DOCTOR)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .doctorId(3L)
                        .active(true)
                        .build(),

                // ID 5 -> Doctor ID 4 (Khoa 2 - Hô hấp & Dị ứng)
                User.builder()
                        .email("yen.le@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("BS. CKI. Lê Thị Hoàng Yến")
                        .phone("0902000004")
                        .role(UserRole.DOCTOR)
                        .departmentId(2L)
                        .departmentName("Khối Ngoại")
                        .doctorId(4L)
                        .active(true)
                        .build(),
                // ID 6 -> Doctor ID 5 (Khoa 2 - Hô hấp & Dị ứng)
                User.builder()
                        .email("hoa.pham@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("BS. CKII. Phạm Thị Hoa")
                        .phone("0902000005")
                        .role(UserRole.DOCTOR)
                        .departmentId(2L)
                        .departmentName("Khối Ngoại")
                        .doctorId(5L)
                        .active(true)
                        .build(),
                // ID 7 -> Doctor ID 6 (Khoa 2 - Hô hấp & Dị ứng)
                User.builder()
                        .email("nam.hoang@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("ThS. BS. Hoàng Hoài Nam")
                        .phone("0902000006")
                        .role(UserRole.DOCTOR)
                        .departmentId(2L)
                        .departmentName("Khối Ngoại")
                        .doctorId(6L)
                        .active(true)
                        .build(),

                // ID 8 -> Doctor ID 7 (Khoa 3 - Da liễu)
                User.builder()
                        .email("lan.nguyen@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("BS. CKI. Nguyễn Thị Lan")
                        .phone("0902000007")
                        .role(UserRole.DOCTOR)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .doctorId(7L)
                        .active(true)
                        .build(),
                // ID 9 -> Doctor ID 8 (Khoa 3 - Da liễu)
                User.builder()
                        .email("duc.vu@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("ThS. BS. Vũ Minh Đức")
                        .phone("0902000008")
                        .role(UserRole.DOCTOR)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .doctorId(8L)
                        .active(true)
                        .build(),
                // ID 10 -> Doctor ID 9 (Khoa 3 - Da liễu)
                User.builder()
                        .email("huong.mai@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("BS. Mai Thu Hương")
                        .phone("0902000009")
                        .role(UserRole.DOCTOR)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .doctorId(9L)
                        .active(true)
                        .build(),

                // ============================================================
                // user_id = 11 -> 19: 9 Điều dưỡng (Khớp nurses.id 1 -> 9)
                // ============================================================
                // ID 11 -> Nurse ID 1 (Phòng khám 101 - Nội)
                User.builder()
                        .email("hang.trinh@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Trịnh Thu Hằng")
                        .phone("0903000001")
                        .role(UserRole.NURSE)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .active(true)
                        .build(),
                // ID 12 -> Nurse ID 2 (Phòng khám 201 - Dị ứng)
                User.builder()
                        .email("mai.do@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Đỗ Phương Mai")
                        .phone("0903000002")
                        .role(UserRole.NURSE)
                        .departmentId(2L)
                        .departmentName("Khối Ngoại")
                        .active(true)
                        .build(),
                // ID 13 -> Nurse ID 3 (Phòng khám 205 - Da liễu)
                User.builder()
                        .email("ly.nguyen@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Nguyễn Thảo Ly")
                        .phone("0903000003")
                        .role(UserRole.NURSE)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .active(true)
                        .build(),
                // ID 14 -> Nurse ID 4 (Phòng Cấp cứu & Điện tim)
                User.builder()
                        .email("oanh.tran@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Trần Kim Oanh")
                        .phone("0903000004")
                        .role(UserRole.NURSE)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .active(true)
                        .build(),
                // ID 15 -> Nurse ID 5 (Buồng Test dị nguyên & Hô hấp ký)
                User.builder()
                        .email("thao.le@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Lê Phương Thảo")
                        .phone("0903000005")
                        .role(UserRole.NURSE)
                        .departmentId(2L)
                        .departmentName("Khối Ngoại")
                        .active(true)
                        .build(),
                // ID 16 -> Nurse ID 6 (Buồng Tiểu phẫu & Laser da)
                User.builder()
                        .email("nhi.pham@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Phạm Yến Nhi")
                        .phone("0903000006")
                        .role(UserRole.NURSE)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .active(true)
                        .build(),
                // ID 17 -> Nurse ID 7 (Quầy Tiếp đón Sảnh A)
                User.builder()
                        .email("linh.vu@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Vũ Thùy Linh")
                        .phone("0903000007")
                        .role(UserRole.NURSE)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .active(true)
                        .build(),
                // ID 18 -> Nurse ID 8 (Khu Kiosk Tự phục vụ)
                User.builder()
                        .email("ngoc.bui@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Bùi Ánh Ngọc")
                        .phone("0903000008")
                        .role(UserRole.NURSE)
                        .departmentId(2L)
                        .departmentName("Khối Ngoại")
                        .active(true)
                        .build(),
                // ID 19 -> Nurse ID 9 (Bàn Đo Dấu hiệu Sinh tồn)
                User.builder()
                        .email("tram.hoang@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("Y tá Hoàng Bích Trâm")
                        .phone("0903000009")
                        .role(UserRole.NURSE)
                        .departmentId(1L)
                        .departmentName("Khối Nội")
                        .active(true)
                        .build(),

                // ============================================================
                // user_id = 20 -> 25: 6 Dược sĩ (Khớp pharmacists.id 1 -> 6)
                // ============================================================
                // ID 20 -> Pharmacist ID 1 (Quầy Dược Lâm Sàng #1)
                User.builder()
                        .email("thao.dang@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("DS. Đặng Thu Thảo")
                        .phone("0904000001")
                        .role(UserRole.PHARMACIST)
                        .pharmacistId(1L)
                        .active(true)
                        .build(),
                // ID 21 -> Pharmacist ID 2 (Quầy Dược Lâm Sàng #2)
                User.builder()
                        .email("kien.nguyen@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("DS. Nguyễn Trung Kiên")
                        .phone("0904000002")
                        .role(UserRole.PHARMACIST)
                        .pharmacistId(2L)
                        .active(true)
                        .build(),
                // ID 22 -> Pharmacist ID 3 (Cửa phát thuốc BHYT #1)
                User.builder()
                        .email("ha.tran@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("DSCĐ. Trần Thanh Hà")
                        .phone("0904000003")
                        .role(UserRole.PHARMACIST)
                        .pharmacistId(3L)
                        .active(true)
                        .build(),
                // ID 23 -> Pharmacist ID 4 (Cửa phát thuốc BHYT #2)
                User.builder()
                        .email("long.le@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("DSCĐ. Lê Hoàng Long")
                        .phone("0904000004")
                        .role(UserRole.PHARMACIST)
                        .pharmacistId(4L)
                        .active(true)
                        .build(),
                // ID 24 -> Pharmacist ID 5 (Cửa phát thuốc Thu phí #1)
                User.builder()
                        .email("phuc.do@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("DSCĐ. Đỗ Hồng Phúc")
                        .phone("0904000005")
                        .role(UserRole.PHARMACIST)
                        .pharmacistId(5L)
                        .active(true)
                        .build(),
                // ID 25 -> Pharmacist ID 6 (Cửa phát thuốc Thu phí #2)
                User.builder()
                        .email("nhung.vu@smartclinic.vn")
                        .password(encodedPassword)
                        .fullName("DSCĐ. Vũ Cẩm Nhung")
                        .phone("0904000006")
                        .role(UserRole.PHARMACIST)
                        .pharmacistId(6L)
                        .active(true)
                        .build(),

                // ============================================================
                // user_id = 26: Tiếp đón viên (RECEPTIONIST)
                // ============================================================
                User.builder()
                        .email("reception@clinic.vn")
                        .password(encodedPassword)
                        .fullName("Nguyễn Thị Hồng Nhung")
                        .phone("0905000001")
                        .role(UserRole.RECEPTIONIST)
                        .active(true)
                        .build(),

                // ============================================================
                // user_id = 27: Bệnh nhân mẫu (PATIENT)
                // ============================================================
                User.builder()
                        .email("patient@clinic.vn")
                        .password(encodedPassword)
                        .fullName("Nguyễn Văn An")
                        .phone("0912345678")
                        .role(UserRole.PATIENT)
                        .active(true)
                        .build()
        );

        userRepository.saveAll(seedUsers);
        log.info(">>> Đã khởi tạo tự động {} tài khoản chuẩn khớp với bảng doctors, nurses, pharmacists!", seedUsers.size());
    }
}