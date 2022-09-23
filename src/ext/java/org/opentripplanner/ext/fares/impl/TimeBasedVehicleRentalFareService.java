package org.opentripplanner.ext.fares.impl;

import java.io.Serializable;
import java.util.Currency;
import java.util.List;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.fares.FareService;

/**
 * This appears to be used in combination with transit using an AddingMultipleFareService.
 */
public class TimeBasedVehicleRentalFareService implements FareService, Serializable {

  private static final long serialVersionUID = 5226621661906177942L;

  // Each entry is <max time, cents at that time>; the list is sorted in
  // ascending time order
  private final List<P2<Integer>> pricing_by_second;

  private final Currency currency;

  protected TimeBasedVehicleRentalFareService(
    Currency currency,
    List<P2<Integer>> pricingBySecond
  ) {
    this.currency = currency;
    this.pricing_by_second = pricingBySecond;
  }

  @Override
  public ItineraryFares getCost(Itinerary itinerary) {
    var totalCost = itinerary
      .getLegs()
      .stream()
      .filter(Leg::getRentedVehicle)
      .mapToInt(this::getLegCost)
      .sum();

    ItineraryFares fare = ItineraryFares.empty();
    fare.addFare(FareType.regular, new Money(currency, totalCost));
    return fare;
  }

  private int getLegCost(Leg pathLeg) {
    int rideCost = 0;
    long rideTime = pathLeg.getDuration().toSeconds();
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
}
