package org.opentripplanner.ext.realtimeresolver;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
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
        .map(l -> {
          var ref = l.getLegReference();
          if (ref == null) {
            return l;
          }

          var leg = ref.getLeg(transitService);
          if (leg != null) {
            return leg;
          }

          return l;
        })
        .collect(Collectors.toList());

      it.setLegs(legs);
    });
  }
}
