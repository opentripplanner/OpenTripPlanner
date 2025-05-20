package org.opentripplanner.model.plan.paging.cursor;

import java.time.Duration;

public class PageCursorConstants {

  /**
   * The search-window start and end is [inclusive, exclusive], so to calculate the start of the
   * search-window from the last time included in the search window we need to include one extra
   * minute at the end.
   * <p>
   * The value is set to 60 seconds because raptor operates in one minute increments.
   */
  public static final Duration SEARCH_WINDOW_END_EXCLUSIVITY_TIME_ADDITION = Duration.ofSeconds(60);
}
