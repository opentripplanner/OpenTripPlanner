package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.COBB_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.GCT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.MARTA_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.AtlantaFareService.XPRESS_AGENCY_ID;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;

public class AtlantaFareServiceTest implements PlanTestConstants {

  public static final float DEFAULT_TEST_RIDE_PRICE = 3.49f;
  public static final float DEFAULT_RIDE_PRICE_IN_CENTS = DEFAULT_TEST_RIDE_PRICE * 100;
  private static AtlantaFareService atlFareService;

  @BeforeAll
  public static void setUpClass() {
    Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();
    atlFareService = new TestAtlantaFareService(regularFareRules.values());
  }

  @Test
  public void fromMartaTransfers() {
    List<Leg> rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(XPRESS_AGENCY_ID, 1));
    calculateFare(rides, 349);

    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(GCT_AGENCY_ID, 1));
    calculateFare(rides, 349);

    // to GCT Express
    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(GCT_AGENCY_ID, "101", 1));
    calculateFare(rides, 349);

    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(COBB_AGENCY_ID, 1));
    calculateFare(rides, 349);

    // To Cobb Express
    rides = List.of(getLeg(MARTA_AGENCY_ID, 0), getLeg(COBB_AGENCY_ID, "101", 1));
    calculateFare(rides, 349);
  }

  @Test
  public void fromCobbTransfers() {
    List<Leg> rides = List.of(getLeg(COBB_AGENCY_ID, 0), getLeg(MARTA_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS);

    // Local to express
    rides = List.of(getLeg(COBB_AGENCY_ID, 0), getLeg(COBB_AGENCY_ID, "101", 1));
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS + 100);

    rides = List.of(getLeg(COBB_AGENCY_ID, 0), getLeg(XPRESS_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS + 150);

    // Express to local
    rides = List.of(getLeg(COBB_AGENCY_ID, "101", 0), getLeg(COBB_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS + 100);

    rides = List.of(getLeg(COBB_AGENCY_ID, "101", 0), getLeg(GCT_AGENCY_ID, "102", 1));
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS * 2 + 300);

    // Local to circulator to express
    rides =
      List.of(
        getLeg(COBB_AGENCY_ID, 0),
        getLeg(COBB_AGENCY_ID, "BLUE", 1),
        getLeg(COBB_AGENCY_ID, "101", 1)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS + 100);
  }

  @Test
  public void fromGctTransfers() {
    List<Leg> rides = List.of(getLeg(GCT_AGENCY_ID, 0), getLeg(MARTA_AGENCY_ID, 1));
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS);
  }

  @Test
  public void tooManyLegs() {
    List<Leg> rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(MARTA_AGENCY_ID, 3),
      getLeg(MARTA_AGENCY_ID, 4),
      getLeg(MARTA_AGENCY_ID, 5)
    );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS * 2);

    rides =
      List.of(
        getLeg(MARTA_AGENCY_ID, 0),
        getLeg(MARTA_AGENCY_ID, 1),
        getLeg(GCT_AGENCY_ID, 2),
        getLeg(GCT_AGENCY_ID, 3),
        getLeg(MARTA_AGENCY_ID, 4),
        getLeg(COBB_AGENCY_ID, 5)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS * 2);

    rides =
      List.of(
        getLeg(GCT_AGENCY_ID, 0),
        getLeg(MARTA_AGENCY_ID, 1),
        getLeg(MARTA_AGENCY_ID, 2),
        getLeg(MARTA_AGENCY_ID, 3)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS);

    rides =
      List.of(
        getLeg(GCT_AGENCY_ID, 0),
        getLeg(MARTA_AGENCY_ID, 1),
        getLeg(MARTA_AGENCY_ID, 2),
        getLeg(MARTA_AGENCY_ID, 3),
        // new transfer - only got 3 from GCT
        getLeg(MARTA_AGENCY_ID, 4)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS * 2);

    rides =
      List.of(
        getLeg(MARTA_AGENCY_ID, 0),
        getLeg(MARTA_AGENCY_ID, 1),
        getLeg(MARTA_AGENCY_ID, 2),
        getLeg(GCT_AGENCY_ID, 3),
        getLeg(GCT_AGENCY_ID, 4)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS);
  }

  @Test
  public void expiredTransfer() {
    List<Leg> rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, 1),
      getLeg(MARTA_AGENCY_ID, 181),
      getLeg(MARTA_AGENCY_ID, 179)
    );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS * 2);

    rides =
      List.of(
        getLeg(MARTA_AGENCY_ID, 0),
        getLeg(GCT_AGENCY_ID, 1),
        getLeg(GCT_AGENCY_ID, 181),
        getLeg(MARTA_AGENCY_ID, 181 + 178),
        getLeg(MARTA_AGENCY_ID, 181 + 179)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
  }

  @Test
  public void useStreetcar() {
    final float STREETCAR_PRICE = DEFAULT_RIDE_PRICE_IN_CENTS - 100f;
    List<Leg> rides = List.of(
      getLeg(MARTA_AGENCY_ID, 0),
      getLeg(MARTA_AGENCY_ID, "atlsc", 1),
      getLeg(MARTA_AGENCY_ID, 2),
      getLeg(MARTA_AGENCY_ID, 3),
      getLeg(MARTA_AGENCY_ID, 4)
    );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS + STREETCAR_PRICE);

    rides =
      List.of(
        getLeg(COBB_AGENCY_ID, 0),
        getLeg(MARTA_AGENCY_ID, "atlsc", 1),
        getLeg(COBB_AGENCY_ID, "101", 2)
      );
    calculateFare(rides, DEFAULT_RIDE_PRICE_IN_CENTS + 100 + STREETCAR_PRICE);
  }

  /**
   * These tests are designed to specifically validate ATL fares. Since these fares are hard-coded,
   * it is acceptable to make direct calls to the ATL fare service with predefined routes. Where the
   * default fare is applied a test substitute {@link AtlantaFareServiceTest#DEFAULT_TEST_RIDE_PRICE} is
   * used. This will be the same for all cash fare types except when overriden above.
   */
  private static void calculateFare(List<Leg> rides, float expectedFareInCents) {
    ItineraryFares fare = new ItineraryFares();
    atlFareService.populateFare(fare, null, FareType.electronicRegular, rides, null);
    assertEquals(expectedFareInCents, fare.getFare(FareType.electronicRegular).cents());
  }

  private static Leg getLeg(String agencyId, long startTimeMins) {
    return createLeg(agencyId, "-1", startTimeMins);
  }

  private static Leg getLeg(String agencyId, String shortName, long startTimeMins) {
    return createLeg(agencyId, shortName, startTimeMins);
  }

  private static Leg createLeg(String agencyId, String shortName, long startTimeMins) {
    Agency agency = Agency
      .of(new FeedScopedId("A", agencyId))
      .withName(agencyId)
      .withTimezone(ZoneIds.NEW_YORK.getId())
      .build();

    // Set up stops
    RegularStop firstStop = RegularStop
      .of(new FeedScopedId("A", "1"))
      .withCoordinate(new WgsCoordinate(1, 1))
      .withName(new NonLocalizedString("first stop"))
      .build();
    RegularStop lastStop = RegularStop
      .of(new FeedScopedId("A", "2"))
      .withCoordinate(new WgsCoordinate(1, 2))
      .withName(new NonLocalizedString("last stop"))
      .build();

    FeedScopedId routeFeedScopeId = new FeedScopedId("A", "123");
    Route route = Route
      .of(routeFeedScopeId)
      .withAgency(agency)
      .withShortName(shortName)
      .withLongName(new NonLocalizedString("123"))
      .withMode(TransitMode.BUS)
      .build();

    int start = (int) (T11_00 + (startTimeMins * 60));
    var itin = newItinerary(Place.forStop(firstStop), start)
      .bus(route, 1, start, T11_12, Place.forStop(lastStop))
      .build();

    return itin.getLegs().get(0);
  }

  private static class TestAtlantaFareService extends AtlantaFareService {

    public TestAtlantaFareService(Collection<FareRuleSet> regularFareRules) {
      super(regularFareRules);
    }

    @Override
    protected float getLegPrice(Leg leg, FareType fareType, Collection<FareRuleSet> fareRules) {
      String routeShortName = leg.getRoute().getShortName().toLowerCase();
      // Testing, return default test ride price.
      return switch (routeShortName) {
        case "101" -> DEFAULT_TEST_RIDE_PRICE + 1;
        case "102" -> DEFAULT_TEST_RIDE_PRICE + 2;
        case "atlsc" -> DEFAULT_TEST_RIDE_PRICE - 1;
        case "blue" -> 0;
        default -> DEFAULT_TEST_RIDE_PRICE; // free circulator
      };
    }
  }
}
