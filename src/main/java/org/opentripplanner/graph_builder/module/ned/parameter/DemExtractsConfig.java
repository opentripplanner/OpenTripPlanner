package org.opentripplanner.graph_builder.module.ned.parameter;

import java.net.URI;
import java.util.List;

public class DemExtractsConfig {

  private final List<DemExtractConfig> demExtractConfigs;

  public DemExtractsConfig(List<DemExtractConfig> demExtractConfigs) {
    this.demExtractConfigs = List.copyOf(demExtractConfigs);
  }

  public List<DemExtractConfig> demExtracts() {
    return demExtractConfigs;
  }

  public List<URI> demFiles() {
    return demExtractConfigs.stream().map(DemExtractConfig::source).toList();
  }
}
