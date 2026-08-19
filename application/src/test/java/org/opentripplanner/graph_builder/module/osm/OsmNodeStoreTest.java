package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmNode;

class OsmNodeStoreTest {

  private static final double EPSILON = 1e-7;

  @Test
  void addAndGetRoundTripsCoordinates() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(52.5, 13.4).build());

    var retrieved = store.get(1);
    assertEquals(1, retrieved.getId());
    assertEquals(52.5, retrieved.lat, EPSILON);
    assertEquals(13.4, retrieved.lon, EPSILON);
  }

  @Test
  void getReturnsNullForUnknownId() {
    var store = new OsmNodeStore();
    assertNull(store.get(42));
  }

  @Test
  void containsReflectsAddedNodes() {
    var store = new OsmNodeStore();
    assertFalse(store.contains(1));

    store.add(OsmNode.of().withId(1).withLatLon(0, 0).build());

    assertTrue(store.contains(1));
    assertFalse(store.contains(2));
  }

  @Test
  void sizeCountsDistinctNodes() {
    var store = new OsmNodeStore();
    assertEquals(0, store.size());

    store.add(OsmNode.of().withId(1).withLatLon(0, 0).build());
    store.add(OsmNode.of().withId(2).withLatLon(1, 1).build());

    assertEquals(2, store.size());
  }

  @Test
  void addIsANoOpForAnIdThatIsAlreadyPresent() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(0, 0).withTag("name", "first").build());
    // adding a second node under the same id must not overwrite the original data
    store.add(OsmNode.of().withId(1).withLatLon(9, 9).withTag("name", "second").build());

    var retrieved = store.get(1);
    assertEquals(0, retrieved.lat, EPSILON);
    assertEquals(0, retrieved.lon, EPSILON);
    assertEquals("first", retrieved.getTag("name"));
    assertEquals(1, store.size());
  }

  @Test
  void taglessNodeHasNoTags() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(0, 0).build());

    assertTrue(store.get(1).getTags().isEmpty());
  }

  @Test
  void taggedNodeRoundTripsTags() {
    var store = new OsmNodeStore();
    store.add(
      OsmNode.of()
        .withId(1)
        .withLatLon(0, 0)
        .withTag("highway", "traffic_signals")
        .withTag("name", "foo")
        .build()
    );

    var retrieved = store.get(1);
    assertEquals("traffic_signals", retrieved.getTag("highway"));
    assertEquals("foo", retrieved.getTag("name"));
    assertEquals(2, retrieved.getTags().size());
  }

  @Test
  void nodeWithoutProviderRoundTripsWithNullProvider() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(0, 0).build());

    assertNull(store.get(1).getOsmProvider());
  }

  @Test
  void singleProviderRoundTrips() {
    var store = new OsmNodeStore();
    var provider = TestOsmProvider.EMPTY;
    store.add(OsmNode.of().withId(1).withLatLon(0, 0).withOsmProvider(provider).build());
    store.add(OsmNode.of().withId(2).withLatLon(1, 1).withOsmProvider(provider).build());

    assertSame(provider, store.get(1).getOsmProvider());
    assertSame(provider, store.get(2).getOsmProvider());
  }

  @Test
  void multipleDistinctProvidersRoundTripCorrectly() {
    var store = new OsmNodeStore();
    var providerA = new TestOsmProvider(List.of(), List.of(), List.of());
    var providerB = new TestOsmProvider(List.of(), List.of(), List.of());
    var providerC = new TestOsmProvider(List.of(), List.of(), List.of());

    store.add(OsmNode.of().withId(1).withLatLon(0, 0).withOsmProvider(providerA).build());
    store.add(OsmNode.of().withId(2).withLatLon(1, 1).withOsmProvider(providerB).build());
    store.add(OsmNode.of().withId(3).withLatLon(2, 2).withOsmProvider(providerC).build());
    // a later node re-using the first (default, unindexed) provider
    store.add(OsmNode.of().withId(4).withLatLon(3, 3).withOsmProvider(providerA).build());

    assertSame(providerA, store.get(1).getOsmProvider());
    assertSame(providerB, store.get(2).getOsmProvider());
    assertSame(providerC, store.get(3).getOsmProvider());
    assertSame(providerA, store.get(4).getOsmProvider());
  }

  @Test
  void tooManyDistinctProvidersThrows() {
    var store = new OsmNodeStore();
    // the first provider is "free" (index 0, never written to the index map), so this fills
    // up the 127 indices the byte-sized index map can hold.
    for (int i = 0; i < 127; i++) {
      var provider = new TestOsmProvider(List.of(), List.of(), List.of());
      store.add(OsmNode.of().withId(i).withLatLon(0, 0).withOsmProvider(provider).build());
    }

    var oneTooMany = new TestOsmProvider(List.of(), List.of(), List.of());
    var node = OsmNode.of().withId(1000).withLatLon(0, 0).withOsmProvider(oneTooMany).build();
    assertThrows(IllegalStateException.class, () -> store.add(node));
  }

  @Test
  void reconstructedNodesAreEqualButNotTheSameObject() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(0, 0).build());

    var first = store.get(1);
    var second = store.get(1);

    assertEquals(first, second);
    assertNotSame(first, second);
  }

  @Test
  void getCoordinateMatchesGetNodeGetCoordinate() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(52.5, 13.4).build());

    var coordinate = store.getCoordinate(1);
    var nodeCoordinate = store.get(1).getCoordinate();

    assertEquals(nodeCoordinate.x, coordinate.x, EPSILON);
    assertEquals(nodeCoordinate.y, coordinate.y, EPSILON);
  }

  @Test
  void getCoordinateReturnsNullForUnknownId() {
    var store = new OsmNodeStore();
    assertNull(store.getCoordinate(1));
  }

  @Test
  void coordinatesRoundTripAtExtremesAndWithNegativeValues() {
    var store = new OsmNodeStore();
    store.add(OsmNode.of().withId(1).withLatLon(-90, -180).build());
    store.add(OsmNode.of().withId(2).withLatLon(90, 180).build());
    store.add(OsmNode.of().withId(3).withLatLon(-33.865143, 151.2099).build());

    assertCoordinate(-90, -180, store.get(1));
    assertCoordinate(90, 180, store.get(2));
    assertCoordinate(-33.865143, 151.2099, store.get(3));
  }

  private static void assertCoordinate(double lat, double lon, OsmNode node) {
    assertEquals(lat, node.lat, EPSILON);
    assertEquals(lon, node.lon, EPSILON);
  }

  @Test
  void coordinatesWithMoreThanSevenDecimalDigitsAreRoundedHarmlessly() {
    var store = new OsmNodeStore();
    // OSM coordinates only have 7 decimal digits of precision (~1cm), matching the fixed-point
    // encoding used internally, so extra digits beyond that are rounded away on storage - this is
    // harmless since no valid OSM data ever carries that much (meaningless) extra precision.
    store.add(OsmNode.of().withId(1).withLatLon(52.529371938, 13.419374729).build());
    store.add(OsmNode.of().withId(2).withLatLon(52.449457231, 13.330484832).build());

    var first = store.get(1);
    assertEquals(52.5293719, first.lat, EPSILON);
    assertEquals(13.4193747, first.lon, EPSILON);

    var second = store.get(2);
    assertEquals(52.4494572, second.lat, EPSILON);
    assertEquals(13.3304848, second.lon, EPSILON);
  }
}
