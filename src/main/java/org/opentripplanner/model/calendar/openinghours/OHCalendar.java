package org.opentripplanner.model.calendar.openinghours;

import java.util.List;

public class OHCalendar {
    private final List<OpeningHours> openingHours;

    public OHCalendar(List<OpeningHours> openingHours) {
        // Create an immutable list
        this.openingHours = List.copyOf(openingHours);
    }
}
