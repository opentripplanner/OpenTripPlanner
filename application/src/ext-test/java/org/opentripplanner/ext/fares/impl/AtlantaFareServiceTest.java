package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.COBB_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.GCT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.MARTA_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.XPRESS_AGENCY_ID;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model.basic.Money.USD;
import static org.opentripplanner.transit.model.basic.Money.usDollars;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl._support.FareModelForTest;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

public class AtlantaFareServiceTest implements PlanTestConstants {

  private static final Money DEFAULT_TEST_RIDE_PRICE = usDollars(3.49f);
  private static final String FEED_ID = "A";
  private static AtlantaFareService atlFareService;

  @BeforeAll
  public static void setUpClass() {
    Map<FeedScopedId, FareRuleSet> regularFareRules = Map.of(
      new FeedScopedId(FEED_ID, "regular"),
      FareModelForTest.INSIDE_CITY_CENTER_SET
    );
    atlFareService = new TestAtlantaFareService(regularFareRules.values());
  }

  @Test
  void fromMartaTransfers() {
    List<Leg> rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(XPRESS_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);

    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(GCT_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);

    // to GCT Express
    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(GCT_AGENCY_ID, "101", 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);

    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(COBB_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);

    // To Cobb Express
    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(COBB_AGENCY_ID, "101", 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);
  }

  @Test
  void nullShortName() {
    var legs = List.of(getLeg(GCT_AGENCY_ID, null, 1));
    calculateFare(legs, usDollars(3.49f));
  }

  @Test
  void fromCobbTransfers() {
    List<Leg> rides = List.of(getLeg(COBB_AGENCY_ID, 0), getLeg(MARTA_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);

    // Local to express
    rides = List.of(getLeg(COBB_AGENCY_ID, 0), getLeg(COBB_AGENCY_ID, "101", 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1)));

    rides = List.of(getLeg(COBB_AGENCY_ID, 0), getLeg(XPRESS_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1.5f)));

