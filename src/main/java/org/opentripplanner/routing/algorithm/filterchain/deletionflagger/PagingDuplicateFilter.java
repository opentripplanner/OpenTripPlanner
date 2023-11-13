package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.pagecursor.PagingDeduplicationParameters;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;

/**
 * The PagingDuplicateFilter is used to remove potential duplicates when paging. It uses information
 * from the page cursor to determine which itineraries are potential duplicates.
 * <p>
 * Based on where the previous results were cropped the potential duplicates will appear either at
 * the top of the list, or the bottom. If the previous results were cropped at the top, then the
 * potential duplicates will appear at the bottom of the list. If the previous results were cropped
 * at the bottom, then the potential duplicates will appear at the top of the list.
 */
public class PagingDuplicateFilter implements ItineraryDeletionFlagger {

  public static final String TAG = "paging-deduplication-filter";

  private final PagingDeduplicationParameters deduplicationParams;
  private final Comparator<ItinerarySortKey> sortOrderComparator;

  public PagingDuplicateFilter(PagingDeduplicationParameters deduplicationParams) {
    this.deduplicationParams = deduplicationParams;
    this.sortOrderComparator = SortOrderComparator.comparator(deduplicationParams.sortOrder);
  }

  @Override
  public String name() {
    return TAG;
  }

  private boolean sortsIntoDeduplicationAreaRelativeToRemovedItinerary(Itinerary itinerary) {
    return switch (deduplicationParams.deduplicationSection) {
      case HEAD -> sortOrderComparator.compare(itinerary, deduplicationParams) < 0;
      case TAIL -> sortOrderComparator.compare(itinerary, deduplicationParams) > 0;
    };
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .filter(it ->
        (
          it.startTime().toInstant().isAfter(deduplicationParams.windowStart) &&
          it.startTime().toInstant().isBefore(deduplicationParams.windowEnd) &&
          sortsIntoDeduplicationAreaRelativeToRemovedItinerary(it)
        )
      )
      .collect(Collectors.toList());
  }
}
