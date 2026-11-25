package org.opentripplanner.modularization.harness;

/**
 * One difference between the baseline and the candidate.
 *
 * @param scenario the scenario (file name without extension)
 * @param path the path of the value inside the response, e.g. {@code data.stops[3].name}
 * @param baseline the baseline value as JSON text, or {@link Differ#ABSENT}
 * @param candidate the candidate value as JSON text, or {@link Differ#ABSENT}
 */
public record Difference(String scenario, String path, String baseline, String candidate) {
  @Override
  public String toString() {
    return "%s: %s%n    baseline : %s%n    candidate: %s".formatted(
      scenario,
      path,
      abbreviate(baseline),
      abbreviate(candidate)
    );
  }

  private static String abbreviate(String s) {
    return s.length() <= 160 ? s : s.substring(0, 157) + "...";
  }
}
