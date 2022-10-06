package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.List;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractsConfig {

  public final List<OsmExtractConfig> osmExtractConfigs;

  public OsmExtractsConfig(List<OsmExtractConfig> osmExtractConfigs) {
    this.osmExtractConfigs = List.copyOf(osmExtractConfigs);
  }

  public List<URI> osmFiles() {
    return osmExtractConfigs.stream().map(OsmExtractConfig::source).toList();
  }
}
