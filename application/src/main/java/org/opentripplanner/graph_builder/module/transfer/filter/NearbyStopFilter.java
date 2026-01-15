package org.opentripplanner.graph_builder.module.transfer.filter;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.graphfinder.NearbyStop;

/**
 * Filters nearby stops for transfer generation based on trip patterns and flex trips.
 * Returns only stops where boarding or alighting is possible, and for each pattern/trip,
 * returns only the closest stop to minimize transfer generation.
 */
interface NearbyStopFilter {
  /**
   * Checks if a stop should be included as a transfer origin/destination.
   *
   * @param id the stop-location to check
   * @param reverseDirection true for arrival searches (checks boarding), false for departure
   *                        searches (checks alighting)
   * @return true if the stop has relevant trip patterns or flex trips
   */
  boolean includeFromStop(FeedScopedId id, boolean reverseDirection);

  /**
   * Filters nearby stops to find optimal transfer points. For each trip pattern or flex trip,
   * returns only the closest stop.
   * <p>
   * Note: The result will include the origin stop if it is a StopVertex. This is intentional - we
   * don't want to return the next stop down the line for patterns passing through the origin.
   *
   * @param nearbyStops the stops to filter, typically from a street search
   * @param reverseDirection true for arrival searches (filters by boarding), false for departure
   *                        searches (filters by alighting)
   * @return filtered stops, one per relevant trip pattern/flex trip
   */
  Collection<NearbyStop> filterToStops(
    Collection<NearbyStop> nearbyStops,
    boolean reverseDirection
  );
}
