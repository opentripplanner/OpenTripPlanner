package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.openstreetmap.OsmProvider;

public class OsmDatabaseTest {

  /**
   * The way https://www.openstreetmap.org/way/13876983 does not contain the tag lcn (local cycling network)
   * but because it is part of a relation that _does_, the tag is copied from the relation to the way.
   * This test assert that this is really happening.
   */
  @Test
  void bicycleRouteRelations() {
    var osmdb = new OsmDatabase(DataImportIssueStore.NOOP);
    var provider = new OsmProvider(
      new File("src/test/resources/germany/ehningen-minimal.osm.pbf"),
      true
    );
    provider.readOSM(osmdb);
    osmdb.postLoad();

    var way = osmdb.getWay(13876983L);
    assertNotNull(way);

    assertEquals(way.getTag("lcn"), "yes");
    assertEquals(way.getTag("name"), "Gärtringer Weg");
  }

  /**
   * When extracting Austria, Geofabrik produces data where a public transport relation that crosses
   * a border (https://www.openstreetmap.org/relation/4027804) references ways that are not in the
   * extract. This needs to be dealt with gracefully.
   */
  @Test
  void invalidPublicTransportRelation() {
    var osmdb = new OsmDatabase(DataImportIssueStore.NOOP);
    var url = getClass().getResource("brenner-invalid-relation-reference.osm.pbf");
    assertNotNull(url);
    var file = new File(url.getFile());
    var provider = new OsmProvider(file, true);
    provider.readOSM(osmdb);
    osmdb.postLoad();

    var way = osmdb.getWay(302732658L);
    assertNotNull(way);
    assertEquals(way.getTag("public_transport"), "platform");
  }
}
