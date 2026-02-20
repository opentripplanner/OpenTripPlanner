package org.opentripplanner.routing.linking;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.linking.internal.VertexCreationService.LocationType;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * This is a factory that is responsible for linking origin, destination and visit via locations
 * that contain coordinates to the Graph used in the A-Star search. This factory also validates that
 * it was possible to link the locations to the graph and throws {@link RoutingValidationException}
 * if it was not possible. The responsibility of cleaning up the temporary vertices and edges is on
 * the {@link TemporaryVerticesContainer}.
 */
public class LinkingContextFactory {

  private final Graph graph;
  private final VertexCreationService vertexCreationService;
  private final Function<FeedScopedId, Collection<FeedScopedId>> resolveSiteIds;
  /**
   * This can be either a station, a multi-modal station or a group of stations.
   */
  private final Function<FeedScopedId, Optional<WgsCoordinate>> findStopLocationsGroupCentroid;

  /**
   * Construct a factory when stop locations are potentially used for locations.
   */
  public LinkingContextFactory(
    Graph graph,
    VertexCreationService vertexCreationService,
    Function<FeedScopedId, Collection<FeedScopedId>> resolveSiteIds,
    Function<FeedScopedId, Optional<WgsCoordinate>> findStopLocationsGroupCentroid
  ) {
    this.graph = graph;
    this.vertexCreationService = vertexCreationService;
    this.resolveSiteIds = resolveSiteIds;
    this.findStopLocationsGroupCentroid = findStopLocationsGroupCentroid;
  }

  /**
   * Construct a factory when stop locations are not used for locations.
   */
  public LinkingContextFactory(Graph graph, VertexCreationService vertexCreationService) {
    this(graph, vertexCreationService, id -> Set.of(), id -> Optional.empty());
  }

  /**
   * Links locations to the Graph used in the A-Star search. This method also validates that it was
   * possible to link the locations to the graph and throws {@link RoutingValidationException} if it
   * was not possible. The responsibility of cleaning up the temporary vertices and edges is on the
   * {@link TemporaryVerticesContainer}.
   */
  public LinkingContext create(
    TemporaryVerticesContainer container,
    LinkingContextRequest request
  ) {
    var from = request.from();
    var fromVertices = getFromVertices(container, request);
    var fromStopVertices = getStopVertices(request.from());
    var to = request.to();
    var toVertices = getToVertices(container, request);
    var toStopVertices = getStopVertices(request.to());
    var visitViaLocationsWithCoordinates = request.viaLocationsWithCoordinates();
    var verticesForVisitViaLocationsWithCoordinates = getVerticesForViaLocationsWithCoordinates(
      container,
      request
    );
    checkIfVerticesFound(
      from,
      fromVertices,
      to,
      toVertices,
      visitViaLocationsWithCoordinates,
      verticesForVisitViaLocationsWithCoordinates
    );
    addAdjustedEdges(
      container,
      fromVertices,
      toVertices,
      visitViaLocationsWithCoordinates,
      verticesForVisitViaLocationsWithCoordinates
    );
    var verticesByLocation = getVerticesByLocation(
      from,
      fromVertices,
      to,
      toVertices,
      verticesForVisitViaLocationsWithCoordinates
    );
    return new LinkingContext(verticesByLocation, fromStopVertices, toStopVertices);
  }

  private Set<Vertex> getFromVertices(
    TemporaryVerticesContainer container,
    LinkingContextRequest request
  ) {
    var from = request.from();
    if (from == null || !from.isSpecified()) {
      return Set.of();
    }
    var modes = request.accessMode() != StreetMode.NOT_SET
      ? EnumSet.of(request.accessMode())
      : EnumSet.noneOf(StreetMode.class);
    if (request.directMode() != StreetMode.NOT_SET) {
      modes.add(request.directMode());
    }
    return getStreetVerticesForLocation(container, from, modes, LocationType.FROM);
  }

  private Set<Vertex> getToVertices(
    TemporaryVerticesContainer container,
    LinkingContextRequest request
  ) {
    var to = request.to();
    if (to == null || !to.isSpecified()) {
      return Set.of();
    }
    var modes = request.egressMode() != StreetMode.NOT_SET
      ? EnumSet.of(request.egressMode())
      : EnumSet.noneOf(StreetMode.class);
    if (request.directMode() != StreetMode.NOT_SET) {
      modes.add(request.directMode());
    }
    return getStreetVerticesForLocation(container, to, modes, LocationType.TO);
  }

  private Map<GenericLocation, Set<Vertex>> getVerticesForViaLocationsWithCoordinates(
    TemporaryVerticesContainer container,
    LinkingContextRequest request
  ) {
    var visitViaLocationsWithCoordinates = request.viaLocationsWithCoordinates();
    if (visitViaLocationsWithCoordinates.isEmpty()) {
      return Map.of();
    }
    var modes = EnumSet.noneOf(StreetMode.class);
    if (request.accessMode() != StreetMode.NOT_SET) {
      modes.add(request.accessMode());
    }
    if (request.egressMode() != StreetMode.NOT_SET) {
      modes.add(request.egressMode());
    }
    if (request.transferMode() != StreetMode.NOT_SET) {
      modes.add(request.transferMode());
    }
    if (request.directMode() != StreetMode.NOT_SET) {
      modes.add(request.directMode());
    }
    return visitViaLocationsWithCoordinates
      .stream()
      .collect(
        Collectors.toMap(
          location -> location,
          location ->
            getStreetVerticesForLocation(
              container,
              location,
              modes,
              LocationType.VISIT_VIA_LOCATION
            )
        )
      );
  }

