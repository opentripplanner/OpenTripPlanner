package org.opentripplanner.ext.fares;

import com.google.common.collect.Multimap;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;

/**
 * Takes fares and applies them to the legs of an itinerary.
 */
public class FaresToItineraryMapper {

  public static void addFaresToLegs(ItineraryFares fares, Itinerary i) {
    var itineraryInstances = fares
      .getItineraryProducts()
      .stream()
      .map(fp -> {
        var instanceId = fp.uniqueInstanceId(i.firstLeg().getStartTime());
        return new FareProductUse(instanceId, fp);
      })
      .toList();

    final Multimap<Leg, FareProductUse> legProductsFromComponents = fares.legProductsFromComponents();

    i
      .getLegs()
      .stream()
      .filter(ScheduledTransitLeg.class::isInstance)
      .forEach(l -> {
        var legInstances = fares.getLegProducts().get(l);
        l.setFareProducts(
          ListUtils.combine(itineraryInstances, legProductsFromComponents.get(l), legInstances)
        );
      });
  }
}
