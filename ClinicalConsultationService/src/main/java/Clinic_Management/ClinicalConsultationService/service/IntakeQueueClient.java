package Clinic_Management.ClinicalConsultationService.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
public class IntakeQueueClient {

    private final RestClient restClient;

    public IntakeQueueClient(@Value("${services.patient-intake.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Completing an encounter must never hang on a slow intake service.
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }


    public void updateTicketStatus(String ticketNumber, String status) {
        try {
            restClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tickets/{ticketNumber}/status")
                            .queryParam("status", status)
                            .build(ticketNumber))
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Không thể đồng bộ trạng thái ticket {} sang PatientIntakeService: {}",
                    ticketNumber, ex.getMessage());
        }
    }

    public void syncConfirmedDiagnosis(
            String ticketNumber,
            String icd10Code,
            String diseaseName,
            String doctorName) {
        try {
            restClient.patch()
                    .uri("/tickets/{ticketNumber}/confirmed-diagnosis", ticketNumber)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "icd10Code", icd10Code != null ? icd10Code : "",
                            "diseaseName", diseaseName != null ? diseaseName : "",
                            "doctorName", doctorName != null ? doctorName : ""
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Không thể đồng bộ chẩn đoán xác nhận của ticket {} sang PatientIntakeService: {}",
                    ticketNumber, ex.getMessage());
        }
    }

}
