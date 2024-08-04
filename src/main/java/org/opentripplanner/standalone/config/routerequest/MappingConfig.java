package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_6;

import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;
import org.opentripplanner.routing.api.request.preference.MappingFeature;
import org.opentripplanner.routing.api.request.preference.MappingPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class MappingConfig {

  public static void mapItineraryFilterParams(
    String parameterName,
    NodeAdapter root,
    MappingPreferences.Builder builder
  ) {
    NodeAdapter c = root
      .of(parameterName)
      .since(V2_6)
      .summary(
        "Configure itinerary filters that may modify itineraries, sort them, and filter away less preferable results."
      )
      .asObject();

    if (c.isEmpty()) {
      return;
    }
    var dft = builder.original();

    builder
      .withOptInFeatures(
        c
          .of("debug")
          .since(V2_0)
          .summary(ItineraryFilterDebugProfile.OFF.typeDescription())
          .description(docEnumValueList(MappingFeature.values()))
          .asEnumSet(MappingFeature.class)
      )
      .build();
  }

}
