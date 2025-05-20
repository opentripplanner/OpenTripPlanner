package org.opentripplanner.routing.algorithm.filterchain.filters.street;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;
import org.opentripplanner.street.search.TraverseMode;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (parkAndRideDurationRatio == 0)
 */
public class RemoveParkAndRideWithMostlyWalkingFilter implements RemoveItineraryFlagger {

  private final double parkAndRideDurationRatio;

  public RemoveParkAndRideWithMostlyWalkingFilter(double ratio) {
    this.parkAndRideDurationRatio = ratio;
  }

  @Override
  public String name() {
    return "park-and-ride-vs-walk-filter";
  }

  @Override
  public Predicate<Itinerary> shouldBeFlaggedForRemoval() {
    return itinerary -> {
      if (itinerary.hasTransit()) {
        return false;
      }

      double carDuration = itinerary
        .legs()
        .stream()
        .filter(StreetLeg.class::isInstance)
        .map(StreetLeg.class::cast)
        .filter(l -> l.getMode() == TraverseMode.CAR)
        .mapToDouble(l -> l.duration().toSeconds())
        .sum();
      double totalDuration = itinerary.totalDuration().toSeconds();

      return (carDuration != 0 && (carDuration / totalDuration) <= parkAndRideDurationRatio);
    };
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    if (itineraries.size() == 1) {
      return List.of();
    }

    return itineraries.stream().filter(shouldBeFlaggedForRemoval()).collect(Collectors.toList());
  }
}
