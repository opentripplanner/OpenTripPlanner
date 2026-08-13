package org.opentripplanner.graph_builder.module.islandpruning;

public class IslandPruningParametersBuilder {

  private int pruningThresholdIslandWithoutStops;
  private int pruningThresholdIslandWithStops;
  private double adaptivePruningFactor;
  private int adaptivePruningDistance;

  IslandPruningParametersBuilder() {
    this(IslandPruningParameters.DEFAULTS);
  }

  public IslandPruningParametersBuilder(IslandPruningParameters original) {
    this.pruningThresholdIslandWithoutStops = original.pruningThresholdIslandWithoutStops();
    this.pruningThresholdIslandWithStops = original.pruningThresholdIslandWithStops();
    this.adaptivePruningFactor = original.adaptivePruningFactor();
    this.adaptivePruningDistance = original.adaptivePruningDistance();
  }

  int pruningThresholdIslandWithoutStops() {
    return pruningThresholdIslandWithoutStops;
  }

  /**
   * Island without stops and with less than this number of street vertices will be pruned.
   */
  public IslandPruningParametersBuilder withPruningThresholdIslandWithoutStops(
    int pruningThresholdIslandWithoutStops
  ) {
    this.pruningThresholdIslandWithoutStops = pruningThresholdIslandWithoutStops;
    return this;
  }

  int pruningThresholdIslandWithStops() {
    return pruningThresholdIslandWithStops;
  }

  /**
   * Island with stops and with less than this number of street vertices will be pruned.
   */
  public IslandPruningParametersBuilder withPruningThresholdIslandWithStops(
    int pruningThresholdIslandWithStops
  ) {
    this.pruningThresholdIslandWithStops = pruningThresholdIslandWithStops;
    return this;
  }

  double adaptivePruningFactor() {
    return adaptivePruningFactor;
  }

  /**
   * Coefficient for how much larger islands (compared to the threshold values above) get pruned
   * if they are close enough to the rest of the graph.
   */
  public IslandPruningParametersBuilder withAdaptivePruningFactor(double adaptivePruningFactor) {
    this.adaptivePruningFactor = adaptivePruningFactor;
    return this;
  }

  int adaptivePruningDistance() {
    return adaptivePruningDistance;
  }

  /**
   * Search radius in meters when looking for island neighbours.
   */
  public IslandPruningParametersBuilder withAdaptivePruningDistance(int adaptivePruningDistance) {
    this.adaptivePruningDistance = adaptivePruningDistance;
    return this;
  }

  public IslandPruningParameters build() {
    return new IslandPruningParameters(this);
  }
}
