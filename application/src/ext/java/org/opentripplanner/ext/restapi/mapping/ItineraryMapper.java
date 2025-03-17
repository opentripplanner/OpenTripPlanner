package org.opentripplanner.ext.restapi.mapping;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.ext.restapi.model.ApiItinerary;
import org.opentripplanner.model.plan.Itinerary;

public class ItineraryMapper {

  private final LegMapper legMapper;
  private final FareMapper fareMapper;

  public ItineraryMapper(Locale locale, boolean addIntermediateStops) {
    this.legMapper = new LegMapper(locale, addIntermediateStops);
    this.fareMapper = new FareMapper(locale);
  }

  public List<ApiItinerary> mapItineraries(Collection<Itinerary> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(this::mapItinerary).collect(Collectors.toList());
  }

  public ApiItinerary mapItinerary(Itinerary domain) {
    if (domain == null) {
      return null;
    }
    ApiItinerary api = new ApiItinerary();
    api.duration = domain.totalDuration().toSeconds();
    api.startTime = GregorianCalendar.from(domain.startTime());
    api.endTime = GregorianCalendar.from(domain.endTime());
    api.walkTime = domain.totalStreetDuration().toSeconds();
    api.transitTime = domain.totalTransitDuration().toSeconds();
    api.waitingTime = domain.totalWaitingDuration().toSeconds();
    api.walkDistance = domain.totalStreetDistanceMeters();
    // We list only the generalizedCostIncludingPenalty, this is the least confusing. We intend to
    // delete this endpoint soon, so we will not make the proper change and add the
    // generalizedCostIncludingPenalty to the response and update the debug client to show it.
    api.generalizedCost = domain.generalizedCostIncludingPenalty().toSeconds();
    api.elevationLost = domain.totalElevationLost();
    api.elevationGained = domain.totalElevationGained();
    api.transfers = domain.numberOfTransfers();
    api.tooSloped = domain.isTooSloped();
    api.arrivedAtDestinationWithRentedBicycle = domain.isArrivedAtDestinationWithRentedVehicle();
    api.fare = fareMapper.mapFare(domain);
    api.legs = legMapper.mapLegs(domain.legs());
    api.systemNotices = SystemNoticeMapper.mapSystemNotices(domain.systemNotices());
    api.accessibilityScore = domain.accessibilityScore();

    return api;
  }
}
