package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.TestOsmProvider;
import org.opentripplanner.osm.model.OsmMemberType;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmRelation;
import org.opentripplanner.osm.model.OsmRelationMember;
import org.opentripplanner.osm.model.OsmWay;
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

    var chain = OsmWay.of().withId(999).withTag("barrier", "chain").addNodeRef(1, 2).build();

    var path = OsmWay.of().withId(1).withTag("highway", "path").addNodeRef(2, 3).build();

    osmdb.addWay(chain);
    osmdb.addWay(path);
    osmdb.doneSecondPhaseWays();

    assertFalse(osmdb.isNodeBelongsToWay(1L));
    assertTrue(osmdb.isNodeBelongsToWay(2L));
    assertTrue(osmdb.isNodeBelongsToWay(3L));
  }

  @Test
  void testWayIsntKeptForAreas() {
    var n1 = OsmNode.of().withId(1).withLatLon(0, 0).build();
    var n2 = OsmNode.of().withId(2).withLatLon(0, 3).build();
    var n3 = OsmNode.of().withId(3).withLatLon(3, 3).build();
    var n4 = OsmNode.of().withId(4).withLatLon(3, 0).build();
    var n5 = OsmNode.of().withId(5).withLatLon(0.3, 1).build();
    var n6 = OsmNode.of().withId(6).withLatLon(0.3, 1.5).build();
    var n7 = OsmNode.of().withId(7).withLatLon(0.7, 1.5).build();
    var n8 = OsmNode.of().withId(8).withLatLon(0.7, 1).build();
    var n9 = OsmNode.of().withId(9).withLatLon(0.3, 2).build();
    var n10 = OsmNode.of().withId(10).withLatLon(0.3, 2.5).build();
    var n11 = OsmNode.of().withId(11).withLatLon(0.7, 2.5).build();
    var n12 = OsmNode.of().withId(12).withLatLon(0.7, 2).build();
    var n13 = OsmNode.of().withId(13).withLatLon(3, 3).build();
    var n14 = OsmNode.of().withId(14).withLatLon(3, 4).build();
    var n15 = OsmNode.of().withId(15).withLatLon(4, 3).build();

    var simpleArea = OsmWay.of()
      .withTag("public_transport", "platform")
      .addNodeRef(13, 14, 15, 13)
      .build();

    var outerRing = OsmWay.of()
      .withId(1)
      .addNodeRef(1, 2, 3, 4, 1)
      .withTag("highway", "residential")
      .build();

    var innerRing = OsmWay.of().withId(2).addNodeRef(5, 6, 7, 8, 5).build();

    var innerRingWithBarrier = OsmWay.of()
      .withId(3)
      .addNodeRef(9, 10, 11, 12, 9)
      .withTag("barrier", "chain")
      .build();

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

    var multipolygon = OsmRelation.of()
      .addTag("type", "multipolygon")
      .addTag("highway", "pedestrian")
      .addMember(outerMember)
      .addMember(innerMember)
      .addMember(innerBarrierMember)
      .build();

    var provider = TestOsmProvider.of().build();

    // Set OsmProvider on all entities
    multipolygon = multipolygon.copy().withOsmProvider(provider).build();
    simpleArea = simpleArea.copy().withOsmProvider(provider).build();
    outerRing = outerRing.copy().withOsmProvider(provider).build();
    innerRing = innerRing.copy().withOsmProvider(provider).build();
    innerRingWithBarrier = innerRingWithBarrier.copy().withOsmProvider(provider).build();
    n1 = n1.copy().withOsmProvider(provider).build();
    n2 = n2.copy().withOsmProvider(provider).build();
    n3 = n3.copy().withOsmProvider(provider).build();
    n4 = n4.copy().withOsmProvider(provider).build();
    n5 = n5.copy().withOsmProvider(provider).build();
    n6 = n6.copy().withOsmProvider(provider).build();
    n7 = n7.copy().withOsmProvider(provider).build();
    n8 = n8.copy().withOsmProvider(provider).build();
    n9 = n9.copy().withOsmProvider(provider).build();
    n10 = n10.copy().withOsmProvider(provider).build();
    n11 = n11.copy().withOsmProvider(provider).build();
    n12 = n12.copy().withOsmProvider(provider).build();
    n13 = n13.copy().withOsmProvider(provider).build();
    n14 = n14.copy().withOsmProvider(provider).build();
    n15 = n15.copy().withOsmProvider(provider).build();

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
