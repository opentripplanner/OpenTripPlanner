package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.module.transfer.api.MaxDurationRule;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfileConfig;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.CarPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

/**
 * Parses the new {@code transfers:} build-config block (see
 * {@code opentripplanner/OpenTripPlanner#7998}) into a {@link TransferProfilesConfig}. Entirely
 * separate from {@link RegularTransferConfig}/{@code transferRequests}, which
 * {@code DirectTransferGenerator} keeps reading unchanged for FLEX.
 * <p>
 * Only the {@code preferences} sub-fields that are actually consulted by a street search are
 * exposed - a profile's preferences only ever back a street search for transfer generation, never
 * transit routing, so e.g. {@code boardCost} (a transit-boarding preference) has no place here.
 */
public class TransferProfilesConfigMapper {

  /** Config key -> profile id. Fixed set for now (see {@code TransferProfilesConfig}'s doc). */
  private static final Map<String, RaptorTransferProfile> PROFILE_NAMES = new LinkedHashMap<>();

  static {
    PROFILE_NAMES.put("walk", RaptorTransferProfile.WALK);
    PROFILE_NAMES.put("wheelchair", RaptorTransferProfile.WHEELCHAIR);
    PROFILE_NAMES.put("bicycle", RaptorTransferProfile.BICYCLE);
    PROFILE_NAMES.put("car", RaptorTransferProfile.CAR);
    PROFILE_NAMES.put("scooter", RaptorTransferProfile.SCOOTER);
  }

  public static TransferProfilesConfig map(NodeAdapter root) {
    var c = root
      .of("transfers")
      .since(V2_10)
      .summary("Transfer profiles for the raptor-data regular-transfer pipeline.")
      .description(
        """
        Replaces `transferRequests` for regular (non-FLEX) transfer generation. One entry per
        named profile (`walk`, `wheelchair`, `bicycle`, `car`, `scooter`), each declaring its own
        preferences, duration limits, and optionally a `base` profile to deduplicate its
        discovered paths against. If this block is omitted entirely, a single default `walk`
        profile is used, mirroring the implicit default of the old `transferRequests` config.
        """
      )
      .asObject();

    Duration defaultMaxDuration = c
      .of("defaultMaxDuration")
      .since(V2_10)
      .summary(
        "The default max duration for all profiles, unless overridden by a profile's own `maxDurations`."
      )
      .asDuration(Duration.ofMinutes(30));

    CostLinearFunction deduplicateDelta = c
      .of("deduplicateDelta")
      .since(V2_10)
      .summary(
        "Cost tolerance used when deduplicating a profile's discovered path against its `base`."
      )
      .asCostLinearFunction(CostLinearFunction.of(Duration.ZERO, 0.0));

    List<TransferProfileConfig> profiles = new ArrayList<>();
    for (var entry : PROFILE_NAMES.entrySet()) {
      String name = entry.getKey();
      if (!c.exist(name)) {
        continue;
      }
      RaptorTransferProfile profileId = entry.getValue();
      var p = c
        .of(name)
        .since(V2_10)
        .summary("The " + name + " transfer profile.")
        .asObject();
      profiles.add(mapProfile(profileId, p, defaultMaxDuration));
    }

    if (profiles.isEmpty()) {
      // No `transfers` block (or an empty one) configured - fall back to a single default WALK
      // profile, mirroring the implicit default of the old `transferRequests` config.
      var p = c.of("walk").since(V2_10).summary("The walk transfer profile.").asObject();
      profiles.add(mapProfile(RaptorTransferProfile.WALK, p, defaultMaxDuration));
    }

    return new TransferProfilesConfig(defaultMaxDuration, deduplicateDelta, profiles);
  }

  private static TransferProfileConfig mapProfile(
    RaptorTransferProfile profileId,
    NodeAdapter p,
    Duration defaultMaxDuration
  ) {
    String baseName = p
      .of("base")
      .since(V2_10)
      .summary("Profile to deduplicate paths against.")
      .asString(null);
    RaptorTransferProfile base = mapBase(baseName);

    List<MaxDurationRule> maxDurations = p
      .of("maxDurations")
      .since(V2_10)
      .summary("Duration limits for this profile, optionally restricted to specific target stops.")
      .asObjects(List.of(), md -> mapMaxDurationRule(md, defaultMaxDuration));
    if (maxDurations.isEmpty()) {
      maxDurations = List.of(new MaxDurationRule(defaultMaxDuration, null));
    }

    var prefs = p
      .of("preferences")
      .since(V2_10)
      .summary("Street-search preferences used to discover and cost this profile's paths.")
      .asObject();
    RouteRequest preferences = mapPreferences(profileId, prefs);

    return new TransferProfileConfig(profileId, base, preferences, maxDurations);
  }

  @Nullable
  private static RaptorTransferProfile mapBase(@Nullable String baseName) {
    if (baseName == null) {
      return null;
    }
    RaptorTransferProfile base = PROFILE_NAMES.get(baseName);
    if (base == null) {
      throw new IllegalArgumentException(
        "Unknown transfer profile referenced by 'base': " + baseName
      );
    }
    return base;
  }

