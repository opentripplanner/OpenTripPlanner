package org.opentripplanner.model.plan.pagecursor;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;

/**
 * This class hold all the information needed to page to the next/previous page. It is
 * serialized as base64 when passed on to the client. The base64 encoding is done to prevent the
 * client from using the information inside the cursor.
 * <p>
 * This class is internal to the router, only the serialized string is passed to/from the clients.
 */
public class PageCursor {
    public final Instant earliestDepartureTime;
    public final Instant latestArrivalTime;
    public final Duration searchWindow;
    public final boolean reverseFilteringDirection;

    PageCursor(
            Instant earliestDepartureTime,
            Instant latestArrivalTime,
            Duration searchWindow,
            boolean reverseFilteringDirection
    ) {
        this.earliestDepartureTime = earliestDepartureTime;
        this.latestArrivalTime = latestArrivalTime;
        this.searchWindow = searchWindow;
        this.reverseFilteringDirection = reverseFilteringDirection;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(PageCursor.class)
                .addTime("edt", earliestDepartureTime)
                .addTime("lat", latestArrivalTime)
                .addDuration("searchWindow", searchWindow)
                .addBoolIfTrue("reverseFilteringDirection", reverseFilteringDirection)
                .toString();
    }

    @Nullable
    public String encode() {
        return PageCursorSerializer.encode(this);
    }

    @Nullable
    public static PageCursor decode(String cursor) {
        return PageCursorSerializer.decode(cursor);
    }
}
