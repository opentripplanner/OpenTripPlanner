package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.AIRPORT_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.AIRPORT_TO_CITY_CENTER_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_A_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_B_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.CITY_CENTER_C_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.FREE_TRANSFERS_IN_CITY_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.INSIDE_CITY_CENTER_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_ATTRIBUTE;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_ROUTE;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_SET;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.OTHER_FEED_STOP;
import static org.opentripplanner.ext.fares.impl.FareModelForTest.SUBURB_STOP;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.Money;

class DefaultFareServiceTest implements PlanTestConstants {

  private static final Money TEN_DOLLARS = Money.usDollars(10);

  @Test
  void noRules() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of());
    var itin = newItinerary(A, T11_00).bus(1, T11_05, T11_12, B).build();
    var fare = service.calculateFares(itin);
    assertEquals(ItineraryFares.empty(), fare);
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

    var legProducts = fare.getLegProducts().get(itin.getTransitLeg(0));
    assertEquals(
      "[FareProductUse[id=1d270201-412b-3b86-80f6-92ab144fa2e5, product=FareProduct{id: 'F:airport-to-city-center', name: 'regular', amount: $10.00}]]",
      legProducts.toString()
    );
  }

  @Test
  void applyToSeveralLegs() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of(FREE_TRANSFERS_IN_CITY_SET));
    var itin = newItinerary(Place.forStop(CITY_CENTER_A_STOP), T11_00)
      .bus(1, T11_00, T11_12, Place.forStop(CITY_CENTER_B_STOP))
      .bus(1, T11_16, T11_20, Place.forStop(CITY_CENTER_C_STOP))
      .build();

    var fare = service.calculateFares(itin);
    assertNotNull(fare);

    var legProducts = fare.getLegProducts();

    var firstLeg = itin.getTransitLeg(0);
    var secondLeg = itin.getTransitLeg(1);

    var firstProducts = legProducts.get(firstLeg);
    var secondProducts = legProducts.get(secondLeg);

    assertEquals(firstProducts, secondProducts);

    assertEquals(
      "[FareProductUse[id=ddbf1572-18bc-3724-8b64-e1c7d5c8b6c6, product=FareProduct{id: 'F:free-transfers', name: 'regular', amount: $20.00}]]",
      firstProducts.toString()
    );
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

    var legProducts = fare.getLegProducts();

    var firstLeg = itin.getLegs().getFirst();

    assertEquals(
      "[FareProductUse[id=ccadd1d3-f284-31a4-9d58-0a300198950f, product=FareProduct{id: 'F:airport-to-city-center', name: 'regular', amount: $10.00}]]",
      legProducts.get(firstLeg).toString()
    );

    var secondLeg = itin.getLegs().get(1);
    assertEquals(
      "[FareProductUse[id=c58974dd-9a2f-3f42-90ec-c62a7b0dfd51, product=FareProduct{id: 'F:airport-to-city-center', name: 'regular', amount: $10.00}]]",
      legProducts.get(secondLeg).toString()
    );
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

    var fareProducts = List.copyOf(fare.getLegProducts().values());
    assertEquals(1, fareProducts.size());

    var fp = fareProducts.get(0).product();
    assertEquals(AIRPORT_TO_CITY_CENTER_SET.getFareAttribute().getId(), fp.id());
    assertEquals(TEN_DOLLARS, fp.price());

    var firstBusLeg = itin.firstTransitLeg().get();
    //assertEquals(List.of(firstBusLeg), fp.legs());

    var legProducts = fare.getLegProducts();
    assertEquals(1, legProducts.size());
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

    var fareProductIds = result
      .getLegProducts()
      .values()
      .stream()
      .map(r -> r.product().id())
      .toList();

    assertEquals(
      List.of(AIRPORT_TO_CITY_CENTER_SET.getFareAttribute().getId(), OTHER_FEED_ATTRIBUTE.getId()),
      fareProductIds
    );

    var legProducts = result.getLegProducts();
    var firstBusLeg = itin.getTransitLeg(0);
    assertEquals(
      "[FareProductUse[id=1d270201-412b-3b86-80f6-92ab144fa2e5, product=FareProduct{id: 'F:airport-to-city-center', name: 'regular', amount: $10.00}]]",
      legProducts.get(firstBusLeg).toString()
    );
    var secondBusLeg = itin.getTransitLeg(2);
    assertEquals(
      "[FareProductUse[id=678d201c-e839-35c3-ae7b-1bc3834da5e5, product=FareProduct{id: 'F2:other-feed-attribute', name: 'regular', amount: $10.00}]]",
      legProducts.get(secondBusLeg).toString()
    );
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

    var legProducts = result.getLegProducts();

    var firstBusLeg = itin.getTransitLeg(0);
    var secondBusLeg = itin.getTransitLeg(2);
    var finalBusLeg = itin.getTransitLeg(4);

    assertEquals(
      "[FareProductUse[id=5d0d58f4-b97a-38db-921c-8b5fc6392b54, product=FareProduct{id: 'F2:other-feed-attribute', name: 'regular', amount: $10.00}]]",
      legProducts.get(firstBusLeg).toString()
    );
    assertEquals(
      "[FareProductUse[id=1d270201-412b-3b86-80f6-92ab144fa2e5, product=FareProduct{id: 'F:airport-to-city-center', name: 'regular', amount: $10.00}]]",
      legProducts.get(secondBusLeg).toString()
    );
    assertEquals(
      "[FareProductUse[id=5d0d58f4-b97a-38db-921c-8b5fc6392b54, product=FareProduct{id: 'F2:other-feed-attribute', name: 'regular', amount: $10.00}]]",
      legProducts.get(finalBusLeg).toString()
    );
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
    var resultProductIds = result
      .getLegProducts()
      .values()
      .stream()
      .map(r -> r.product().id())
      .toList();
    assertEquals(List.of(OTHER_FEED_ATTRIBUTE.getId()), resultProductIds);
  }
}
