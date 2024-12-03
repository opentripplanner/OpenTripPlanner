package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.util.EnumMap;
import java.util.Map;
import org.opentripplanner.graph_builder.module.TransferParameters;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransferConfig {

  public static Map<StreetMode, TransferParameters> map(NodeAdapter root, String parameterName) {
    return root
      .of(parameterName)
      .since(V2_7)
      .summary("Configures properties for transfer calculations.")
      .asEnumMap(StreetMode.class, TransferParametersMapper::map, new EnumMap<>(StreetMode.class));
  }
}