    // Express to local
    rides = List.of(getLeg(COBB_AGENCY_ID, "101", 0), getLeg(COBB_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1)));

    rides = List.of(getLeg(COBB_AGENCY_ID, "101", 0), getLeg(GCT_AGENCY_ID, "102", 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE).plus(usDollars(3)));

    // Local to circulator to express
    rides = List.of(
      getLeg(COBB_AGENCY_ID, 0),
      getLeg(COBB_AGENCY_ID, "BLUE", 1),
      getLeg(COBB_AGENCY_ID, "101", 1)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1)));
  }

  @Test
  void fromGctTransfers() {
    List<Leg> rides = List.of(getLeg(GCT_AGENCY_ID, 0), getLeg(MARTA_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);
  }

  @Test
  void tooManyLegs() {
    List<Leg> rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(MARTA_AGENCY_ID, 3),
      getLeg(MARTA_AGENCY_ID, 4),
      getLeg(MARTA_AGENCY_ID, 5)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE));

    rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(GCT_AGENCY_ID, 2),
      getLeg(GCT_AGENCY_ID, 3),
      getLeg(MARTA_AGENCY_ID, 4),
      getLeg(COBB_AGENCY_ID, 5)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE));

    rides = List.of(
      getLeg(GCT_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(MARTA_AGENCY_ID, 3)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);

    rides = List.of(
      getLeg(GCT_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(MARTA_AGENCY_ID, 3),
      // new transfer - only got 3 from GCT
      getLeg(MARTA_AGENCY_ID, 4)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE));

    rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(GCT_AGENCY_ID, 3),
      getLeg(GCT_AGENCY_ID, 4)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE);
  }

  @Test
  void expiredTransfer() {
    List<Leg> rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 181),
      getLeg(MARTA_AGENCY_ID, 179)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE));

    rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(GCT_AGENCY_ID, 1),
      getLeg(GCT_AGENCY_ID, 181),
      getLeg(MARTA_AGENCY_ID, 181 + 178),
      getLeg(MARTA_AGENCY_ID, 181 + 179)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE));
  }

  @Test
  void useStreetcar() {
    var STREETCAR_PRICE = DEFAULT_TEST_RIDE_PRICE.minus(usDollars(1));
    List<Leg> rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, "atlsc", 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(MARTA_AGENCY_ID, 3),
      getLeg(MARTA_AGENCY_ID, 4)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(STREETCAR_PRICE));

    rides = List.of(
      getLeg(COBB_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, "atlsc", 1),
      getLeg(COBB_AGENCY_ID, "101", 2)
    );
    calculateFare(rides, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1)).plus(STREETCAR_PRICE));
  }

  @Test
  void fullItinerary() {
    var itin = createItinerary(MARTA_AGENCY_ID, "1", 0);
    var fares = atlFareService.calculateFares(itin);
    assertNotNull(fares);
    assertTrue(fares.getLegProducts().isEmpty());
    var itineraryProducts = fares.getItineraryProducts();
    assertFalse(itineraryProducts.isEmpty());
    var fp = itineraryProducts.stream().filter(p -> p.name().equals("regular")).findAny().get();
    assertEquals(Money.usDollars(3.49f), fp.price());
  }

  /**
   * These tests are designed to specifically validate ATL fares. Since these fares are hard-coded,
   * it is acceptable to make direct calls to the ATL fare service with predefined routes. Where the
   * default fare is applied a test substitute {@link AtlantaFareServiceTest#DEFAULT_TEST_RIDE_PRICE} is
   * used. This will be the same for all cash fare types except when overriden above.
   */
  private static void calculateFare(List<Leg> rides, Money expectedFare) {
    var fare = atlFareService.calculateFaresForType(USD, FareType.electronicRegular, rides, null);
    assertEquals(
      expectedFare,
      fare
        .getItineraryProducts()
        .stream()
        .filter(fp -> fp.name().equals(FareType.electronicRegular.name()))
        .findFirst()
        .get()
        .price()
    );

    var fareProducts = fare
      .getItineraryProducts()
      .stream()
      .filter(fp -> fp.id().getId().equals(FareType.electronicRegular.name()))
      .toList();

    assertEquals(1, fareProducts.size());
    var fp = fareProducts.get(0);
    assertEquals(expectedFare, fp.price());
  }

  private static Leg getLeg(String agencyId, long startTimeMins) {
    return createLeg(agencyId, "-1", startTimeMins);
  }

  private static Leg getLeg(String agencyId, String shortName, long startTimeMins) {
    return createLeg(agencyId, shortName, startTimeMins);
  }

  private static Leg createLeg(String agencyId, String shortName, long startTimeMins) {
    final var itin = createItinerary(agencyId, shortName, startTimeMins);
    return itin.legs().get(0);
  }

  private static Itinerary createItinerary(String agencyId, String shortName, long startTimeMins) {
    var siteRepositoryBuilder = SiteRepository.of();
    Agency agency = Agency.of(new FeedScopedId(FEED_ID, agencyId))
      .withName(agencyId)
      .withTimezone(ZoneIds.NEW_YORK.getId())
      .build();

    // Set up stops
    RegularStop firstStop = siteRepositoryBuilder
      .regularStop(new FeedScopedId(FEED_ID, "1"))
      .withCoordinate(new WgsCoordinate(1, 1))
      .withName(new NonLocalizedString("first stop"))
      .build();
    RegularStop lastStop = siteRepositoryBuilder
      .regularStop(new FeedScopedId(FEED_ID, "2"))
      .withCoordinate(new WgsCoordinate(1, 2))
      .withName(new NonLocalizedString("last stop"))
      .build();

    FeedScopedId routeFeedScopeId = new FeedScopedId(FEED_ID, "123");
    Route route = Route.of(routeFeedScopeId)
      .withAgency(agency)
      .withShortName(shortName)
      .withLongName(new NonLocalizedString("123"))
      .withMode(TransitMode.BUS)
      .build();

    int start = (int) (T11_00 + (startTimeMins * 60));
    int end = (int) (T11_00 + ((startTimeMins + 12) * 60));
    return newItinerary(Place.forStop(firstStop), start)
      .bus(route, 1, start, end, Place.forStop(lastStop))
      .build();
  }

  private static class TestAtlantaFareService extends AtlantaFareService {

    public TestAtlantaFareService(Collection<FareRuleSet> regularFareRules) {
      super(regularFareRules);
    }

    @Override
    protected Money getLegPrice(Leg leg, FareType fareType, Collection<FareRuleSet> fareRules) {
      var routeShortName = leg.route().getShortName();
      if (routeShortName == null) {
        return DEFAULT_TEST_RIDE_PRICE;
      }
      routeShortName = leg.route().getShortName().toLowerCase();

      // Testing, return default test ride price.
      return switch (routeShortName) {
        case "101" -> DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1));
        case "102" -> DEFAULT_TEST_RIDE_PRICE.plus(usDollars(2));
        case "atlsc" -> DEFAULT_TEST_RIDE_PRICE.minus(usDollars(1));
        case "blue" -> Money.ZERO_USD;
        default -> DEFAULT_TEST_RIDE_PRICE; // free circulator
      };
    }
  }
}
