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
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Money;

class DefaultFareServiceTest implements PlanTestConstants {

  private static final Money TEN_DOLLARS = Money.usDollars(10);
  private static final FareProduct OTHER_FEED_PRODUCT = FareProduct.of(
    OTHER_FEED_ATTRIBUTE.getId(),
    "regular",
    TEN_DOLLARS
  ).build();
  private static final FareProduct AIRPORT_TO_CITY_CENTER_PRODUCT = FareProduct.of(
    AIRPORT_TO_CITY_CENTER_SET.getFareAttribute().getId(),
    "regular",
    TEN_DOLLARS
  ).build();

  @Test
  void noRules() {
    var service = new DefaultFareService();
    service.addFareRules(FareType.regular, List.of());
    var itin = newItinerary(A, T11_00).bus(1, T11_05, T11_12, B).build();
    var fare = service.calculateFares(itin);
    assertEquals(ItineraryFare.empty(), fare);
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

    var legProducts = fare.getLegProducts().get(itin.transitLeg(0));
    assertEquals(
      List.of(
        // reminder: the FareProductUse's id are a (non-random) UUID computed from the start time of the leg
        // plus some properties from the product itself like the id, price, rider category and medium
        new FareProductUse("1d270201-412b-3b86-80f6-92ab144fa2e5", AIRPORT_TO_CITY_CENTER_PRODUCT)
      ),
      legProducts
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

    var firstLeg = itin.transitLeg(0);
    var secondLeg = itin.transitLeg(1);

    var firstProducts = legProducts.get(firstLeg);
    var secondProducts = legProducts.get(secondLeg);

    assertEquals(firstProducts, secondProducts);
    assertEquals(
      List.of(
        new FareProductUse(
          "ddbf1572-18bc-3724-8b64-e1c7d5c8b6c6",
          FareProduct.of(
            FREE_TRANSFERS_IN_CITY_SET.getFareAttribute().getId(),
            "regular",
            TEN_DOLLARS.plus(TEN_DOLLARS)
          ).build()
        )
      ),
      firstProducts
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
        TimetableRepositoryForTest.route("123").build(),
        2,
        T11_12,
        T11_16,
        Place.forStop(CITY_CENTER_B_STOP)
      )
      .build();

    var fare = service.calculateFares(itin);
    assertNotNull(fare);

    var legProducts = fare.getLegProducts();

    var firstLeg = itin.legs().getFirst();
    var secondLeg = itin.legs().get(1);
    assertEquals(
      List.of(
        new FareProductUse("ccadd1d3-f284-31a4-9d58-0a300198950f", AIRPORT_TO_CITY_CENTER_PRODUCT)
      ),
      legProducts.get(firstLeg)
    );
    assertEquals(
      List.of(
        new FareProductUse("c58974dd-9a2f-3f42-90ec-c62a7b0dfd51", AIRPORT_TO_CITY_CENTER_PRODUCT)
      ),
      legProducts.get(secondLeg)
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
    var firstBusLeg = itin.transitLeg(0);
    assertEquals(
      List.of(
        new FareProductUse("1d270201-412b-3b86-80f6-92ab144fa2e5", AIRPORT_TO_CITY_CENTER_PRODUCT)
      ),
      legProducts.get(firstBusLeg)
    );
    var secondBusLeg = itin.transitLeg(2);
    assertEquals(
      List.of(new FareProductUse("678d201c-e839-35c3-ae7b-1bc3834da5e5", OTHER_FEED_PRODUCT)),
      legProducts.get(secondBusLeg)
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

    var firstBusLeg = itin.transitLeg(0);
    var secondBusLeg = itin.transitLeg(2);
    var finalBusLeg = itin.transitLeg(4);

    assertEquals(
      List.of(new FareProductUse("5d0d58f4-b97a-38db-921c-8b5fc6392b54", OTHER_FEED_PRODUCT)),
      legProducts.get(firstBusLeg)
    );
    assertEquals(
      List.of(
        new FareProductUse("1d270201-412b-3b86-80f6-92ab144fa2e5", AIRPORT_TO_CITY_CENTER_PRODUCT)
      ),
      legProducts.get(secondBusLeg)
    );
    assertEquals(
      List.of(new FareProductUse("5d0d58f4-b97a-38db-921c-8b5fc6392b54", OTHER_FEED_PRODUCT)),
      legProducts.get(finalBusLeg)
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
