package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.test.support.ResourceLoader;

public class BicycleNetworkRelationsTest {

  /* The way https://www.openstreetmap.org/way/13876983 does not contain the tag lcn (local cycling network)
   * but because it is part of a relation that _does_, the tag is copied from the relation to the way.
   * This test assert that this is really happening.
   */
  @Test
  public void testBicycleRouteRelations() {
    var issueStore = DataImportIssueStore.NOOP;
    var osmdb = new OsmDatabase(issueStore, Set.of());
    var provider = new OsmProvider(ResourceLoader.file("/osm/ehningen-minimal.osm.pbf"), true);
    provider.readOSM(osmdb);
    osmdb.postLoad();

    var way = osmdb.getWay(13876983L);
    assertNotNull(way);

    assertEquals(way.getTag("lcn"), "yes");
    assertEquals(way.getTag("name"), "Gärtringer Weg");
  }
}
