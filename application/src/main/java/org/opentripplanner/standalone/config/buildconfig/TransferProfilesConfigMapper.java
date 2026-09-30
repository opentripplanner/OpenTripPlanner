package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfileConfig;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Parses the new {@code transfers:} build-config block (see
 * {@code opentripplanner/OpenTripPlanner#7998}) into a {@link TransferProfilesConfig}. Entirely
 * separate from {@link RegularTransferConfig}/{@code transferRequests}, which
 * {@code DirectTransferGenerator} keeps reading unchanged for FLEX.
 * <p>
 * Only {@code walk} is supported for now (see {@code TransferProfileType}) - support for other
 * profiles, and for a duration limit, is deliberately deferred until designed further.
 */
public class TransferProfilesConfigMapper {

  public static TransferProfilesConfig map(NodeAdapter root) {
    var c = root
      .of("transfers")
      .since(V2_10)
      .summary("Transfer profiles for the raptor-data regular-transfer pipeline.")
      .description(
        """
        Replaces `transferRequests` for regular (non-FLEX) transfer generation. Only the `walk`
        profile is supported for now. If this block is omitted entirely, a single default `walk`
        profile is used, mirroring the implicit default of the old `transferRequests` config.
        """
      )
      .asObject();

    List<TransferProfileConfig> profiles = new ArrayList<>();
    if (c.exist("walk")) {
      var p = c.of("walk").since(V2_10).summary("The walk transfer profile.").asObject();
      profiles.add(mapProfile(p));
    } else {
      // No `transfers` block (or an empty one) configured - fall back to a single default WALK
      // profile, mirroring the implicit default of the old `transferRequests` config.
      profiles.add(new TransferProfileConfig(TransferProfileType.WALK, WalkPreferences.DEFAULT));
    }

    return new TransferProfilesConfig(profiles);
  }

  private static TransferProfileConfig mapProfile(NodeAdapter p) {
    var prefs = p
      .of("preferences")
      .since(V2_10)
      .summary("Street-search preferences used to discover and cost this profile's paths.")
      .asObject();
    WalkPreferences preferences = mapWalkPreferences(prefs);

    return new TransferProfileConfig(TransferProfileType.WALK, preferences);
  }

  private static WalkPreferences mapWalkPreferences(NodeAdapter p) {
    var dft = WalkPreferences.of();
    var original = dft.original();
    dft.withSpeed(
      org.opentripplanner.core.model.basic.Speed.ofMetersPerSecond(
        p
          .of("speed")
          .since(V2_10)
          .summary("Walking speed, in meters per second.")
          .asDouble(original.speed().toMetersPerSecond())
      )
    );
    dft.withReluctance(
      org.opentripplanner.core.model.basic.Reluctance.of(
        p
          .of("reluctance")
          .since(V2_10)
          .summary("A multiplier for the cost of walking.")
          .asDouble(original.reluctance().value())
      )
    );
    dft.withStairs(
      p
        .of("stairs")
        .since(V2_10)
        .summary("Whether stairs are allowed, discouraged, or forbidden for this profile.")
        .asEnum(original.stairs())
    );
    return dft.build();
  }
}
