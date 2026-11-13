package org.opentripplanner.streetadapter.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.street.elevator.ElevatorEdgeIndex;
import org.opentripplanner.street.graph.Graph;

/**
 * Provides the one {@link ElevatorEdgeIndex} of the street graph. The status of the elevators in
 * the street graph is only changed through it.
 */
@Module
public class ElevatorEdgeIndexModule {

  /** The graph must not get or lose elevator edges after the index is created. */
  @Provides
  @Singleton
  public static ElevatorEdgeIndex elevatorEdgeIndex(Graph graph) {
    return ElevatorEdgeIndex.of(graph);
  }
}
