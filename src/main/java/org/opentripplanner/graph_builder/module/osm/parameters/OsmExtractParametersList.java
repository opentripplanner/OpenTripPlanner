package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractParametersList {

  public final List<OsmExtractParameters> parameters;

  public OsmExtractParametersList(Collection<OsmExtractParameters> extracts) {
    parameters = List.copyOf(extracts);
  }

  public List<URI> osmFiles() {
    return parameters.stream().map(OsmExtractParameters::source).toList();
  }
}
