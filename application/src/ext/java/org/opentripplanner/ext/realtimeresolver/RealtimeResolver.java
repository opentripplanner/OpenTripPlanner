package org.opentripplanner.ext.realtimeresolver;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.leg.ScheduledTransitLegBuilder;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TransitService;

public class RealtimeResolver {

  private final TransitService transitService;
  private final TransitAlertService transitAlertService;

  public RealtimeResolver(TransitService transitService, TransitAlertService transitAlertService) {
    this.transitService = transitService;
    this.transitAlertService = transitAlertService;
  }

  /**
   * Loop through all itineraries and populate legs with real-time data using legReference from the original leg
   */
  public static List<Itinerary> populateLegsWithRealtime(
    List<Itinerary> itineraries,
    TransitService transitService,
    TransitAlertService transitAlertService
  ) {
    return new RealtimeResolver(transitService, transitAlertService).addRealtimeInfo(itineraries);
  }

  private List<Itinerary> addRealtimeInfo(List<Itinerary> itineraries) {
    return itineraries.stream().map(this::decorateItinerary).toList();
  }

  private Itinerary decorateItinerary(Itinerary it) {
    // TODO Skip if leg does not contain transit
    if (it.isFlaggedForDeletion()) {
      return it;
    }
    return it.copyOf().transformLegs(this::mapLeg).build();
  }

  private Leg mapLeg(Leg leg) {
    var ref = leg.legReference();
    if (ref == null) {
      return leg;
    }

    // Only ScheduledTransitLeg has leg references atm, so this check is just to be future-proof
    if (!(leg.isScheduledTransitLeg())) {
      return leg;
    }
    var realTimeLeg = ref.getLeg(transitService, transitAlertService);
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
      .withTransferFromPreviousLeg(original.transferFromPrevLeg())
      .withTransferToNextLeg(original.transferToNextLeg())
      .withGeneralizedCost(original.generalizedCost())
      .withAccessibilityScore(original.accessibilityScore())
      .build();
  }
}
