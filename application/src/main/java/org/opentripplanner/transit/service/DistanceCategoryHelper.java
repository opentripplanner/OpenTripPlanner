package org.opentripplanner.transit.service;

import static org.opentripplanner.transit.model.basic.DistanceCategory.LOCAL;
import static org.opentripplanner.transit.model.basic.DistanceCategory.LONG_DISTANCE;
import static org.opentripplanner.transit.model.basic.DistanceCategory.REGIONAL;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.model.basic.DistanceCategory;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Derives the {@link DistanceCategory} of a trip from its main mode and its NeTEx submode or GTFS
 * extended route type, in the same way as {@link ReplacementHelper} derives whether a trip is a
 * replacement.
 * <p>
 * Trips whose submode or route type isn't mapped get the default category of their main mode,
 * which is a best guess based on the generic route type of the mode, e.g. a plain GTFS route type 2
 * is {@link DistanceCategory#REGIONAL}. Replacement services keep the default of their mode, the
 * replacement is a separate filter.
 * <p>
 * The mapping is not configurable. It can't be right for every deployment, e.g. the HS1 domestic
 * services in the UK are mapped to {@link DistanceCategory#LONG_DISTANCE} like all high speed rail,
 * although they are regional services.
 * <p>
 * The tables below are not complete: they only list the route types and submodes whose category
 * differs from the default of their mode. The complete mapping is maintained in
 * {@code DistanceCategoryHelperTest}. NeTEx submodes marked with "twin" have no entry in the agreed
 * mapping, but are mapped like their GTFS counterpart so that both data sources give the same
 * result.
 * <p>
 * The categories are keyed by the trip's main mode: a trip can inherit the submode of its route
 * while having another main mode, e.g. a bus ServiceJourney without a submode on a rail line with
 * the submode regionalRail. Such a bus trip must not get the category of the rail submode.
 */
public final class DistanceCategoryHelper {

  private static final Map<TransitMode, DistanceCategory> DEFAULT_CATEGORIES;
  private static final Map<TransitMode, Map<Integer, DistanceCategory>> GTFS_EXTENDED_TYPES;
  private static final Map<TransitMode, Map<String, DistanceCategory>> NETEX_SUBMODES;

  static {
    var defaults = new EnumMap<TransitMode, DistanceCategory>(TransitMode.class);
    var gtfsTypes = new EnumMap<TransitMode, Map<Integer, DistanceCategory>>(TransitMode.class);
    var netexSubmodes = new EnumMap<TransitMode, Map<String, DistanceCategory>>(TransitMode.class);

    // All modes are local unless listed below: subway (GTFS 401 and 402), bus (700), trolleybus
    // (800), tram (900), monorail (405), cable car and gondola (1300 range), funicular (1400), taxi
    // (1500 range), carpool and snow and ice
    for (var mode : TransitMode.values()) {
      defaults.put(mode, LOCAL);
    }

    // GTFS 100 Railway Service
    defaults.put(TransitMode.RAIL, REGIONAL);
    gtfsTypes.put(
      TransitMode.RAIL,
      Map.ofEntries(
        // High Speed Rail Service
        Map.entry(101, LONG_DISTANCE),
        // Long Distance Trains
        Map.entry(102, LONG_DISTANCE),
        // Inter Regional Rail Service
        Map.entry(103, LONG_DISTANCE),
        // Car Transport Rail Service
        Map.entry(104, LONG_DISTANCE),
        // Sleeper Rail Service
        Map.entry(105, LONG_DISTANCE),
        // Tourist Railway Service
        Map.entry(107, LOCAL),
        // Rail Shuttle (Within Complex)
        Map.entry(108, LOCAL),
        // Suburban Railway
        Map.entry(109, LOCAL),
        // Lorry Transport Rail Service
        Map.entry(112, LONG_DISTANCE),
        // Cross-Country Rail Service
        Map.entry(114, LONG_DISTANCE),
        // Vehicle Transport Rail Service
        Map.entry(115, LONG_DISTANCE),
        // Rack and Pinion Railway
        Map.entry(116, LOCAL),
        // Urban Railway Service, OTP maps 400, 403 and 404 to RAIL, not SUBWAY
        Map.entry(400, LOCAL),
        // Urban Railway Service
        Map.entry(403, LOCAL),
        // All Urban Railway Services
        Map.entry(404, LOCAL)
      )
    );
    netexSubmodes.put(
      TransitMode.RAIL,
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
    );

    // GTFS 200 Coach Service
    defaults.put(TransitMode.COACH, LONG_DISTANCE);
    gtfsTypes.put(
      TransitMode.COACH,
      Map.ofEntries(
        // Shuttle Coach Service
        Map.entry(203, LOCAL),
        // Regional Coach Service
        Map.entry(204, REGIONAL),
        // Sightseeing Coach Service
        Map.entry(206, REGIONAL),
        // Tourist Coach Service
        Map.entry(207, REGIONAL),
        // Commuter Coach Service
        Map.entry(208, LOCAL)
      )
    );
    netexSubmodes.put(
      TransitMode.COACH,
      Map.ofEntries(
        Map.entry("shuttleCoach", LOCAL),
        Map.entry("regionalCoach", REGIONAL),
        // twins
        Map.entry("commuterCoach", LOCAL),
        Map.entry("sightseeingCoach", REGIONAL),
        Map.entry("touristCoach", REGIONAL)
      )
    );

    // GTFS 700 Bus Service
    gtfsTypes.put(
      TransitMode.BUS,
      Map.ofEntries(
        // Regional Bus Service
        Map.entry(701, REGIONAL),
        // Post Bus Service
        Map.entry(706, REGIONAL)
      )
    );
    netexSubmodes.put(
      TransitMode.BUS,
      Map.ofEntries(
        Map.entry("regionalBus", REGIONAL),
        // twin
        Map.entry("postBus", REGIONAL)
      )
    );

    // GTFS 900 Tram Service
    // 903 Regional Tram Service
    gtfsTypes.put(TransitMode.TRAM, Map.of(903, REGIONAL));
    netexSubmodes.put(TransitMode.TRAM, Map.of("regionalTram", REGIONAL));

    // GTFS 1000 Water Transport Service and 1200 Ferry Service
    defaults.put(TransitMode.FERRY, REGIONAL);
    netexSubmodes.put(
      TransitMode.FERRY,
      Map.ofEntries(
        Map.entry("internationalCarFerry", LONG_DISTANCE),
        Map.entry("nationalCarFerry", LONG_DISTANCE),
        Map.entry("localCarFerry", LOCAL),
        Map.entry("internationalPassengerFerry", LONG_DISTANCE),
        Map.entry("nationalPassengerFerry", LONG_DISTANCE),
        Map.entry("localPassengerFerry", LOCAL),
        Map.entry("shuttleFerryService", LOCAL)
      )
    );

    // GTFS 1100 Air Service
    defaults.put(TransitMode.AIRPLANE, LONG_DISTANCE);
    netexSubmodes.put(TransitMode.AIRPLANE, Map.of("shuttleFlight", LOCAL));

    DEFAULT_CATEGORIES = Collections.unmodifiableMap(defaults);
    GTFS_EXTENDED_TYPES = Collections.unmodifiableMap(gtfsTypes);
    NETEX_SUBMODES = Collections.unmodifiableMap(netexSubmodes);
  }

  private DistanceCategoryHelper() {}

  /**
   * Derive the distance category of a trip from its main mode and its NeTEx submode or GTFS
   * extended route type. Only one of the submode and route type is set, depending on the data
   * source. They are only used if they are mapped for the trip's main mode, otherwise the trip
   * gets the default category of the mode.
   */
  public static DistanceCategory distanceCategory(
    TransitMode mainMode,
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    var category = NETEX_SUBMODES.getOrDefault(mainMode, Map.of()).get(netexSubmode.name());
    if (category == null && gtfsExtendedType != null) {
      category = GTFS_EXTENDED_TYPES.getOrDefault(mainMode, Map.of()).get(gtfsExtendedType);
    }
    return category != null ? category : DEFAULT_CATEGORIES.get(mainMode);
  }
}
