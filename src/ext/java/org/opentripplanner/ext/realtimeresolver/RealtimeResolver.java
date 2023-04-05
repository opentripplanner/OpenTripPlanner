package org.opentripplanner.ext.realtimeresolver;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.transit.service.TransitService;

public class RealtimeResolver {

  /**
   * Loop through all itineraries and populate legs with realtime data using legReference from the original leg
   */
  public static void populateLegsWithRealtime(
    List<Itinerary> itineraries,
    TransitService transitService
  ) {
    itineraries.forEach(it -> {
      if (it.isFlaggedForDeletion()) {
        return;
      }
      var legs = it
        .getLegs()
        .stream()
        .map(leg -> {
          var ref = leg.getLegReference();
          if (ref == null) {
            return leg;
          }

          // Only ScheduledTransitLeg has leg references atm, so this check is just to be future-proof
          if (!(leg.isScheduledTransitLeg())) {
            return leg;
          }

          var realtimeLeg = ref.getLeg(transitService);
          if (realtimeLeg != null) {
            return combineReferenceWithOriginal(
              realtimeLeg.asScheduledTransitLeg(),
              leg.asScheduledTransitLeg()
            );
          }
          return leg;
        })
        .collect(Collectors.toList());

      it.setLegs(legs);
    });
  }

  private static Leg combineReferenceWithOriginal(
    ScheduledTransitLeg reference,
    ScheduledTransitLeg original
  ) {
    var leg = new ScheduledTransitLeg(
      reference.getTripTimes(),
      reference.getTripPattern(),
      reference.getBoardStopPosInPattern(),
      reference.getAlightStopPosInPattern(),
      reference.getStartTime(),
      reference.getEndTime(),
      reference.getServiceDate(),
      reference.getZoneId(),
      original.getTransferFromPrevLeg(),
      original.getTransferToNextLeg(),
      original.getGeneralizedCost(),
      original.accessibilityScore()
    );
    reference.getTransitAlerts().forEach(leg::addAlert);
    return leg;
  }
}
