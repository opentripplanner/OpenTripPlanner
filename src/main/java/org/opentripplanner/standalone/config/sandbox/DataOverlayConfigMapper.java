package org.opentripplanner.standalone.config.sandbox;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayConfig;
import org.opentripplanner.ext.dataoverlay.configuration.IndexVariable;
import org.opentripplanner.ext.dataoverlay.configuration.ParameterBinding;
import org.opentripplanner.ext.dataoverlay.configuration.TimeUnit;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class DataOverlayConfigMapper {

  public static DataOverlayConfig map(NodeAdapter c) {
    if (c.isEmpty()) {
      return null;
    }
    return new DataOverlayConfig(
      c.of("fileName").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(),
      c
        .of("latitudeVariable")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(),
      c
        .of("longitudeVariable")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(),
      c
        .of("timeVariable")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(),
      c.of("timeFormat").withDoc(NA, /*TODO DOC*/"TODO").asEnum(TimeUnit.class),
      mapIndexVariables(
        c
          .of("indexVariables")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      ),
      mapRequestParameters(
        c
          .of("requestParameters")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .withDescription(/*TODO DOC*/"TODO")
          .asObject()
      )
    );
  }

  private static List<IndexVariable> mapIndexVariables(NodeAdapter c) {
    return c
      .asList()
      .stream()
      .map(DataOverlayConfigMapper::mapIndexVariable)
      .collect(Collectors.toList());
  }

  private static IndexVariable mapIndexVariable(NodeAdapter c) {
    return new IndexVariable(
      c.of("name").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(),
      c
        .of("displayName")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(),
      c.of("variable").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString()
    );
  }

  private static List<ParameterBinding> mapRequestParameters(NodeAdapter c) {
    return c
      .asList()
      .stream()
      .map(DataOverlayConfigMapper::mapRequestParameter)
      .collect(Collectors.toList());
  }

  private static ParameterBinding mapRequestParameter(NodeAdapter c) {
    return new ParameterBinding(
      c.of("name").withDoc(NA, /*TODO DOC*/"TODO").asEnum(ParameterName.class),
      c.of("variable").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString(),
      c.of("formula").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString()
    );
  }
}
