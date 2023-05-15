package org.opentripplanner.routing.api.request.preference;

import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * This enum controls the number of itineraries returned with debug information - itineraries
 * deleted by the itinerary filter chain. When listing all itineraries, including the deleted
 * ones, the filter-chain sometimes return too many itineraries for practical usage.
 */
public enum ItineraryFilterDebugProfile implements DocumentedEnum<ItineraryFilterDebugProfile> {
  OFF("By default, the debug itinerary filters is turned off."),
  LIST_ALL("List all itineraries, including all deleted itineraries."),
  LIMIT_TO_SEARCH_WINDOW(
    """
    Return all itineraries, including deleted ones, inside the actual search-window used
    (the requested search-window may differ)."""
  ),
  LIMIT_TO_NUM_OF_ITINERARIES(
    """
    Only return the requested number of itineraries, counting both actual and deleted ones.
    The top `numItineraries` using the request sort order is returned. This does not work
    with paging, itineraries after the limit, but inside the search-window are skipped when
    moving to the next page."""
  );

  private final String description;

  ItineraryFilterDebugProfile(String description) {
    this.description = description.stripIndent().trim();
  }

  public boolean debugEnabled() {
    return this != OFF;
  }

  /**
   * This method is used to map to the new enum type from the old boolean type, still
   * present in some APIs.
   */
  public static ItineraryFilterDebugProfile ofDebugEnabled(boolean enabled) {
    return enabled ? LIST_ALL : OFF;
  }

  @Override
  public String typeDescription() {
    return (
      """
      Enable this to attach a system notice to itineraries instead of removing them. This is very
      convenient when tuning the itinerary-filter-chain."""
    );
  }

  @Override
  public String enumValueDescription() {
    return description;
  }
}
