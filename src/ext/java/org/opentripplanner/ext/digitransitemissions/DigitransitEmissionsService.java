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

  public DigitransitEmissionsService(Map<String, DigitransitEmissions> emissions) {
    this.emissions = emissions;
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
      float emis = (float) getEmissionsForCarItinerary(carLegs);
      return emis;
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
        String key = feedScopedAgencyId + ":" + modeName;

        if (key != null && this.emissions.containsKey(key)) {
          return this.emissions.get(key).getEmissionsPerPassenger() * legDistanceInKm;
        }
        return -1;
      });
    DoubleSummaryStatistics stats = emissionsStream.summaryStatistics();
    if (stats.getMin() < 0) {
      return -1;
    }
    Double sum = stats.getSum();

    return sum;
  }

  private double getEmissionsForCarItinerary(List<StreetLeg> carLegs) {
    if (this.emissions.containsKey(TraverseMode.CAR.toString())) {
      double emis = carLegs
        .stream()
        .mapToDouble(leg -> {
          double carLegDistanceInKm = leg.getDistanceMeters() / 1000;
          return (
            this.emissions.get(TraverseMode.CAR.toString()).getEmissionsPerPassenger() *
            carLegDistanceInKm
          );
        })
        .sum();
      return emis;
    }
    return -1;
  }
}
