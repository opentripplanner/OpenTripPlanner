package org.opentripplanner.street.elevator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;

/**
 * Finds the {@link ElevatorHopEdge}s of an elevator by its id, see {@link ElevatorHopEdge#id()},
 * and sets them in or out of service. An elevator is usually several hop edges: one for each pair
 * of levels and direction. Elevators without an id are not indexed, so they are always in service.
 * <p>
 * The index is built once from the graph, and assumes that no elevator edges are added or removed
 * afterward. The status is changed in place on the edges, see
 * {@link ElevatorHopEdge#setOutOfService(boolean)}, so it may only be changed from the street
 * writer thread.
 */
public final class ElevatorEdgeIndex {

  private final Map<String, List<ElevatorHopEdge>> edgesById;

  private ElevatorEdgeIndex(Map<String, List<ElevatorHopEdge>> edgesById) {
    this.edgesById = edgesById;
  }

  /** Index the elevators with an id in the given graph. */
  public static ElevatorEdgeIndex of(Graph graph) {
    return of(graph.findEdges(ElevatorHopEdge.class));
  }

  /** Index the elevators with an id among the given edges. */
  public static ElevatorEdgeIndex of(Iterable<ElevatorHopEdge> edges) {
    var edgesById = new HashMap<String, Set<ElevatorHopEdge>>();
    for (var edge : edges) {
      edge
        .id()
        .ifPresent(id -> edgesById.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(edge));
    }
    var result = new HashMap<String, List<ElevatorHopEdge>>();
    edgesById.forEach((id, set) -> result.put(id, List.copyOf(set)));
    return new ElevatorEdgeIndex(Map.copyOf(result));
  }

  /** The ids of all the elevators in the index. */
  public Set<String> elevatorIds() {
    return edgesById.keySet();
  }

  /** Whether an elevator with the given id is in the index. */
  public boolean contains(String elevatorId) {
    return edgesById.containsKey(elevatorId);
  }

  /**
   * Set the elevators with the given ids out of service, and all the other elevators in service.
   * Calling it again with the same ids changes nothing.
   *
   * @return the given ids that are not in the index, these are ignored
   */
  public Set<String> setOutOfService(Collection<String> outOfServiceIds) {
    var outOfService = Set.copyOf(outOfServiceIds);
    edgesById.forEach((id, edges) -> {
      boolean value = outOfService.contains(id);
      edges.forEach(edge -> edge.setOutOfService(value));
    });

    var unknown = new HashSet<String>();
    for (var id : outOfService) {
      if (!edgesById.containsKey(id)) {
        unknown.add(id);
      }
    }
    return Set.copyOf(unknown);
  }
}
