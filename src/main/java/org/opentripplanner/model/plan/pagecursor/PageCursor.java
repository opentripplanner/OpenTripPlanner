package org.opentripplanner.model.plan.pagecursor;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.plan.SortOrder;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * This class holds all the information needed to page to the next/previous page. It is
 * serialized as base64 when passed on to the client. The base64 encoding is done to prevent the
 * client from using the information inside the cursor.
 * <p>
 * The PageCursor class is internal to the router, only the serialized string is passed to/from the
 * clients.
 */
public class PageCursor {
    public final PageType type;
    public final SortOrder originalSortOrder;
    public final Instant earliestDepartureTime;
    public final Instant latestArrivalTime;
    public final Duration searchWindow;

    PageCursor(
            PageType type,
            SortOrder originalSortOrder,
            Instant earliestDepartureTime,
            Instant latestArrivalTime,
            Duration searchWindow
    ) {
        this.type = type;
        this.searchWindow = searchWindow;
        this.earliestDepartureTime = earliestDepartureTime;
        this.latestArrivalTime = latestArrivalTime;
        this.originalSortOrder = originalSortOrder;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(PageCursor.class)
                .addEnum("type", type)
                .addEnum("sortOrder", originalSortOrder)
                .addTime("edt", earliestDepartureTime)
                .addTime("lat", latestArrivalTime)
                .addDuration("searchWindow", searchWindow)
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
