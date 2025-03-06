package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Street graph pruning settings
 */
public class IslandPruningConfig {

  public int pruningThresholdIslandWithoutStops;
  public int pruningThresholdIslandWithStops;
  public int adaptivePruningDistance;
  public double adaptivePruningFactor;

  public static IslandPruningConfig fromConfig(NodeAdapter root) {
    return fromSubConfig(
      root
        .of("islandPruning")
        .since(V2_3)
        .summary("Settings for fixing street graph connectivity errors")
        .asObject()
    );
  }

  /** Create a IslandPruningConfig from a JSON configuration node. */
  public static IslandPruningConfig fromSubConfig(NodeAdapter config) {
    IslandPruningConfig islandPruning = new IslandPruningConfig();

    islandPruning.pruningThresholdIslandWithStops = config
      .of("islandWithStopsMaxSize")
      .since(V2_3)
      .summary("When a graph island with stops in it should be pruned.")
      .description(
        """
        This field indicates the pruning threshold for islands with stops. Any such island under this
        edge count will be pruned.
        """
      )
      .asInt(2);

    islandPruning.pruningThresholdIslandWithoutStops = config
      .of("islandWithoutStopsMaxSize")
      .since(V2_3)
      .summary("When a graph island without stops should be pruned.")
      .description(
        """
        This field indicates the pruning threshold for islands without stops. Any such island under
        this edge count will be pruned.
        """
      )
      .asInt(10);

    islandPruning.adaptivePruningDistance = config
      .of("adaptivePruningDistance")
      .since(V2_3)
      .summary("Search distance for analyzing islands in pruning.")
      .description(
        """
        The distance after which disconnected sub graph is considered as real island in pruning heuristics.
        """
      )
      .asInt(250);

    islandPruning.adaptivePruningFactor = config
      .of("adaptivePruningFactor")
      .since(V2_3)
      .summary("Defines how much pruning thresholds grow maximally by distance.")
      .description(
        """
        Expands the pruning thresholds as the distance of an island from the rest of the graph gets smaller.
        Even fairly large disconnected sub graphs should be removed if they are badly entangled with other graph.
        """
      )
      .asDouble(50);

    return islandPruning;
  }
}
