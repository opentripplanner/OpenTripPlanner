package org.opentripplanner.routing.fares.impl;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.fares.FareService;

public class AddingMultipleFareService implements FareService, Serializable {

    private static final long serialVersionUID = -5313317664330954335L;

    private List<FareService> subServices;

    protected AddingMultipleFareService(List<FareService> subServices) {
        this.subServices = subServices;
    }

    @Override
    public Fare getCost(Itinerary itinerary) {

        Fare fare = null;

        for (FareService subService : subServices) {
            Fare subFare = subService.getCost(itinerary);
            if (subFare == null) {
                // No fare, next one please
                continue;
            }
            if (fare == null) {
                // Pick first defined fare
                fare = new Fare(subFare);
            } else {
                // Merge subFare with existing fare
                // Must use a temporary as we need to keep fare clean during the loop on FareType
                Fare newFare = new Fare(fare);
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
                        newFare.addFare(fareType, cost.getCurrency(),
                                cost.getCents() + subCost.getCents());
                    } else if (cost == null && subCost != null) {
                        // Add new cost
                        // Note: this should not happen often: only if a fare
                        // did not compute a "regular" fare.
                        newFare.addFare(fareType, subCost.getCurrency(), subCost.getCents());
                    }
                }
                fare = newFare;
            }
        }

        // Can be null here if no sub-service has returned fare
        return fare;
    }
}
