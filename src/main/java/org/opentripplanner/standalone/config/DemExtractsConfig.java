package org.opentripplanner.standalone.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DemExtractsConfig {

  public final List<DemExtractConfig> demExtractConfigs = new ArrayList<>();

  public DemExtractsConfig(NodeAdapter config) {
    for (NodeAdapter nodeAdapter : config.asList()) {
      demExtractConfigs.add(new DemExtractConfig(nodeAdapter));
    }
  }

  public Collection<URI> demFiles() {
    return demExtractConfigs.stream().map(demExtractConfig -> demExtractConfig.source).toList();
  }
}
