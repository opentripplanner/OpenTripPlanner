package org.opentripplanner.transit.model.basic;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * The geographical scope of a transit service: how far it travels and which kind of journeys it
 * is meant for. This lets clients tell apart services that share a main mode, e.g. suburban,
 * regional and long-distance trains, which all have the main mode {@link TransitMode#RAIL}.
 * <p>
 * The scope is not part of the input data, but derived from the NeTEx submode or the GTFS
 * extended route type, in the same way as the replacement feature. It is only derived for the
 * modes in {@link #derivedForModes()}, and never for replacement services. Services which can not
 * be classified have no scope.
 * <p>
 * A submode or route type only gives a scope to trips of the main mode it belongs to. A trip can
 * inherit the submode of its route while having a different main mode, e.g. a bus ServiceJourney
 * without a submode on a rail line with {@code regionalRail}; such a bus trip has no scope.
 */
public enum GeographicalScope {
  /** Services connecting a city with its suburbs, e.g. S-Bahn or commuter coaches. */
  SUBURBAN,
  /** Services connecting the towns and cities of a region. */
  REGIONAL,
  /** National and international services, including high speed rail and night trains. */
  LONG_DISTANCE;

  private static final List<TransitMode> DERIVED_FOR_MODES = List.of(
    TransitMode.RAIL,
    TransitMode.COACH
  );

  /**
   * Rail: 101 High Speed Rail, 102 Long Distance Trains, 103 Inter Regional Rail, 105 Sleeper Rail,
   * 106 Regional Rail, 109 Suburban Railway, 114 Cross-Country Rail. Coach: 201 International
   * Coach, 202 National Coach, 204 Regional Coach, 208 Commuter Coach.
   * <p>
   * 104 Car Transport Rail is deliberately not mapped: it covers both long-distance car trains and
   * short car shuttles like the Sylt Shuttle. The urban railway types (400, 403, 404) are not
   * mapped either, as they don't fit any of the scopes.
   */
  private static final Map<TransitMode, Map<Integer, GeographicalScope>> BY_GTFS_EXTENDED_TYPE =
    Map.of(
      TransitMode.RAIL,
      Map.ofEntries(
        Map.entry(101, LONG_DISTANCE),
        Map.entry(102, LONG_DISTANCE),
        Map.entry(103, REGIONAL),
        Map.entry(105, LONG_DISTANCE),
        Map.entry(106, REGIONAL),
        Map.entry(109, SUBURBAN),
        Map.entry(114, LONG_DISTANCE)
      ),
      TransitMode.COACH,
      Map.ofEntries(
        Map.entry(201, LONG_DISTANCE),
        Map.entry(202, LONG_DISTANCE),
        Map.entry(204, REGIONAL),
        Map.entry(208, SUBURBAN)
      )
    );

  /** NeTEx RailSubmode and CoachSubmode values. */
  private static final Map<TransitMode, Map<String, GeographicalScope>> BY_NETEX_SUBMODE = Map.of(
    TransitMode.RAIL,
    Map.ofEntries(
      Map.entry("highSpeedRail", LONG_DISTANCE),
      Map.entry("longDistance", LONG_DISTANCE),
      Map.entry("international", LONG_DISTANCE),
      Map.entry("sleeperRailService", LONG_DISTANCE),
      Map.entry("nightRail", LONG_DISTANCE),
      Map.entry("crossCountryRail", LONG_DISTANCE),
      Map.entry("interregionalRail", REGIONAL),
      Map.entry("regionalRail", REGIONAL),
      Map.entry("suburbanRailway", SUBURBAN)
    ),
    TransitMode.COACH,
    Map.ofEntries(
      Map.entry("internationalCoach", LONG_DISTANCE),
      Map.entry("nationalCoach", LONG_DISTANCE),
      Map.entry("regionalCoach", REGIONAL),
      Map.entry("commuterCoach", SUBURBAN)
    )
  );

  /**
   * The main modes for which a scope can be derived. A trip of any other mode never has a scope.
   */
  public static List<TransitMode> derivedForModes() {
    return DERIVED_FOR_MODES;
  }

  /**
   * Derive the geographical scope of a trip from its NeTEx submode or GTFS extended route type.
   * Only one of them is set, depending on the data source. Both are only used if they belong to the
   * trip's main mode.
   *
   * @return the scope, or null if the trip can not be classified
   */
  @Nullable
  public static GeographicalScope of(
    TransitMode mainMode,
    SubMode netexSubmode,
    @Nullable Integer gtfsExtendedType
  ) {
    var scope = BY_NETEX_SUBMODE.getOrDefault(mainMode, Map.of()).get(netexSubmode.name());
    if (scope == null && gtfsExtendedType != null) {
      scope = BY_GTFS_EXTENDED_TYPE.getOrDefault(mainMode, Map.of()).get(gtfsExtendedType);
    }
    return scope;
  }
}
