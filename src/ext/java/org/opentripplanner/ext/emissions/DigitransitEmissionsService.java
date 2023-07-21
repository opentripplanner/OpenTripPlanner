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
import org.opentripplanner.transit.model.framework.FeedScopedId;

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

  public float getEmissionsForRoute(Itinerary itinerary) {
    List<TransitLeg> transitLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(TransitLeg.class::cast)
      .toList();

    if (!transitLegs.isEmpty()) {
      return getEmissionsForTransitRoute(transitLegs);
    }

    List<StreetLeg> carLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof StreetLeg)
      .map(StreetLeg.class::cast)
      .filter(leg -> leg.getMode() == TraverseMode.CAR)
      .toList();

    if (!carLegs.isEmpty()) {
      return getEmissionsForCarRoute(carLegs);
    }

    return 0.0F;
  }

  private float getEmissionsForTransitRoute(List<TransitLeg> transitLegs) {
    return (float) transitLegs
      .stream()
      .mapToDouble(leg -> {
        FeedScopedId agencyIdFeedScoped = leg.getAgency().getId();
        double legDistanceInKm = leg.getDistanceMeters() / 1000;
        DigitransitEmissionsAgency digitransitEmissionsAgency = getEmissionsByAgencyId(
          agencyIdFeedScoped.getId()
        );
        float emissionsForAgencyPerKm = digitransitEmissionsAgency != null
          ? digitransitEmissionsAgency.getAverageCo2EmissionsByModePerPerson(leg.getMode().name())
          : 0;
        double emissionsPerLeg = legDistanceInKm * emissionsForAgencyPerKm;
        return emissionsPerLeg;
      })
      .sum();
  }

  private float getEmissionsForCarRoute(List<StreetLeg> carLegs) {
    return (float) carLegs
      .stream()
      .mapToDouble(leg -> {
        DigitransitEmissionsAgency digitransitEmissionsAgency = getEmissionsByAgencyId(
          TraverseMode.CAR.toString()
        );
        float avgCarEmissions = digitransitEmissionsAgency.getAverageCo2EmissionsByModePerPerson(
          leg.getMode().name()
        );
        double carLegDistanceInKm = leg.getDistanceMeters() / 1000;
        double carEmissionsPerLeg = carLegDistanceInKm * avgCarEmissions;
        return carEmissionsPerLeg;
      })
      .sum();
  }
}
