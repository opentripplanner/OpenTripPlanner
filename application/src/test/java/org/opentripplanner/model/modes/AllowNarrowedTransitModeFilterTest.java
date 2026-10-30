package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.GeographicalScope;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.ReplacementRequirement;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

public class AllowNarrowedTransitModeFilterTest {

  private static final SubMode LOCAL_BUS = SubMode.getOrBuildAndCacheForever("localBus");
  private static final SubMode RAIL_REPLACEMENT_BUS = SubMode.getOrBuildAndCacheForever(
    "railReplacementBus"
  );
  private static final SubMode REGIONAL_RAIL = SubMode.getOrBuildAndCacheForever("regionalRail");
  private static final SubMode SUBURBAN_RAILWAY = SubMode.getOrBuildAndCacheForever(
    "suburbanRailway"
  );
  private static final SubMode REPLACEMENT_RAIL_SERVICE = SubMode.getOrBuildAndCacheForever(
    "replacementRailService"
  );
  private static final SubMode LONG_DISTANCE_RAIL = SubMode.getOrBuildAndCacheForever(
    "longDistance"
  );
  private static final int GTFS_LONG_DISTANCE_RAIL = 102;
  private static final int GTFS_REGIONAL_RAIL = 106;
  private static final int GTFS_SUBURBAN_RAILWAY = 109;
  private static final int GTFS_REPLACEMENT_RAIL = 110;

