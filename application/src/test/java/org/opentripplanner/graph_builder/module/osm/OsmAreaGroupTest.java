package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmLevelSource;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;

class OsmAreaGroupTest {

  private static final Set<OsmLevel> LEVEL_0_SET = Set.of(
    new OsmLevel(0.0, "0", OsmLevelSource.LEVEL_TAG)
  );
  private static final Set<OsmLevel> LEVEL_1_SET = Set.of(
    new OsmLevel(1.0, "1", OsmLevelSource.LEVEL_TAG)
  );

  private static final TLongObjectHashMap<OsmNode> NODES = new TLongObjectHashMap<>();

  static {
    NODES.put(1, OsmNode.of().withId(1).withLatLon(0, 0).build());
    NODES.put(2, OsmNode.of().withId(2).withLatLon(0, 1).build());
    NODES.put(3, OsmNode.of().withId(3).withLatLon(1, 1).build());
    NODES.put(4, OsmNode.of().withId(4).withLatLon(1, 0).build());
    NODES.put(5, OsmNode.of().withId(5).withLatLon(-0.5, -1).build());
    NODES.put(6, OsmNode.of().withId(6).withLatLon(-1, 0).build());
    NODES.put(7, OsmNode.of().withId(7).withLatLon(1, 2).build());
    NODES.put(8, OsmNode.of().withId(8).withLatLon(2, 2).build());
    NODES.put(9, OsmNode.of().withId(9).withLatLon(2, 0).build());
    NODES.put(10, OsmNode.of().withId(10).withLatLon(0, 2).build());
    NODES.put(11, OsmNode.of().withId(11).withLatLon(-1, 3).build());
  }

  private static final OsmWay L0_1_2_3_4_1 = OsmWay.of()
    .withTag("highway", "living_street")
    .addNodeRef(1, 2, 3, 4, 1)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay L0_5_2_1_5 = OsmWay.of()
    .withTag("highway", "living_street")
    .addNodeRef(5, 2, 1, 5)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay L0_1_5_6_1 = OsmWay.of()
    .withTag("highway", "living_street")
    .addNodeRef(1, 5, 6, 1)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay L0_1_2_3_7_8_9_1 = OsmWay.of()
    .withTag("highway", "living_street")
    .addNodeRef(1, 2, 3, 7, 8, 9, 1)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay L0_2_10_7_11_6_2 = OsmWay.of()
    .withTag("highway", "living_street")
    .addNodeRef(2, 10, 7, 11, 6, 2)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay L1_1_2_5_1 = OsmWay.of()
    .withTag("highway", "living_street")
    .withTag("level", "1")
    .addNodeRef(1, 2, 5, 1)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay PEDESTRIAN_1_2_3_4_1 = OsmWay.of()
    .withTag("highway", "pedestrian")
    .withTag("access", "no")
    .withTag("foot", "yes")
    .addNodeRef(1, 2, 3, 4, 1)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay PEDESTRIAN_5_2_1_5 = OsmWay.of()
    .withTag("highway", "pedestrian")
    .withTag("access", "no")
    .withTag("foot", "yes")
    .addNodeRef(5, 2, 1, 5)
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BARRIER_3_2_1 = OsmWay.of()
    .addNodeRef(3, 2, 1)
    .withTag("barrier", "wall")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BARRIER_1_4 = OsmWay.of()
    .addNodeRef(1, 4)
    .withTag("barrier", "wall")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BARRIER_2_3 = OsmWay.of()
    .addNodeRef(2, 3)
    .withTag("barrier", "wall")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BARRIER_5_1_4 = OsmWay.of()
    .addNodeRef(5, 1, 4)
    .withTag("barrier", "wall")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BARRIER_1_3_2 = OsmWay.of()
    .addNodeRef(1, 3, 2)
    .withTag("barrier", "wall")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BARRIER_3_1 = OsmWay.of()
    .addNodeRef(3, 1)
    .withTag("barrier", "wall")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  private static final OsmWay BOLLARD_1_2_3 = OsmWay.of()
    .addNodeRef(1, 2, 3)
    .withTag("barrier", "bollard")
    .withOsmProvider(TestOsmProvider.EMPTY)
    .build();

  @Test
  void shouldGroupWithTwoConsecutiveNodes() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap()
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldGroupWithBarrierNotSharingSameTwoNodes() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_5_1_4)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldNotGroupWithOnlyOneNodeInCommon() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_1_5_6_1);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap()
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithTwoConsecutiveNodesOnAWall() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    // the shared edge 1-2 is also shared by the barrier
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_3_2_1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithBarrierSharingTwoNonConsecutiveNodes() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    // a1 and a2 shares the edge 1-2, but the barrier goes 1-3-2
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_1_3_2)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldGroupWithBollardBetweenPedestrianAreas() {
    OsmArea a1 = createArea(PEDESTRIAN_1_2_3_4_1);
    OsmArea a2 = createArea(PEDESTRIAN_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BOLLARD_1_2_3)
    );
    assertEquals(1, result.size());
  }

  @Test
  void shouldNotGroupWithWallBetweenPedestrianAreas() {
    OsmArea a1 = createArea(PEDESTRIAN_1_2_3_4_1);
    OsmArea a2 = createArea(PEDESTRIAN_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_3_2_1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithBollardBetweenCarAccessibleAreas() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BOLLARD_1_2_3)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithTwoConsecutiveNodesAtTheEndOfTwoWalls() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_1_4, BARRIER_2_3)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldNotGroupWithTwoDistinctCommonNodes() {
    OsmArea a1 = createArea(L0_1_2_3_7_8_9_1);
    OsmArea a2 = createArea(L0_2_10_7_11_6_2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap()
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithBarrierRunningAlongMultipleSharedEdges() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_1_2_3_7_8_9_1);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_3_2_1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithBarrierCuttingThroughArea() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_1_2_3_7_8_9_1);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_0_SET),
      generateBarrierMap(BARRIER_3_1)
    );
    assertEquals(1, result.size());
  }

  @Test
  void shouldNotGroupBetweenLevels() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L1_1_2_5_1);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0_SET, a2, LEVEL_1_SET),
      generateBarrierMap()
    );
    assertEquals(2, result.size());
  }

  private static OsmArea createArea(OsmWay closedWay) {
    return new OsmArea(closedWay, List.of(closedWay), List.of(), NODES);
  }

  private static Multimap<OsmNode, OsmWay> generateBarrierMap(OsmWay... barriers) {
    Multimap<OsmNode, OsmWay> result = HashMultimap.create();
    for (var barrier : barriers) {
      for (var nid : barrier.getNodeRefs().toArray()) {
        result.put(NODES.get(nid), barrier);
      }
    }
    return result;
  }
}
