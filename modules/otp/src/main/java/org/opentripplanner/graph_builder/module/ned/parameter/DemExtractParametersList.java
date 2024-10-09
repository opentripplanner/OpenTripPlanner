package org.opentripplanner.graph_builder.module.ned.parameter;

import java.net.URI;
import java.util.List;

public class DemExtractParametersList {

  private final List<DemExtractParameters> extracts;

  public DemExtractParametersList(List<DemExtractParameters> demExtractParameters) {
    this.extracts = List.copyOf(demExtractParameters);
  }

  public List<DemExtractParameters> demExtracts() {
    return extracts;
  }

  public List<URI> demFiles() {
    return extracts.stream().map(DemExtractParameters::source).toList();
  }
}
