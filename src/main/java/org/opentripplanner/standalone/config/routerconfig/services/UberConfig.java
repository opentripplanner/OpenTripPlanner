package org.opentripplanner.standalone.config.routerconfig.services;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import java.util.List;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class UberConfig {

  public static RideHailingServiceParameters create(NodeAdapter c) {
    return new RideHailingServiceParameters(
      c.of("clientId").since(V2_3).summary("OAuth client id to access the API.").asString(),
      c.of("clientSecret").since(V2_3).summary("OAuth client secret to access the API.").asString(),
      c
        .of("wheelchairAccessibleRideType")
        .since(V2_3)
        .summary("The id of the requested wheelchair accessible ride type.")
        .asString(),
      c
        .of("bannedRideTypes")
        .since(V2_3)
        .summary("The IDs of those ride types that should not be used for estimates.")
        .asStringList(List.of())
    );
  }
}
