package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.List;
import org.opentripplanner.standalone.config.NodeAdapter;

public class DemExtractsConfig {

  public final List<DemExtractConfig> demExtractConfigs;

  public DemExtractsConfig(NodeAdapter config) {
    demExtractConfigs =
      config
        .asList()
        .stream()
        .map(demConfig -> DemExtractConfigBuilder.of(demConfig).build())
        .toList();
  }

  public List<URI> demFiles() {
    return demExtractConfigs.stream().map(DemExtractConfig::source).toList();
  }
}
