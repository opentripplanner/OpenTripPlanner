package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.LegMode;

/**
 * This is used to filter out bike rental itineraries that contain mostly walking. The value
 * describes the ratio of the total itinerary that has to consist of bike rental to allow the
 * itinerary.
 * <p>
 * This filter is turned off by default (parkAndRideDurationRatio == 0)
 */
public class RemoveParkAndRideWithMostlyWalkingFilter implements ItineraryDeletionFlagger {

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
      var containsTransit = itinerary
        .getLegs()
        .stream()
        .anyMatch(l -> l != null && l.getMode().isTransit());

      double carDuration = itinerary
        .getLegs()
        .stream()
        .filter(l -> l.getMode() == LegMode.CAR)
        .mapToDouble(l -> l.getDuration().toSeconds())
        .sum();
      double totalDuration = itinerary.getDuration().toSeconds();

      return (
        !containsTransit &&
        carDuration != 0 &&
        (carDuration / totalDuration) <= parkAndRideDurationRatio
      );
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
