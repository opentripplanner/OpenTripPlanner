package org.opentripplanner.transit.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.transit.model.basic.DistanceCategory.LOCAL;
import static org.opentripplanner.transit.model.basic.DistanceCategory.LONG_DISTANCE;
import static org.opentripplanner.transit.model.basic.DistanceCategory.REGIONAL;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.gtfs.mapping.TransitModeMapper;

class DistanceCategoryTest {

  /**
   * The agreed categorization of the GTFS extended route types. The OTP mode is not part of the
   * table, it is derived from the route type like in the GTFS import.
   */
  @ParameterizedTest
  @CsvSource({
    // Rail
    "100, REGIONAL",
    "101, LONG_DISTANCE",
    "102, LONG_DISTANCE",
    "103, LONG_DISTANCE",
    "104, LONG_DISTANCE",
    "105, LONG_DISTANCE",
    "106, REGIONAL",
    "107, LOCAL",
    "108, LOCAL",
    "109, LOCAL",
    "110, REGIONAL",
    "111, REGIONAL",
    "112, LONG_DISTANCE",
    "113, REGIONAL",
    "114, LONG_DISTANCE",
    "115, LONG_DISTANCE",
    "116, LOCAL",
    "117, REGIONAL",
    // Coach
    "200, LONG_DISTANCE",
    "201, LONG_DISTANCE",
    "202, LONG_DISTANCE",
    "203, LOCAL",
    "204, REGIONAL",
    "205, LONG_DISTANCE",
    "206, REGIONAL",
    "207, REGIONAL",
    "208, LOCAL",
    "209, LONG_DISTANCE",
    // Urban railway
    "400, LOCAL",
    "401, LOCAL",
    "402, LOCAL",
    "403, LOCAL",
    "404, LOCAL",
    "405, LOCAL",
    // Bus
    "700, LOCAL",
    "701, REGIONAL",
    "702, LOCAL",
    "703, LOCAL",
    "704, LOCAL",
    "705, LOCAL",
    "706, REGIONAL",
    "707, LOCAL",
    "708, LOCAL",
    "709, LOCAL",
    "710, LOCAL",
    "711, LOCAL",
    "712, LOCAL",
    "713, LOCAL",
    "714, REGIONAL",
    "715, LOCAL",
    "716, LOCAL",
    // Trolleybus and tram
    "800, LOCAL",
    "900, LOCAL",
    "901, LOCAL",
    "902, LOCAL",
    "903, REGIONAL",
    "904, LOCAL",
    "905, LOCAL",
    "906, LOCAL",
    // Water, air, cable, funicular and taxi
    "1000, REGIONAL",
    "1100, LONG_DISTANCE",
    "1200, REGIONAL",
    "1300, LOCAL",
    "1301, LOCAL",
    "1302, LOCAL",
    "1303, LOCAL",
    "1304, LOCAL",
    "1305, LOCAL",
    "1306, LOCAL",
    "1307, LOCAL",
    "1400, LOCAL",
    "1500, LOCAL",
    "1501, LOCAL",
    "1502, LOCAL",
    "1503, LOCAL",
    "1504, LOCAL",
    "1505, LOCAL",
    "1506, LOCAL",
    "1507, LOCAL",
  })
  void ofGtfsExtendedType(int gtfsType, DistanceCategory expected) {
    var mode = TransitModeMapper.mapMode(gtfsType);
    assertThat(DistanceCategory.of(mode, SubMode.UNKNOWN, gtfsType)).isEqualTo(expected);
  }

