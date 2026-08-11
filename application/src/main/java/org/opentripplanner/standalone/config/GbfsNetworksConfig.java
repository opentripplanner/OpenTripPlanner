package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.gbfs.network.GbfsNetworkParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Maps the {@code gbfs} section of {@code otp-config.json} into {@link GbfsNetworkOverrides}.
 * <p>
 * The section lives in {@code otp-config.json} because it is the only configuration file read in
 * both the graph build and the serve phase, and both the vehicle rental graph builder and the
 * vehicle rental service directory need these values.
 * <p>
 * Field inheritance is resolved while parsing rather than by merging afterwards: the
 * {@code defaults} block is mapped first, and the resulting parameters are used as the fallback
 * value for every field of every listed network.
 */
public class GbfsNetworksConfig {

  public static GbfsNetworkOverrides map(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_10)
      .summary("Per-network GBFS configuration shared by the vehicle rental sandboxes.")
      .description(
        """
        `vehicleRentalServiceDirectory` loads its feeds from a GBFS manifest and takes its
        per-network settings from here, keyed by the GBFS `system_id`.

        This section lives in `otp-config.json` because it is the only configuration file that is
        read both when the graph is built and when it is served. Note that it is not embedded in
        the graph, so it must be present in the deployment directory in both phases.
        """
      )
      .asObject();

    if (c.isEmpty()) {
      return GbfsNetworkOverrides.none();
    }

    var defaults = mapNetworkParameters(
      c
        .of("defaults")
        .since(V2_10)
        .summary("Values applied to every network that does not set them itself.")
        .description(
          """
          A network listed in `networks` overrides only the fields it names and inherits the rest
          from here. Setting defaults does not by itself widen which networks are loaded - see
          `includeUnlistedNetworks`.
          """
        )
        .asObject(),
      GbfsNetworkParameters.DEFAULT
    );

    var includeUnlistedNetworks = c
      .of("includeUnlistedNetworks")
      .since(V2_10)
      .summary("Whether networks in the GBFS manifest but absent from `networks` are loaded.")
      .description(
        """
        When `false` such a network is skipped with a warning, so `networks` acts as a whitelist.
        When `true` it is loaded with `defaults` applied.
        """
      )
      .asBoolean(false);

    return new GbfsNetworkOverrides(defaults, includeUnlistedNetworks, mapNetworks(c, defaults));
  }

  private static Map<String, GbfsNetworkParameters> mapNetworks(
    NodeAdapter config,
    GbfsNetworkParameters defaults
  ) {
    var networks = config
      .of("networks")
      .since(V2_10)
      .summary("Per-network overrides, keyed by the GBFS `system_id`.")
      .asObjects(List.of(), node ->
        new NamedNetwork(
          node
            .of("network")
            .since(V2_10)
            .summary("The GBFS `system_id` of the network these values apply to.")
            .asString(),
          mapNetworkParameters(node, defaults)
        )
      );

    var byNetwork = new LinkedHashMap<String, GbfsNetworkParameters>();
    for (var it : networks) {
      byNetwork.put(it.network(), it.parameters());
    }
    return byNetwork;
  }

  private static GbfsNetworkParameters mapNetworkParameters(
    NodeAdapter node,
    GbfsNetworkParameters defaults
  ) {
    return new GbfsNetworkParameters(
      node
        .of("geofencingZones")
        .since(V2_10)
        .summary("Which phase computes and applies this network's geofencing zones.")
        .description(
          """
          - `realtime` - the vehicle rental updater loads and applies the zones.
          - `off` - the zones are not processed.
          """
        )
        .asEnum(defaults.geofencingZones()),
      node
        .of("requireDropOffInsideBusinessArea")
        .since(V2_10)
        .summary("Whether a rented vehicle must be dropped off before leaving the business area.")
        .description(
          """
          A business area is inferred from geofencing zones whose ride and traversal rules are all
          permissive. When enabled, the router forces a drop-off at the border of that area,
          preventing itineraries that leave the operator's service area with a rented vehicle.

          Has no effect when `geofencingZones` is `off`.
          """
        )
        .asBoolean(defaults.requireDropOffInsideBusinessArea()),
      node
        .of("allowKeepingVehicleAtDestination")
        .since(V2_10)
        .summary("Whether a vehicle rented from a station may be kept at the destination.")
        .description(
          """
          When disabled a vehicle rented from a station must be returned to another station, so an
          itinerary can only end with the vehicle parked at one.
          """
        )
        .asBoolean(defaults.allowKeepingVehicleAtDestination())
    );
  }

  /** A network entry, pairing the name it is keyed by with its resolved parameters. */
  private record NamedNetwork(String network, GbfsNetworkParameters parameters) {}
}
