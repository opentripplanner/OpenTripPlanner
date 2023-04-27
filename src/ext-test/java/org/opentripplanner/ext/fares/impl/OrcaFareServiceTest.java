package org.opentripplanner.ext.fares.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.COMM_TRANS_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.KC_METRO_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.KITSAP_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.PIERCE_COUNTY_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.SKAGIT_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.SOUND_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareService.WASHINGTON_STATE_FERRIES_AGENCY_ID;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_12;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.core.FareType.regular;

import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;

public class OrcaFareServiceTest {

  public static final Currency USD = Currency.getInstance("USD");
  private static TestOrcaFareService orcaFareService;
  public static final float DEFAULT_TEST_RIDE_PRICE = 3.49f;
  private static final int DEFAULT_RIDE_PRICE_IN_CENTS = (int) (DEFAULT_TEST_RIDE_PRICE * 100);

  @BeforeAll
  public static void setUpClass() {
    Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();
    orcaFareService = new TestOrcaFareService(regularFareRules.values());
  }

  /**
   * These tests are designed to specifically validate Orca fares. Since these fares are hard-coded, it is acceptable
   * to make direct calls to the Orca fare service with predefined routes. Where the default fare is applied a test
   * substitute {@link OrcaFareServiceTest#DEFAULT_TEST_RIDE_PRICE} is used. This will be the same for all cash fare
   * types.
   */
  private static void calculateFare(List<Leg> legs, FareType fareType, float expectedFareInCents) {
    ItineraryFares fare = new ItineraryFares();
    orcaFareService.populateFare(fare, USD, fareType, legs, null);
    Assertions.assertEquals(expectedFareInCents, fare.getFare(fareType).amount());
  }

  private static void assertLegFareEquals(
    int fare,
    Leg leg,
    ItineraryFares fares,
    boolean hasXfer
  ) {
    var legFareProducts = fares.getLegProducts().get(leg);

    var rideCost = legFareProducts
      .stream()
      .map(FareProductUse::product)
      .filter(fp ->
        fp.medium().name().equals("electronic") &&
        fp.category().name().equals("regular") &&
        fp.name().equals("rideCost")
      )
      .findFirst();
    if (rideCost.isEmpty()) {
      Assertions.fail("Missing leg fare product.");
    }
    Assertions.assertEquals(fare, rideCost.get().amount().amount());

    var transfer = legFareProducts
      .stream()
      .map(FareProductUse::product)
      .filter(fp ->
        fp.medium().name().equals("electronic") &&
        fp.category().name().equals("regular") &&
        fp.name().equals("transfer")
      )
      .findFirst();
    Assertions.assertEquals(hasXfer, transfer.isPresent(), "Incorrect transfer leg fare product.");
  }