  private static MaxDurationRule mapMaxDurationRule(NodeAdapter md, Duration defaultMaxDuration) {
    Duration maxDuration = md
      .of("maxDuration")
      .since(V2_10)
      .summary("The duration limit for this entry.")
      .asDuration(defaultMaxDuration);
    Set<StreetMode> allowedModes = md.exist("allowedModes")
      ? md
          .of("allowedModes")
          .since(V2_10)
          .summary(
            "Restrict this duration to stops reachable by at least one of these modes' trips."
          )
          .asEnumSet(StreetMode.class)
      : null;
    return new MaxDurationRule(maxDuration, allowedModes);
  }

  private static RouteRequest mapPreferences(RaptorTransferProfile profileId, NodeAdapter prefs) {
    StreetMode streetMode = toStreetMode(profileId);
    var builder = RouteRequest.defaultValue().copyOf();
    builder.withJourney(jb ->
      jb
        .withTransfer(new StreetRequest(streetMode))
        .withWheelchair(profileId == RaptorTransferProfile.WHEELCHAIR)
    );
    builder.withPreferences(routingPrefs -> {
      switch (profileId) {
        case WALK -> routingPrefs.withWalk(w -> mapWalkFields(prefs, w));
        case WHEELCHAIR -> {
          routingPrefs.withWalk(w -> mapWalkFields(prefs, w));
          routingPrefs.withWheelchair(wc -> mapWheelchairFields(prefs, wc));
        }
        case BICYCLE, SCOOTER -> routingPrefs.withBike(b -> mapBikeFields(prefs, b));
        case CAR -> routingPrefs.withCar(cp -> mapCarFields(prefs, cp));
      }
    });
    return builder.buildDefault();
  }

  /** {@code CAR} has no street-search speed preference - real-world speed comes from OSM tags. */
  private static StreetMode toStreetMode(RaptorTransferProfile profileId) {
    return switch (profileId) {
      case WALK, WHEELCHAIR -> StreetMode.WALK;
      case BICYCLE, SCOOTER -> StreetMode.BIKE;
      case CAR -> StreetMode.CAR;
    };
  }

  private static void mapWalkFields(NodeAdapter p, WalkPreferences.Builder w) {
    var dft = w.original();
    w.withSpeed(
      p
        .of("speed")
        .since(V2_10)
        .summary("Walking speed, in meters per second.")
        .asDouble(dft.speed())
    );
    w.withReluctance(
      p
        .of("reluctance")
        .since(V2_10)
        .summary("A multiplier for the cost of walking.")
        .asDouble(dft.reluctance())
    );
    w.withStairsReluctance(
      p
        .of("stairsReluctance")
        .since(V2_10)
        .summary("How much stairs should be avoided.")
        .asDouble(dft.stairsReluctance())
    );
  }

  private static void mapWheelchairFields(NodeAdapter p, WheelchairPreferences.Builder wc) {
    var dft = wc.original();
    wc.withMaxSlope(
      p
        .of("maxSlope")
        .since(V2_10)
        .summary("The maximum slope as a fraction of 1.")
        .asDouble(dft.maxSlope())
    );
    wc.withSlopeExceededReluctance(
      p
        .of("slopeExceededReluctance")
        .since(V2_10)
        .summary("How much streets with a slope over the maximum should be avoided.")
        .asDouble(dft.slopeExceededReluctance())
    );
    wc.withInaccessibleStreetReluctance(
      p
        .of("inaccessibleStreetReluctance")
        .since(V2_10)
        .summary("A multiplier for the cost of a street edge that is not wheelchair-accessible.")
        .asDouble(dft.inaccessibleStreetReluctance())
    );
    wc.withStairsReluctance(
      p
        .of("stairsReluctance")
        .since(V2_10)
        .summary("How much stairs should be avoided.")
        .asDouble(dft.stairsReluctance())
    );
    if (p.exist("elevator")) {
      var elevator = p
        .of("elevator")
        .since(V2_10)
        .summary("Elevator accessibility cost.")
        .asObject();
      var originalElevator = dft.elevator();
      int unknownCost = elevator
        .of("unknownCost")
        .since(V2_10)
        .summary("The cost to add when traversing an elevator with unknown accessibility.")
        .asInt(originalElevator.unknownCost());
      int inaccessibleCost = elevator
        .of("inaccessibleCost")
        .since(V2_10)
        .summary("The cost to add when traversing an elevator known to be inaccessible.")
        .asInt(originalElevator.inaccessibleCost());
      wc.withElevatorCost(unknownCost, inaccessibleCost);
    }
  }

  private static void mapBikeFields(NodeAdapter p, BikePreferences.Builder b) {
    var dft = b.original();
    b.withSpeed(
      p
        .of("speed")
        .since(V2_10)
        .summary("Cycling speed, in meters per second.")
        .asDouble(dft.speed())
    );
    b.withReluctance(
      p
        .of("reluctance")
        .since(V2_10)
        .summary("A multiplier for the cost of cycling.")
        .asDouble(dft.reluctance())
    );
  }

  private static void mapCarFields(NodeAdapter p, CarPreferences.Builder cp) {
    var dft = cp.original();
    cp.withReluctance(
      p
        .of("reluctance")
        .since(V2_10)
        .summary("A multiplier for the cost of driving.")
        .asDouble(dft.reluctance())
    );
  }
}
