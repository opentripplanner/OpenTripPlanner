package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.List;
import org.opentripplanner.standalone.config.NodeAdapter;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractsConfig {

  public final List<OsmExtractConfig> osmExtractConfigs;

  public OsmExtractsConfig(NodeAdapter config) {
    osmExtractConfigs =
      config
        .asList()
        .stream()
        .map(osmConfig -> OsmExtractConfigBuilder.of(osmConfig).build())
        .toList();
  }

  public List<URI> osmFiles() {
    return osmExtractConfigs.stream().map(OsmExtractConfig::source).toList();
  }
}
