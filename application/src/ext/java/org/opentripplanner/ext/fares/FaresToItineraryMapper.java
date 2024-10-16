package org.opentripplanner.ext.fares;

import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;

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
      leg.setFareProducts(ListUtils.combine(itineraryFareUses, legUses));
      return leg;
    });
  }
}
