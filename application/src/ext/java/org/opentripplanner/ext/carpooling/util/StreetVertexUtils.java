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
 * Creates temporary, request-scoped graph vertices for carpooling — one flavor for the
 * passenger's pickup/dropoff and one for each point along the driver's trip. Both flavors
 * always link {@code BIDIRECTIONAL} (as a {@link LocationType#VISIT_VIA_LOCATION}), because
 * carpool routing passes <em>through</em> these points rather than starting or ending at them.
 *
 * <h2>Why not reuse {@code LinkingContext}?</h2>
 *
 * OTP's request pipeline already links the passenger's origin and destination through
 * {@code LinkingContext}, so it's tempting to reach into that and reuse those vertices. We
 * don't, for three reasons:
 *
 * <ol>
 *   <li><b>Directionality breaks driver routing.</b> {@code LinkingContext} links the request
 *       {@code from} as {@code INCOMING} and the {@code to} as {@code OUTGOING} (see
 *       {@link VertexCreationService#mapDirection}). That makes each vertex a one-way boundary:
 *       the origin has only outgoing edges, the destination has only incoming. This is the
 *       right shape for a normal A* that starts at the origin and ends at the destination.
 *       <br><br>
 *       Carpooling is different — the driver <em>passes through</em> both the pickup and the
 *       dropoff. The driver's CAR search must arrive at AND depart from the pickup (to continue
 *       toward the dropoff), and arrive at AND depart from the dropoff (to continue toward the
 *       driver's own trip end). When the pickup/dropoff happens to land on a splitter created
 *       by the request's own linking — the common "passenger is already on a drivable road"
 *       case — only the half-edges created by that splitter exist at the splitter, and a
 *       directional linking gives you only one direction of them. The driver's CAR A* then
 *       cannot pass through. {@code BIDIRECTIONAL} linking creates both half-edges per split
 *       and fixes this unconditionally.</li>
 *
 *   <li><b>Driver waypoints aren't in {@code LinkingContext} at all.</b> The driver's trip
 *       has a list of route points that come from the carpooling repository, not from the
 *       passenger's routing request. {@code LinkingContext} has never seen them, so they have
 *       to be linked on demand regardless. Consolidating passenger-vertex creation into the
 *       same facade is the obvious follow-through.</li>
 * </ol>
 *
 * <h2>Cleanup</h2>
 *
 * All vertices and their temporary edges are registered with the caller's
 * {@link TemporaryVerticesContainer} via {@link VertexCreationService}; when the container
 * is closed at the end of the request, everything added here is yanked from the graph.
 */
public class StreetVertexUtils {

  private final VertexCreationService vertexCreationService;
  private final TemporaryVerticesContainer temporaryVerticesContainer;

  /**
   * @param vertexCreationService creates and links the temporary graph vertices.
   * @param temporaryVerticesContainer collects every vertex and edge created here so the request's
   *        {@code try-with-resources} cleanup yanks them all from the graph at the end of the
   *        search.
   */
  public StreetVertexUtils(
    VertexCreationService vertexCreationService,
    TemporaryVerticesContainer temporaryVerticesContainer
  ) {
    this.vertexCreationService = vertexCreationService;
    this.temporaryVerticesContainer = temporaryVerticesContainer;
  }

  /**
   * Creates a bidirectionally-linked, walk-mode-linked passenger vertex. The passenger is a
   * pedestrian reaching the curb, so {@code WALK} is the correct linking mode: the splitter it
   * produces inherits the parent edge's permissions, so a walk-only edge yields a pedestrian
   * splitter (the snap's walk A* will expand past it) and a walk+car edge yields a car-capable
   * splitter the driver can pass through in either direction.
   */
  @Nullable
  public Vertex createPassengerVertex(WgsCoordinate coord) {
    return linkCoordinate(coord, TraverseMode.WALK, "Carpooling Passenger Waypoint");
  }

  /**
   * Creates a fresh, car-linked vertex for a driver trip waypoint. Driver trips do not pass
   * through OTP's request-linking pipeline, so their intermediate stops are always linked
   * on-demand — and they must be linked in {@link TraverseMode#CAR} because only the driver
   * traverses those vertices.
   */
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
    // VertexCreationService logs the link failure; here we just lift it to a null return.
    if (vertex.getIncoming().isEmpty() && vertex.getOutgoing().isEmpty()) {
      return null;
    }
    return vertex;
  }
}
