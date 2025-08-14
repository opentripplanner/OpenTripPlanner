package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.COMM_TRANS_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.COMM_TRANS_FLEX_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.KC_METRO_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.KITSAP_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.PIERCE_COUNTY_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.SKAGIT_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.SOUND_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.WASHINGTON_STATE_FERRIES_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.WHATCOM_AGENCY_ID;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.core.FareType.regular;
import static org.opentripplanner.transit.model.basic.Money.USD;
import static org.opentripplanner.transit.model.basic.Money.ZERO_USD;
import static org.opentripplanner.transit.model.basic.Money.usDollars;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.impl._support.FareModelForTest;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

public class OrcaFareServiceTest {

  private static final Money ONE_DOLLAR = usDollars(1f);
  private static final Money TWO_DOLLARS = usDollars(2);
  private static final Money HALF_FERRY_FARE = usDollars(1.75f);
  private static final Money ORCA_SPECIAL_FARE = usDollars(1.00f);
  public static final Money VASHON_WATER_TAXI_CASH_FARE = usDollars(6.75f);
  public static final Money WEST_SEATTLE_WATER_TAXI_CASH_FARE = usDollars(5.75f);
  private static final String FEED_ID = "A";
  private static TestOrcaFareService orcaFareService;
  public static final Money DEFAULT_TEST_RIDE_PRICE = usDollars(3.49f);

  @BeforeAll
  public static void setUpClass() {
    Map<FeedScopedId, FareRuleSet> regularFareRules = Map.of(
      new FeedScopedId(FEED_ID, "regular"),
      FareModelForTest.INSIDE_CITY_CENTER_SET
    );
    orcaFareService = new TestOrcaFareService(regularFareRules.values());
  }

  /**
   * These tests are designed to specifically validate Orca fares. Since these fares are hard-coded, it is acceptable
   * to make direct calls to the Orca fare service with predefined routes. Where the default fare is applied a test
   * substitute {@link OrcaFareServiceTest#DEFAULT_TEST_RIDE_PRICE} is used. This will be the same for all cash fare
   * types.
   */
  private static void calculateFare(List<Leg> legs, FareType fareType, Money expectedPrice) {
    var itinerary = Itinerary.ofScheduledTransit(legs).withGeneralizedCost(Cost.ZERO).build();
    var itineraryFares = orcaFareService.calculateFares(itinerary);
    assertEquals(
      expectedPrice,
      itineraryFares
        .getItineraryProducts()
        .stream()
        .filter(fareProduct -> fareProduct.name().equals(fareType.name()))
        .findFirst()
        .get()
        .price()
    );
  }

  private static void assertLegFareEquals(int fare, Leg leg, ItineraryFare fares, boolean hasXfer) {
    var legFareProducts = fares.getLegProducts().get(leg);

    var rideCost = legFareProducts
      .stream()
      .filter(fpl -> {
        var fp = fpl.fareProduct();
        return (
          fp.medium().name().equals("electronic") &&
          fp.category().name().equals("regular") &&
          fp.name().equals("rideCost")
        );
      })
      .findFirst();
    if (rideCost.isEmpty()) {
      Assertions.fail("Missing leg fare product.");
    }
    assertEquals(fare, rideCost.get().fareProduct().price().minorUnitAmount());

    var transfer = legFareProducts
      .stream()
      .filter(fpl -> {
        var fp = fpl.fareProduct();
        return (
          fp.medium().name().equals("electronic") &&
          fp.category().name().equals("regular") &&
          fp.name().equals("transfer")
        );
      })
      .findFirst();
    assertEquals(hasXfer, transfer.isPresent(), "Incorrect transfer leg fare product.");
  }

