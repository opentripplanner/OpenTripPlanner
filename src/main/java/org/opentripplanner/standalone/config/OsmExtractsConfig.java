package org.opentripplanner.standalone.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractsConfig {

  public final List<OsmExtractConfig> osmExtractConfigs = new ArrayList<>();

  OsmExtractsConfig(NodeAdapter config) {
    for (NodeAdapter nodeAdapter : config.asList()) {
      osmExtractConfigs.add(new OsmExtractConfig(nodeAdapter));
    }
  }

  public List<URI> osmFiles() {
    return osmExtractConfigs.stream().map(osmFeedConfig -> osmFeedConfig.source).toList();
  }
}
