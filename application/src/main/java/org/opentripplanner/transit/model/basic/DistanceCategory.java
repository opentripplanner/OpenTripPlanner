package org.opentripplanner.transit.model.basic;

import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The distance category of a transit service: how far it travels and which kind of journeys it
 * is meant for. This lets clients tell apart services that share a main mode, e.g. suburban,
 * regional and long-distance trains, which all have the main mode {@link TransitMode#RAIL}.
 * <p>
 * The category is not part of the input data, but derived from the NeTEx submode or the GTFS
 * extended route type, in the same way as the replacement feature. Every trip has a category; there
 * is deliberately no "unknown" value, as it would complicate the clients. Trips whose submode or
 * route type isn't mapped get the default category of their main mode, which is a best guess based on
 * the generic route type of the mode, e.g. a plain GTFS route type 2 is {@link #REGIONAL}.
 * <p>
 * The mapping is not configurable. It can't be right for every deployment, e.g. the HS1 domestic
 * services in the UK are mapped to {@link #LONG_DISTANCE} like all high speed rail, although they
 * are regional services.
 */
public enum DistanceCategory {
  /**
   * Services within a city or its suburbs, e.g. S-Bahn, urban railways, trams, shuttles and
   * local buses.
   */
  LOCAL,
  /** Services connecting the towns and cities of a region. */
  REGIONAL,
  /** National and international services, including high speed rail and night trains. */
  LONG_DISTANCE;

  /**
   * The categories of one main mode.
   *
   * @param defaultCategory the category of all trips of the mode that aren't mapped, derived from the
   *                     generic GTFS route type of the mode, e.g. 100 for rail
   * @param gtfsExtendedTypes the categories of GTFS extended route types that differ from the default
   * @param netexSubmodes the categories of NeTEx submodes that differ from the default
   */
  private record ModeCategories(
    DistanceCategory defaultCategory,
    Map<Integer, DistanceCategory> gtfsExtendedTypes,
    Map<String, DistanceCategory> netexSubmodes
  ) {
    ModeCategories(DistanceCategory defaultCategory) {
      this(defaultCategory, Map.of(), Map.of());
    }
  }

  /**
   * The categories are keyed by the trip's main mode: a trip can inherit the submode of its route while
   * having another main mode, e.g. a bus ServiceJourney without a submode on a rail line with the
   * submode regionalRail. Such a bus trip must not get the category of the rail submode.
   */
  private static final Map<TransitMode, ModeCategories> CATEGORIES = categories();

  /**
   * Only the types that differ from the default of their mode are listed. NeTEx submodes marked
   * with "twin" have no entry in the agreed mapping, but are mapped like their GTFS counterpart so
   * that both data sources give the same result.
   */
  private static Map<TransitMode, ModeCategories> categories() {
    var categories = new EnumMap<TransitMode, ModeCategories>(TransitMode.class);

    // GTFS 100 Railway Service
    categories.put(
      TransitMode.RAIL,
      new ModeCategories(
        REGIONAL,
        Map.ofEntries(
          // 101 High Speed Rail, 102 Long Distance Trains, 103 Inter Regional Rail,
          // 104 Car Transport Rail
          Map.entry(101, LONG_DISTANCE),
          Map.entry(102, LONG_DISTANCE),
          Map.entry(103, LONG_DISTANCE),
          Map.entry(104, LONG_DISTANCE),
          // 105 Sleeper Rail, 107 Tourist Railway, 108 Rail Shuttle (Within Complex)
          Map.entry(105, LONG_DISTANCE),
          Map.entry(107, LOCAL),
          Map.entry(108, LOCAL),
          // 109 Suburban Railway, 112 Lorry Transport Rail, 114 Cross-Country Rail
          Map.entry(109, LOCAL),
          Map.entry(112, LONG_DISTANCE),
          Map.entry(114, LONG_DISTANCE),
          // 115 Vehicle Transport Rail, 116 Rack and Pinion Railway
          Map.entry(115, LONG_DISTANCE),
          Map.entry(116, LOCAL),
          // 400 and 403 Urban Railway Service, 404 All Urban Railway Services
          Map.entry(400, LOCAL),
          Map.entry(403, LOCAL),
          Map.entry(404, LOCAL)
        ),
        Map.ofEntries(
          Map.entry("local", LOCAL),
          Map.entry("highSpeedRail", LONG_DISTANCE),
          Map.entry("suburbanRailway", LOCAL),
          Map.entry("interregionalRail", LONG_DISTANCE),
          Map.entry("longDistance", LONG_DISTANCE),
          Map.entry("international", LONG_DISTANCE),
          Map.entry("railShuttle", LOCAL),
          Map.entry("crossCountryRail", LONG_DISTANCE),
          // twins
          Map.entry("airportLinkRail", LOCAL),
          Map.entry("carTransportRailService", LONG_DISTANCE),
          Map.entry("nightRail", LONG_DISTANCE),
          Map.entry("sleeperRailService", LONG_DISTANCE),
          Map.entry("rackAndPinionRailway", LOCAL),
          Map.entry("touristRailway", LOCAL)
        )
      )
    );

    // GTFS 200 Coach Service
    categories.put(
      TransitMode.COACH,
      new ModeCategories(
        LONG_DISTANCE,
        Map.ofEntries(
          // 203 Shuttle Coach, 204 Regional Coach, 206 Sightseeing Coach, 207 Tourist Coach
          Map.entry(203, LOCAL),
          Map.entry(204, REGIONAL),
          Map.entry(206, REGIONAL),
          Map.entry(207, REGIONAL),
          // 208 Commuter Coach
          Map.entry(208, LOCAL)
        ),
        Map.ofEntries(
          Map.entry("shuttleCoach", LOCAL),
          Map.entry("regionalCoach", REGIONAL),
          // twins
          Map.entry("commuterCoach", LOCAL),
          Map.entry("sightseeingCoach", REGIONAL),
          Map.entry("touristCoach", REGIONAL)
        )
      )
    );

    // GTFS 700 Bus Service
    categories.put(
      TransitMode.BUS,
      new ModeCategories(
        LOCAL,
        // 701 Regional Bus, 706 Post Bus Service, 714 Rail Replacement Bus
        Map.ofEntries(Map.entry(701, REGIONAL), Map.entry(706, REGIONAL), Map.entry(714, REGIONAL)),
        Map.ofEntries(
          Map.entry("regionalBus", REGIONAL),
          // twins
          Map.entry("postBus", REGIONAL),
          Map.entry("railReplacementBus", REGIONAL)
        )
      )
    );

    // GTFS 900 Tram Service, 903 Regional Tram
    categories.put(
      TransitMode.TRAM,
      new ModeCategories(LOCAL, Map.of(903, REGIONAL), Map.of("regionalTram", REGIONAL))
    );

    // GTFS 1000 Water Transport Service and 1200 Ferry Service
    categories.put(
      TransitMode.FERRY,
      new ModeCategories(
        REGIONAL,
        Map.of(),
        Map.ofEntries(
          Map.entry("internationalCarFerry", LONG_DISTANCE),
          Map.entry("nationalCarFerry", LONG_DISTANCE),
          Map.entry("localCarFerry", LOCAL),
          Map.entry("internationalPassengerFerry", LONG_DISTANCE),
          Map.entry("nationalPassengerFerry", LONG_DISTANCE),
          Map.entry("localPassengerFerry", LOCAL),
          Map.entry("shuttleFerryService", LOCAL)
        )
      )
    );

    // GTFS 1100 Air Service
    categories.put(
      TransitMode.AIRPLANE,
      new ModeCategories(LONG_DISTANCE, Map.of(), Map.of("shuttleFlight", LOCAL))
    );

    // All other modes are local: subway (GTFS 401 and 402), monorail (405), trolleybus (800),
    // cable car and gondola (1300 range), funicular (1400), taxi (1500 range), carpool and snow
    // and ice
    for (var mode : TransitMode.values()) {
      categories.computeIfAbsent(mode, m -> new ModeCategories(LOCAL));
    }
    return categories;
  }

  /**
   * Derive the distance category of a trip from its main mode and its NeTEx submode or GTFS
   * extended route type. Only one of the submode and route type is set, depending on the data
   * source. They are only used if they are mapped for the trip's main mode, otherwise the trip
   * gets the default category of the mode.
   */
  public static DistanceCategory of(
    TransitMode mainMode,
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    var categories = CATEGORIES.get(mainMode);
    var category = categories.netexSubmodes().get(netexSubmode.name());
    if (category == null && gtfsExtendedType != null) {
      category = categories.gtfsExtendedTypes().get(gtfsExtendedType);
    }
    return category != null ? category : categories.defaultCategory();
  }
}
