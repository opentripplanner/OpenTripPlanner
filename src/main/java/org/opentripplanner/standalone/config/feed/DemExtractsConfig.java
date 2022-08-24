package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.standalone.config.NodeAdapter;

public class DemExtractsConfig {

  public final List<DemExtractConfig> demExtractConfigs = new ArrayList<>();

  public DemExtractsConfig(NodeAdapter config) {
    for (NodeAdapter nodeAdapter : config.asList()) {
      demExtractConfigs.add(DemExtractConfigBuilder.of(nodeAdapter).build());
    }
  }

  public Collection<URI> demFiles() {
    return demExtractConfigs.stream().map(demExtractConfig -> demExtractConfig.source).toList();
  }
}