  private Set<TransitStopVertex> getStopVertices(GenericLocation location) {
    if (location != null && location.stopId != null) {
      return findStopOrChildStopVertices(location.stopId);
    }
    return Set.of();
  }

  private Map<GenericLocation, Set<Vertex>> getVerticesByLocation(
    GenericLocation from,
    Set<Vertex> fromVertices,
    GenericLocation to,
    Set<Vertex> toVertices,
    Map<GenericLocation, Set<Vertex>> visitViaLocationVertices
  ) {
    var verticesByLocation = new HashMap<GenericLocation, Set<Vertex>>();
    verticesByLocation.put(from, fromVertices);
    verticesByLocation.put(to, toVertices);
    verticesByLocation.putAll(visitViaLocationVertices);
    return Collections.unmodifiableMap(verticesByLocation);
  }

  private void addAdjustedEdges(
    TemporaryVerticesContainer container,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    List<GenericLocation> visitViaLocationsWithCoordinates,
    Map<GenericLocation, Set<Vertex>> visitViaLocationVertices
  ) {
    addAdjustedEdgesBetween(container, fromVertices, toVertices);
    if (visitViaLocationsWithCoordinates.isEmpty()) {
      return;
    }
    addAdjustedEdgesBetween(
      container,
      fromVertices,
      visitViaLocationVertices.get(visitViaLocationsWithCoordinates.getFirst())
    );
    addAdjustedEdgesBetween(
      container,
      visitViaLocationVertices.get(visitViaLocationsWithCoordinates.getLast()),
      toVertices
    );
    var i = 1;
    while (i < visitViaLocationsWithCoordinates.size()) {
      Set<Vertex> fromViaVertices = visitViaLocationVertices.get(
        visitViaLocationsWithCoordinates.get(i - 1)
      );
      Set<Vertex> toViaVertices = visitViaLocationVertices.get(
        visitViaLocationsWithCoordinates.get(i)
      );
      addAdjustedEdgesBetween(container, fromViaVertices, toViaVertices);
      i++;
    }
  }

  private void addAdjustedEdgesBetween(
    TemporaryVerticesContainer container,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices
  ) {
    for (Vertex fromVertex : fromVertices) {
      for (Vertex toVertex : toVertices) {
        container.addEdgeCollection(SameEdgeAdjuster.adjust(fromVertex, toVertex, graph));
      }
    }
  }

  /**
   * Gets a set of vertices corresponding to the location provided. It first tries to match one of
   * the stop or station types by id, and if not successful, it uses the coordinates if provided.
   */
  private Set<Vertex> getStreetVerticesForLocation(
    TemporaryVerticesContainer container,
    GenericLocation location,
    EnumSet<StreetMode> streetModes,
    LocationType type
  ) {
    if (!location.isSpecified()) {
      return Set.of();
    }

    // Differentiate between driving and non-driving, as driving is not available from transit stops
    List<TraverseMode> modes = streetModes
      .stream()
      .map(streetMode -> vertexCreationService.getTraverseModeForLinker(streetMode, type))
      .distinct()
      .toList();

    var results = new HashSet<Vertex>();
    if (location.stopId != null) {
      if (!modes.stream().allMatch(TraverseMode::isInCar)) {
        results.addAll(getStreetVerticesForStop(location));
      }
      if (modes.stream().anyMatch(TraverseMode::isInCar)) {
        // Ensure that there is a car routable vertex (that can originate from stop's coordinate).
        var carRoutableVertex = getCarRoutableStreetVertex(container, location, type);
        carRoutableVertex.ifPresent(results::add);
      }
    }

    // If no vertices found from stop ID lookup and coordinates are available, use coordinates as fallback
    if (results.isEmpty() && location.getCoordinate() != null) {
      // Connect a temporary vertex from coordinate to graph
      results.add(
        vertexCreationService.createVertexFromCoordinate(
          container,
          location.getCoordinate(),
          location.label,
          modes,
          type
        )
      );
    }

    return results;
  }

  private Set<Vertex> getStreetVerticesForStop(GenericLocation location) {
    // check if there is a stop by the given id
    var stopVertex = graph.findStopVertex(location.stopId);
    if (stopVertex.isPresent()) {
      return Set.of(stopVertex.get());
    }

    // station centroids may be used instead of child stop vertices for stations
    var centroidVertex = graph.findStationCentroidVertex(location.stopId);
    if (centroidVertex.isPresent()) {
      return Set.of(centroidVertex.get());
    }

    // in the regular case you want to resolve a (multi-modal) station into its child stops
    var childVertices = findStopOrChildStopVertices(location.stopId);
    if (!childVertices.isEmpty()) {
      return childVertices.stream().map(Vertex.class::cast).collect(Collectors.toUnmodifiableSet());
    }
    return Set.of();
  }

