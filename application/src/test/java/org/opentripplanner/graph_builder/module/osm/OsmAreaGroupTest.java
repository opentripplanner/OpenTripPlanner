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

  private static final OsmWay L0_WAY1 = new OsmWay();
  private static final OsmWay L0_WAY2 = new OsmWay();
  private static final OsmWay L0_WAY3 = new OsmWay();
  private static final OsmWay L0_WAY4 = new OsmWay();
  private static final OsmWay L0_WAY5 = new OsmWay();
  private static final OsmWay L1_WAY1 = new OsmWay();

  private static final OsmWay PEDESTRIAN_WAY1 = new OsmWay();
  private static final OsmWay PEDESTRIAN_WAY2 = new OsmWay();
  private static final OsmWay BARRIER1 = new OsmWay();
  private static final OsmWay BARRIER2 = new OsmWay();
  private static final OsmWay BARRIER3 = new OsmWay();
  private static final OsmWay BARRIER4 = new OsmWay();
  private static final OsmWay BOLLARD = new OsmWay();

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

    L0_WAY1.addTag("highway", "living_street");
    L0_WAY1.addNodeRef(1);
    L0_WAY1.addNodeRef(2);
    L0_WAY1.addNodeRef(3);
    L0_WAY1.addNodeRef(4);
    L0_WAY1.addNodeRef(1);

    L0_WAY2.addTag("highway", "living_street");
    L0_WAY2.addNodeRef(5);
    L0_WAY2.addNodeRef(2);
    L0_WAY2.addNodeRef(1);
    L0_WAY2.addNodeRef(5);

    L0_WAY3.addTag("highway", "living_street");
    L0_WAY3.addNodeRef(1);
    L0_WAY3.addNodeRef(5);
    L0_WAY3.addNodeRef(6);
    L0_WAY3.addNodeRef(1);

    L0_WAY4.addTag("highway", "living_street");
    L0_WAY4.addNodeRef(1);
    L0_WAY4.addNodeRef(2);
    L0_WAY4.addNodeRef(3);
    L0_WAY4.addNodeRef(7);
    L0_WAY4.addNodeRef(8);
    L0_WAY4.addNodeRef(9);
    L0_WAY4.addNodeRef(1);

    L0_WAY5.addTag("highway", "living_street");
    L0_WAY5.addNodeRef(2);
    L0_WAY5.addNodeRef(10);
    L0_WAY5.addNodeRef(7);
    L0_WAY5.addNodeRef(11);
    L0_WAY5.addNodeRef(6);
    L0_WAY5.addNodeRef(2);

    L1_WAY1.addTag("highway", "living_street");
    L1_WAY1.addTag("level", "1");
    L1_WAY1.addNodeRef(1);
    L1_WAY1.addNodeRef(2);
    L1_WAY1.addNodeRef(5);
    L1_WAY1.addNodeRef(1);

    PEDESTRIAN_WAY1.addTag("highway", "pedestrian");
    PEDESTRIAN_WAY1.addTag("access", "no");
    PEDESTRIAN_WAY1.addTag("foot", "yes");
    PEDESTRIAN_WAY1.addNodeRef(1);
    PEDESTRIAN_WAY1.addNodeRef(2);
    PEDESTRIAN_WAY1.addNodeRef(3);
    PEDESTRIAN_WAY1.addNodeRef(4);
    PEDESTRIAN_WAY1.addNodeRef(1);

    PEDESTRIAN_WAY2.addTag("highway", "pedestrian");
    PEDESTRIAN_WAY2.addTag("access", "no");
    PEDESTRIAN_WAY2.addTag("foot", "yes");
    PEDESTRIAN_WAY2.addNodeRef(5);
    PEDESTRIAN_WAY2.addNodeRef(2);
    PEDESTRIAN_WAY2.addNodeRef(1);
    PEDESTRIAN_WAY2.addNodeRef(5);

    BARRIER1.addNodeRef(1);
    BARRIER1.addNodeRef(2);
    BARRIER1.addNodeRef(3);
    BARRIER1.addTag("barrier", "wall");

    BARRIER2.addNodeRef(1);
    BARRIER2.addNodeRef(4);
    BARRIER2.addTag("barrier", "wall");

    BARRIER3.addNodeRef(2);
    BARRIER3.addNodeRef(3);
    BARRIER3.addTag("barrier", "wall");

    BARRIER4.addNodeRef(1);
    BARRIER4.addNodeRef(4);
    BARRIER4.addNodeRef(5);
    BARRIER4.addTag("barrier", "wall");

    BOLLARD.addNodeRef(1);
    BOLLARD.addNodeRef(2);
    BOLLARD.addNodeRef(3);
    BOLLARD.addTag("barrier", "bollard");
  }

  private static final OsmProvider osmProvider = new TestOsmProvider(
    List.of(),
    List.of(
      L0_WAY1,
      L0_WAY2,
      L0_WAY3,
      L0_WAY4,
      L0_WAY5,
      L1_WAY1,
      PEDESTRIAN_WAY1,
      PEDESTRIAN_WAY2,
      BARRIER1,
      BARRIER2,
      BARRIER3,
      BARRIER4,
      BOLLARD
    ),
    nodes.valueCollection().stream().toList()
  );

  static {
    new OsmTagMapper().populateProperties(osmProvider.getWayPropertySet());
  }

  @Test
  void shouldGroupWithTwoConsecutiveNodes() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L0_WAY2);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_0), generateBarrierMap());
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldGroupWithBarrierNotSharingSameTwoNodes() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L0_WAY2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER4)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldNotGroupWithOnlyOneNodeInCommon() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L0_WAY3);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_0), generateBarrierMap());
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithTwoConsecutiveNodesOnAWall() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L0_WAY2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithBollardBetweenPedestrianAreas() {
    OsmArea a1 = createArea(PEDESTRIAN_WAY1);
    OsmArea a2 = createArea(PEDESTRIAN_WAY2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BOLLARD)
    );
    assertEquals(1, result.size());
  }

  @Test
  void shouldNotGroupWithWallBetweenPedestrianAreas() {
    OsmArea a1 = createArea(PEDESTRIAN_WAY1);
    OsmArea a2 = createArea(PEDESTRIAN_WAY2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER1)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupWithBollardBetweenCarAccessibleAreas() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L0_WAY2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BOLLARD)
    );
    assertEquals(2, result.size());
  }

  @Test
  void shouldGroupWithTwoConsecutiveNodesAtTheEndOfTwoWalls() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L0_WAY2);
    var result = OsmAreaGroup.groupAreas(
      Map.of(a1, LEVEL_0, a2, LEVEL_0),
      generateBarrierMap(BARRIER2, BARRIER3)
    );
    assertEquals(1, result.size());
    assertEquals(Set.of(a1, a2), Set.copyOf(result.getFirst().areas));
  }

  @Test
  void shouldNotGroupWithTwoDistinctCommonNodes() {
    OsmArea a1 = createArea(L0_WAY4);
    OsmArea a2 = createArea(L0_WAY5);
    var result = OsmAreaGroup.groupAreas(Map.of(a1, LEVEL_0, a2, LEVEL_0), generateBarrierMap());
    assertEquals(2, result.size());
  }

  @Test
  void shouldNotGroupBetweenLevels() {
    OsmArea a1 = createArea(L0_WAY1);
    OsmArea a2 = createArea(L1_WAY1);
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
