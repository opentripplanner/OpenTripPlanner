package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.framework.collection.ListSection;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;

/**
 * This class is used to enforce the cut/limit between two pages. It removes potential duplicates
 * and keep missed itineraries. It uses information from the page cursor to determine which
 * itineraries are potential duplicates and missed ones.
 * <p>
 * Based on where the previous results were cropped the potential duplicates will appear either at
 * the top of the list, or the bottom. If the previous results were cropped at the top, then the
 * potential duplicates will appear at the bottom of the list. If the previous results were cropped
 * at the bottom, then the potential duplicates will appear at the top of the list.
 */
public class PagingFilter implements ItineraryDeletionFlagger {

  public static final String TAG = "paging-filter";

  private final ListSection deduplicateSection;
  private final ItinerarySortKey itineraryPageCut;
  private final Comparator<ItinerarySortKey> sortOrderComparator;

  public PagingFilter(
    SortOrder sortOrder,
    ListSection deduplicateSection,
    ItinerarySortKey itineraryPageCut
  ) {
    this.deduplicateSection = deduplicateSection;
    this.itineraryPageCut = itineraryPageCut;
    this.sortOrderComparator = SortOrderComparator.comparator(sortOrder);
  }

  @Override
  public String name() {
    return TAG;
  }

  private boolean sortsIntoDeduplicationAreaRelativeToRemovedItinerary(Itinerary itinerary) {
    return switch (deduplicateSection) {
      case HEAD -> sortOrderComparator.compare(itinerary, itineraryPageCut) <= 0;
      case TAIL -> sortOrderComparator.compare(itinerary, itineraryPageCut) >= 0;
    };
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    return itineraries
      .stream()
      .filter(this::sortsIntoDeduplicationAreaRelativeToRemovedItinerary)
      .collect(Collectors.toList());
  }
}
