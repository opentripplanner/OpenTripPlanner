package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;

class OsmAreaGroupTest {

  private static final OsmWay L0_1_2_3_4_1 = new OsmWay();
  private static final OsmWay L0_5_2_1_5 = new OsmWay();
  private static final OsmWay L0_1_5_6_1 = new OsmWay();
  private static final OsmWay L0_1_2_3_7_8_9_1 = new OsmWay();
  private static final OsmWay L0_2_10_7_11_6_2 = new OsmWay();
  private static final OsmWay L1_1_2_5_1 = new OsmWay();

  private static final OsmWay PEDESTRIAN_1_2_3_4_1 = new OsmWay();
  private static final OsmWay PEDESTRIAN_5_2_1_5 = new OsmWay();
  private static final OsmWay BARRIER_3_2_1 = new OsmWay();
  private static final OsmWay BARRIER_1_4 = new OsmWay();
  private static final OsmWay BARRIER_2_3 = new OsmWay();
  private static final OsmWay BARRIER_5_1_4 = new OsmWay();
  private static final OsmWay BARRIER_1_3_2 = new OsmWay();
  private static final OsmWay BARRIER_3_1 = new OsmWay();
  private static final OsmWay BOLLARD_1_2_3 = new OsmWay();

  private static final OsmLevel LEVEL_0 = new OsmLevel(
    0,
    0,
    "0",
    "0",
    OsmLevel.Source.LEVEL_TAG,
    true
  );
  private static final OsmLevel LEVEL_1 = new OsmLevel(
    1,
    5,
    "1",
    "1",
    OsmLevel.Source.LEVEL_TAG,
    true
  );

  private static final TLongObjectHashMap<OsmNode> nodes = new TLongObjectHashMap<>();

  static {
    nodes.put(1, new OsmNode(0, 0));
    nodes.put(2, new OsmNode(0, 1));
    nodes.put(3, new OsmNode(1, 1));
    nodes.put(4, new OsmNode(1, 0));
    nodes.put(5, new OsmNode(-0.5, -1));
    nodes.put(6, new OsmNode(-1, 0));
    nodes.put(7, new OsmNode(1, 2));
    nodes.put(8, new OsmNode(2, 2));
    nodes.put(9, new OsmNode(2, 0));
    nodes.put(10, new OsmNode(0, 2));
    nodes.put(11, new OsmNode(-1, 3));

    for (var key : nodes.keys()) {
      nodes.get(key).setId(key);
    }

    L0_1_2_3_4_1.addTag("highway", "living_street");
    L0_1_2_3_4_1.addNodeRef(1);
    L0_1_2_3_4_1.addNodeRef(2);
    L0_1_2_3_4_1.addNodeRef(3);
    L0_1_2_3_4_1.addNodeRef(4);
    L0_1_2_3_4_1.addNodeRef(1);

    L0_5_2_1_5.addTag("highway", "living_street");
    L0_5_2_1_5.addNodeRef(5);
    L0_5_2_1_5.addNodeRef(2);
    L0_5_2_1_5.addNodeRef(1);
    L0_5_2_1_5.addNodeRef(5);

    L0_1_5_6_1.addTag("highway", "living_street");
    L0_1_5_6_1.addNodeRef(1);
    L0_1_5_6_1.addNodeRef(5);
    L0_1_5_6_1.addNodeRef(6);
    L0_1_5_6_1.addNodeRef(1);

    L0_1_2_3_7_8_9_1.addTag("highway", "living_street");
    L0_1_2_3_7_8_9_1.addNodeRef(1);
    L0_1_2_3_7_8_9_1.addNodeRef(2);
    L0_1_2_3_7_8_9_1.addNodeRef(3);
    L0_1_2_3_7_8_9_1.addNodeRef(7);
    L0_1_2_3_7_8_9_1.addNodeRef(8);
    L0_1_2_3_7_8_9_1.addNodeRef(9);
    L0_1_2_3_7_8_9_1.addNodeRef(1);

    L0_2_10_7_11_6_2.addTag("highway", "living_street");
    L0_2_10_7_11_6_2.addNodeRef(2);
    L0_2_10_7_11_6_2.addNodeRef(10);
    L0_2_10_7_11_6_2.addNodeRef(7);
    L0_2_10_7_11_6_2.addNodeRef(11);
    L0_2_10_7_11_6_2.addNodeRef(6);
    L0_2_10_7_11_6_2.addNodeRef(2);

    L1_1_2_5_1.addTag("highway", "living_street");
    L1_1_2_5_1.addTag("level", "1");
    L1_1_2_5_1.addNodeRef(1);
    L1_1_2_5_1.addNodeRef(2);
    L1_1_2_5_1.addNodeRef(5);
    L1_1_2_5_1.addNodeRef(1);

    PEDESTRIAN_1_2_3_4_1.addTag("highway", "pedestrian");
    PEDESTRIAN_1_2_3_4_1.addTag("access", "no");
    PEDESTRIAN_1_2_3_4_1.addTag("foot", "yes");
    PEDESTRIAN_1_2_3_4_1.addNodeRef(1);
    PEDESTRIAN_1_2_3_4_1.addNodeRef(2);
    PEDESTRIAN_1_2_3_4_1.addNodeRef(3);
    PEDESTRIAN_1_2_3_4_1.addNodeRef(4);
    PEDESTRIAN_1_2_3_4_1.addNodeRef(1);

    PEDESTRIAN_5_2_1_5.addTag("highway", "pedestrian");
    PEDESTRIAN_5_2_1_5.addTag("access", "no");
    PEDESTRIAN_5_2_1_5.addTag("foot", "yes");
    PEDESTRIAN_5_2_1_5.addNodeRef(5);
    PEDESTRIAN_5_2_1_5.addNodeRef(2);
    PEDESTRIAN_5_2_1_5.addNodeRef(1);
    PEDESTRIAN_5_2_1_5.addNodeRef(5);

    BARRIER_3_2_1.addNodeRef(3);
    BARRIER_3_2_1.addNodeRef(2);
    BARRIER_3_2_1.addNodeRef(1);
    BARRIER_3_2_1.addTag("barrier", "wall");

    BARRIER_1_4.addNodeRef(1);
    BARRIER_1_4.addNodeRef(4);
    BARRIER_1_4.addTag("barrier", "wall");

    BARRIER_2_3.addNodeRef(2);
    BARRIER_2_3.addNodeRef(3);
    BARRIER_2_3.addTag("barrier", "wall");

    BARRIER_5_1_4.addNodeRef(5);
    BARRIER_5_1_4.addNodeRef(1);
    BARRIER_5_1_4.addNodeRef(4);
    BARRIER_5_1_4.addTag("barrier", "wall");

    BARRIER_1_3_2.addNodeRef(1);
    BARRIER_1_3_2.addNodeRef(3);
    BARRIER_1_3_2.addNodeRef(2);
    BARRIER_1_3_2.addTag("barrier", "wall");

    BARRIER_3_1.addNodeRef(3);
    BARRIER_3_1.addNodeRef(1);
    BARRIER_3_1.addTag("barrier", "wall");

    BOLLARD_1_2_3.addNodeRef(1);
    BOLLARD_1_2_3.addNodeRef(2);
    BOLLARD_1_2_3.addNodeRef(3);
    BOLLARD_1_2_3.addTag("barrier", "bollard");
  }

