package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_6;

import org.opentripplanner.routing.api.request.preference.MappingFeature;
import org.opentripplanner.routing.api.request.preference.MappingPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class MappingConfig {

  public static void mapMappingParams(
    String parameterName,
    NodeAdapter root,
    MappingPreferences.Builder builder
  ) {
    NodeAdapter c = root
      .of(parameterName)
      .since(V2_6)
      .summary("Configure mapping of the internal data structures into itineraries.")
      .asObject();

    if (c.isEmpty()) {
      return;
    }

    builder
      .withOptInFeatures(
        c
          .of("optInFeatures")
          .since(V2_6)
          .summary(MappingFeature.TRANSFER_LEG_ON_SAME_STOP.typeDescription())
          .description(docEnumValueList(MappingFeature.values()))
          .asEnumSet(MappingFeature.class)
      )
      .build();
  }
}
