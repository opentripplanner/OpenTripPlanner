package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.util.List;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;
import org.opentripplanner.transfer.dataimport.api.FlexTransferParameters;

/**
 * Parses the {@code transfers.flex} block of the build config, the configuration of the transfers
 * created for flex routing.
 */
public class FlexTransferConfig {

  public static FlexTransferParameters map(NodeAdapter transfers) {
    var flex = transfers
      .of("flex")
      .since(V2_10)
      .summary("The configuration of the transfers created for flex routing.")
      .description(
        """
        These replace the old `maxTransferDuration` and `transferRequests` of the build config.
        """
      )
      .asObject();
    var dft = FlexTransferParameters.DEFAULT;

    var maxDuration = flex
      .of("maxDuration")
      .since(V2_10)
      .summary(
        "Transfers up to this duration with a mode-specific speed value will be pre-calculated and included in the Graph."
      )
      .asDuration(dft.maxDuration());

    var requests = flex
      .of("requests")
      .since(V2_10)
      .summary("Routing requests to use for pre-calculating stop-to-stop transfers.")
      .description(
        """
        It will use the street network if OSM data has already been loaded into the graph. Otherwise it
        will use straight-line distance between stops.

        If not set, the default behavior is to generate stop-to-stop transfers using the default request
        with street mode set to WALK. Only the requests with street mode WALK are used.

        **Example**

        ```JSON
        // build-config.json
        {
          "transfers": {
            "flex": {
              "requests": [
                { "modes": "WALK" },
                { "modes": "WALK", "wheelchairAccessibility": { "enabled": true }}
              ]
            }
          }
        }
        ```
        """
      )
      .asObjects(List.of(RouteRequest.defaultValue()), RouteRequestConfig::mapRouteRequest);

    return new FlexTransferParameters(maxDuration, requests);
  }
}
