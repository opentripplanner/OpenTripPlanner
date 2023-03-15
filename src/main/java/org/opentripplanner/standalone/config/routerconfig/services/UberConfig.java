package org.opentripplanner.standalone.config.routerconfig.services;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.ext.ridehailing.service.CarHailingServiceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class UberConfig {

  public static CarHailingServiceParameters.UberServiceParameters create(NodeAdapter c) {
    return new CarHailingServiceParameters.UberServiceParameters(
      c.of("clientId").since(V2_3).summary("OAuth client id to access the API.").asString(),
      c.of("clientSecret").since(V2_3).summary("OAuth client secret to access the API.").asString(),
      c.of("wheelchairAccessibleRideType").since(V2_3).summary("HTTP headers to add.").asString()
    );
  }
}