  /**
   * The agreed categorization of the NeTEx submodes, plus the submodes marked as "twin" in
   * {@link DistanceCategory}, which are mapped like their GTFS counterpart.
   */
  @ParameterizedTest
  @CsvSource({
    // Rail
    "RAIL, local, LOCAL",
    "RAIL, highSpeedRail, LONG_DISTANCE",
    "RAIL, suburbanRailway, LOCAL",
    "RAIL, regionalRail, REGIONAL",
    "RAIL, interregionalRail, LONG_DISTANCE",
    "RAIL, longDistance, LONG_DISTANCE",
    "RAIL, international, LONG_DISTANCE",
    "RAIL, railShuttle, LOCAL",
    "RAIL, crossCountryRail, LONG_DISTANCE",
    "RAIL, airportLinkRail, LOCAL",
    "RAIL, carTransportRailService, LONG_DISTANCE",
    "RAIL, nightRail, LONG_DISTANCE",
    "RAIL, sleeperRailService, LONG_DISTANCE",
    "RAIL, rackAndPinionRailway, LOCAL",
    "RAIL, touristRailway, LOCAL",
    "RAIL, replacementRailService, REGIONAL",
    "RAIL, specialTrain, REGIONAL",
    // Coach
    "COACH, internationalCoach, LONG_DISTANCE",
    "COACH, nationalCoach, LONG_DISTANCE",
    "COACH, shuttleCoach, LOCAL",
    "COACH, regionalCoach, REGIONAL",
    "COACH, commuterCoach, LOCAL",
    "COACH, sightseeingCoach, REGIONAL",
    "COACH, touristCoach, REGIONAL",
    "COACH, specialCoach, LONG_DISTANCE",
    "COACH, schoolCoach, LONG_DISTANCE",
    // Metro
    "SUBWAY, urbanRailway, LOCAL",
    "SUBWAY, metro, LOCAL",
    // Bus
    "BUS, localBus, LOCAL",
    "BUS, regionalBus, REGIONAL",
    "BUS, shuttleBus, LOCAL",
    "BUS, postBus, REGIONAL",
    "BUS, railReplacementBus, REGIONAL",
    // Tram
    "TRAM, cityTram, LOCAL",
    "TRAM, localTram, LOCAL",
    "TRAM, regionalTram, REGIONAL",
    "TRAM, shuttleTram, LOCAL",
    // Water
    "FERRY, internationalCarFerry, LONG_DISTANCE",
    "FERRY, nationalCarFerry, LONG_DISTANCE",
    "FERRY, regionalCarFerry, REGIONAL",
    "FERRY, localCarFerry, LOCAL",
    "FERRY, internationalPassengerFerry, LONG_DISTANCE",
    "FERRY, nationalPassengerFerry, LONG_DISTANCE",
    "FERRY, regionalPassengerFerry, REGIONAL",
    "FERRY, localPassengerFerry, LOCAL",
    "FERRY, shuttleFerryService, LOCAL",
    // Air
    "AIRPLANE, internationalFlight, LONG_DISTANCE",
    "AIRPLANE, domesticFlight, LONG_DISTANCE",
    "AIRPLANE, intercontinentalFlight, LONG_DISTANCE",
    "AIRPLANE, domesticScheduledFlight, LONG_DISTANCE",
    "AIRPLANE, shuttleFlight, LOCAL",
    "AIRPLANE, intercontinentalCharterFlight, LONG_DISTANCE",
    "AIRPLANE, internationalCharterFlight, LONG_DISTANCE",
    "AIRPLANE, domesticCharterFlight, LONG_DISTANCE",
    "AIRPLANE, SchengenAreaFlight, LONG_DISTANCE",
    "AIRPLANE, shortHaulInternationalFlight, LONG_DISTANCE",
  })
  void ofNetexSubmode(TransitMode mode, String submode, DistanceCategory expected) {
    assertThat(DistanceCategory.of(mode, SubMode.of(submode), null)).isEqualTo(expected);
  }

