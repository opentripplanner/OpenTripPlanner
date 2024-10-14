package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.ext.fares.impl.CombinedInterlinedLegsFareService.CombinationMode.ALWAYS;
import static org.opentripplanner.ext.fares.impl.CombinedInterlinedLegsFareService.CombinationMode.SAME_ROUTE;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.AIRPORT_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.AIRPORT_TO_CITY_CENTER_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_A_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_B_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.INSIDE_CITY_CENTER_SET;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.fares.impl.CombinedInterlinedLegsFareService.CombinationMode;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.network.Route;

class CombinedInterlinedLegsFareServiceTest implements PlanTestConstants {

  static final Route route = TransitModelForTest.route("route-1").build();
  static final Itinerary interlinedWithDifferentRoute = newItinerary(
    Place.forStop(AIRPORT_STOP),
    T11_00
  )
    .bus(1, T11_05, T11_12, Place.forStop(CITY_CENTER_A_STOP))
    .staySeatedBus(route, 2, T11_12, T11_16, Place.forStop(CITY_CENTER_B_STOP))
    .build();

  static final Itinerary interlinedWithSameRoute = newItinerary(Place.forStop(AIRPORT_STOP), T11_00)
    .bus(route, 1, T11_05, T11_12, Place.forStop(CITY_CENTER_A_STOP))
    .staySeatedBus(route, 2, T11_12, T11_16, Place.forStop(CITY_CENTER_B_STOP))
    .build();
  static Money tenDollars = Money.usDollars(10);
  static Money twentyDollars = Money.usDollars(20);

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(ALWAYS, interlinedWithSameRoute, tenDollars, "same routes"),
      Arguments.of(ALWAYS, interlinedWithDifferentRoute, tenDollars, "different routes"),
      Arguments.of(SAME_ROUTE, interlinedWithSameRoute, tenDollars, "same routes"),
      Arguments.of(SAME_ROUTE, interlinedWithDifferentRoute, twentyDollars, "different routes")
    );
  }

  @ParameterizedTest(
    name = "Itinerary with {3} and combination mode {0} should lead to a fare of {2}"
  )
  @MethodSource("testCases")
  void modes(CombinationMode mode, Itinerary itinerary, Money totalPrice, String hint) {
    var service = new CombinedInterlinedLegsFareService(mode);
    service.addFareRules(
      FareType.regular,
      List.of(AIRPORT_TO_CITY_CENTER_SET, INSIDE_CITY_CENTER_SET)
    );

    var fare = service.calculateFares(itinerary);
    assertNotNull(fare);

    var firstLeg = itinerary.getTransitLeg(0);
    var uses = fare.getLegProducts().get(firstLeg);
    assertEquals(1, uses.size());

    var secondLeg = itinerary.getTransitLeg(1);
    uses = fare.getLegProducts().get(secondLeg);
    assertEquals(1, uses.size());

    var sum = fare
      .getLegProducts()
      .values()
      .stream()
      .distinct()
      .map(p -> p.product().price())
      .reduce(Money.ZERO_USD, Money::plus);
    assertEquals(totalPrice, sum);
  }

  @Test
  void legFares() {
    var itinerary = interlinedWithSameRoute;
    var service = new CombinedInterlinedLegsFareService(ALWAYS);
    service.addFareRules(
      FareType.regular,
      List.of(AIRPORT_TO_CITY_CENTER_SET, INSIDE_CITY_CENTER_SET)
    );

    var fare = service.calculateFares(itinerary);

    var firstLeg = itinerary.getTransitLeg(0);
    var uses = List.copyOf(fare.getLegProducts().get(firstLeg));
    assertEquals(1, uses.size());

    var firstLegUse = uses.getFirst();
    assertEquals(tenDollars, firstLegUse.product().price());

    var secondLeg = itinerary.getTransitLeg(1);
    uses = List.copyOf(fare.getLegProducts().get(secondLeg));
    assertEquals(1, uses.size());

    var secondLegUse = uses.getFirst();
    assertEquals(tenDollars, secondLegUse.product().price());

    // the same far product is used for both legs as you only need to buy one
    assertEquals(secondLegUse, firstLegUse);
  }
}
