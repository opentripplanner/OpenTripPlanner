package org.opentripplanner.model.calendar.openinghours;

import java.time.Instant;

public class OHSearchContext {

    public OHSearchContext(Instant searchStartTime) {

    }


    public boolean isOpen(OHCalendar ctx, long epochSecond) {
        return true;
    }

    public boolean enterOk(OHCalendar ctx, long epochSecond) {
        return isOpen(ctx, epochSecond);
    }

    public boolean exitOk(OHCalendar ctx, long epochSecond) {
        return false;
    }
}