  /**
   * Test to confirm the correct transfer cost per fare type within a single agency.
   */
  @Test
  public void calculateFareForSingleAgency() {
    List<Leg> rides = List.of(getLeg(COMM_TRANS_AGENCY_ID, "400", 0));
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.senior, 200f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 200f);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.electronicSenior, 200f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * WSF do not accept free transfers. This test is to make sure the rider is charged the cash price for WSF as well
   * as the highest fare where Orca can be used.
   */
  @Test
  public void calculateFareWithNoFreeTransfer() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 1),
      getLeg(COMM_TRANS_AGENCY_ID, 2)
    );
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
    calculateFare(
      rides,
      FareType.senior,
      DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS + 125
    );
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f);
    calculateFare(
      rides,
      FareType.electronicRegular,
      0f + DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS
    );
    calculateFare(rides, FareType.electronicSenior, 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 125f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Check to make sure the fare by leg is calculated properly for a trip with two rides.
   */
  @Test
  public void calculateFareByLeg() {
    List<Leg> rides = List.of(getLeg(KITSAP_TRANSIT_AGENCY_ID, 0), getLeg(COMM_TRANS_AGENCY_ID, 2));
    ItineraryFares fares = new ItineraryFares();
    orcaFareService.populateFare(fares, USD, FareType.electronicRegular, rides, null);

    assertLegFareEquals(349, rides.get(0), fares, false);
    assertLegFareEquals(0, rides.get(1), fares, true);
  }

  /**
   * Total trip time is 2h 30m. The first four transfers are within the permitted two hour window. A single (highest)
   * Orca fare will be charged for these transfers. The fifth transfer is outside of the original two hour window so
   * a single Orca fare for this leg is applied and the two hour window will start again. The final transfer is within
   * the new two hour window and will be free.
   */
  @Test
  public void calculateFareThatExceedsTwoHourFreeTransferWindow() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 150)
    );
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
    calculateFare(rides, FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 100f + 0f + 0f + 0f + 100f + 0f);
    calculateFare(
      rides,
      FareType.electronicRegular,
      DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f + DEFAULT_RIDE_PRICE_IN_CENTS + 0f
    );
    calculateFare(rides, FareType.electronicSenior, 100f + 0f + 0f + 0f + 100f + 0f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Total trip time is 2h 30m. Calculate fare with two free transfer windows which include agencies which do not permit
   * free transfers. The free transfers will be applied for Kitsap, but not for WSF nor Skagit. Note: Not a real world
   * trip!
   */
  @Test
  public void calculateFareThatIncludesNoFreeTransfers() {
    List<Leg> rides = List.of(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 30, "VashonIsland-Fauntelroy"),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(SKAGIT_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 150, "Fauntleroy-VashonIsland")
    );
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 4 + 610f);
    calculateFare(rides, FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 3 + 50f + 305f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(
      rides,
      FareType.electronicSpecial,
      100f + DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 610f
    );
    calculateFare(rides, FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS * 3 + 610f);
    calculateFare(rides, FareType.electronicSenior, 100f + 50f + 100f + 305f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Total trip time is 4h 30m. This is equivalent to three transfer windows and therefore three Orca fare charges.
   */
  @Test
  public void calculateFareThatExceedsTwoHourFreeTransferWindowTwice() {
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
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 10);
    calculateFare(rides, FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 10);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(
      rides,
      FareType.electronicSpecial,
      100f + 0f + 0f + 0f + 100f + 0f + 0f + 0f + 100f + 0f
    );
    calculateFare(
      rides,
      FareType.electronicRegular,
      DEFAULT_RIDE_PRICE_IN_CENTS +
      0f +
      0f +
      0f +
      DEFAULT_RIDE_PRICE_IN_CENTS +
      0f +
      0f +
      0f +
      DEFAULT_RIDE_PRICE_IN_CENTS
    );
    calculateFare(
      rides,
      FareType.electronicSenior,
      100f + 0f + 0f + 0f + 100f + 0f + 0f + 0f + 100f + 0f
    );
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * This trip starts with a cash fare so the free transfer window doesn't start until the second transfer. Therefore,
   * all subsequent transfers will come under one transfer window and only one Orca discount charge will apply.
   */
  @Test
  public void calculateFareThatStartsWithACashFare() {
    List<Leg> rides = List.of(
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 149)
    );
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
    calculateFare(rides, FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(
      rides,
      FareType.electronicSpecial,
      DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 0f + 0f + 0f + 0f
    );
    calculateFare(
      rides,
      FareType.electronicRegular,
      DEFAULT_RIDE_PRICE_IN_CENTS + DEFAULT_RIDE_PRICE_IN_CENTS + 0f + 0f + 0f
    );
    calculateFare(
      rides,
      FareType.electronicSenior,
      DEFAULT_RIDE_PRICE_IN_CENTS + 100f + 0f + 0f + 0f + 0f
    );
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Single trip with Kitsap transit fast ferry east to confirm correct non Orca fares are applied.
   */
  @Test
  public void calculateFareForKitsapFastFerryEastAgency() {
    List<Leg> rides = List.of(getLeg(KITSAP_TRANSIT_AGENCY_ID, 0, 4, "Kitsap Fast Ferry", "east"));
    calculateFare(rides, regular, 200f);
    calculateFare(rides, FareType.senior, 200f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.electronicRegular, 200f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Single trip (Point Defiance - Tahlequah) with WSF transit to confirm correct non Orca fares are applied.
   */
  @Test
  public void calculateFareForWSFPtToTahlequah() {
    List<Leg> rides = List.of(
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0, "Point Defiance - Tahlequah")
    );
    calculateFare(rides, regular, 610f);
    calculateFare(rides, FareType.senior, 305f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 610f);
    calculateFare(rides, FareType.electronicRegular, 610f);
    calculateFare(rides, FareType.electronicSenior, 305f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Single trip with Link Light Rail to ensure distance fare is calculated correctly.
   */
  @Test
  public void calculateFareForLightRailLeg() {
    List<Leg> rides = List.of(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Roosevelt Station", "Int'l Dist/Chinatown")
    );
    calculateFare(rides, regular, 250f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 250f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
    // Ensure that it works in reverse
    rides =
      List.of(
        getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Int'l Dist/Chinatown", "Roosevelt Station")
      );
    calculateFare(rides, regular, 250f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 250f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  @Test
  public void calculateFareForSounderLeg() {
    List<Leg> rides = List.of(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "S Line", 0, "King Street Station", "Auburn Station")
    );
    calculateFare(rides, regular, 425f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 425f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
    // Ensure that it works in reverse
    rides =
      List.of(
        getLeg(SOUND_TRANSIT_AGENCY_ID, "N Line", 0, "King Street Station", "Everett Station")
      );
    calculateFare(rides, regular, 500f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 500f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  /**
   * Create a few Sound Transit trips but under the contracted agency IDs.
   * SoundTransit contracts their bus service, so their routes show under the contracted agency's IDs in the GTFS feed.
   * Make sure that we get ST's bus fare and not the contracted agency's fare.
   */
  @Test
  public void calculateSoundTransitBusFares() {
    List<Leg> rides = List.of(
      getLeg(COMM_TRANS_AGENCY_ID, "512", 0),
      getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "594", 120),
      getLeg(KC_METRO_AGENCY_ID, "550", 240)
    );
    calculateFare(rides, regular, 975f);
    calculateFare(rides, FareType.senior, 300f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 450f);
    calculateFare(rides, FareType.electronicRegular, 975f);
    calculateFare(rides, FareType.electronicSenior, 300f);
    calculateFare(rides, FareType.electronicYouth, 0f);

    // Also make sure that PT's 500 and 501 get regular Pierce fare and not ST's fare
    rides =
      List.of(
        getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "500", 0),
        getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "501", 60)
      );
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
    calculateFare(rides, FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  @Test
  public void calculateCashFreeTransferKCMetro() {
    List<Leg> rides = List.of(
      getLeg(KC_METRO_AGENCY_ID, 0),
      getLeg(KC_METRO_AGENCY_ID, 20),
      getLeg(COMM_TRANS_AGENCY_ID, 45),
      getLeg(KC_METRO_AGENCY_ID, 60),
      getLeg(KC_METRO_AGENCY_ID, 130)
    );
    calculateFare(rides, regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
    calculateFare(rides, FareType.senior, 325f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 300f);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
    calculateFare(rides, FareType.electronicSenior, 125f); // Transfer extended by CT ride
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  @Test
  public void calculateTransferExtension() {
    List<Leg> rides = List.of(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Int'l Dist/Chinatown", "Roosevelt Station"), // 2.50
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 60, "Roosevelt Station", "Angle Lake Station"), // 3.25, should extend transfer
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 140, "Int'l Dist/Chinatown", "Angle Lake Station") // 3.00, should be free under extended transfer
    );
    calculateFare(rides, regular, 250f + 325f + 300f);
    calculateFare(rides, FareType.senior, 100f * 3);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f * 2);
    calculateFare(rides, FareType.electronicRegular, 325f); // transfer extended on second leg
    calculateFare(rides, FareType.electronicSenior, 100f * 2);
    calculateFare(rides, FareType.electronicYouth, 0f);
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

    var fare = new ItineraryFares();
    orcaFareService.populateFare(fare, USD, type, legs, null);
    assertNotNull(fare.getFare(type));
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

    var fare = new ItineraryFares();
    orcaFareService.populateFare(fare, USD, type, legs, null);
    assertNotNull(fare.getFare(type));
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
    Agency agency = Agency
      .of(new FeedScopedId("A", agencyId))
      .withName(agencyId)
      .withTimezone(ZoneIds.NEW_YORK.getId())
      .build();

    // Set up stops
    RegularStop firstStop = RegularStop
      .of(new FeedScopedId("A", "1"))
      .withCoordinate(new WgsCoordinate(1, 1))
      .withName(new NonLocalizedString(firstStopName))
      .build();
    RegularStop lastStop = RegularStop
      .of(new FeedScopedId("A", "2"))
      .withCoordinate(new WgsCoordinate(1, 2))
      .withName(new NonLocalizedString(lastStopName))
      .build();

    FeedScopedId routeFeedScopeId = new FeedScopedId("A", routeId);
    NonLocalizedString longName = null;
    if (routeLongName != null) {
      longName = new NonLocalizedString(routeLongName);
    }
    Route route = Route
      .of(routeFeedScopeId)
      .withAgency(agency)
      .withShortName(shortName)
      .withLongName(longName)
      // TODO: Way to convert from TransitMode to int (GTFS)?
      .withMode(TransitMode.BUS)
      .withGtfsType(transitMode)
      .build();

    int start = (int) (T11_00 + (startTimeMins * 60));
    var itin = newItinerary(Place.forStop(firstStop), start)
      .transit(route, tripId, start, T11_12, 5, 7, Place.forStop(lastStop), null, null, null)
      .build();

    return itin.getLegs().get(0);
  }

  private static class TestOrcaFareService extends OrcaFareService {

    public TestOrcaFareService(Collection<FareRuleSet> regularFareRules) {
      super(regularFareRules);
    }

    @Override
    protected float calculateCost(
      FareType fareType,
      List<Leg> rides,
      Collection<FareRuleSet> fareRules
    ) {
      return DEFAULT_TEST_RIDE_PRICE;
    }
  }
}
