package org.opentripplanner.ext.carpooling;

import com.google.common.collect.ArrayListMultimap;
import java.util.Collection;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.site.AreaStop;

/**
 * The CarpoolingRepository interface allows for the management and retrieval of carpooling trips.
 */
public interface CarpoolingRepository {
  Collection<CarpoolTrip> getCarpoolTrips();
  void addCarpoolTrip(CarpoolTrip trip);
  CarpoolTrip getCarpoolTripByBoardingArea(AreaStop stop);
  CarpoolTrip getCarpoolTripByAlightingArea(AreaStop stop);

  ArrayListMultimap<StreetVertex, AreaStop> getBoardingAreasForVertex();
  ArrayListMultimap<StreetVertex, AreaStop> getAlightingAreasForVertex();
}
