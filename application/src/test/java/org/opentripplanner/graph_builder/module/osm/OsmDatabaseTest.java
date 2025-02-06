package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.DefaultOsmProvider;
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
}
