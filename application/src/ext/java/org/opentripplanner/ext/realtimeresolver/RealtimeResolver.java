package org.opentripplanner.ext.realtimeresolver;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.ScheduledTransitLegBuilder;
import org.opentripplanner.transit.service.TransitService;

public class RealtimeResolver {

  private final TransitService transitService;

  public RealtimeResolver(TransitService transitService) {
    this.transitService = transitService;
  }

  /**
   * Loop through all itineraries and populate legs with real-time data using legReference from the original leg
   */
  public static List<Itinerary> populateLegsWithRealtime(
    List<Itinerary> itineraries,
    TransitService transitService
  ) {
    return new RealtimeResolver(transitService).addRealtimeInfo(itineraries);
  }

  private List<Itinerary> addRealtimeInfo(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::decorateItinerary).toList();
  }

  private Itinerary decorateItinerary(Itinerary it) {
    // TODO Skip if leg does not contain transit
    if (it.isFlaggedForDeletion()) {
      return it;
    }
    return it.copyOf().withLegs(legs -> legs.stream().map(this::mapLeg).toList()).build();
  }

  private Leg mapLeg(Leg leg) {
    var ref = leg.getLegReference();
    if (ref == null) {
      return leg;
    }

    // Only ScheduledTransitLeg has leg references atm, so this check is just to be future-proof
    if (!(leg.isScheduledTransitLeg())) {
      return leg;
    }
    var realTimeLeg = ref.getLeg(transitService);
    if (realTimeLeg == null) {
      return leg;
    }
    return combineReferenceWithOriginal(
      realTimeLeg.asScheduledTransitLeg(),
      leg.asScheduledTransitLeg()
    );
  }

  private static Leg combineReferenceWithOriginal(
    ScheduledTransitLeg reference,
    ScheduledTransitLeg original
  ) {
    return new ScheduledTransitLegBuilder<>(reference)
      .withTransferFromPreviousLeg(original.getTransferFromPrevLeg())
      .withTransferToNextLeg(original.getTransferToNextLeg())
      .withGeneralizedCost(original.getGeneralizedCost())
      .withAccessibilityScore(original.accessibilityScore())
      .build();
  }
}
