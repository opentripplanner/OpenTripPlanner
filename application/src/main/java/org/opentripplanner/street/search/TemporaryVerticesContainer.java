package org.opentripplanner.street.search;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.SameEdgeAdjuster;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for linking the RouteRequest origin and destination to the Graph used
 * in the A-Star search, as well as removing them after the search has been done. It implements
 * AutoCloseable, in order to be able to use the try-with-resources statement, making the clean-up
 * automatic.
 */
public class TemporaryVerticesContainer implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(TemporaryVerticesContainer.class);

  private final Graph graph;
  private final Set<DisposableEdgeCollection> tempEdges;
  private final Set<Vertex> fromVertices;
  private final Set<Vertex> toVertices;
  private final GenericLocation from;
  private final GenericLocation to;
  private final VertexLinker vertexLinker;

  public TemporaryVerticesContainer(
    Graph graph,
    GenericLocation from,
    GenericLocation to,
    StreetMode accessMode,
    StreetMode egressMode
  ) {
    this.tempEdges = new HashSet<>();

    this.graph = graph;
    this.vertexLinker = graph.getLinker();
    this.from = from;
    this.to = to;
    fromVertices = getStreetVerticesForLocation(from, accessMode, false, tempEdges);
    toVertices = getStreetVerticesForLocation(to, egressMode, true, tempEdges);

    checkIfVerticesFound();

    if (fromVertices != null && toVertices != null) {
      for (Vertex fromVertex : fromVertices) {
        for (Vertex toVertex : toVertices) {
          tempEdges.add(SameEdgeAdjuster.adjust(fromVertex, toVertex, graph));
        }
      }
    }
  }

  /* INSTANCE METHODS */

  /**
   * Tear down this container, removing any temporary edges from the "permanent" graph objects. This
   * enables all temporary objects for garbage collection.
   */
  public void close() {
    this.tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
  }

  public Set<Vertex> getFromVertices() {
    return fromVertices;
  }

  public Set<Vertex> getToVertices() {
    return toVertices;
  }

  /**
   * Get the stop vertices that correspond to the from location. If the from location only contains
   * coordinates, this will return an empty set. If the from location is a station id this will
   * return the child stops of that station.
   */
  public Set<TransitStopVertex> getFromStopVertices() {
    if (from.stopId == null) {
      return Set.of();
    }
    return graph.findStopOrChildStopsVertices(from.stopId);
  }

  /**
   * Get the stop vertices that corresponds to the to location. If the to location only contains
   * coordinates, this will return an empty set. If the to location is a station id this will
   * return the child stops of that station.
   */
  public Set<TransitStopVertex> getToStopVertices() {
    if (to.stopId == null) {
      return Set.of();
    }
    return graph.findStopOrChildStopsVertices(to.stopId);
  }

  /* PRIVATE METHODS */

  /**
   * Gets a set of vertices corresponding to the location provided. It first tries to match one of
   * the stop or station types by id, and if not successful, it uses the coordinates if provided.
   *
   * @param endVertex: whether this is a start vertex (if it's false) or end vertex (if it's true)
   */
  @Nullable
  private Set<Vertex> getStreetVerticesForLocation(
    GenericLocation location,
    StreetMode streetMode,
    boolean endVertex,
    Set<DisposableEdgeCollection> tempEdges
  ) {
    if (!location.isSpecified()) {
      return null;
    }

    // Differentiate between driving and non-driving, as driving is not available from transit stops
    TraverseMode mode = getTraverseModeForLinker(streetMode, endVertex);

    if (mode.isInCar()) {
      // Fetch coordinate from stop, if not given in request
      if (location.stopId != null && location.getCoordinate() == null) {
        var stopVertex = graph.getStopVertexForStopId(location.stopId);
        if (stopVertex != null) {
          var c = stopVertex.getStop().getCoordinate();
          location = new GenericLocation(
            location.label,
            location.stopId,
            c.latitude(),
            c.longitude()
          );
        }
      }
    } else {
      // Check if Stop/StopCollection is found by FeedScopeId
      if (location.stopId != null) {
        var streetVertices = graph.findStopVertices(location.stopId);
        if (!streetVertices.isEmpty()) {
          return streetVertices;
        }
      }
    }

    // Check if coordinate is provided and connect it to graph
    if (location.getCoordinate() != null) {
      return Set.of(
        createVertexFromCoordinate(
          location.getCoordinate(),
          location.label,
          streetMode,
          endVertex,
          tempEdges
        )
      );
    }

    return null;
  }

  private TraverseMode getTraverseModeForLinker(StreetMode streetMode, boolean endVertex) {
    TraverseMode nonTransitMode = TraverseMode.WALK;
    // for park and ride we will start in car mode and walk to the end vertex
    boolean parkAndRideDepart = streetMode == StreetMode.CAR_TO_PARK && !endVertex;
    boolean onlyCarAvailable = streetMode == StreetMode.CAR;
    if (onlyCarAvailable || parkAndRideDepart) {
      nonTransitMode = TraverseMode.CAR;
    }
    return nonTransitMode;
  }

  private Vertex createVertexFromCoordinate(
    Coordinate coordinate,
    @Nullable String label,
    StreetMode streetMode,
    boolean endVertex,
    Set<DisposableEdgeCollection> tempEdges
  ) {
    if (endVertex) {
      LOG.debug("Creating end vertex for {}", coordinate);
    } else {
      LOG.debug("Creating start vertex for {}", coordinate);
    }

    I18NString name;
    if (label == null || label.isEmpty()) {
      if (endVertex) {
        name = new LocalizedString("destination");
      } else {
        name = new LocalizedString("origin");
      }
    } else {
      name = new NonLocalizedString(label);
    }

    var temporaryStreetLocation = new TemporaryStreetLocation(coordinate, name);

    TraverseMode nonTransitMode = getTraverseModeForLinker(streetMode, endVertex);

    tempEdges.add(
      vertexLinker.linkVertexForRequest(
        temporaryStreetLocation,
        new TraverseModeSet(nonTransitMode),
        endVertex ? LinkingDirection.OUTGOING : LinkingDirection.INCOMING,
        endVertex
          ? (vertex, streetVertex) ->
            List.of(
              TemporaryFreeEdge.createTemporaryFreeEdge(
                streetVertex,
                (TemporaryStreetLocation) vertex
              )
            )
          : (vertex, streetVertex) ->
            List.of(
              TemporaryFreeEdge.createTemporaryFreeEdge(
                (TemporaryStreetLocation) vertex,
                streetVertex
              )
            )
      )
    );

    if (
      temporaryStreetLocation.getIncoming().isEmpty() &&
      temporaryStreetLocation.getOutgoing().isEmpty()
    ) {
      LOG.warn("Couldn't link {}", coordinate);
    }

    temporaryStreetLocation.setWheelchairAccessible(true);

    return temporaryStreetLocation;
  }

  private void checkIfVerticesFound() {
    List<RoutingError> routingErrors = new ArrayList<>();

    // check that vertices where found if from-location was specified
    if (from.isSpecified() && isDisconnected(fromVertices, true)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(from), InputField.FROM_PLACE)
      );
    }

    // check that vertices where found if to-location was specified
    if (to.isSpecified() && isDisconnected(toVertices, false)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(to), InputField.TO_PLACE)
      );
    }

    // if from and to share any vertices, the user is already at their destination, and the result
    // is a trivial path
    if (
      fromVertices != null &&
      toVertices != null &&
      !Sets.intersection(fromVertices, toVertices).isEmpty()
    ) {
      routingErrors.add(new RoutingError(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, null));
    }

    if (!routingErrors.isEmpty()) {
      throw new RoutingValidationException(routingErrors);
    }
  }

  private static boolean isDisconnected(Set<Vertex> vertices, boolean isFrom) {
    // Not connected if linking was not attempted, and vertices were not specified in the request.
    if (vertices == null) {
      return true;
    }

    Predicate<Vertex> isNotTransit = Predicate.not(TransitStopVertex.class::isInstance);
    Predicate<Vertex> hasNoIncoming = v -> v.getIncoming().isEmpty();
    Predicate<Vertex> hasNoOutgoing = v -> v.getOutgoing().isEmpty();

    // Not connected if linking did not create incoming/outgoing edges depending on the
    // direction and the end.
    Predicate<Vertex> isNotConnected = isFrom ? hasNoOutgoing : hasNoIncoming;

    return vertices.stream().allMatch(isNotTransit.and(isNotConnected));
  }

  private RoutingErrorCode getRoutingErrorCodeForDisconnected(GenericLocation location) {
    Coordinate coordinate = location.getCoordinate();
    GeometryFactory gf = GeometryUtils.getGeometryFactory();
    return coordinate != null && graph.getConvexHull().disjoint(gf.createPoint(coordinate))
      ? RoutingErrorCode.OUTSIDE_BOUNDS
      : RoutingErrorCode.LOCATION_NOT_FOUND;
  }
}
