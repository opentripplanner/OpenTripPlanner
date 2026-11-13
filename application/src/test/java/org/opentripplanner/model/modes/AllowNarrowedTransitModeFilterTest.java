package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.DistanceCategory;
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
  private static final SubMode REGIONAL_BUS = SubMode.getOrBuildAndCacheForever("regionalBus");
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
  void matchDistanceCategory() {
    var regionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );

    assertTrue(regionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertTrue(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
    assertFalse(regionalRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertFalse(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_SUBURBAN_RAILWAY));
    assertFalse(regionalRail.match(TransitMode.RAIL, LONG_DISTANCE_RAIL, null));
    assertFalse(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_LONG_DISTANCE_RAIL));
    assertFalse(regionalRail.match(TransitMode.COACH, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
  }

  @Test
  void matchTripsWithoutSpecificSubmodeOrRouteType() {
    // Every trip has a category. Trips which aren't mapped get the default of their mode.
    var regionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    assertTrue(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, null));
    assertTrue(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 2));
    assertTrue(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 100));

    var longDistanceRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategory(DistanceCategory.LONG_DISTANCE)
        .build()
    );
    assertFalse(longDistanceRail.match(TransitMode.RAIL, SubMode.UNKNOWN, 2));
  }

  @Test
  void matchDistanceCategoryOfAnyMode() {
    var localBus = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS).withDistanceCategory(DistanceCategory.LOCAL).build()
    );
    assertTrue(localBus.match(TransitMode.BUS, LOCAL_BUS, null));
    assertTrue(localBus.match(TransitMode.BUS, SubMode.UNKNOWN, 704));
    assertTrue(localBus.match(TransitMode.BUS, SubMode.UNKNOWN, 3));
    assertFalse(localBus.match(TransitMode.BUS, REGIONAL_BUS, null));
    assertFalse(localBus.match(TransitMode.BUS, SubMode.UNKNOWN, 701));

    var regionalBus = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    assertTrue(regionalBus.match(TransitMode.BUS, REGIONAL_BUS, null));
    assertTrue(regionalBus.match(TransitMode.BUS, RAIL_REPLACEMENT_BUS, null));
    assertTrue(regionalBus.match(TransitMode.BUS, SubMode.UNKNOWN, 714));
    assertFalse(regionalBus.match(TransitMode.BUS, LOCAL_BUS, null));
  }

  @Test
  void matchDistanceCategoryAndReplacement() {
    // The distance category is combined with AND, not with OR as the submode and replacement
    var nonReplacementRegionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.FORBIDDEN)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    assertTrue(nonReplacementRegionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertTrue(
      nonReplacementRegionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL)
    );
    assertFalse(nonReplacementRegionalRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    // A replacement service is regional too, but it is forbidden
    assertFalse(nonReplacementRegionalRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
    assertFalse(
      nonReplacementRegionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REPLACEMENT_RAIL)
    );

    var replacementRegionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.REQUIRED)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    assertTrue(replacementRegionalRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
    assertTrue(
      replacementRegionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REPLACEMENT_RAIL)
    );
    assertFalse(replacementRegionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));

    var replacementLocalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.REQUIRED)
        .withDistanceCategory(DistanceCategory.LOCAL)
        .build()
    );
    assertFalse(replacementLocalRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
  }

  @Test
  void matchDistanceCategoryAndSubMode() {
    var filter = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withSubMode(REGIONAL_RAIL)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    assertTrue(filter.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(filter.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
  }

  /**
   * A NeTEx ServiceJourney with its own transport mode but without a submode inherits the submode
   * of its line, so e.g. a coach trip on a rail line has the submode regionalRail. The submode of
   * another mode gives no category, the trip gets the default category of its own mode.
   */
  @Test
  void inheritedSubmodeOfOtherModeGivesDefaultCategoryOfMode() {
    var regionalCoach = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.COACH)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    var longDistanceCoach = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.COACH)
        .withDistanceCategory(DistanceCategory.LONG_DISTANCE)
        .build()
    );
    assertFalse(regionalCoach.match(TransitMode.COACH, REGIONAL_RAIL, null));
    assertTrue(longDistanceCoach.match(TransitMode.COACH, REGIONAL_RAIL, null));

    var regionalBus = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS)
        .withDistanceCategory(DistanceCategory.REGIONAL)
        .build()
    );
    var localBus = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.BUS).withDistanceCategory(DistanceCategory.LOCAL).build()
    );
    assertFalse(regionalBus.match(TransitMode.BUS, REGIONAL_RAIL, null));
    assertTrue(localBus.match(TransitMode.BUS, REGIONAL_RAIL, null));
  }
}
