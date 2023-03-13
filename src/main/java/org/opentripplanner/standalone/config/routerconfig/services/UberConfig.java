package org.opentripplanner.standalone.config.routerconfig.services;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.ext.carhailing.service.uber.UberServiceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class UberConfig {

  public static UberServiceParameters create(NodeAdapter c) {
    return new UberServiceParameters(
      c.of("clientId").since(V2_3).summary("OAuth client id to access the API.").asString(),
      c.of("clientSecret").since(V2_3).summary("OAuth client secret to access the API.").asString(),
      c.of("wheelchairAccessibleRideType").since(V2_3).summary("HTTP headers to add.").asString()
    );
  }
}
