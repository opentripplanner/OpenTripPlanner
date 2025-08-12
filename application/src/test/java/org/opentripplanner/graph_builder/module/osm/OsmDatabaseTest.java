package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.model.OsmNode;
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
}
