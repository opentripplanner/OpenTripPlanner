package org.opentripplanner.ext.digitransitemissions;

import java.io.Serializable;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.DoubleStream;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Sandbox
public class DigitransitEmissionsService implements Serializable, EmissionsService {

  private Map<String, DigitransitEmissions> emissions;
  private double carAvgEmissions;

  public DigitransitEmissionsService(
    Map<String, DigitransitEmissions> emissions,
    double carAvgEmissions
  ) {
    this.emissions = emissions;
    this.carAvgEmissions = carAvgEmissions;
  }

  @Override
  public Float getEmissionsForItinerary(Itinerary itinerary) {
    List<TransitLeg> transitLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(TransitLeg.class::cast)
      .toList();

    if (!transitLegs.isEmpty()) {
      Double emissions = getEmissionsForTransitItinerary(transitLegs);
      return emissions != null ? emissions.floatValue() : null;
    }

    List<StreetLeg> carLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof StreetLeg)
      .map(StreetLeg.class::cast)
      .filter(leg -> leg.getMode() == TraverseMode.CAR)
      .toList();

    if (!carLegs.isEmpty()) {
      return (float) getEmissionsForCarItinerary(carLegs);
    }
    return null;
  }

  private Double getEmissionsForTransitItinerary(List<TransitLeg> transitLegs) {
    DoubleStream emissionsStream = transitLegs
      .stream()
      .mapToDouble(leg -> {
        double legDistanceInKm = leg.getDistanceMeters() / 1000;
        String feedScopedRouteId =
          leg.getAgency().getId().getFeedId() + ":" + leg.getRoute().getId().getId();
        if (feedScopedRouteId != null && this.emissions.containsKey(feedScopedRouteId)) {
          return this.emissions.get(feedScopedRouteId).getEmissionsPerPassenger() * legDistanceInKm;
        }
        // Emissions value for the leg is missing
        return -1;
      });
    DoubleSummaryStatistics stats = emissionsStream.summaryStatistics();
    Double sum = stats.getSum();
    // Check that no emissions value is invalid and the result is a number.
    if (stats.getMin() < 0 || Double.isNaN(sum)) {
      return null;
    }
    return sum;
  }

  private double getEmissionsForCarItinerary(List<StreetLeg> carLegs) {
    return carLegs
      .stream()
      .mapToDouble(leg -> {
        double carLegDistanceInKm = leg.getDistanceMeters() / 1000;
        return (this.carAvgEmissions * carLegDistanceInKm);
      })
      .sum();
  }
}
