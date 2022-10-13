package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractsConfig {

  public final List<OsmExtractParameters> parameters;

  public OsmExtractsConfig(Collection<OsmExtractParameters> extracts) {
    parameters = List.copyOf(extracts);
  }

  public List<URI> osmFiles() {
    return parameters.stream().map(OsmExtractParameters::source).toList();
  }
}
