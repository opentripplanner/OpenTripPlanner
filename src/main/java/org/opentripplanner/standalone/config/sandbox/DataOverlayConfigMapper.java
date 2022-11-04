package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;

import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayConfig;
import org.opentripplanner.ext.dataoverlay.configuration.IndexVariable;
import org.opentripplanner.ext.dataoverlay.configuration.ParameterBinding;
import org.opentripplanner.ext.dataoverlay.configuration.TimeUnit;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class DataOverlayConfigMapper {

  public static DataOverlayConfig map(NodeAdapter root, String dataOverlayName) {
    var node = root
      .of(dataOverlayName)
      .since(V2_2)
      .summary("Config for the DataOverlay Sandbox module")
      .asObject();

    if (node.isEmpty()) {
      return null;
    }
    return new DataOverlayConfig(
      node.of("fileName").since(NA).summary("TODO").asString(),
      node.of("latitudeVariable").since(NA).summary("TODO").asString(),
      node.of("longitudeVariable").since(NA).summary("TODO").asString(),
      node.of("timeVariable").since(NA).summary("TODO").asString(),
      node.of("timeFormat").since(NA).summary("TODO").asEnum(TimeUnit.class),
      node
        .of("indexVariables")
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObjects(DataOverlayConfigMapper::mapIndexVariable),
      node
        .of("requestParameters")
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObjects(DataOverlayConfigMapper::mapRequestParameter)
    );
  }

  private static IndexVariable mapIndexVariable(NodeAdapter c) {
    return new IndexVariable(
      c.of("name").since(NA).summary("TODO").asString(),
      c.of("displayName").since(NA).summary("TODO").asString(),
      c.of("variable").since(NA).summary("TODO").asString()
    );
  }

  private static ParameterBinding mapRequestParameter(NodeAdapter c) {
    return new ParameterBinding(
      c.of("name").since(NA).summary("TODO").asEnum(ParameterName.class),
      c.of("variable").since(NA).summary("TODO").asString(),
      c.of("formula").since(NA).summary("TODO").asString()
    );
  }
}
