package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
  private static final int GTFS_LONG_DISTANCE_RAIL = 102;
  private static final int GTFS_REGIONAL_RAIL = 106;

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

  /**
   * Which trips have which category is tested in {@code DistanceCategoryHelperTest}, this only
   * tests how the category is combined with the other conditions of the filter.
   */
  @Test
  void matchDistanceCategory() {
    var regionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
        .build()
    );

    assertTrue(regionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertTrue(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
    assertFalse(regionalRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertFalse(regionalRail.match(TransitMode.RAIL, SubMode.UNKNOWN, GTFS_LONG_DISTANCE_RAIL));
    assertFalse(regionalRail.match(TransitMode.COACH, SubMode.UNKNOWN, GTFS_REGIONAL_RAIL));
  }

  @Test
  void matchOneOfSeveralDistanceCategories() {
    var localAndRegionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withDistanceCategories(List.of(DistanceCategory.LOCAL, DistanceCategory.REGIONAL))
        .build()
    );
    assertTrue(localAndRegionalRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    assertTrue(localAndRegionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(localAndRegionalRail.match(TransitMode.RAIL, LONG_DISTANCE_RAIL, null));
  }

  @Test
  void matchDistanceCategoryAndReplacement() {
    // The distance category is combined with AND, not with OR as the submode and replacement
    var nonReplacementRegionalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.FORBIDDEN)
        .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
        .build()
    );
    assertTrue(nonReplacementRegionalRail.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(nonReplacementRegionalRail.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
    // A replacement service is regional too, but it is forbidden
    assertFalse(nonReplacementRegionalRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));

    var replacementLocalRail = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withReplacement(ReplacementRequirement.REQUIRED)
        .withDistanceCategories(List.of(DistanceCategory.LOCAL))
        .build()
    );
    // A replacement service, but not a local one
    assertFalse(replacementLocalRail.match(TransitMode.RAIL, REPLACEMENT_RAIL_SERVICE, null));
  }

  @Test
  void matchDistanceCategoryAndSubMode() {
    var filter = new AllowNarrowedTransitModeFilter(
      NarrowedTransitMode.of(TransitMode.RAIL)
        .withSubMode(REGIONAL_RAIL)
        .withDistanceCategories(List.of(DistanceCategory.REGIONAL))
        .build()
    );
    assertTrue(filter.match(TransitMode.RAIL, REGIONAL_RAIL, null));
    assertFalse(filter.match(TransitMode.RAIL, SUBURBAN_RAILWAY, null));
  }
}
