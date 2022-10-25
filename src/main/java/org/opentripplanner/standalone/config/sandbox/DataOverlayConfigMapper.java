package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

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
      .withDoc(NA, /*TODO DOC*/"TODO")
      .withDescription(/*TODO DOC*/"TODO")
      .asObject();

    if (node.isEmpty()) {
      return null;
    }
    return new DataOverlayConfig(
      node.of("fileName").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      node.of("latitudeVariable").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      node.of("longitudeVariable").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      node.of("timeVariable").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      node.of("timeFormat").withDoc(NA, /*TODO DOC*/"TODO").asEnum(TimeUnit.class),
      node
        .of("indexVariables")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withDescription(/*TODO DOC*/"TODO")
        .asObjects(DataOverlayConfigMapper::mapIndexVariable),
      node
        .of("requestParameters")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withDescription(/*TODO DOC*/"TODO")
        .asObjects(DataOverlayConfigMapper::mapRequestParameter)
    );
  }

  private static IndexVariable mapIndexVariable(NodeAdapter c) {
    return new IndexVariable(
      c.of("name").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      c.of("displayName").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      c.of("variable").withDoc(NA, /*TODO DOC*/"TODO").asString()
    );
  }

  private static ParameterBinding mapRequestParameter(NodeAdapter c) {
    return new ParameterBinding(
      c.of("name").withDoc(NA, /*TODO DOC*/"TODO").asEnum(ParameterName.class),
      c.of("variable").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      c.of("formula").withDoc(NA, /*TODO DOC*/"TODO").asString()
    );
  }
}
