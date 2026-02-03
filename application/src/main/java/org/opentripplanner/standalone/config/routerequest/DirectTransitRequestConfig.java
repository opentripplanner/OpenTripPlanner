package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_9;

import org.opentripplanner.routing.api.request.preference.DirectTransitPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class DirectTransitRequestConfig {

  static void map(NodeAdapter root, DirectTransitPreferences.Builder builder) {
    NodeAdapter c = root
      .of("directTransitSearch")
      .since(V2_9)
      .summary("Extend the search result with extra results using a direct transit search")
      .description(
        """
        The direct transit search finds results using a single transit leg, limited to a specified
        cost relaxation. It will include results even if they are not optimal in regard to the criteria
        in the main raptor search.

        This feature is off by default!
        """
      )
      .asObject();

    if (c.isEmpty()) {
      return;
    }
    var dft = DirectTransitPreferences.DEFAULT;

    builder
      .withEnabled(
        c
          .of("enabled")
          .since(V2_9)
          .summary("Enable the direct transit search")
          .asBoolean(dft.enabled())
      )
      .withCostRelaxFunction(
        c
          .of("costRelaxFunction")
          .since(V2_9)
          .summary("The generalized-cost window for which paths to include.")
          .description(
            """
            A generalized-cost relax function of `2x + 10m` will include paths that have a cost up
            to 2 times plus 10 minutes compared to the cheapest path. I.e. if the cheapest path has
            a cost of 100m the results will include paths with a cost 210m.
            """
          )
          .asCostLinearFunction(dft.costRelaxFunction())
      )
      .withExtraAccessEgressReluctance(
        c
          .of("extraAccessEgressReluctance")
          .since(V2_9)
          .summary("Add an extra cost factor to access/egress legs for these results")
          .description(
            """
            The cost for access/egress will be multiplied by this reluctance. This can be used to limit
            the amount of walking.
            """
          )
          .asDouble(dft.extraAccessEgressReluctance())
      )
      .withMaxAccessEgressDuration(
        c
          .of("maxAccessEgressDuration")
          .since(V2_9)
          .summary("A limit on the duration of access/egress for the direct transit search")
          .description(
            """
            This will limit the duration of access/egress for this search only. The default is the
            as for the regular search. Setting this to a higher value than what is used for the regular
            search will have have no effect.

            If set to zero, the search won't include results where access or egress is necessary. In
            this case the direct transit search will only be used when searching to and from a stop
            or station.
            """
          )
          .asDuration(dft.maxAccessEgressDuration().orElse(null))
      );
  }
}
