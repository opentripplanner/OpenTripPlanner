package org.opentripplanner.ext.digitransitemissions;

import java.io.Serializable;
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
      return (float) getEmissionsForTransitItinerary(transitLegs);
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

  private double getEmissionsForTransitItinerary(List<TransitLeg> transitLegs) {
    DoubleStream emissionsStream = transitLegs
      .stream()
      .mapToDouble(leg -> {
        double legDistanceInKm = leg.getDistanceMeters() / 1000;
        FeedScopedId feedScopedAgencyId = leg.getAgency().getId();
        String modeName = leg.getMode().name();

        String key =
          feedScopedAgencyId +
          ":" +
          leg.getRoute().getId().getId() +
          ":" +
          leg.getRoute().getShortName() +
          ":" +
          modeName;

        if (key != null && this.emissions.containsKey(key)) {
          return this.emissions.get(key).getEmissionsPerPassenger() * legDistanceInKm;
        }
        return -1;
      });
    DoubleSummaryStatistics stats = emissionsStream.summaryStatistics();
    Double sum = stats.getSum();
    if (stats.getMin() < 0 || Double.isNaN(sum)) {
      return -1;
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
