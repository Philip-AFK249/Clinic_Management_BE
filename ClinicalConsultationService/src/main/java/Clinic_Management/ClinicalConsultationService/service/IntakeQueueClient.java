package Clinic_Management.ClinicalConsultationService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IntakeQueueClient {

    private final RestClient restClient;

    public IntakeQueueClient(@Value("${services.patient-intake.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
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
            System.err.println("Không thể đồng bộ trạng thái ticket sang PatientIntakeService: " + ex.getMessage());
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
                    .body(java.util.Map.of(
                            "icd10Code", icd10Code != null ? icd10Code : "",
                            "diseaseName", diseaseName != null ? diseaseName : "",
                            "doctorName", doctorName != null ? doctorName : ""
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            System.err.println("Không thể đồng bộ chẩn đoán xác nhận sang PatientIntakeService: " + ex.getMessage());
        }
    }

}