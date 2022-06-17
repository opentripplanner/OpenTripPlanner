package org.opentripplanner.api.mapping;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.opentripplanner.api.model.ApiItinerary;
import org.opentripplanner.model.plan.Itinerary;

public class ItineraryMapper {

  private final LegMapper legMapper;

  public ItineraryMapper(Locale locale, boolean addIntermediateStops) {
    this.legMapper = new LegMapper(locale, addIntermediateStops);
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

    api.duration = (long) domain.getDurationSeconds();
    api.startTime = GregorianCalendar.from(domain.startTime());
    api.endTime = GregorianCalendar.from(domain.endTime());
    api.walkTime = domain.getNonTransitTimeSeconds();
    api.transitTime = domain.getTransitTimeSeconds();
    api.waitingTime = domain.getWaitingTimeSeconds();
    api.walkDistance = domain.getNonTransitDistanceMeters();
    api.generalizedCost = domain.getGeneralizedCost();
    api.elevationLost = domain.getElevationLost();
    api.elevationGained = domain.getElevationGained();
    api.transfers = domain.getNumberOfTransfers();
    api.tooSloped = domain.isTooSloped();
    api.arrivedAtDestinationWithRentedBicycle = domain.isArrivedAtDestinationWithRentedVehicle();
    api.fare = domain.getFare();
    api.legs = legMapper.mapLegs(domain.getLegs());
    api.systemNotices = SystemNoticeMapper.mapSystemNotices(domain.getSystemNotices());
    api.accessibilityScore = domain.getAccessibilityScore();

    return api;
  }
}
