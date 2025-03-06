package org.opentripplanner.graph_builder.module.ned;

import java.util.HashMap;
import java.util.Map;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opentripplanner.astar.model.BinHeap;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ElevationFlattened;
import org.opentripplanner.graph_builder.issues.ElevationProfileFailure;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetElevationExtensionBuilder;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assigns elevation to edges which don't yet have elevation set, which may be for example tunnels,
 * bridges or islands.
 * <p>
 * Elevation may be missing from a {@link StreetEdge} for two reasons: 1. the source DEM files
 * contained no data for the whole geometry 2. {@link StreetEdge#isSlopeOverride()} is set
 * <p>
 * The elevation for missing edges is set through its vertices, with the elevation for the from/to
 * vertices being used to set the elevation profile.
 * <ol>
 * <li>
 *   The source elevations are determined for vertices using edges with an existing elevation
 *   profile, along with values from the {@code ele} tag
 * </li>
 * <li>
 *   All vertices within {@code maxElevationPropagationMeters} of vertices with elevation
 *   without elevation are visited
 * </li>
 * <li>
 *   Foreach vertex without elevation the first two paths from a vertex with elevation are used
 *   to interpolate elevations
 * </li>
 * <li>
 *   If a vertex only had a single path, then the last known elevation is used
 * </li>
 * </li>
 *   Once elevations for vertices are interpolated they are used to set the elevation profile
 *   for the incoming / outgoing StreetEdges
 * </li>
 * </ol>
 */
class MissingElevationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MissingElevationHandler.class);

  private final DataImportIssueStore issueStore;
  private final Map<Vertex, Double> existingElevationForVertices;
  private final double maxElevationPropagationMeters;

  public MissingElevationHandler(
    DataImportIssueStore issueStore,
    Map<Vertex, Double> existingElevationsForVertices,
    double maxElevationPropagationMeters
  ) {
    this.issueStore = issueStore;
    this.existingElevationForVertices = existingElevationsForVertices;
    this.maxElevationPropagationMeters = maxElevationPropagationMeters;
  }

  /**
   * Assign missing elevations by interpolating from nearby points with known elevation; also handle
   * osm ele tags
   */
  void run() {
    LOG.debug("Assigning missing elevations");

    // elevation for each vertex (known or interpolated)
    var pq = createPriorityQueue(existingElevationForVertices);
    var elevations = new HashMap<>(existingElevationForVertices);

    // Grow an SPT outward from vertices with known elevations into regions where the
    // elevation is not known. When branches meet, follow the back pointers through the region
    // of unknown elevation, setting elevations via interpolation.
    propagateElevationToNearbyVertices(pq, elevations);

    // Assign elevations to street edges based on the vertices
    elevations
      .keySet()
      .forEach(vertex -> {
        vertex
          .getIncomingStreetEdges()
          .forEach(edge -> assignElevationToEdgeIfPossible(elevations, edge));
        vertex
          .getOutgoingStreetEdges()
          .forEach(edge -> assignElevationToEdgeIfPossible(elevations, edge));
      });
  }

  private BinHeap<ElevationRepairState> createPriorityQueue(Map<Vertex, Double> elevations) {
    var pq = new BinHeap<ElevationRepairState>();

    elevations.forEach(
      ((vertex, elevation) -> {
          vertex
            .getIncoming()
            .forEach(edge -> {
              if (edge.getDistanceMeters() < maxElevationPropagationMeters) {
                pq.insert(
                  new ElevationRepairState(
                    vertex,
                    elevation,
                    edge.getFromVertex(),
                    edge.getDistanceMeters()
                  ),
                  edge.getDistanceMeters()
                );
              }
            });

          vertex
            .getOutgoing()
            .forEach(edge -> {
              if (edge.getDistanceMeters() < maxElevationPropagationMeters) {
                pq.insert(
                  new ElevationRepairState(
                    vertex,
                    elevation,
                    edge.getToVertex(),
                    edge.getDistanceMeters()
                  ),
                  edge.getDistanceMeters()
                );
              }
            });
        })
    );

    return pq;
  }

  private void propagateElevationToNearbyVertices(
    BinHeap<ElevationRepairState> pq,
    Map<Vertex, Double> elevations
  ) {
    // Stores vertices without elevation which were visited by a single path from a vertices with elevation
    var pending = new HashMap<Vertex, ElevationRepairState>();

    while (!pq.empty()) {
      ElevationRepairState currentState = pq.extract_min();
      if (elevations.containsKey(currentState.currentVertex)) {
        continue;
      }

      if (pending.containsKey(currentState.currentVertex)) {
        var otherState = pending.get(currentState.currentVertex);
        if (otherState.initialVertex != currentState.currentVertex) {
          interpolateElevationsAlongBackPath(otherState, currentState, elevations, pending);
          interpolateElevationsAlongBackPath(currentState, otherState, elevations, pending);
        } else {
          continue;
        }
      } else {
        pending.put(currentState.currentVertex, currentState);
      }

      for (Edge e : currentState.currentVertex.getIncoming()) {
        var nsVertex = e.getFromVertex();
        var nsDistance = currentState.distance + e.getDistanceMeters();
        if (elevations.containsKey(nsVertex) || nsDistance > maxElevationPropagationMeters) {
          continue;
        }

        pq.insert(new ElevationRepairState(currentState, nsVertex, nsDistance), nsDistance);
      }

      for (Edge e : currentState.currentVertex.getOutgoing()) {
        var nsVertex = e.getToVertex();
        var nsDistance = currentState.distance + e.getDistanceMeters();
        if (elevations.containsKey(nsVertex) || nsDistance > maxElevationPropagationMeters) {
          continue;
        }

        pq.insert(new ElevationRepairState(currentState, nsVertex, nsDistance), nsDistance);
      }
    }

    // Copy elevation to all pending vertices (where only one path with an initial elevation was found)
    pending.forEach((vertex, elevationRepairState) -> {
      if (!elevations.containsKey(vertex)) {
        elevations.put(vertex, lastKnowElevationForState(elevationRepairState, elevations));
      }
    });
  }

  private Double lastKnowElevationForState(
    ElevationRepairState elevationRepairState,
    Map<Vertex, Double> elevations
  ) {
    var backState = elevationRepairState.previousState;
    while (backState != null) {
      if (elevations.containsKey(backState.currentVertex)) {
        return elevations.get(backState.currentVertex);
      }
      backState = backState.previousState;
    }

    return elevationRepairState.initialElevation;
  }

  private void interpolateElevationsAlongBackPath(
    ElevationRepairState stateToBackTrack,
    ElevationRepairState alternateState,
    Map<Vertex, Double> elevations,
    Map<Vertex, ElevationRepairState> pending
  ) {
    var elevationDiff = alternateState.initialElevation - stateToBackTrack.initialElevation;
    var totalDistance = stateToBackTrack.distance + alternateState.distance;

    var currentState = stateToBackTrack;
    while (currentState != null) {
      if (!elevations.containsKey(currentState.currentVertex)) {
        var elevation =
          currentState.initialElevation + elevationDiff * (currentState.distance / totalDistance);
        elevation = DoubleUtils.roundTo1Decimal(elevation);

        elevations.put(currentState.currentVertex, elevation);
        pending.remove(currentState.currentVertex);
      }

      currentState = currentState.previousState;
    }
  }

  private void assignElevationToEdgeIfPossible(Map<Vertex, Double> elevations, StreetEdge edge) {
    if (edge.getElevationProfile() != null) {
      return;
    }

    Double fromElevation = elevations.get(edge.getFromVertex());
    Double toElevation = elevations.get(edge.getToVertex());

    if (fromElevation == null || toElevation == null) {
      if (!edge.isElevationFlattened() && !edge.isSlopeOverride()) {
        issueStore.add(new ElevationProfileFailure(edge, "Failed to propagate elevation data"));
      }
      return;
    }

    Coordinate[] coords = new Coordinate[] {
      new Coordinate(0, fromElevation),
      new Coordinate(edge.getDistanceMeters(), toElevation),
    };

    PackedCoordinateSequence profile = new PackedCoordinateSequence.Double(coords);

    try {
      StreetElevationExtensionBuilder.of(edge)
        .withElevationProfile(profile)
        .withComputed(true)
        .build()
        .ifPresent(edge::setElevationExtension);

      if (edge.isElevationFlattened()) {
        issueStore.add(new ElevationFlattened(edge));
      }
    } catch (Exception ex) {
      issueStore.add(new ElevationProfileFailure(edge, ex.getMessage()));
    }
  }

  private static class ElevationRepairState {

    final ElevationRepairState previousState;
    final Vertex initialVertex;
    final Double initialElevation;
    final Vertex currentVertex;
    final Double distance;

    ElevationRepairState(
      ElevationRepairState previousState,
      Vertex currentVertex,
      Double distance
    ) {
      this.previousState = previousState;
      this.initialVertex = previousState.initialVertex;
      this.initialElevation = previousState.initialElevation;
      this.currentVertex = currentVertex;
      this.distance = distance;
    }

    ElevationRepairState(
      Vertex initialVertex,
      Double initialElevation,
      Vertex currentVertex,
      Double distance
    ) {
      this.previousState = null;
      this.initialVertex = initialVertex;
      this.initialElevation = initialElevation;
      this.currentVertex = currentVertex;
      this.distance = distance;
    }

    @Override
    public String toString() {
      return (
        "ElevationRepairState{" +
        "initialVertex=" +
        initialVertex +
        ", initialElevation=" +
        initialElevation +
        ", currentVertex=" +
        currentVertex +
        ", distance=" +
        distance +
        '}'
      );
    }
  }
}
