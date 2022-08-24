package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.standalone.config.NodeAdapter;

/**
 * Configure the list of OpenStreetMap extracts.
 */
public class OsmExtractsConfig {

  public final List<OsmExtractConfig> osmExtractConfigs = new ArrayList<>();

  public OsmExtractsConfig(NodeAdapter config) {
    for (NodeAdapter nodeAdapter : config.asList()) {
      osmExtractConfigs.add(OsmExtractConfigBuilder.of(nodeAdapter).build());
    }
  }

  public List<URI> osmFiles() {
    return osmExtractConfigs.stream().map(osmFeedConfig -> osmFeedConfig.source).toList();
  }
}
