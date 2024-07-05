package org.opentripplanner.model.plan.paging.cursor;

/**
 * Used to tell which way the paging is going, to the {@link #NEXT_PAGE} or to the {@link
 * #PREVIOUS_PAGE}.
 */
public enum PageType {
  /**
   * The previous page is used to prepend a new set of itineraries BEFORE the current result.
   * Depending on the sort order, the previous page may hold itineraries which depart/arrive after
   * or before the current result.
   */
  PREVIOUS_PAGE,

  /**
   * The next page is used to append a new set of itineraries AFTER the current result. Depending on
   * the sort order, the next page may hold itineraries which depart/arrive after or before the
   * current result.
   */
  NEXT_PAGE;

  public boolean isNext() {
    return this == NEXT_PAGE;
  }
}
