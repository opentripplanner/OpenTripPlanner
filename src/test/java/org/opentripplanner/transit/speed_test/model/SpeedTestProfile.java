package org.opentripplanner.transit.speed_test.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorProfile;
import org.opentripplanner.raptor.spi.SearchDirection;

public enum SpeedTestProfile {
  standard(
    "rr",
    "Standard Range Raptor, super fast [ transfers, arrival time, travel time ].",
    RaptorProfile.STANDARD,
    SearchDirection.FORWARD
  ),
  std_reverse(
    "rrr",
    "Reverse Standard Range Raptor",
    RaptorProfile.STANDARD,
    SearchDirection.REVERSE
  ),
  best_time(
    "bt",
    "Best Time Range Raptor, super fast. Arrival time only, no path.",
    RaptorProfile.BEST_TIME,
    SearchDirection.FORWARD
  ),
  std_best_time_reverse(
    "btr",
    "Reverse Best Time Range Raptor",
    RaptorProfile.BEST_TIME,
    SearchDirection.REVERSE
  ),
  min_travel_duration_std(
    "td",
    "Standard Range Raptor without waiting time.",
    RaptorProfile.MIN_TRAVEL_DURATION,
    SearchDirection.FORWARD
  ),
  min_travel_duration_std_reverse(
    "tdr",
    "Reverse Standard Range Raptor without waiting time.",
    RaptorProfile.MIN_TRAVEL_DURATION,
    SearchDirection.REVERSE
  ),
  min_travel_duration_best_time(
    "tb",
    "Best Time Range Raptor without waiting time.",
    RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME,
    SearchDirection.FORWARD
  ),
  min_travel_duration_best_time_reverse(
    "tbr",
    "Reverse Best Time Range Raptor without waiting time.",
    RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME,
    SearchDirection.REVERSE
  ),
  mc_range_raptor(
    "mc",
    "Multi-Criteria Range Raptor [ transfers, arrival time, travel time, cost ].",
    RaptorProfile.MULTI_CRITERIA,
    SearchDirection.FORWARD
  ),
  mc_destination(
    "md",
    "Multi-Criteria Range Raptor with destination pruning.",
    RaptorProfile.MULTI_CRITERIA,
    SearchDirection.FORWARD,
    Optimization.PARETO_CHECK_AGAINST_DESTINATION
  );

  final String shortName;
  final String description;
  final RaptorProfile raptorProfile;
  final SearchDirection direction;
  final List<Optimization> optimizations;

  SpeedTestProfile(
    String shortName,
    String description,
    RaptorProfile profile,
    SearchDirection direction,
    Optimization... optimizations
  ) {
    this.shortName = shortName;
    this.description = description;
    this.raptorProfile = profile;
    this.direction = direction;
    this.optimizations = Arrays.asList(optimizations);
  }

  public static SpeedTestProfile[] parse(String profiles) {
    return Arrays
      .stream(profiles.split(","))
      .map(SpeedTestProfile::parseOne)
      .toArray(SpeedTestProfile[]::new);
  }

  public static List<String> options() {
    return Arrays.stream(values()).map(SpeedTestProfile::description).collect(Collectors.toList());
  }

  public RaptorProfile raptorProfile() {
    return raptorProfile;
  }

  public SearchDirection direction() {
    return direction;
  }

  public List<Optimization> optimizations() {
    return optimizations;
  }

  /* private methods */

  private static SpeedTestProfile parseOne(String value) {
    try {
      return valueOf(value);
    } catch (IllegalArgumentException ignore) {
      for (SpeedTestProfile it : values()) {
        if (it.shortName.equalsIgnoreCase(value)) {
          return it;
        }
      }
      throw new IllegalArgumentException(
        "Profile is not valid: '" +
        value +
        "'\nProfiles:\n\t" +
        Arrays
          .stream(values())
          .map(SpeedTestProfile::description)
          .collect(Collectors.joining("\n\t"))
          .replace('.', ' ')
      );
    }
  }

  private String description() {
    StringBuilder text;

    if (name().equals(shortName)) {
      text = new StringBuilder(String.format("%s : %s", name(), description));
    } else {
      text = new StringBuilder(String.format("%s, %s : %s", shortName, name(), description));
    }

    if (raptorProfile != null) {
      text.append(String.format("\n·%22s%s", "", raptorProfile));
      text.append(", ").append(direction.name());
      for (Optimization it : optimizations) {
        text.append(", ").append(it.name());
      }
    }
    return text.toString();
  }
}
