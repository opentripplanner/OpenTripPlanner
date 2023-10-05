package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.AIRPORT_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.AIRPORT_TO_CITY_CENTER_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_A_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_B_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.INSIDE_CITY_CENTER_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_ATTRIBUTE;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_ROUTE;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.SUBURB_STOP;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;

class DefaultFareServiceTest implements PlanTestConstants {

  private static final Money TEN_DOLLARS = Money.usDollars(10);
  private static final Money TWENTY_DOLLARS = Money.usDollars(20);

  @Test
  void noRules() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of());
    var itin = newItinerary(A, T11_00).bus(1, T11_05, T11_12, B).build();
    var fare = service.calculateFares(itin);
    assertNull(fare);
  }

  @Test
  void simpleZoneBasedFare() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(AIRPORT_TO_CITY_CENTER_SET));
    var itin = newItinerary(Place.forStop(AIRPORT_STOP), T11_00)
      .bus(1, T11_00, T11_12, Place.forStop(CITY_CENTER_A_STOP))
      .build();
    var fare = service.calculateFares(itin);
    assertNotNull(fare);

    var price = fare.getFare(FareType.regular);

    assertEquals(TEN_DOLLARS, price);

    var fp = fare.getItineraryProducts().get(0);
    assertEquals(TEN_DOLLARS, fp.price());
    assertEquals("F:regular", fp.id().toString());

    var lp = fare.legProductsFromComponents();
    assertEquals(1, lp.size());
    var product = lp.values().iterator().next().product();
    assertEquals(TEN_DOLLARS, product.price());

    // the leg products from the components and the "true" leg products are different collections
    assertTrue(fare.getLegProducts().isEmpty());
  }

  @Test
  void shouldNotCombineInterlinedLegs() {
    var service = new DefaultFareService();
    service.addFareRules(
      FareType.regular,
      List.of(AIRPORT_TO_CITY_CENTER_SET, INSIDE_CITY_CENTER_SET)
    );

    var itin = newItinerary(Place.forStop(AIRPORT_STOP), T11_00)
      .bus(1, T11_05, T11_12, Place.forStop(CITY_CENTER_A_STOP))
      .staySeatedBus(
        TransitModelForTest.route("123").build(),
        2,
        T11_12,
        T11_16,
        Place.forStop(CITY_CENTER_B_STOP)
      )
      .build();

    var fare = service.calculateFares(itin);
    assertNotNull(fare);

    var price = fare.getFare(FareType.regular);

    assertEquals(TWENTY_DOLLARS, price);

    assertTrue(fare.getLegProducts().isEmpty());

    var legProductsFromComponents = fare.legProductsFromComponents();

    var firstLeg = itin.getLegs().get(0);
    var products = List.copyOf(legProductsFromComponents.get(firstLeg));

    assertEquals(TEN_DOLLARS, products.get(0).product().price());

    var secondLeg = itin.getLegs().get(1);
    products = List.copyOf(legProductsFromComponents.get(secondLeg));
    assertEquals(TEN_DOLLARS, products.get(0).product().price());

    assertFalse(fare.getItineraryProducts().isEmpty());
    assertEquals(1, fare.getItineraryProducts().size());
    assertEquals(TWENTY_DOLLARS, fare.getItineraryProducts().get(0).price());
  }

  @Test
  void unknownLeg() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(AIRPORT_TO_CITY_CENTER_SET));

    var itin = newItinerary(Place.forStop(AIRPORT_STOP), T11_00)
      .bus(1, T11_00, T11_12, Place.forStop(CITY_CENTER_A_STOP))
      .bus(3, T11_20, T11_33, Place.forStop(SUBURB_STOP))
      .build();

    var fare = service.calculateFares(itin);
    assertNotNull(fare);

    var price = fare.getFare(FareType.regular);
    assertEquals(Money.usDollars(-0.01f), price);

    var components = fare.getComponents(FareType.regular);
    assertEquals(1, components.size());

    var component = components.get(0);
    assertEquals(AIRPORT_TO_CITY_CENTER_SET.getFareAttribute().getId(), component.fareId());
    assertEquals(TEN_DOLLARS, component.price());

    var firstBusLeg = itin.firstTransitLeg().get();
    assertEquals(List.of(firstBusLeg), component.legs());

    var legProductsFromComponent = fare.legProductsFromComponents();
    assertEquals(1, legProductsFromComponent.size());
  }

  @Test
  void multipleFeeds() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(AIRPORT_TO_CITY_CENTER_SET, OTHER_FEED_SET));
    var itin = newItinerary(Place.forStop(AIRPORT_STOP))
      .bus(1, T11_00, T11_05, Place.forStop(CITY_CENTER_A_STOP))
      .walk(10, Place.forStop(OTHER_FEED_STOP))
      .bus(OTHER_FEED_ROUTE, 2, T11_20, T11_32, Place.forStop(OTHER_FEED_STOP))
      .build();
    var result = service.calculateFares(itin);

    var resultComponents = result
      .getComponents(FareType.regular)
      .stream()
      .map(r -> r.fareId())
      .toList();

    var resultPrice = result.getFare(FareType.regular);

    assertEquals(
      List.of(AIRPORT_TO_CITY_CENTER_SET.getFareAttribute().getId(), OTHER_FEED_ATTRIBUTE.getId()),
      resultComponents
    );

    assertEquals(TWENTY_DOLLARS, resultPrice);
  }

  @Test
  void multipleFeedsWithTransfersWithinFeed() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(INSIDE_CITY_CENTER_SET, OTHER_FEED_SET));
    var itin = newItinerary(Place.forStop(OTHER_FEED_STOP))
      .bus(OTHER_FEED_ROUTE, 2, T11_00, T11_05, Place.forStop(OTHER_FEED_STOP))
      .walk(10, Place.forStop(CITY_CENTER_A_STOP))
      .bus(1, T11_00, T11_05, Place.forStop(CITY_CENTER_A_STOP))
      .walk(10, Place.forStop(OTHER_FEED_STOP))
      .bus(OTHER_FEED_ROUTE, 2, T11_20, T11_32, Place.forStop(OTHER_FEED_STOP))
      .build();
    var result = service.calculateFares(itin);

    var resultComponents = result
      .getComponents(FareType.regular)
      .stream()
      .map(r -> r.fareId())
      .toList();

    var resultPrice = result.getFare(FareType.regular);
    assertEquals(
      List.of(INSIDE_CITY_CENTER_SET.getFareAttribute().getId(), OTHER_FEED_ATTRIBUTE.getId()),
      resultComponents
    );

    assertEquals(TWENTY_DOLLARS, resultPrice);
  }

  @Test
  void multipleFeedsWithUnknownFareLegs() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(AIRPORT_TO_CITY_CENTER_SET, OTHER_FEED_SET));
    var itin = newItinerary(Place.forStop(AIRPORT_STOP))
      .bus(1, T11_00, T11_05, Place.forStop(OTHER_FEED_STOP))
      .walk(10, Place.forStop(OTHER_FEED_STOP))
      .bus(OTHER_FEED_ROUTE, 2, T11_20, T11_32, Place.forStop(OTHER_FEED_STOP))
      .build();
    var result = service.calculateFares(itin);
    var resultComponents = result
      .getComponents(FareType.regular)
      .stream()
      .map(r -> r.fareId())
      .toList();
    var resultPrice = result.getFare(FareType.regular);
    assertEquals(List.of(OTHER_FEED_ATTRIBUTE.getId()), resultComponents);
    assertEquals(Money.usDollars(-0.01f), resultPrice);
  }
}
