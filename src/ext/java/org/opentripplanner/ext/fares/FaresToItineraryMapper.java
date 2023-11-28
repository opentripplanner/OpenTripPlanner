package org.opentripplanner.ext.fares;

import com.google.common.collect.Multimap;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

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

    final Multimap<Leg, FareProductUse> legProductsFromComponents = fares.legProductsFromComponents();

    i.transformTransitLegs(leg -> {
      var legInstances = fares.getLegProducts().get(leg);
      leg.setFareProducts(
        ListUtils.combine(itineraryFareUses, legProductsFromComponents.get(leg), legInstances)
      );
      return leg;
    });
  }
}
