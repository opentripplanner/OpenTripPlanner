package org.opentripplanner.model.plan.paging.cursor;

import java.time.Duration;
import java.time.Instant;
import javax.annotation.Nullable;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.utils.collection.ListSection;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class holds all the information needed to page to the next/previous page. It is serialized
 * as base64 when passed on to the client. The base64 encoding is done to prevent the client from
 * using the information inside the cursor.
 * <p>
 * The PageCursor class is internal to the router, only the serialized string is passed to/from the
 * clients.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE
 *
 * @param generalizedCostMaxLimit The cost limit is used to filter itineraries based on the
 *                                generalized-cost computed in the first page. This is resource
 *                                intensive to compute so we do not want to compute it again in
 *                                next/previous pages.
 */
public record PageCursor(
  PageType type,
  SortOrder originalSortOrder,
  @Nullable Instant earliestDepartureTime,
  @Nullable Instant latestArrivalTime,
  Duration searchWindow,
  @Nullable ItinerarySortKey itineraryPageCut,
  @Nullable Cost generalizedCostMaxLimit
) {
  public boolean containsItineraryPageCut() {
    return itineraryPageCut != null;
  }

  public boolean containsGeneralizedCostMaxLimit() {
    return generalizedCostMaxLimit != null;
  }

  @Nullable
  public String encode() {
    return PageCursorSerializer.encode(this);
  }

  /**
   * @throws IllegalArgumentException if cursor can not be decoded
   */
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
    return ToStringBuilder.of(PageCursor.class)
      .addEnum("type", type)
      .addEnum("sortOrder", originalSortOrder)
      .addDateTime("edt", earliestDepartureTime)
      .addDateTime("lat", latestArrivalTime)
      .addDuration("searchWindow", searchWindow)
      .addObj("generalizedCostMaxLimit", generalizedCostMaxLimit)
      // This will only include the sort vector, not everything else in the itinerary
      .addObjOp("itineraryPageCut", itineraryPageCut, ItinerarySortKey::keyAsString)
      .toString();
  }
}
