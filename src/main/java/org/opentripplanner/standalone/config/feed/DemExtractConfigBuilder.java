package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class DemExtractConfigBuilder {

  private URI source;
  private Double elevationUnitMultiplier;

  public static DemExtractConfigBuilder of(NodeAdapter config) {
    DemExtractConfigBuilder demExtractConfigBuilder = new DemExtractConfigBuilder();
    demExtractConfigBuilder.source =
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri();
    demExtractConfigBuilder.elevationUnitMultiplier =
      config
        .of("elevationUnitMultiplier")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .asDoubleOptional()
        .orElse(null);
    return demExtractConfigBuilder;
  }

  public DemExtractConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public DemExtractConfig build() {
    return new DemExtractConfig(this);
  }

  public URI getSource() {
    return source;
  }

  public Double getElevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }
}
