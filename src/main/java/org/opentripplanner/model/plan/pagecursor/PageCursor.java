package org.opentripplanner.model.plan.pagecursor;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.SortOrder;

/**
 * This class holds all the information needed to page to the next/previous page. It is serialized
 * as base64 when passed on to the client. The base64 encoding is done to prevent the client from
 * using the information inside the cursor.
 * <p>
 * The PageCursor class is internal to the router, only the serialized string is passed to/from the
 * clients.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE
 */
public class PageCursor {

  public final PageType type;
  public final SortOrder originalSortOrder;
  public final Instant earliestDepartureTime;
  public final Instant latestArrivalTime;
  public final Duration searchWindow;

  public ItineraryPageCut itineraryPageCut;

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

  public PageCursor withItineraryPageCut(ItineraryPageCut itineraryPageCut) {
    this.itineraryPageCut = itineraryPageCut;
    return this;
  }

  public boolean containsItineraryPageCut() {
    return itineraryPageCut != null;
  }

  @Nullable
  public String encode() {
    return PageCursorSerializer.encode(this);
  }

  @Nullable
  public static PageCursor decode(String cursor) {
    return PageCursorSerializer.decode(cursor);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(PageCursor.class)
      .addEnum("type", type)
      .addEnum("sortOrder", originalSortOrder)
      .addDateTime("edt", earliestDepartureTime)
      .addDateTime("lat", latestArrivalTime)
      .addDuration("searchWindow", searchWindow)
      .addObj("itineraryPageCut", itineraryPageCut)
      .toString();
  }
}
