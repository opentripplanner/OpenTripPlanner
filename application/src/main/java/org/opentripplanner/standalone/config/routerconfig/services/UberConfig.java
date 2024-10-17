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
        .of("wheelchairAccessibleProductId")
        .since(V2_3)
        .summary("The id of the requested wheelchair-accessible product ID.")
        .description("See `bannedProductIds` for a list of product IDs.")
        .asString(),
      c
        .of("bannedProductIds")
        .since(V2_3)
        .summary("The IDs of those product ids that should not be used for estimates.")
        .description(
          """
          See the current [list of Uber product ids](https://gist.github.com/leonardehrenfried/70f1346b045ad58224a6f43e4ef9ce7c).
          """
        )
        .asStringList(List.of())
    );
  }
}
