package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.warmup.api.WarmupApi;
import org.opentripplanner.warmup.api.WarmupParameters;

/**
 * Maps the {@code warmup} section of {@code router-config.json} into a {@link WarmupParameters}
 * record consumed by the warmup module.
 */
public final class WarmupConfig {

  private static final List<StreetMode> DEFAULT_ACCESS_MODES = List.of(
    StreetMode.WALK,
    StreetMode.CAR_TO_PARK
  );
  private static final List<StreetMode> DEFAULT_EGRESS_MODES = List.of(
    StreetMode.WALK,
    StreetMode.WALK
  );

  private WarmupConfig() {}

  @Nullable
  public static WarmupParameters mapWarmupConfig(String parameterName, NodeAdapter root) {
    if (!root.exist(parameterName)) {
      return null;
    }

    var c = root
      .of(parameterName)
      .since(V2_10)
      .summary("Configure application warmup by running transit searches during startup.")
      .description(
        """
        When configured, OTP runs transit routing queries between the given locations
        during startup. This warms up the application (JIT compilation, GraphQL schema
        caches, routing data structures, etc.) before production traffic arrives.
        Queries start after the Raptor transit data is created and stop when all updaters
        are primed (the health endpoint would return "UP").
        If no updaters are configured, no warmup queries are run.
        """
      )
      .asObject();

    WarmupApi api = c
      .of("api")
      .since(V2_10)
      .summary("Which GraphQL API to use for warmup queries.")
      .description(docEnumValueList(WarmupApi.values()))
      .asEnum(WarmupApi.TRANSMODEL);

    WgsCoordinate from = mapCoordinate(c, "from", "Origin location for warmup searches.", "origin");
    WgsCoordinate to = mapCoordinate(
      c,
      "to",
      "Destination location for warmup searches.",
      "destination"
    );

    var accessModeStrings = c
      .of("accessModes")
      .since(V2_10)
      .summary("Access modes to cycle through in warmup queries.")
      .description(
        "Ordered list of `StreetMode` values used as access modes. " +
          "Each entry is paired with the egress mode at the same index. " +
          docEnumValueList(StreetMode.values())
      )
      .asStringList(DEFAULT_ACCESS_MODES.stream().map(Enum::name).toList());
    List<StreetMode> accessModes = accessModeStrings
      .stream()
      .map(s -> StreetMode.valueOf(s))
      .toList();

    var egressModeStrings = c
      .of("egressModes")
      .since(V2_10)
      .summary("Egress modes to cycle through in warmup queries.")
      .description(
        "Ordered list of `StreetMode` values used as egress modes. " +
          "Each entry is paired with the access mode at the same index. " +
          docEnumValueList(StreetMode.values())
      )
      .asStringList(DEFAULT_EGRESS_MODES.stream().map(Enum::name).toList());
    List<StreetMode> egressModes = egressModeStrings
      .stream()
      .map(s -> StreetMode.valueOf(s))
      .toList();

    if (accessModes.size() != egressModes.size()) {
      throw new IllegalArgumentException(
        "warmup.accessModes and warmup.egressModes must have the same number of entries, " +
          "got %d access modes and %d egress modes.".formatted(
            accessModes.size(),
            egressModes.size()
          )
      );
    }

    return new WarmupParameters(api, from, to, accessModes, egressModes);
  }

  private static WgsCoordinate mapCoordinate(
    NodeAdapter parent,
    String name,
    String summary,
    String noun
  ) {
    var node = parent.of(name).since(V2_10).summary(summary).asObject();
    double lat = node.of("lat").since(V2_10).summary("Latitude of the " + noun + ".").asDouble();
    double lon = node.of("lon").since(V2_10).summary("Longitude of the " + noun + ".").asDouble();
    return new WgsCoordinate(lat, lon);
  }
}
