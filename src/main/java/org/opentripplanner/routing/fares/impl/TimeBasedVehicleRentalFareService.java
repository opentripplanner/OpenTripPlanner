package org.opentripplanner.routing.fares.impl;

import java.io.Serializable;
import java.util.Currency;
import java.util.List;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.WrappedCurrency;
import org.opentripplanner.routing.fares.FareService;

/**
 * This appears to be used in combination with transit using an AddingMultipleFareService.
 */
public class TimeBasedVehicleRentalFareService implements FareService, Serializable {

    private static final long serialVersionUID = 5226621661906177942L;

    // Each entry is <max time, cents at that time>; the list is sorted in
    // ascending time order
    private List<P2<Integer>> pricing_by_second;

    private Currency currency;

    protected TimeBasedVehicleRentalFareService(Currency currency, List<P2<Integer>> pricingBySecond) {
        this.currency = currency;
        this.pricing_by_second = pricingBySecond;
    }

    private int getLegCost(Leg pathLeg) {
        int rideCost = 0;
        long rideTime = pathLeg.getDuration();
        for (P2<Integer> bracket : pricing_by_second) {
            int time = bracket.first;
            if (rideTime < time) {
                rideCost = bracket.second;
                // FIXME this break seems to exit at the first matching bracket rather than the last.
                break;
            }
        }
        return rideCost;
    }

    @Override
    public Fare getCost(Itinerary itinerary) {

        var totalCost = itinerary.legs.stream()
                .filter(l -> l.getRentedVehicle())
                .mapToInt(this::getLegCost).sum();

        Fare fare = new Fare();
        fare.addFare(FareType.regular, new WrappedCurrency(currency), totalCost);
        return fare;
    }
}
