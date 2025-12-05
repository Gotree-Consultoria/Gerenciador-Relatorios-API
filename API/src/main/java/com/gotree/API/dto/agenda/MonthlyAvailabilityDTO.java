package com.gotree.API.dto.agenda;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MonthlyAvailabilityDTO {

        private LocalDate date;
        private boolean morningBusy;
        private boolean afternoonBusy;
        private boolean fullDayBusy;
}
