package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.model.OsmMemberType;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.test.support.ResourceLoader;

public class OsmDatabaseTest {

  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(OsmDatabaseTest.class);

  /**
   * The way https://www.openstreetmap.org/way/13876983 does not contain the tag lcn (local cycling network)
   * but because it is part of a relation that _does_, the tag is copied from the relation to the way.
   * This test assert that this is really happening.
   */
  @Test
  void bicycleRouteRelations() {
    var osmdb = new OsmDatabase(DataImportIssueStore.NOOP);
    var provider = new DefaultOsmProvider(RESOURCE_LOADER.file("ehningen-minimal.osm.pbf"), true);
    provider.readOsm(osmdb);
    osmdb.postLoad();

    var way = osmdb.getWay(13876983L);
    assertNotNull(way);

    assertEquals("yes", way.getTag("lcn"));
    assertEquals("Gärtringer Weg", way.getTag("name"));
  }

  /**
   * When extracting Austria, Geofabrik produces data where a public transport relation that crosses
   * a border (https://www.openstreetmap.org/relation/4027804) references ways that are not in the
   * extract. This needs to be dealt with gracefully.
   */
  @Test
  void invalidPublicTransportRelation() {
    var osmdb = new OsmDatabase(DataImportIssueStore.NOOP);
    var file = RESOURCE_LOADER.file("brenner-invalid-relation-reference.osm.pbf");
    var provider = new DefaultOsmProvider(file, true);
    provider.readOsm(osmdb);
    osmdb.postLoad();

    var way = osmdb.getWay(302732658L);
    assertNotNull(way);
    assertEquals("platform", way.getTag("public_transport"));
  }

  @Test
  void isNodeBelongsToWayShouldNotReturnTrueForNodesSolelyOnBarriers() {
    var osmdb = new OsmDatabase(DataImportIssueStore.NOOP);

    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0, 1);
    n2.setId(2);
    var n3 = new OsmNode(0, 2);
    n3.setId(3);

    var chain = new OsmWay();
    chain.addTag("barrier", "chain");
    chain.setId(999);
    chain.addNodeRef(1);
    chain.addNodeRef(2);

    var path = new OsmWay();
    path.setId(1);
    path.addTag("highway", "path");
    path.addNodeRef(2);
    path.addNodeRef(3);

    osmdb.addWay(chain);
    osmdb.addWay(path);
    osmdb.doneSecondPhaseWays();

    assertFalse(osmdb.isNodeBelongsToWay(1L));
    assertTrue(osmdb.isNodeBelongsToWay(2L));
    assertTrue(osmdb.isNodeBelongsToWay(3L));
  }

  @Test
  void testWayIsntKeptForAreas() {
    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0, 3);
    n2.setId(2);
    var n3 = new OsmNode(3, 3);
    n3.setId(3);
    var n4 = new OsmNode(3, 0);
    n4.setId(4);
    var n5 = new OsmNode(0.3, 1);
    n5.setId(5);
    var n6 = new OsmNode(0.3, 1.5);
    n6.setId(6);
    var n7 = new OsmNode(0.7, 1.5);
    n7.setId(7);
    var n8 = new OsmNode(0.7, 1);
    n8.setId(8);
    var n9 = new OsmNode(0.3, 2);
    n9.setId(9);
    var n10 = new OsmNode(0.3, 2.5);
    n10.setId(10);
    var n11 = new OsmNode(0.7, 2.5);
    n11.setId(11);
    var n12 = new OsmNode(0.7, 2);
    n12.setId(12);
    var n13 = new OsmNode(3, 3);
    n13.setId(13);
    var n14 = new OsmNode(3, 4);
    n14.setId(14);
    var n15 = new OsmNode(4, 3);
    n15.setId(15);

    var simpleArea = new OsmWay();
    simpleArea.addTag("public_transport", "platform");
    simpleArea.addNodeRef(13);
    simpleArea.addNodeRef(14);
    simpleArea.addNodeRef(15);
    simpleArea.addNodeRef(13);

    var outerRing = new OsmWay();
    outerRing.setId(1);
    outerRing.addNodeRef(1);
    outerRing.addNodeRef(2);
    outerRing.addNodeRef(3);
    outerRing.addNodeRef(4);
    outerRing.addNodeRef(1);
    outerRing.addTag("highway", "residential");

    var innerRing = new OsmWay();
    innerRing.setId(2);
    innerRing.addNodeRef(5);
    innerRing.addNodeRef(6);
    innerRing.addNodeRef(7);
    innerRing.addNodeRef(8);
    innerRing.addNodeRef(5);

    var innerRingWithBarrier = new OsmWay();
    innerRingWithBarrier.setId(3);
    innerRingWithBarrier.addNodeRef(9);
    innerRingWithBarrier.addNodeRef(10);
    innerRingWithBarrier.addNodeRef(11);
    innerRingWithBarrier.addNodeRef(12);
    innerRingWithBarrier.addNodeRef(9);
    innerRingWithBarrier.addTag("barrier", "chain");

    var multipolygon = new OsmRelation();
    multipolygon.addTag("type", "multipolygon");
    multipolygon.addTag("highway", "pedestrian");

    var outerMember = new OsmRelationMember();
    outerMember.setRole("outer");
    outerMember.setType(OsmMemberType.WAY);
    outerMember.setRef(1);

    var innerMember = new OsmRelationMember();
    innerMember.setRole("inner");
    innerMember.setType(OsmMemberType.WAY);
    innerMember.setRef(2);

    var innerBarrierMember = new OsmRelationMember();
    innerBarrierMember.setRole("inner");
    innerBarrierMember.setType(OsmMemberType.WAY);
    innerBarrierMember.setRef(3);

    multipolygon.addMember(outerMember);
    multipolygon.addMember(innerMember);
    multipolygon.addMember(innerBarrierMember);

    var provider = new TestOsmProvider(
      List.of(multipolygon),
      List.of(simpleArea, outerRing, innerRing, innerRingWithBarrier),
      List.of(n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11, n12, n13, n14, n15)
    );
    new OsmTagMapper().populateProperties(provider.getWayPropertySet());

    var osmdb = new OsmDatabase(DataImportIssueStore.NOOP);
    osmdb.addRelation(multipolygon);
    osmdb.doneFirstPhaseRelations();
    osmdb.addWay(simpleArea);
    osmdb.addWay(outerRing);
    osmdb.addWay(innerRing);
    osmdb.addWay(innerRingWithBarrier);
    osmdb.doneSecondPhaseWays();
    osmdb.addNode(n1);
    osmdb.addNode(n2);
    osmdb.addNode(n3);
    osmdb.addNode(n4);
    osmdb.addNode(n5);
    osmdb.addNode(n6);
    osmdb.addNode(n7);
    osmdb.addNode(n8);
    osmdb.addNode(n9);
    osmdb.addNode(n10);
    osmdb.addNode(n11);
    osmdb.addNode(n12);
    osmdb.addNode(n13);
    osmdb.addNode(n14);
    osmdb.addNode(n15);
    osmdb.doneThirdPhaseNodes();

    // innerRing and simpleArea should no longer exist
    assertEquals(2, osmdb.getWays().size());
    assertNull(osmdb.getWay(innerRing.getId()));

    // simpleArea and multipolygon
    assertEquals(2, osmdb.getWalkableAreas().size());

    // innerRingWithBarrier should not be polluted with the highway tag when fetched from the way
    assertFalse(osmdb.getWay(innerRingWithBarrier.getId()).hasTag("highway"));
  }
}
