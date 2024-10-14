package org.opentripplanner.model.plan.paging.cursor;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.framework.collection.ListSection;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.plan.ItinerarySortKey;
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
public record PageCursor(
  PageType type,
  SortOrder originalSortOrder,
  Instant earliestDepartureTime,
  Instant latestArrivalTime,
  Duration searchWindow,
  @Nullable ItinerarySortKey itineraryPageCut
) {
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

  /**
   * When paging we must crop the list of itineraries in the right end according to the sorting of
   * the original search and according to the paging direction(next or previous).
   */
  public ListSection cropItinerariesAt() {
    // Depart after search
    if (originalSortOrder().isSortedByAscendingArrivalTime()) {
      return switch (type) {
        case NEXT_PAGE -> ListSection.TAIL;
        case PREVIOUS_PAGE -> ListSection.HEAD;
      };
    }
    // Arrive by search
    else {
      return switch (type) {
        case NEXT_PAGE -> ListSection.HEAD;
        case PREVIOUS_PAGE -> ListSection.TAIL;
      };
    }
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
      // This will only include the sort vector, not everything else in the itinerary
      .addObjOp("itineraryPageCut", itineraryPageCut, ItinerarySortKey::keyAsString)
      .toString();
  }
}
