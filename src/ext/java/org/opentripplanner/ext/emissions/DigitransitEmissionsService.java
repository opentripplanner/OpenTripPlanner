package org.opentripplanner.ext.emissions;

import java.util.HashMap;
import java.util.List;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.street.search.TraverseMode;

@Sandbox
public class DigitransitEmissionsService implements EmissionsService {

  private HashMap<String, DigitransitEmissionsAgency> emissionByAgency;

  public DigitransitEmissionsService(DigitransitEmissions[] emissions) {
    this.emissionByAgency = new HashMap<>();
    for (DigitransitEmissions e : emissions) {
      DigitransitEmissionsMode mode = new DigitransitEmissionsMode(
        e.getMode(),
        e.getAvg(),
        e.getP_avg()
      );
      if (!this.emissionByAgency.containsKey(e.getAgency_id())) {
        this.emissionByAgency.put(
            e.getAgency_id(),
            new DigitransitEmissionsAgency(e.getDb(), e.getAgency_id(), e.getAgency_name())
          );
      }
      this.emissionByAgency.get(e.getAgency_id()).addMode(mode);
    }
  }

  @Override
  public HashMap<String, DigitransitEmissionsAgency> getEmissionByAgency() {
    return emissionByAgency;
  }

  public DigitransitEmissionsAgency getEmissionsByAgencyId(String agencyId) {
    if (agencyId != null && this.emissionByAgency.containsKey(agencyId)) {
      return this.emissionByAgency.get(agencyId);
    }
    return null;
  }

  public Float getEmissionsForRoute(Itinerary itinerary) {
    List<TransitLeg> transitLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(TransitLeg.class::cast)
      .toList();

    if (!transitLegs.isEmpty()) {
      return (float) getEmissionsForTransitRoute(transitLegs);
    }

    List<StreetLeg> carLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof StreetLeg)
      .map(StreetLeg.class::cast)
      .filter(leg -> leg.getMode() == TraverseMode.CAR)
      .toList();

    if (!carLegs.isEmpty()) {
      return (float) getEmissionsForCarRoute(carLegs);
    }
    return null;
  }

  private double getEmissionsForTransitRoute(List<TransitLeg> transitLegs) {
    return transitLegs
      .stream()
      .mapToDouble(leg -> {
        double legDistanceInKm = leg.getDistanceMeters() / 1000;
        DigitransitEmissionsAgency digitransitEmissionsAgency = getEmissionsByAgencyId(
          leg.getAgency().getId().getId()
        );
        return digitransitEmissionsAgency != null
          ? digitransitEmissionsAgency.getAverageCo2EmissionsByModeAndDistancePerPerson(
            leg.getMode().name(),
            legDistanceInKm
          )
          : -1;
      })
      .sum();
  }

  private double getEmissionsForCarRoute(List<StreetLeg> carLegs) {
    return carLegs
      .stream()
      .mapToDouble(leg -> {
        DigitransitEmissionsAgency digitransitEmissionsAgency = getEmissionsByAgencyId(
          TraverseMode.CAR.toString()
        );
        double carLegDistanceInKm = leg.getDistanceMeters() / 1000;
        return digitransitEmissionsAgency != null
          ? digitransitEmissionsAgency.getAverageCo2EmissionsByModeAndDistancePerPerson(
            leg.getMode().name(),
            carLegDistanceInKm
          )
          : -1;
      })
      .sum();
  }
}