  /**
   * Test to confirm the correct transfer cost per fare type within a single agency.
   */
  @Test
  void calculateFareForSingleAgency() {
    List<Leg> rides = List.of(getLeg(COMM_TRANS_AGENCY_ID, "400", 0));
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.senior, usDollars(1.25f));
    calculateFare(rides, FareType.youth, ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(1.25f));
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.electronicSenior, usDollars(1.25f));
    calculateFare(rides, FareType.electronicYouth, ZERO_USD);
  }

  /**
   * WSF do not accept free transfers. This test is to make sure the rider is charged the cash price for WSF as well
   * as the highest fare where Orca can be used.
   */
  @Test
  void calculateFareWithNoFreeTransfer() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 1),
      getLeg(COMM_TRANS_AGENCY_ID, 2)
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(3));
    calculateFare(rides, FareType.senior, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(2.25f)));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(
      rides,
      FareType.electronicSpecial,
      DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1.25f))
    );
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.electronicSenior, DEFAULT_TEST_RIDE_PRICE.plus(usDollars(1.25f)));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Check to make sure the fare by leg is calculated properly for a trip with two rides.
   */
  @Test
  void calculateFareByLeg() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(COMM_TRANS_AGENCY_ID, 2),
      getLeg(COMM_TRANS_FLEX_AGENCY_ID, 3)
    );
    var fares = orcaFareService.calculateFaresForType(USD, FareType.electronicRegular, rides, null);

    assertLegFareEquals(349, rides.get(0), fares, false);
    assertLegFareEquals(0, rides.get(1), fares, true);
    assertLegFareEquals(0, rides.get(2), fares, true);
  }

  /**
   * Total trip time is 2h 30m. The first four transfers are within the permitted two hour window. A single (highest)
   * Orca fare will be charged for these transfers. The fifth transfer is outside of the original two hour window so
   * a single Orca fare for this leg is applied and the two hour window will start again. The final transfer is within
   * the new two hour window and will be free.
   */
  @Test
  void calculateFareThatExceedsTwoHourFreeTransferWindow() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 150)
    );

    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.senior, TWO_DOLLARS);
    calculateFare(rides, FareType.youth, ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, TWO_DOLLARS);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.electronicSenior, TWO_DOLLARS);
    calculateFare(rides, FareType.electronicYouth, ZERO_USD);
  }

  /**
   * Total trip time is 2h 30m. Calculate fare with two free transfer windows which include agencies which do not permit
   * free transfers. The free transfers will be applied for Kitsap, but not for WSF nor Skagit. Note: Not a real world
   * trip!
   */
  @Test
  void calculateFareThatIncludesNoFreeTransfers() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 30, "VashonIsland-Fauntelroy"),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(SKAGIT_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 121),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 150, "Fauntleroy-VashonIsland")
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(5));
    calculateFare(
      rides,
      FareType.senior,
      ONE_DOLLAR.plus(ONE_DOLLAR).plus(HALF_FERRY_FARE.times(2)).plus(usDollars(0.5f))
    );
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    // We don't get any fares for the skagit transit leg below here because they don't accept ORCA (electronic)
    calculateFare(
      rides,
      FareType.electronicSpecial,
      ONE_DOLLAR.plus(ONE_DOLLAR).plus(DEFAULT_TEST_RIDE_PRICE.times(2))
    );
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE.times(4));
    calculateFare(
      rides,
      FareType.electronicSenior,
      ONE_DOLLAR.plus(ONE_DOLLAR).plus(HALF_FERRY_FARE.times(2))
    );
    calculateFare(rides, FareType.electronicYouth, ZERO_USD);
  }

  /**
   * Total trip time is 4h 30m. This is equivalent to three transfer windows and therefore three Orca fare charges.
   */
  @Test
  void calculateFareThatExceedsTwoHourFreeTransferWindowTwice() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 150),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 180),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 210),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 240),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 270)
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(3));
    calculateFare(rides, FareType.senior, usDollars(3));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(3));
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE.times(3));
    calculateFare(rides, FareType.electronicSenior, usDollars(3));
    calculateFare(rides, FareType.electronicYouth, ZERO_USD);
  }

  /**
   * This trip starts with a cash fare so the free transfer window doesn't start until the second transfer. Therefore,
   * all subsequent transfers will come under one transfer window and only one Orca discount charge will apply.
   */
  @Test
  void calculateFareThatStartsWithACashFare() {
    List<Leg> rides = List.of(
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 149)
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.senior, DEFAULT_TEST_RIDE_PRICE.plus(ONE_DOLLAR));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, DEFAULT_TEST_RIDE_PRICE.plus(ONE_DOLLAR));
    calculateFare(
      rides,
      FareType.electronicRegular,
      DEFAULT_TEST_RIDE_PRICE.plus(DEFAULT_TEST_RIDE_PRICE)
    );
    calculateFare(rides, FareType.electronicSenior, DEFAULT_TEST_RIDE_PRICE.plus(ONE_DOLLAR));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Single trip with Kitsap transit fast ferry to confirm correct non Orca fares are applied.
   */
  @Test
  void calculateFareForKitsapFastFerry() {
    List<Leg> rides = List.of(getLeg(KITSAP_TRANSIT_AGENCY_ID, 0, 4, "404", "east"));
    calculateFare(rides, regular, TWO_DOLLARS);
    calculateFare(rides, FareType.senior, ONE_DOLLAR);
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, ONE_DOLLAR);
    calculateFare(rides, FareType.electronicRegular, TWO_DOLLARS);
    calculateFare(rides, FareType.electronicSenior, ONE_DOLLAR);
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);

    rides = List.of(getLeg(KITSAP_TRANSIT_AGENCY_ID, 0, 4, "404", "west"));
    calculateFare(rides, regular, usDollars(10f));
    calculateFare(rides, FareType.senior, usDollars(5f));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(5f));
    calculateFare(rides, FareType.electronicRegular, usDollars(10f));
    calculateFare(rides, FareType.electronicSenior, usDollars(5f));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Single trip (Point Defiance - Tahlequah) with WSF transit to confirm correct non Orca fares are applied.
   */
  @Test
  void calculateFareForWSFPtToTahlequah() {
    List<Leg> rides = List.of(
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0, "Point Defiance - Tahlequah")
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.senior, HALF_FERRY_FARE);
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.electronicSenior, HALF_FERRY_FARE);
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Single trip with Link Light Rail to ensure distance fare is calculated correctly.
   */
  @Test
  void calculateFareForSTRail() {
    List<Leg> rides = List.of(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Roosevelt Station", "Int'l Dist/Chinatown"),
      getLeg(SOUND_TRANSIT_AGENCY_ID, "S Line", 100, "King Street Station", "Auburn Station")
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.senior, TWO_DOLLARS);
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, ORCA_SPECIAL_FARE);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.electronicSenior, ONE_DOLLAR);
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Test King County Water Taxis
   */
  @Test
  void calculateWaterTaxiFares() {
    List<Leg> rides = List.of(getLeg(KC_METRO_AGENCY_ID, "973", 1));
    calculateFare(rides, regular, WEST_SEATTLE_WATER_TAXI_CASH_FARE);
    calculateFare(rides, FareType.senior, usDollars(2.50f));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(3.75f));
    calculateFare(rides, FareType.electronicRegular, usDollars(5f));
    calculateFare(rides, FareType.electronicSenior, usDollars(2.50f));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);

    rides = List.of(getLeg(KC_METRO_AGENCY_ID, "975", 1));

    calculateFare(rides, regular, VASHON_WATER_TAXI_CASH_FARE);
    calculateFare(rides, FareType.senior, usDollars(3f));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(4.50f));
    calculateFare(rides, FareType.electronicRegular, usDollars(5.75f));
    calculateFare(rides, FareType.electronicSenior, usDollars(3f));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Create a few Sound Transit trips but under the contracted agency IDs.
   * SoundTransit contracts their bus service, so their routes show under the contracted agency's IDs in the GTFS feed.
   * Make sure that we get ST's bus fare and not the contracted agency's fare.
   */
  @Test
  void calculateSoundTransitBusFares() {
    List<Leg> rides = List.of(
      getLeg(COMM_TRANS_AGENCY_ID, "512", 0),
      getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "594", 120),
      getLeg(KC_METRO_AGENCY_ID, "550", 240)
    );
    calculateFare(rides, regular, usDollars(9.75f));
    calculateFare(rides, FareType.senior, usDollars(3.00f));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(3f));
    calculateFare(rides, FareType.electronicRegular, usDollars(9.75f));
    calculateFare(rides, FareType.electronicSenior, usDollars(3.00f));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);

    // Also make sure that PT's 500 and 501 get regular Pierce fare and not ST's fare
    rides = List.of(
      getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "500", 0),
      getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "501", 60)
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.senior, TWO_DOLLARS);
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(1f));
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE);
    calculateFare(rides, FareType.electronicSenior, ONE_DOLLAR);
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  @Test
  void calculateCashFreeTransferKCMetroAndKitsap() {
    List<Leg> rides = List.of(
      getLeg(KC_METRO_AGENCY_ID, 0),
      getLeg(KC_METRO_AGENCY_ID, 20),
      getLeg(COMM_TRANS_AGENCY_ID, 45),
      getLeg(KC_METRO_AGENCY_ID, 60),
      getLeg(KC_METRO_AGENCY_ID, 130),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 131),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 132)
    );
    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(4));
    calculateFare(rides, FareType.senior, usDollars(4.25f));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(1.25f));
    calculateFare(rides, FareType.electronicRegular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.electronicSenior, usDollars(1.25f)); // Transfer extended by CT ride
    calculateFare(rides, FareType.electronicYouth, ZERO_USD);
  }

  @Test
  void calculateTransferExtension() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0, 4, "Kitsap Fast Ferry", "east"), // 2.00
      getLeg(KC_METRO_AGENCY_ID, 100), // Default ride price, extends transfer for regular fare
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 150, 4, "Kitsap Fast Ferry", "west") // 10.00
    );
    var regularFare = usDollars(2.00f).plus(DEFAULT_TEST_RIDE_PRICE).plus(usDollars(10f));
    calculateFare(rides, regular, regularFare);
    calculateFare(rides, FareType.senior, usDollars(7f));
    calculateFare(rides, FareType.youth, Money.ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, usDollars(6f));
    calculateFare(rides, FareType.electronicRegular, usDollars(10f)); // transfer extended on second leg
    calculateFare(rides, FareType.electronicSenior, usDollars(6f));
    calculateFare(rides, FareType.electronicYouth, Money.ZERO_USD);
  }

  /**
   * Tests fares from non ORCA accepting agencies
   */
  @Test
  void testNonOrcaAgencies() {
    List<Leg> rides = List.of(
      getLeg(SKAGIT_TRANSIT_AGENCY_ID, 0, "80X"),
      getLeg(WHATCOM_AGENCY_ID, 0, "80X")
    );

    calculateFare(rides, regular, DEFAULT_TEST_RIDE_PRICE.times(2));
    calculateFare(rides, FareType.senior, usDollars(0.50f).times(2));
    // TODO: Check that these are undefined, not zero
    calculateFare(rides, FareType.youth, ZERO_USD);
    calculateFare(rides, FareType.electronicSpecial, ZERO_USD);
    calculateFare(rides, FareType.electronicRegular, ZERO_USD);
  }

  static Stream<Arguments> allTypes() {
    return Arrays.stream(FareType.values()).map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("allTypes")
  void nullLongName(FareType type) {
    var legs = List.of(
      createLeg(
        WASHINGTON_STATE_FERRIES_AGENCY_ID,
        "1-Line",
        0,
        1,
        "route1",
        "trip1",
        null,
        "first stop",
        "last stop"
      )
    );

    var fare = orcaFareService.calculateFaresForType(USD, type, legs, null);
    assertFalse(fare.getLegProducts().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("allTypes")
  void nullShortName(FareType type) {
    var legs = List.of(
      createLeg(
        WASHINGTON_STATE_FERRIES_AGENCY_ID,
        null,
        0,
        1,
        "route1",
        "trip1",
        "long name",
        "first stop",
        "last stop"
      )
    );

    var fare = orcaFareService.calculateFaresForType(USD, type, legs, null);
    assertFalse(fare.getLegProducts().isEmpty());
  }

  @Test
  void fullItinerary() {
    var itinerary = createItinerary(
      WASHINGTON_STATE_FERRIES_AGENCY_ID,
      "1-Line",
      0,
      1,
      "route1",
      "trip1",
      null,
      "first stop",
      "last stop"
    );
    var fares = orcaFareService.calculateFares(itinerary);
    assertNotNull(fares);

    assertFalse(fares.getItineraryProducts().isEmpty());
    assertFalse(fares.getLegProducts().isEmpty());

    var firstLeg = itinerary.legs().getFirst();
    var uses = fares.getLegProducts().get(firstLeg);
    assertEquals(7, uses.size());

    var regular = uses
      .stream()
      .filter(u -> u.fareProduct().category().name().equals("regular"))
      .toList()
      .getFirst();
    assertEquals(Money.usDollars(3.49f), regular.fareProduct().price());
  }

  private static Leg getLeg(String agencyId, long startTimeMins) {
    return createLeg(agencyId, "-1", 3, startTimeMins, "test", "test", "");
  }

  private static Leg getLeg(String agencyId, long startTimeMins, String routeLongName) {
    return createLeg(agencyId, "-1", 3, startTimeMins, "test", "test", routeLongName);
  }

  private static Leg getLeg(
    String agencyId,
    long startTimeMins,
    int routeType,
    String routeId,
    String tripId
  ) {
    return createLeg(agencyId, "-1", routeType, startTimeMins, routeId, tripId, "");
  }

  private static Leg getLeg(String agencyId, String shortName, long startTimeMins) {
    return createLeg(agencyId, shortName, 3, startTimeMins, "test", "test", "");
  }

  private static Leg getLeg(
    String agencyId,
    String shortName,
    long startTimeMins,
    String firstStopName,
    String lastStopName
  ) {
    return createLeg(
      agencyId,
      shortName,
      3,
      startTimeMins,
      "test",
      "test",
      "",
      firstStopName,
      lastStopName
    );
  }

  /**
   * Create a {@link Leg} containing route data that will be used by {@link OrcaFareService} to determine the
   * correct ride type.
   */
  private static Leg createLeg(
    String agencyId,
    String shortName,
    int routeType,
    long startTimeMins,
    String routeId,
    String tripId,
    String routeLongName
  ) {
    return createLeg(
      agencyId,
      shortName,
      routeType,
      startTimeMins,
      routeId,
      tripId,
      routeLongName,
      "",
      ""
    );
  }

  /**
   * Create a {@link Leg} containing route data that will be used by {@link OrcaFareService} to determine the
   * correct ride type.
   */
  private static Leg createLeg(
    String agencyId,
    String shortName,
    int transitMode,
    long startTimeMins,
    String routeId,
    String tripId,
    @Nullable String routeLongName,
    String firstStopName,
    String lastStopName
  ) {
    final var itin = createItinerary(
      agencyId,
      shortName,
      transitMode,
      startTimeMins,
      routeId,
      tripId,
      routeLongName,
      firstStopName,
      lastStopName
    );

    return itin.legs().get(0);
  }

  private static Itinerary createItinerary(
    String agencyId,
    String shortName,
    int transitMode,
    long startTimeMins,
    String routeId,
    String tripId,
    @Nullable String routeLongName,
    String firstStopName,
    String lastStopName
  ) {
    // Use the agency ID as feed ID to make sure that we have a new feed ID for each different agency
    // This tests to make sure we are calculating transfers across feeds correctly.
    Agency agency = Agency.of(new FeedScopedId(agencyId, agencyId))
      .withName(agencyId)
      .withTimezone(ZoneIds.NEW_YORK.getId())
      .build();

    var siteRepositoryBuilder = SiteRepository.of();

    // Set up stops
    RegularStop firstStop = siteRepositoryBuilder
      .regularStop(new FeedScopedId(agencyId, "1"))
      .withCoordinate(new WgsCoordinate(1, 1))
      .withName(new NonLocalizedString(firstStopName))
      .build();
    RegularStop lastStop = siteRepositoryBuilder
      .regularStop(new FeedScopedId(agencyId, "2"))
      .withCoordinate(new WgsCoordinate(1, 2))
      .withName(new NonLocalizedString(lastStopName))
      .build();

    FeedScopedId routeFeedScopeId = new FeedScopedId(agencyId, routeId);
    NonLocalizedString longName = null;
    if (routeLongName != null) {
      longName = new NonLocalizedString(routeLongName);
    }
    Route route = Route.of(routeFeedScopeId)
      .withAgency(agency)
      .withShortName(shortName)
      .withLongName(longName)
      // TODO: Way to convert from TransitMode to int (GTFS)?
      .withMode(TransitMode.BUS)
      .withGtfsType(transitMode)
      .build();

    int start = (int) (T11_00 + (startTimeMins * 60));
    int end = (int) (T11_00 + ((startTimeMins + 12) * 60));
    return newItinerary(Place.forStop(firstStop), start)
      .transit(route, tripId, start, end, 5, 7, Place.forStop(lastStop), null, null, null)
      .build();
  }

  private static class TestOrcaFareService extends OrcaFareService {

    public TestOrcaFareService(Collection<FareRuleSet> regularFareRules) {
      super(regularFareRules);
    }

    @Override
    protected Optional<Money> calculateCost(
      FareType fareType,
      List<Leg> rides,
      Collection<FareRuleSet> fareRules
    ) {
      return Optional.of(DEFAULT_TEST_RIDE_PRICE);
    }
  }
}
