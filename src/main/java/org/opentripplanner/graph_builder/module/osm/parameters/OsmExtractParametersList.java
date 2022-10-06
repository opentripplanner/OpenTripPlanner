package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractParametersList {

  private final List<OsmExtractParameters> extracts;

  public OsmExtractParametersList(Collection<OsmExtractParameters> extracts) {
    this.extracts = List.copyOf(extracts);
  }

  public List<URI> osmFiles() {
    return extracts.stream().map(OsmExtractParameters::source).toList();
  }

  public List<OsmExtractParameters> extracts() {
    return extracts;
  }
}
