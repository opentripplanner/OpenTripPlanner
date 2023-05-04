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
          As of May 2023 Uber had the following product IDs in Portland:
          
            - `6d5eb4b2-3c85-4ef3-854e-3219da0f0df3`: Premier
            - `1196d0dd-423b-4a81-a1d8-615367d3a365`: UberX Share
            - `b6e63411-bf85-4bc7-aca2-bb2e53a20ba4`: Comfort Electric
            - `a6eef2e1-c99a-436f-bde9-fefb9181c0b0`: UberX
            - `62037135-bd5a-43bf-bd77-d4558ffe2bf8`: UberX Priority
            - `0410f2a9-7019-405b-a5ff-d0c92c59339d`: Comfort
            - `a5e722b3-6a20-4c90-b5d7-f9a523904348`: UberXL
            - `4c6e2bde-9242-4634-93f0-8182a4d96e15`: Uber Green
            - `8ddc7ce4-67d1-4ac4-8b56-205bd6a6314e`: Assist
            - `f58761e5-8dd5-4940-a472-872f1236c596`: Uber Pet
            - `c076b1fb-3146-49ec-b56e-eec8348e75bd`: Connect
            - `0e9145be-98bb-48dd-a0bf-32964ac8df19`: WAV
          """
        )
        .asStringList(List.of())
    );
  }
}
