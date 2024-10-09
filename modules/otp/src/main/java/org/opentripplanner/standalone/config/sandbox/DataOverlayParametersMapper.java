package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParametersBuilder;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class DataOverlayParametersMapper {

  public static DataOverlayParameters map(NodeAdapter c) {
    var builder = new DataOverlayParametersBuilder();

    for (String param : DataOverlayParameters.parametersAsString()) {
      c
        .of(param)
        .since(NA)
        .summary("TODO")
        .asDoubleOptional()
        .ifPresent(it -> builder.add(param, it));
    }
    return builder.build();
  }
}
