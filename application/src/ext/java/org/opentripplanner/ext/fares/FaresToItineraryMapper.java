package org.opentripplanner.ext.fares;

import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Takes fares and applies them to the legs of an itinerary.
 */
public class FaresToItineraryMapper {

  public static void addFaresToLegs(ItineraryFares fares, Itinerary i) {
    var itineraryFareUses = fares
      .getItineraryProducts()
      .stream()
      .map(fp -> {
        var instanceId = fp.uniqueInstanceId(i.firstLeg().getStartTime());
        return new FareProductUse(instanceId, fp);
      })
      .toList();

    i.transformTransitLegs(leg -> {
      var legUses = fares.getLegProducts().get(leg);
      var allUses = ListUtils.combine(itineraryFareUses, legUses);
      return switch (leg) {
        case ScheduledTransitLeg l -> l.copy().withFareProducts(allUses).build();
        case FlexibleTransitLeg l -> l.copy().withFareProducts(allUses).build();
        case Leg l -> leg;
      };
    });
  }
}
