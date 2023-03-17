package org.opentripplanner.standalone.config.routerconfig.services;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class UberConfig {

  public static RideHailingServiceParameters.UberServiceParameters create(NodeAdapter c) {
    return new RideHailingServiceParameters.UberServiceParameters(
      c.of("clientId").since(V2_3).summary("OAuth client id to access the API.").asString(),
      c.of("clientSecret").since(V2_3).summary("OAuth client secret to access the API.").asString(),
      c
        .of("wheelchairAccessibleRideType")
        .since(V2_3)
        .summary("The id of the requested wheelchair accessible ride type.")
        .asString()
    );
  }
}