  private static final OsmProvider osmProvider = new TestOsmProvider(
    List.of(),
    List.of(
      L0_1_2_3_4_1,
      L0_5_2_1_5,
      L0_1_5_6_1,
      L0_1_2_3_7_8_9_1,
      L0_2_10_7_11_6_2,
      L1_1_2_5_1,
      PEDESTRIAN_1_2_3_4_1,
      PEDESTRIAN_5_2_1_5,
      BARRIER_3_2_1,
      BARRIER_1_4,
      BARRIER_2_3,
      BARRIER_5_1_4,
      BARRIER_1_3_2,
      BARRIER_3_1,
      BOLLARD_1_2_3
    ),
    nodes.valueCollection().stream().toList()
  );

  static {
    new OsmTagMapper().populateProperties(osmProvider.getWayPropertySet());
  }

  @Test
  void shouldGroupWithTwoConsecutiveNodes() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_0), generateBarrierMap());
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldGroupWithBarrierNotSharingSameTwoNodes() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER_5_1_4)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldNotGroupWithOnlyOneNodeInCommon() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_1_5_6_1);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_0), generateBarrierMap());
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithTwoConsecutiveNodesOnAWall() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    // the shared edge 1-2 is also shared by the barrier
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
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
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
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
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BOLLARD_1_2_3)
    );
    assertEquals(1, result.size());
  }

  @Test
  void shouldNotGroupWithWallBetweenPedestrianAreas() {
    OsmArea a1 = createArea(PEDESTRIAN_1_2_3_4_1);
    OsmArea a2 = createArea(PEDESTRIAN_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER_3_2_1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithBollardBetweenCarAccessibleAreas() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BOLLARD_1_2_3)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithTwoConsecutiveNodesAtTheEndOfTwoWalls() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_5_2_1_5);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER_1_4, BARRIER_2_3)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldNotGroupWithTwoDistinctCommonNodes() {
    OsmArea a1 = createArea(L0_1_2_3_7_8_9_1);
    OsmArea a2 = createArea(L0_2_10_7_11_6_2);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_0), generateBarrierMap());
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithBarrierRunningAlongMultipleSharedEdges() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_1_2_3_7_8_9_1);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER_3_2_1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithBarrierCuttingThroughArea() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L0_1_2_3_7_8_9_1);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER_3_1)
    );
    assertEquals(1, result.size());
  }

  @Test
  void shouldNotGroupBetweenLevels() {
    OsmArea a1 = createArea(L0_1_2_3_4_1);
    OsmArea a2 = createArea(L1_1_2_5_1);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_1), generateBarrierMap());
    assertEquals(2, result.size());
  }

  private static OsmArea createArea(OsmWay closedWay) {
    return new OsmArea(closedWay, List.of(closedWay), List.of(), nodes);
  }

  private static Multimap<OsmNode, OsmWay> generateBarrierMap(OsmWay... barriers) {
    Multimap<OsmNode, OsmWay> result = HashMultimap.create();
    for (var barrier : barriers) {
      for (var nid : barrier.getNodeRefs().toArray()) {
        result.put(nodes.get(nid), barrier);
      }
    }
    return result;
  }
}
