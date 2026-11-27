package org.opentripplanner.transit.service;

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
import org.opentripplanner.transit.model.basic.DistanceCategory;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class DistanceCategoryHelperTest {

  /**
   * The agreed categorization of the GTFS extended route types. The OTP mode is not part of the
   * table, it is derived from the route type like in the GTFS import.
   */
  @ParameterizedTest
  @CsvSource({
    // Railway Service
    "100, REGIONAL",
    // High Speed Rail Service
    "101, LONG_DISTANCE",
    // Long Distance Trains
    "102, LONG_DISTANCE",
    // Inter Regional Rail Service
    "103, LONG_DISTANCE",
    // Car Transport Rail Service
    "104, LONG_DISTANCE",
    // Sleeper Rail Service
    "105, LONG_DISTANCE",
    // Regional Rail Service
    "106, REGIONAL",
    // Tourist Railway Service
    "107, LOCAL",
    // Rail Shuttle (Within Complex)
    "108, LOCAL",
    // Suburban Railway
    "109, LOCAL",
    // Replacement Rail Service
    "110, REGIONAL",
    // Special Rail Service
    "111, REGIONAL",
    // Lorry Transport Rail Service
    "112, LONG_DISTANCE",
    // All Rail Services
    "113, REGIONAL",
    // Cross-Country Rail Service
    "114, LONG_DISTANCE",
    // Vehicle Transport Rail Service
    "115, LONG_DISTANCE",
    // Rack and Pinion Railway
    "116, LOCAL",
    // Additional Rail Service
    "117, REGIONAL",
    // Coach Service
    "200, LONG_DISTANCE",
    // International Coach Service
    "201, LONG_DISTANCE",
    // National Coach Service
    "202, LONG_DISTANCE",
    // Shuttle Coach Service
    "203, LOCAL",
    // Regional Coach Service
    "204, REGIONAL",
    // Special Coach Service
    "205, LONG_DISTANCE",
    // Sightseeing Coach Service
    "206, REGIONAL",
    // Tourist Coach Service
    "207, REGIONAL",
    // Commuter Coach Service
    "208, LOCAL",
    // All Coach Services
    "209, LONG_DISTANCE",
    // Urban Railway Service
    "400, LOCAL",
    // Metro Service
    "401, LOCAL",
    // Underground Service
    "402, LOCAL",
    // Urban Railway Service
    "403, LOCAL",
    // All Urban Railway Services
    "404, LOCAL",
    // Monorail
    "405, LOCAL",
    // Bus Service
    "700, LOCAL",
    // Regional Bus Service
    "701, REGIONAL",
    // Express Bus Service
    "702, LOCAL",
    // Stopping Bus Service
    "703, LOCAL",
    // Local Bus Service
    "704, LOCAL",
    // Night Bus Service
    "705, LOCAL",
    // Post Bus Service
    "706, REGIONAL",
    // Special Needs Bus
    "707, LOCAL",
    // Mobility Bus Service
    "708, LOCAL",
    // Mobility Bus for Registered Disabled
    "709, LOCAL",
    // Sightseeing Bus
    "710, LOCAL",
    // Shuttle Bus
    "711, LOCAL",
    // School Bus
    "712, LOCAL",
    // School and Public Service Bus
    "713, LOCAL",
    // Rail Replacement Bus Service, keeps the bus default like 110 keeps the rail default
    "714, LOCAL",
    // Demand and Response Bus Service
    "715, LOCAL",
    // All Bus Services
    "716, LOCAL",
    // Trolleybus Service
    "800, LOCAL",
    // Tram Service
    "900, LOCAL",
    // City Tram Service
    "901, LOCAL",
    // Local Tram Service
    "902, LOCAL",
    // Regional Tram Service
    "903, REGIONAL",
    // Sightseeing Tram Service
    "904, LOCAL",
    // Shuttle Tram Service
    "905, LOCAL",
    // All Tram Services
    "906, LOCAL",
    // Water Transport Service
    "1000, REGIONAL",
    // Air Service
    "1100, LONG_DISTANCE",
    // Ferry Service
    "1200, REGIONAL",
    // Aerial Lift Service
    "1300, LOCAL",
    // Telecabin Service
    "1301, LOCAL",
    // Cable Car Service
    "1302, LOCAL",
    // Elevator Service
    "1303, LOCAL",
    // Chair Lift Service
    "1304, LOCAL",
    // Drag Lift Service
    "1305, LOCAL",
    // Small Telecabin Service
    "1306, LOCAL",
    // All Telecabin Services
    "1307, LOCAL",
    // Funicular Service
    "1400, LOCAL",
    // Taxi Service
    "1500, LOCAL",
    // Communal Taxi Service
    "1501, LOCAL",
    // Water Taxi Service
    "1502, LOCAL",
    // Rail Taxi Service
    "1503, LOCAL",
    // Bike Taxi Service
    "1504, LOCAL",
    // Licensed Taxi Service
    "1505, LOCAL",
    // Private Hire Service Vehicle
    "1506, LOCAL",
    // All Taxi Services
    "1507, LOCAL",
  })
  void ofGtfsExtendedType(int gtfsType, DistanceCategory expected) {
    var mode = TransitModeMapper.mapMode(gtfsType);
    assertThat(DistanceCategoryHelper.distanceCategory(mode, SubMode.UNKNOWN, gtfsType)).isEqualTo(
      expected
    );
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
    "BUS, railReplacementBus, LOCAL",
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
    assertThat(DistanceCategoryHelper.distanceCategory(mode, SubMode.of(submode), null)).isEqualTo(
      expected
    );
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

    assertThat(DistanceCategoryHelper.distanceCategory(mode, SubMode.UNKNOWN, null)).isEqualTo(
      expected
    );
    assertThat(
      DistanceCategoryHelper.distanceCategory(mode, SubMode.of("unknown"), null)
    ).isEqualTo(expected);
    assertThat(DistanceCategoryHelper.distanceCategory(mode, SubMode.UNKNOWN, 99999)).isEqualTo(
      expected
    );
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
    assertThat(
      DistanceCategoryHelper.distanceCategory(TransitMode.BUS, suburbanRailway, null)
    ).isEqualTo(LOCAL);
    assertThat(
      DistanceCategoryHelper.distanceCategory(TransitMode.COACH, regionalRail, null)
    ).isEqualTo(LONG_DISTANCE);
    assertThat(
      DistanceCategoryHelper.distanceCategory(TransitMode.COACH, suburbanRailway, null)
    ).isEqualTo(LONG_DISTANCE);
    assertThat(
      DistanceCategoryHelper.distanceCategory(TransitMode.RAIL, SubMode.of("shuttleCoach"), null)
    ).isEqualTo(REGIONAL);
    assertThat(
      DistanceCategoryHelper.distanceCategory(TransitMode.BUS, SubMode.UNKNOWN, 106)
    ).isEqualTo(LOCAL);
    assertThat(
      DistanceCategoryHelper.distanceCategory(TransitMode.RAIL, SubMode.UNKNOWN, 204)
    ).isEqualTo(REGIONAL);
  }
}
