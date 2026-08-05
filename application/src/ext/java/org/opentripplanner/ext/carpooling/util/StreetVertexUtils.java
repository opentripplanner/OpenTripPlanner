package org.opentripplanner.ext.carpooling.util;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.linking.internal.VertexCreationService.LocationType;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Creates temporary graph vertices — one flavor for a passenger's pickup/dropoff, one for a
 * driver's route points — always linked {@code BIDIRECTIONAL} (as a
 * {@link LocationType#VISIT_VIA_LOCATION}), so each can serve as both an arrival and a departure
 * point.
 * <p>
 * Vertices and edges created here are registered with the given {@link TemporaryVerticesContainer};
 * closing it removes them from the graph.
 */
public class StreetVertexUtils {

  private final VertexCreationService vertexCreationService;
  private final TemporaryVerticesContainer temporaryVerticesContainer;

  public StreetVertexUtils(
    VertexCreationService vertexCreationService,
    TemporaryVerticesContainer temporaryVerticesContainer
  ) {
    this.vertexCreationService = vertexCreationService;
    this.temporaryVerticesContainer = temporaryVerticesContainer;
  }

  /**
   * Creates a bidirectionally-linked passenger vertex in {@code WALK} mode, so its splitter inherits
   * the parent edge's permissions.
   */
  @Nullable
  public Vertex createPassengerVertex(WgsCoordinate coord) {
    return linkCoordinate(coord, TraverseMode.WALK, "Carpooling Passenger Waypoint");
  }

  /** Creates a {@link TraverseMode#CAR}-linked vertex for a driver trip waypoint. */
  @Nullable
  public Vertex createDriverWaypointVertex(WgsCoordinate coord) {
    return linkCoordinate(coord, TraverseMode.CAR, "Carpooling Driver Waypoint");
  }

  @Nullable
  private Vertex linkCoordinate(WgsCoordinate coord, TraverseMode mode, String label) {
    var vertex = vertexCreationService.createVertexFromCoordinate(
      temporaryVerticesContainer,
      coord.asJtsCoordinate(),
      label,
      List.of(mode),
      LocationType.VISIT_VIA_LOCATION
    );
    // The link failure is already logged; lift it to a null return here.
    if (vertex.getIncoming().isEmpty() && vertex.getOutgoing().isEmpty()) {
      return null;
    }
    return vertex;
  }
}
