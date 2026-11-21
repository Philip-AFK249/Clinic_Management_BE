package Clinic_Management.PatientIntakeService.service;

import Clinic_Management.PatientIntakeService.entity.PaymentStatus;
import Clinic_Management.PatientIntakeService.entity.QueueStatus;
import Clinic_Management.PatientIntakeService.entity.QueueTicket;
import Clinic_Management.PatientIntakeService.repository.QueueTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class VietQrService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final QueueTicketRepository queueTicketRepository;

    @Value("${vietqr.bank-id:VCB}")
    private String bankId;

    @Value("${vietqr.account-no:1040466448}")
    private String accountNo;

    @Value("${vietqr.account-name:TRAN NGO VINH PHUC}")
    private String accountName;

    @Value("${vietqr.template:compact2}")
    private String template;

    @Value("${vietqr.deposit-amount:2000}")
    private BigDecimal defaultDepositAmount;

    @Value("${vietqr.hold-minutes:15}")
    private int holdMinutes;


    public BigDecimal getDefaultDepositAmount() {
        return defaultDepositAmount.setScale(2, RoundingMode.HALF_UP);
    }

    public int getHoldMinutes() {
        return holdMinutes;
    }

    /**
     * Sinh mã đối soát duy nhất dạng SC + 6 chữ số ngẫu nhiên (VD: SC482910)
     */
    public String generateUniquePaymentCode() {
        for (int i = 0; i < 20; i++) {
            int number = RANDOM.nextInt(1_000_000);
            String candidate = String.format("SC%06d", number);
            if (!queueTicketRepository.existsByPaymentCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Không thể khởi tạo mã thanh toán đối soát duy nhất. Vui lòng thử lại.");
    }

    /**
     * Tạo URL ảnh VietQR Quick Link chuẩn (không tốn phí API)
     */
    public String buildQrImageUrl(String paymentCode, BigDecimal amount) {
        if (paymentCode == null || paymentCode.isBlank() || amount == null) {
            return null;
        }
        long amountVnd = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        String encodedAddInfo = URLEncoder.encode(paymentCode.trim(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String encodedAccountName = URLEncoder.encode(accountName.trim(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                bankId.trim(),
                accountNo.trim(),
                template.trim(),
                amountVnd,
                encodedAddInfo,
                encodedAccountName
        );
    }

    public String resolveQrUrlForTicket(QueueTicket ticket) {
        if (ticket == null) {
            return null;
        }
        if (ticket.getStatus() != QueueStatus.PENDING_PAYMENT || ticket.getPaymentStatus() != PaymentStatus.UNPAID) {
            return null;
        }
        return buildQrImageUrl(ticket.getPaymentCode(), ticket.getDepositAmount());
    }
}