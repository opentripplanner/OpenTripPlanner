package org.opentripplanner.annotation;

import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterParameters;

public class TestParameter extends PollingGraphUpdaterParameters {

  private String example;

  public TestParameter(NodeAdapter c) {
    super(c);
    this.example = c.asText("example");
  }
}
