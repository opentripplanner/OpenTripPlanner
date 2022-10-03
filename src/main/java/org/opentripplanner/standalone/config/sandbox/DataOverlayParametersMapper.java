package org.opentripplanner.standalone.config.sandbox;

import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParametersBuilder;
import org.opentripplanner.standalone.config.NodeAdapter;

public class DataOverlayParametersMapper {

  public static DataOverlayParameters map(NodeAdapter c) {
    var builder = new DataOverlayParametersBuilder();

    for (String param : DataOverlayParameters.parametersAsString()) {
      c.asDoubleOptional(param).ifPresent(it -> builder.add(param, it));
    }
    return builder.build();
  }
}
