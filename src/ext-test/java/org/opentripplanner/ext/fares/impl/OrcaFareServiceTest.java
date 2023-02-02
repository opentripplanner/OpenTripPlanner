package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.COMM_TRANS_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.KC_METRO_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.KITSAP_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.PIERCE_COUNTY_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.SKAGIT_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.SOUND_TRANSIT_AGENCY_ID;
import static org.opentripplanner.ext.fares.impl.OrcaFareServiceImpl.WASHINGTON_STATE_FERRIES_AGENCY_ID;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_12;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.model.FareContainer;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.RiderCategory;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
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

  private static OrcaFareServiceImpl orcaFareService;
  private static float DEFAULT_RIDE_PRICE_IN_CENTS;

  @BeforeAll
  public static void setUpClass() {
    Map<FeedScopedId, FareRuleSet> regularFareRules = new HashMap<>();
    orcaFareService = new OrcaFareServiceImpl(regularFareRules.values());
    orcaFareService.IS_TEST = true;
    DEFAULT_RIDE_PRICE_IN_CENTS = OrcaFareServiceImpl.DEFAULT_TEST_RIDE_PRICE * 100;
  }

  /**
   * These tests are designed to specifically validate Orca fares. Since these fares are hard-coded, it is acceptable
   * to make direct calls to the Orca fare service with predefined routes. Where the default fare is applied a test
   * substitute {@link OrcaFareServiceImpl#DEFAULT_TEST_RIDE_PRICE} is used. This will be the same for all cash fare
   * types.
   */
  private static void calculateFare(List<Leg> legs, FareType fareType, float expectedFareInCents) {
    ItineraryFares fare = new ItineraryFares();
    orcaFareService.populateFare(fare, null, fareType, legs, null);
    Assertions.assertEquals(expectedFareInCents, fare.getFare(fareType).cents());
  }

  private static void assertLegFareEquals(int fare, Leg leg, ItineraryFares fares, boolean hasXfer) {
    var legFareProducts = fares.getLegProducts().get(leg);

    var rideCost = legFareProducts
      .stream()
      .filter(fp ->
        fp.container().name().equals("electronic") &&
          fp.category().name().equals("regular") &&
          fp.name().equals("rideCost")
      )
      .findFirst();
    if (rideCost.isEmpty()) {
      Assertions.fail("Missing leg fare product.");
    }
    Assertions.assertEquals(fare, rideCost.get().amount().cents());

    var transfer = legFareProducts
      .stream()
      .filter(fp ->
        fp.container().name().equals("electronic") &&
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
    List<Leg> rides = Collections.singletonList(getLeg(COMM_TRANS_AGENCY_ID, "400", 0));
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS);
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
    List<Leg> rides = Arrays.asList(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 1),
      getLeg(COMM_TRANS_AGENCY_ID, 2)
    );
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
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
    List<Leg> rides = Arrays.asList(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(COMM_TRANS_AGENCY_ID, 2)
    );
    ItineraryFares fares = new ItineraryFares();
    orcaFareService.populateFare(fares, null, FareType.electronicRegular, rides, null);

    assertLegFareEquals(349, rides.get(0), fares, false);
    assertLegFareEquals(0, rides.get(1), fares, true);
    //    Assertions.assertFalse(rides.get(0).fareComponents.get(FareType.electronicRegular).isTransfer);
    //    Assertions.assertTrue(rides.get(1).fareComponents.get(FareType.electronicRegular).isTransfer);
  }

  /**
   * Total trip time is 2h 30m. The first four transfers are within the permitted two hour window. A single (highest)
   * Orca fare will be charged for these transfers. The fifth transfer is outside of the original two hour window so
   * a single Orca fare for this leg is applied and the two hour window will start again. The final transfer is within
   * the new two hour window and will be free.
   */
  @Test
  public void calculateFareThatExceedsTwoHourFreeTransferWindow() {
    List<Leg> rides = Arrays.asList(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 150)
    );
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
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
    List<Leg> rides = Arrays.asList(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 30, "VashonIsland-Fauntelroy"),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(SKAGIT_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 150, "Fauntleroy-VashonIsland")
    );
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 4 + 610f);
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
    List<Leg> rides = Arrays.asList(
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
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 10);
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
    List<Leg> rides = Arrays.asList(
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 30),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 60),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 90),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 120),
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 149)
    );
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 6);
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
    List<Leg> rides = Collections.singletonList(
      getLeg(KITSAP_TRANSIT_AGENCY_ID, 0, 4, "Kitsap Fast Ferry", "east")
    );
    calculateFare(rides, FareType.regular, 200f);
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
    List<Leg> rides = Collections.singletonList(
      getLeg(WASHINGTON_STATE_FERRIES_AGENCY_ID, 0, "Point Defiance - Tahlequah")
    );
    calculateFare(rides, FareType.regular, 610f);
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
    List<Leg> rides = Collections.singletonList(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Roosevelt Station", "Int'l Dist/Chinatown")
    );
    calculateFare(rides, FareType.regular, 250f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 250f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
    // Ensure that it works in reverse
    rides =
      Collections.singletonList(
        getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Int'l Dist/Chinatown", "Roosevelt Station")
      );
    calculateFare(rides, FareType.regular, 250f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 250f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  @Test
  public void calculateFareForSounderLeg() {
    List<Leg> rides = Collections.singletonList(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "S Line", 0, "King Street Station", "Auburn Station")
    );
    calculateFare(rides, FareType.regular, 425f);
    calculateFare(rides, FareType.senior, 100f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f);
    calculateFare(rides, FareType.electronicRegular, 425f);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
    // Ensure that it works in reverse
    rides =
      Collections.singletonList(
        getLeg(SOUND_TRANSIT_AGENCY_ID, "N Line", 0, "King Street Station", "Everett Station")
      );
    calculateFare(rides, FareType.regular, 500f);
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
    List<Leg> rides = Arrays.asList(
      getLeg(COMM_TRANS_AGENCY_ID, "512", 0),
      getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "594", 120),
      getLeg(KC_METRO_AGENCY_ID, "550", 240)
    );
    calculateFare(rides, FareType.regular, 975f);
    calculateFare(rides, FareType.senior, 300f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 450f);
    calculateFare(rides, FareType.electronicRegular, 975f);
    calculateFare(rides, FareType.electronicSenior, 300f);
    calculateFare(rides, FareType.electronicYouth, 0f);

    // Also make sure that PT's 500 and 501 get regular Pierce fare and not ST's fare
    rides =
      Arrays.asList(
        getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "500", 0),
        getLeg(PIERCE_COUNTY_TRANSIT_AGENCY_ID, "501", 60)
      );
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
    calculateFare(rides, FareType.senior, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS);
    calculateFare(rides, FareType.electronicSenior, 100f);
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  @Test
  public void calculateCashFreeTransferKCMetro() {
    List<Leg> rides = Arrays.asList(
      getLeg(KC_METRO_AGENCY_ID, 0),
      getLeg(KC_METRO_AGENCY_ID, 20),
      getLeg(COMM_TRANS_AGENCY_ID, 45),
      getLeg(KC_METRO_AGENCY_ID, 60),
      getLeg(KC_METRO_AGENCY_ID, 130)
    );
    calculateFare(rides, FareType.regular, DEFAULT_RIDE_PRICE_IN_CENTS * 3);
    calculateFare(rides, FareType.senior, 325f);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 300f);
    calculateFare(rides, FareType.electronicRegular, DEFAULT_RIDE_PRICE_IN_CENTS * 2);
    calculateFare(rides, FareType.electronicSenior, 125f); // Transfer extended by CT ride
    calculateFare(rides, FareType.electronicYouth, 0f);
  }

  @Test
  public void calculateTransferExtension() {
    List<Leg> rides = Arrays.asList(
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 0, "Int'l Dist/Chinatown", "Roosevelt Station"), // 2.50
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 60, "Roosevelt Station", "Angle Lake Station"), // 3.25, should extend transfer
      getLeg(SOUND_TRANSIT_AGENCY_ID, "1-Line", 140, "Int'l Dist/Chinatown", "Angle Lake Station") // 3.00, should be free under extended transfer
    );
    calculateFare(rides, FareType.regular, 250f + 325f + 300f);
    calculateFare(rides, FareType.senior, 100f * 3);
    calculateFare(rides, FareType.youth, 0f);
    calculateFare(rides, FareType.electronicSpecial, 150f * 2);
    calculateFare(rides, FareType.electronicRegular, 325f); // transfer extended on second leg
    calculateFare(rides, FareType.electronicSenior, 100f * 2);
    calculateFare(rides, FareType.electronicYouth, 0f);
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
   * Create a {@link Leg} containing route data that will be used by {@link OrcaFareServiceImpl} to determine the
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
   * Create a {@link Leg} containing route data that will be used by {@link OrcaFareServiceImpl} to determine the
   * correct ride type.
   */
  private static Leg createLeg(
    String agencyId,
    String shortName,
    int transitMode,
    long startTimeMins,
    String routeId,
    String tripId,
    String routeLongName,
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
    Route route = Route
      .of(routeFeedScopeId)
      .withAgency(agency)
      .withShortName(shortName)
      .withLongName(new NonLocalizedString(routeLongName))
      // TODO: Way to convert from TransitMode to int (GTFS)?
      .withMode(TransitMode.BUS)
      .withGtfsType(transitMode)
      .build();

    int start = (int) (T11_00 + (startTimeMins * 60));
    var itin = newItinerary(Place.forStop(firstStop), start)
      .bus(route, tripId, start, T11_12, Place.forStop(lastStop))
      .build();

    return itin.getLegs().get(0);
  }
}
