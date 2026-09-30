package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;

import java.util.List;
import org.opentripplanner.graph_builder.module.transfer.api.RegularTransferParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;

public class RegularTransferConfig {

  public static RegularTransferParameters map(NodeAdapter root) {
    var builder = RegularTransferParameters.of();
    var dft = RegularTransferParameters.DEFAULT;

    builder.withMaxDuration(
      root
        .of("maxTransferDuration")
        .since(V2_1)
        .summary(
          "Transfers up to this duration with a mode-specific speed value will be pre-calculated and included in the Graph."
        )
        .asDuration(dft.maxDuration())
    );

    builder.withRequests(
      root
        .of("transferRequests")
        .since(OtpVersion.V2_1)
        .summary("Routing requests to use for pre-calculating stop-to-stop transfers.")
        .description(
          """
          It will use the street network if OSM data has already been loaded into the graph. Otherwise it
          will use straight-line distance between stops.

          If not set, the default behavior is to generate stop-to-stop transfers using the default request
          with street mode set to WALK. Use this to change the default or specify more than one way to
          transfer.

          **Example**

          ```JSON
          // build-config.json
          {
            "transferRequests": [
              { "modes": "WALK" },
              { "modes": "WALK", "wheelchairAccessibility": { "enabled": true }}
            ]
          }
          ```
          """
        )
        .asObjects(List.of(RouteRequest.defaultValue()), RouteRequestConfig::mapRouteRequest)
    );
    return builder.build();
  }
}
