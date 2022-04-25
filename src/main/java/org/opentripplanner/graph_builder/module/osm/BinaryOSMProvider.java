package org.opentripplanner.graph_builder.module.osm;

import java.io.File;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.graph_builder.module.osm.contract.OSMParser;
import org.opentripplanner.graph_builder.module.osm.contract.PhaseAwareOSMEntityStore;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes: First the relations, then
 * the ways, then the nodes are also loaded.
 */
public class BinaryOSMProvider extends AbstractOSMProvider {

  public BinaryOSMProvider(File file, boolean cacheDataInMem) {
    super(file, cacheDataInMem);
  }

  public BinaryOSMProvider(DataSource source, boolean cacheDataInMem) {
    super(source, cacheDataInMem);
  }

  @Override
  protected OSMParser createParser(PhaseAwareOSMEntityStore osmdb) {
    return new BinaryOSMParser(osmdb);
  }
}
