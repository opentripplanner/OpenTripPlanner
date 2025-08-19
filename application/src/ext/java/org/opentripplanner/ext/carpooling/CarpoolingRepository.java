package org.opentripplanner.ext.carpooling;

import com.google.common.collect.ArrayListMultimap;
import java.util.Collection;
import java.util.Map;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * The CarpoolingRepository interface allows for the management and retrieval of carpooling trips.
 */
public interface CarpoolingRepository {
  /**
   * Get all available carpool trips for routing.
   */
  Collection<CarpoolTrip> getCarpoolTrips();

  /**
   * Add a carpool trip to the repository.
   */
  void addCarpoolTrip(CarpoolTrip trip);

  CarpoolTrip getCarpoolTripByBoardingArea(AreaStop stop);
  CarpoolTrip getCarpoolTripByAlightingArea(AreaStop stop);

  ArrayListMultimap<StreetVertex, AreaStop> getBoardingAreasForVertex();
  ArrayListMultimap<StreetVertex, AreaStop> getAlightingAreasForVertex();
}
