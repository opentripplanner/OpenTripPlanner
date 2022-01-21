package org.opentripplanner.routing.algorithm.filterchain;


/**
 * OTP sort the returned itineraries according to the request {@code arriveBy} flag. The
 * paging may override the sort order.
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
    STREET_AND_DEPARTURE_TIME
}
