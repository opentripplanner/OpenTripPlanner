package org.opentripplanner.model.plan;

/**
 * OTP sort the returned itineraries according to the request {@code arriveBy} flag. The paging may
 * override the sort order.
 */
public enum SortOrder {
  /**
   * Sort itineraries in order and break ties by:
   * <ol>
   *     <li>Street only - Street only itineraries are sorted before itineraries with transit
   *     <li>Arrival time - Earliest arrival time first
   *     <li>Generalized cost - Lowest cost first
   *     <li>Number of transfers - Lowest number of transfers first
   *     <li>Departure time - Latest departure time first
   * </ol>
   * This is the default for a depart-after search ({@code arriveBy=false}).
   */
  STREET_AND_ARRIVAL_TIME,

  /**
   * Sort itineraries in order and break ties by:
   * <ol>
   *     <li>Street only - Street only itineraries are sorted before itineraries with transit
   *     <li>Departure time - Latest departure time first
   *     <li>Generalized cost - Lowest cost first
   *     <li>Number of transfers - Lowest number of transfers first
   *     <li>Arrival time - Earliest arrival time first
   * </ol>
   * This is the default for an arrive-by search ({@code arriveBy=true}).
   */
  STREET_AND_DEPARTURE_TIME;

  /**
   * The itineraries are sorted by arrival time with the earliest arrival time first. When
   * paging we need to know which end of the list of itineraries we should crop. This method is used
   * to decide that together with the current page type (next/previous).
   * <p>
   * This returns {@code true} for the default depart-after search, and {@code false} for an
   * arrive-by search.
   */
  public boolean isSortedByAscendingArrivalTime() {
    return this == STREET_AND_ARRIVAL_TIME;
  }
}
