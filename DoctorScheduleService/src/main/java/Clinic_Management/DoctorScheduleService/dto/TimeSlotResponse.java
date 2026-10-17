package Clinic_Management.DoctorScheduleService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponse {
    private LocalTime startTime;
    private LocalTime endTime;
    private int totalCapacity;      // total cases that can be booked in this time slot
    private int bookedCount;        // booked cases in this time slot
    private int availableCapacity;  // available cases that can still be booked in this time slot
    @JsonProperty("isAvailable")
    private boolean isAvailable;    // check if the time slot is available for booking (availableCapacity > 0)
}