  private final AllowNarrowedTransitModeFilter fullFilter = new AllowNarrowedTransitModeFilter(
    NarrowedTransitMode.of(TransitMode.BUS)
      .withSubMode(LOCAL_BUS)
      .withReplacement(ReplacementRequirement.REQUIRED)
      .build()
  );
  private final AllowNarrowedTransitModeFilter nullSubmodeFilter =
    new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS)
        .withReplacement(ReplacementRequirement.REQUIRED)
        .build()
    );
  private final AllowNarrowedTransitModeFilter nullReplacementFilter =
    new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS).withSubMode(LOCAL_BUS).build()
    );

  @Test
  void match() {
    // filter.match will always be called with either netexSubmode == SubMode.UNKNOWN or gtfsExtendedType == null
    // depending on the data source type. Both can be true if the data is from a GTFS source and the extended
    // type just happens to be not set.
    assertTrue(fullFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertTrue(fullFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 714));
    assertFalse(fullFilter.match(TransitMode.TRAM, SubMode.UNKNOWN, 714));

    assertTrue(nullSubmodeFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 714));
    assertFalse(nullSubmodeFilter.match(TransitMode.BUS, SubMode.UNKNOWN, 700));
    assertFalse(nullSubmodeFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertFalse(nullSubmodeFilter.match(TransitMode.TRAM, LOCAL_BUS, null));

    assertTrue(nullSubmodeFilter.match(TransitMode.BUS, RAIL_REPLACEMENT_BUS, null));
    assertFalse(nullSubmodeFilter.match(TransitMode.TRAM, RAIL_REPLACEMENT_BUS, null));

    assertTrue(nullReplacementFilter.match(TransitMode.BUS, LOCAL_BUS, null));
    assertFalse(nullReplacementFilter.match(TransitMode.BUS, SubMode.UNKNOWN, null));
    assertFalse(nullReplacementFilter.match(TransitMode.TRAM, LOCAL_BUS, null));
  }

  @Test
  void matchGeographicalScope() {
    var regionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withGeographicalScope(GeographicalScope.REGIONAL)
        .build()
    );

    assertTrue(regionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertTrue(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
    assertFalse(regionalRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertFalse(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_SUBURBAN_RAILWAY));
    assertFalse(regionalRail.match(TransitMode.COACH, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
    // Trips without a derivable scope never match
    assertFalse(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, null));
    assertFalse(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 100));
  }

  @Test
  void matchGeographicalScopeAndReplacement() {
    // The geographical scope is combined with AND, not with OR as the submode and replacement
    var nonReplacementSuburbanRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.FORBIDDEN)
        .withGeographicalScope(GeographicalScope.SUBURBAN)
        .build()
    );
    assertTrue(nonReplacementSuburbanRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertTrue(
      nonReplacementSuburbanRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_SUBURBAN_RAILWAY)
    );
    assertFalse(nonReplacementSuburbanRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(nonReplacementSuburbanRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
    assertFalse(
      nonReplacementSuburbanRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REPLACEMENT_RAIL)
    );

    // A replacement service has no geographical scope of its own, so it can't match both
    var replacementSuburbanRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.REQUIRED)
        .withGeographicalScope(GeographicalScope.SUBURBAN)
        .build()
    );
    assertFalse(replacementSuburbanRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertFalse(replacementSuburbanRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
  }

  @Test
  void matchGeographicalScopeAndSubMode() {
    var filter = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withSubMode(REGIONAL_RAIL)
        .withGeographicalScope(GeographicalScope.REGIONAL)
        .build()
    );
    assertTrue(filter.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(filter.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
  }

  @Test
  void matchExcludedGeographicalScopes() {
    var allButLongDistanceRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withExcludedGeographicalScopes(List.of(GeographicalScope.LONG_DISTANCE))
        .build()
    );

    assertTrue(allButLongDistanceRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertTrue(allButLongDistanceRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertTrue(allButLongDistanceRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
    assertFalse(allButLongDistanceRail.match(TransitMode.RAIL, LONG_DISTANCE_RAIL, null));
    assertFalse(
      allButLongDistanceRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_LONG_DISTANCE_RAIL)
    );
    assertFalse(allButLongDistanceRail.match(TransitMode.COACH, REGIONAL_RAIL, null));
    // Unlike an included scope, an excluded scope keeps trips without a derivable scope
    assertTrue(allButLongDistanceRail.match(TransitMode.RAIL, SubMode.UNKNOWN, null));
    assertTrue(allButLongDistanceRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 2));
    assertTrue(allButLongDistanceRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 100));
  }

  @Test
  void matchAllGeographicalScopesExcluded() {
    // Excluding all scopes leaves exactly the trips which can't be classified
    var unclassifiedRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withExcludedGeographicalScopes(List.of(GeographicalScope.values()))
        .build()
    );

    assertTrue(unclassifiedRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 100));
    assertFalse(unclassifiedRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_SUBURBAN_RAILWAY));
    assertFalse(unclassifiedRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
  }

  @Test
  void matchExcludedGeographicalScopesAndReplacement() {
    var nonReplacementShortDistanceRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.FORBIDDEN)
        .withExcludedGeographicalScopes(List.of(GeographicalScope.LONG_DISTANCE))
        .build()
    );
    assertTrue(nonReplacementShortDistanceRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(nonReplacementShortDistanceRail.match(TransitMode.RAIL, LONG_DISTANCE_RAIL, null));
    assertFalse(
      nonReplacementShortDistanceRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null)
    );

    // Replacement services have no scope, so they are never excluded by scope
    var replacementRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.REQUIRED)
        .withExcludedGeographicalScopes(List.of(GeographicalScope.LONG_DISTANCE))
        .build()
    );
    assertTrue(replacementRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
    assertTrue(replacementRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REPLACEMENT_RAIL));
    assertFalse(replacementRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
  }

  /**
   * A NeTEx ServiceJourney with its own transport mode but without a submode inherits the submode
   * of its line, so e.g. a coach trip on a rail line has the submode regionalRail.
   */
  @Test
  void inheritedSubmodeOfOtherModeGivesNoScope() {
    var regionalCoach = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.COACH)
        .withGeographicalScope(GeographicalScope.REGIONAL)
        .build()
    );
    assertFalse(regionalCoach.match(TransitMode.COACH, REGIONAL_RAIL, null));

    var allButRegionalCoach = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.COACH)
        .withExcludedGeographicalScopes(List.of(GeographicalScope.REGIONAL))
        .build()
    );
    assertTrue(allButRegionalCoach.match(TransitMode.COACH, REGIONAL_RAIL, null));

    var regionalBus = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS)
        .withGeographicalScope(GeographicalScope.REGIONAL)
        .build()
    );
    assertFalse(regionalBus.match(TransitMode.BUS, REGIONAL_RAIL, null));
  }
}
