package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractsConfig {

  public final List<OsmExtractConfig> osmExtractConfigs;

  public OsmExtractsConfig(Collection<OsmExtractConfig> extracts) {
    osmExtractConfigs = List.copyOf(extracts);
  }

  public List<URI> osmFiles() {
    return osmExtractConfigs.stream().map(OsmExtractConfig::source).toList();
  }
}
