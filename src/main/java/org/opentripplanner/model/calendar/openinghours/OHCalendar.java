package org.opentripplanner.model.calendar.openinghours;

import java.time.ZoneId;
import java.util.List;

public class OHCalendar {
    private final ZoneId zoneId;
    private final List<OpeningHours> openingHours;

    public OHCalendar(ZoneId zoneId, List<OpeningHours> openingHours) {
        this.zoneId = zoneId;
        this.openingHours = openingHours;
    }
}
