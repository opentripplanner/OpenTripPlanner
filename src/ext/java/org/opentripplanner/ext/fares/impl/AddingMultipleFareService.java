package org.opentripplanner.ext.fares.impl;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.basic.Money;

public class AddingMultipleFareService implements FareService, Serializable {

  private final List<FareService> subServices;

  protected AddingMultipleFareService(List<FareService> subServices) {
    this.subServices = subServices;
  }

  @Override
  public ItineraryFares getCost(Itinerary itinerary) {
    ItineraryFares fare = null;

    for (FareService subService : subServices) {
      ItineraryFares subFare = subService.getCost(itinerary);
      if (subFare == null) {
        // No fare, next one please
        continue;
      }
      if (fare == null) {
        // Pick first defined fare
        fare = new ItineraryFares(subFare);
      } else {
        // Merge subFare with existing fare
        // Must use a temporary as we need to keep fare clean during the loop on FareType
        ItineraryFares newFare = new ItineraryFares(fare);
        for (FareType fareType : FareType.values()) {
          Money cost = fare.getFare(fareType);
          Money subCost = subFare.getFare(fareType);
          if (cost == null && subCost == null) {
            continue;
          }
          if (cost != null && subCost == null) {
            /*
             * If for a given fare type we have partial data, we try to pickup the
             * default "regular" cost to fill-in the missing information. For example,
             * adding a bike fare which define only a "regular" cost, with some transit
             * fare defining both "regular" and "student" costs. In that case, we
             * probably want the "regular" bike fare to be added to the "student"
             * transit fare too. Here we assume "regular" as a sane default value.
             */
            subCost = subFare.getFare(FareType.regular);
          } else if (cost == null && subCost != null) {
            /* Same, but the other way around. */
            cost = fare.getFare(FareType.regular);
          }

          if (cost != null && subCost != null) {
            // Add sub cost to cost
            newFare.addFare(fareType, new Money(cost.currency(), cost.cents() + subCost.cents()));
          } else if (cost == null && subCost != null) {
            // Add new cost
            // Note: this should not happen often: only if a fare
            // did not compute a "regular" fare.
            newFare.addFare(fareType, subCost);
          }
        }
        fare = newFare;
      }
    }

    // Can be null here if no sub-service has returned fare
    return fare;
  }
}