  private static final Map<TransitMode, DistanceCategory> DEFAULT_CATEGORIES = Map.ofEntries(
    Map.entry(TransitMode.RAIL, REGIONAL),
    Map.entry(TransitMode.COACH, LONG_DISTANCE),
    Map.entry(TransitMode.SUBWAY, LOCAL),
    Map.entry(TransitMode.BUS, LOCAL),
    Map.entry(TransitMode.TRAM, LOCAL),
    Map.entry(TransitMode.FERRY, REGIONAL),
    Map.entry(TransitMode.AIRPLANE, LONG_DISTANCE),
    Map.entry(TransitMode.CABLE_CAR, LOCAL),
    Map.entry(TransitMode.GONDOLA, LOCAL),
    Map.entry(TransitMode.FUNICULAR, LOCAL),
    Map.entry(TransitMode.TROLLEYBUS, LOCAL),
    Map.entry(TransitMode.MONORAIL, LOCAL),
    Map.entry(TransitMode.CARPOOL, LOCAL),
    Map.entry(TransitMode.TAXI, LOCAL),
    Map.entry(TransitMode.SNOW_AND_ICE, LOCAL)
  );

  @Test
  void allModesHaveADefaultCategory() {
    assertThat(DEFAULT_CATEGORIES.keySet()).containsExactlyElementsIn(TransitMode.values());
  }

  /**
   * There is no unknown category, so trips without a specific submode or route type get the default
   * category of their mode, e.g. a plain GTFS route type 2.
   */
  @ParameterizedTest
  @EnumSource(TransitMode.class)
  void unmappedTripsGetTheDefaultCategoryOfTheirMode(TransitMode mode) {
    var expected = DEFAULT_CATEGORIES.get(mode);

    assertThat(DistanceCategory.of(mode, SubMode.UNKNOWN, null)).isEqualTo(expected);
    assertThat(DistanceCategory.of(mode, SubMode.of("unknown"), null)).isEqualTo(expected);
    assertThat(DistanceCategory.of(mode, SubMode.UNKNOWN, 99999)).isEqualTo(expected);
  }

  @Test
  void plainGtfsRouteTypes() {
    assertThat(DistanceCategory.of(TransitMode.RAIL, SubMode.UNKNOWN, 2)).isEqualTo(REGIONAL);
    assertThat(DistanceCategory.of(TransitMode.BUS, SubMode.UNKNOWN, 3)).isEqualTo(LOCAL);
    assertThat(DistanceCategory.of(TransitMode.TRAM, SubMode.UNKNOWN, 0)).isEqualTo(LOCAL);
    assertThat(DistanceCategory.of(TransitMode.FERRY, SubMode.UNKNOWN, 4)).isEqualTo(REGIONAL);
  }

  /**
   * A trip can inherit the submode of its route while having another main mode, e.g. a bus
   * ServiceJourney without a submode on a rail line, and the route type of a trip is the one of its
   * route. The submode and route type are only used for the mode they belong to.
   */
  @Test
  void submodeOrRouteTypeOfOtherModeIsIgnored() {
    var regionalRail = SubMode.of("regionalRail");
    var suburbanRailway = SubMode.of("suburbanRailway");
    assertThat(DistanceCategory.of(TransitMode.BUS, suburbanRailway, null)).isEqualTo(LOCAL);
    assertThat(DistanceCategory.of(TransitMode.COACH, regionalRail, null)).isEqualTo(LONG_DISTANCE);
    assertThat(DistanceCategory.of(TransitMode.COACH, suburbanRailway, null)).isEqualTo(
      LONG_DISTANCE
    );
    assertThat(DistanceCategory.of(TransitMode.RAIL, SubMode.of("shuttleCoach"), null)).isEqualTo(
      REGIONAL
    );
    assertThat(DistanceCategory.of(TransitMode.BUS, SubMode.UNKNOWN, 106)).isEqualTo(LOCAL);
    assertThat(DistanceCategory.of(TransitMode.RAIL, SubMode.UNKNOWN, 204)).isEqualTo(REGIONAL);
  }

  @Test
  void netexSubmodeTakesPrecedenceOverGtfsExtendedType() {
    var rail = TransitMode.RAIL;
    assertThat(DistanceCategory.of(rail, SubMode.of("longDistance"), 109)).isEqualTo(LONG_DISTANCE);
    assertThat(DistanceCategory.of(rail, SubMode.of("suburbanRailway"), 101)).isEqualTo(LOCAL);
    assertThat(DistanceCategory.of(rail, SubMode.of("railShuttle"), 102)).isEqualTo(LOCAL);
  }
}
