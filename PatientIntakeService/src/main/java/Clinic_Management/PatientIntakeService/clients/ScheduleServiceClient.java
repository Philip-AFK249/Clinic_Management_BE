package Clinic_Management.PatientIntakeService.clients;


import Clinic_Management.PatientIntakeService.dto.ScheduleAssignDoctorRequest;
import Clinic_Management.PatientIntakeService.dto.ScheduleAssignDoctorResponse;
import Clinic_Management.PatientIntakeService.dto.ScheduleTimeSlotResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Component
public class ScheduleServiceClient {

    private final RestClient restClient;

    public ScheduleServiceClient(@Value("${services.doctor-schedule.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<ScheduleTimeSlotResponse> getAvailableSlots(Long departmentId, LocalDate date) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/available-slots")
                        .queryParam("departmentId", departmentId)
                        .queryParam("date", date.toString())
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ScheduleAssignDoctorResponse assignDoctor(ScheduleAssignDoctorRequest request) {
        try {
            return restClient.post()
                    .uri("/assign-doctor")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ScheduleAssignDoctorResponse.class);
        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException ex) {
            throw new IllegalStateException("Lỗi điều phối lịch trực: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể kết nối đến DoctorScheduleService (:8081): " + ex.getMessage());
        }
    }
}