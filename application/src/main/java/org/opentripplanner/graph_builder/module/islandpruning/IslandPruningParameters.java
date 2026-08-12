package org.opentripplanner.graph_builder.module.islandpruning;

/**
 * Configures the thresholds used by {@link IslandPruningModule} to detect and prune small,
 * disconnected parts of the street graph.
 */
public record IslandPruningParameters(
  int pruningThresholdIslandWithoutStops,
  int pruningThresholdIslandWithStops,
  double adaptivePruningFactor,
  int adaptivePruningDistance
) {
  public static final IslandPruningParameters DEFAULTS = new IslandPruningParameters(
    10,
    2,
    50,
    250
  );

  public static IslandPruningParametersBuilder of() {
    return new IslandPruningParametersBuilder();
  }

  IslandPruningParameters(IslandPruningParametersBuilder builder) {
    this(
      builder.pruningThresholdIslandWithoutStops(),
      builder.pruningThresholdIslandWithStops(),
      builder.adaptivePruningFactor(),
      builder.adaptivePruningDistance()
    );
  }
}