  /**
   * We need to use coordinates of the stop for cars as an alternative as not all stops are routable
   * with cars.
   */
  private Optional<Vertex> getCarRoutableStreetVertex(
    TemporaryVerticesContainer container,
    GenericLocation location,
    LocationType type
  ) {
    // Fetch coordinate from stop, if not given in request
    if (location.getCoordinate() == null) {
      var stopVertex = graph.getStopVertex(location.stopId);
      if (stopVertex != null) {
        var c = stopVertex.toWgsCoordinate();
        location = new GenericLocation(
          location.label,
          location.stopId,
          c.latitude(),
          c.longitude()
        );
      } else {
        // For car routing, we use station's coordinate instead of child stops' if stop location is
        // a station.
        var coordinate = findStopLocationsGroupCentroid.apply(location.stopId);
        if (coordinate.isPresent()) {
          var c = coordinate.get();
          location = new GenericLocation(
            location.label,
            location.stopId,
            c.latitude(),
            c.longitude()
          );
        }
      }
    }
    return location.getCoordinate() != null
      ? Optional.of(
          vertexCreationService.createVertexFromCoordinate(
            container,
            location.getCoordinate(),
            location.label,
            List.of(TraverseMode.CAR),
            type
          )
        )
      : Optional.empty();
  }

  private void checkIfVerticesFound(
    GenericLocation from,
    Set<Vertex> fromVertices,
    @Nullable GenericLocation to,
    Set<Vertex> toVertices,
    List<GenericLocation> visitViaLocationsWithCoordinates,
    Map<GenericLocation, Set<Vertex>> visitViaLocationVertices
  ) {
    List<RoutingError> routingErrors = new ArrayList<>();

    // check that vertices where found if from-location was specified
    if (isDisconnected(fromVertices, LocationType.FROM)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(from), InputField.FROM_PLACE)
      );
    }

    // check that vertices where found if to-location was specified
    if (to != null && to.isSpecified() && isDisconnected(toVertices, LocationType.TO)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(to), InputField.TO_PLACE)
      );
    }

    // check that vertices where found if visit via locations with coordinates were specified
    if (!visitViaLocationsWithCoordinates.isEmpty()) {
      var errors = visitViaLocationVertices
        .entrySet()
        .stream()
        .filter(entry -> isDisconnected(entry.getValue(), LocationType.VISIT_VIA_LOCATION))
        .map(entry ->
          new RoutingError(
            getRoutingErrorCodeForDisconnected(entry.getKey()),
            InputField.INTERMEDIATE_PLACE
          )
        )
        .toList();
      routingErrors.addAll(errors);
    }

    // if from and to share any vertices, the user is already at their destination, and the result
    // is a trivial path
    if (!Sets.intersection(fromVertices, toVertices).isEmpty()) {
      routingErrors.add(new RoutingError(RoutingErrorCode.WALKING_BETTER_THAN_TRANSIT, null));
    }

    if (!routingErrors.isEmpty()) {
      throw new RoutingValidationException(routingErrors);
    }
  }

  private static boolean isDisconnected(Set<Vertex> vertices, LocationType type) {
    // Not connected if linking was not attempted, and vertices were not specified in the request.
    if (vertices.isEmpty()) {
      return true;
    }

    Predicate<Vertex> isNotTransit = Predicate.not(TransitStopVertex.class::isInstance);
    Predicate<Vertex> hasNoIncoming = v -> v.getIncoming().isEmpty();
    Predicate<Vertex> hasNoOutgoing = v -> v.getOutgoing().isEmpty();

    // Not connected if linking did not create incoming/outgoing edges depending on the
    // location type.
    Predicate<Vertex> isNotConnected = switch (type) {
      case FROM -> isNotTransit.and(hasNoOutgoing);
      case TO -> isNotTransit.and(hasNoIncoming);
      case VISIT_VIA_LOCATION -> hasNoIncoming.or(hasNoOutgoing);
    };

    return vertices.stream().allMatch(isNotTransit.and(isNotConnected));
  }

  private RoutingErrorCode getRoutingErrorCodeForDisconnected(GenericLocation location) {
    Coordinate coordinate = location.getCoordinate();
    GeometryFactory gf = GeometryUtils.getGeometryFactory();
    return coordinate != null && graph.getConvexHull().disjoint(gf.createPoint(coordinate))
      ? RoutingErrorCode.OUTSIDE_BOUNDS
      : RoutingErrorCode.LOCATION_NOT_FOUND;
  }

  private Set<TransitStopVertex> findStopOrChildStopVertices(FeedScopedId stopId) {
    return resolveSiteIds
      .apply(stopId)
      .stream()
      .flatMap(id -> graph.findStopVertex(id).stream())
      .collect(Collectors.toUnmodifiableSet());
  }
}
