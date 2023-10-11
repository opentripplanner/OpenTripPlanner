package org.opentripplanner.ext.digitransitemissions;

import jakarta.inject.Inject;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
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
public class DigitransitEmissionsService implements EmissionsService {

  private Map<FeedScopedId, Emissions> emissions;
  private double carAvgEmissions;

  public DigitransitEmissionsService() {}

  @Inject
  public DigitransitEmissionsService(EmissionsDataModel emissionsDataModel) {
    this.emissions = emissionsDataModel.getEmissions().get();
    this.carAvgEmissions = emissionsDataModel.getCarAvgCo2EmissionsPerMeter().get();
  }

  @Override
  public Double getEmissionsForItinerary(Itinerary itinerary) {
    List<TransitLeg> transitLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(TransitLeg.class::cast)
      .toList();

    if (!transitLegs.isEmpty()) {
      return getEmissionsForTransitItinerary(transitLegs);
    }

    List<StreetLeg> carLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof StreetLeg)
      .map(StreetLeg.class::cast)
      .filter(leg -> leg.getMode() == TraverseMode.CAR)
      .toList();

    if (!carLegs.isEmpty()) {
      return getEmissionsForCarItinerary(carLegs);
    }
    return null;
  }

  private Double getEmissionsForTransitItinerary(List<TransitLeg> transitLegs) {
    DoubleStream emissionsStream = transitLegs
      .stream()
      .mapToDouble(leg -> {
        double legDistanceInMeters = leg.getDistanceMeters();
        FeedScopedId feedScopedRouteId = new FeedScopedId(
          leg.getAgency().getId().getFeedId(),
          leg.getRoute().getId().getId()
        );
        if (feedScopedRouteId != null && this.emissions.containsKey(feedScopedRouteId)) {
          return (
            this.emissions.get(feedScopedRouteId).getEmissionsPerPassenger() * legDistanceInMeters
          );
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
        double carLegDistanceInMeters = leg.getDistanceMeters();
        return (this.carAvgEmissions * carLegDistanceInMeters);
      })
      .sum();
  }
}
