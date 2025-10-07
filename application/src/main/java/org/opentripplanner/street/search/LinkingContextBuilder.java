package org.opentripplanner.street.search;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.SameEdgeAdjuster;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for linking the RouteRequest origin, destination and visit via
 * locations that contain coordinates to the Graph used in the A-Star search. This builder also
 * validates that it was possible to link the locations to the graph. The responsibility of cleaning
 * up the temporary vertices and edges is on the {@link TemporaryVerticesContainer}.
 */
public class LinkingContextBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(LinkingContextBuilder.class);

  private final TemporaryVerticesContainer container;
  private final Graph graph;
  private final VertexLinker vertexLinker;
  private final Function<FeedScopedId, Collection<FeedScopedId>> resolveSiteIds;
  private final List<DisposableEdgeCollection> tempEdges = new ArrayList<>();
  private GenericLocation from = GenericLocation.UNKNOWN;
  private GenericLocation to = GenericLocation.UNKNOWN;
  private List<GenericLocation> visitViaLocationsWithCoordinates = List.of();
  private Set<Vertex> fromVertices = Set.of();
  private Set<Vertex> toVertices = Set.of();
  private Map<GenericLocation, Set<Vertex>> visitViaLocationVertices = Map.of();
  private Set<TransitStopVertex> fromStopVertices = Set.of();
  private Set<TransitStopVertex> toStopVertices = Set.of();

  LinkingContextBuilder(
    TemporaryVerticesContainer container,
    Graph graph,
    VertexLinker vertexLinker,
    Function<FeedScopedId, Collection<FeedScopedId>> resolveSiteIds
  ) {
    this.container = container;
    this.graph = graph;
    this.vertexLinker = vertexLinker;
    this.resolveSiteIds = resolveSiteIds;
  }

  public LinkingContextBuilder withFrom(GenericLocation location, StreetMode mode) {
    return withFrom(location, EnumSet.of(mode));
  }

  public LinkingContextBuilder withFrom(GenericLocation location, EnumSet<StreetMode> modes) {
    this.from = location;
    this.fromVertices = getStreetVerticesForLocation(location, modes, LocationType.FROM);
    if (location.stopId != null) {
      this.fromStopVertices = findStopOrChildStopVertices(location.stopId);
    }
    return this;
  }

  public GenericLocation from() {
    return from;
  }

  public Set<TransitStopVertex> fromStopVertices() {
    return fromStopVertices;
  }

  public LinkingContextBuilder withTo(GenericLocation location, StreetMode mode) {
    return withTo(location, EnumSet.of(mode));
  }

  public LinkingContextBuilder withTo(GenericLocation location, EnumSet<StreetMode> modes) {
    this.to = location;
    this.toVertices = getStreetVerticesForLocation(to, modes, LocationType.TO);
    if (location.stopId != null) {
      this.toStopVertices = findStopOrChildStopVertices(location.stopId);
    }
    return this;
  }

  public GenericLocation to() {
    return to;
  }

  public Set<TransitStopVertex> toStopVertices() {
    return toStopVertices;
  }

  public LinkingContextBuilder withVia(
    List<VisitViaLocation> visitViaLocations,
    EnumSet<StreetMode> modes
  ) {
    var visitViaLocationsWithCoordinates = visitViaLocations
      .stream()
      .map(VisitViaLocation::coordinateLocation)
      .filter(Objects::nonNull)
      .toList();
    if (visitViaLocationsWithCoordinates.isEmpty()) {
      return this;
    }
    this.visitViaLocationsWithCoordinates = visitViaLocationsWithCoordinates;
    this.visitViaLocationVertices = visitViaLocationsWithCoordinates
      .stream()
      .collect(
        Collectors.toMap(
          location -> location,
          location -> getStreetVerticesForLocation(location, modes, LocationType.VISIT_VIA_LOCATION)
        )
      );
    return this;
  }

  public Map<GenericLocation, Set<Vertex>> verticesByLocation() {
    var verticesByLocation = new HashMap<GenericLocation, Set<Vertex>>();
    verticesByLocation.put(from, fromVertices);
    verticesByLocation.put(to, toVertices);
    verticesByLocation.putAll(visitViaLocationVertices);
    return Collections.unmodifiableMap(verticesByLocation);
  }

  public LinkingContext build() {
    checkIfVerticesFound();
    addAdjustedEdges();
    return new LinkingContext(this);
  }

  private void addAdjustedEdges() {
    addAdjustedEdgesBetween(fromVertices, toVertices);
    if (visitViaLocationsWithCoordinates.isEmpty()) {
      return;
    }
    addAdjustedEdgesBetween(
      fromVertices,
      visitViaLocationVertices.get(visitViaLocationsWithCoordinates.getFirst())
    );
    addAdjustedEdgesBetween(
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
      addAdjustedEdgesBetween(fromViaVertices, toViaVertices);
      i++;
    }
  }

  private void addAdjustedEdgesBetween(Set<Vertex> fromVertices, Set<Vertex> toVertices) {
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
      .map(streetMode -> getTraverseModeForLinker(streetMode, type))
      .distinct()
      .toList();

    var results = new HashSet<Vertex>();
    if (location.stopId != null) {
      if (!modes.stream().allMatch(TraverseMode::isInCar)) {
        results.addAll(getStreetVerticesForStop(location));
      }
      if (modes.stream().anyMatch(TraverseMode::isInCar)) {
        // Ensure that there is a car routable vertex (that can originate from stop's coordinate).
        var carRoutableVertex = getCarRoutableStreetVertex(location, type);
        carRoutableVertex.ifPresent(results::add);
      }
    } else if (location.getCoordinate() != null) {
      // Connect a temporary vertex from coordinate to graph
      results.add(
        createVertexFromCoordinate(location.getCoordinate(), location.label, modes, type)
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
  private Optional<Vertex> getCarRoutableStreetVertex(GenericLocation location, LocationType type) {
    // Fetch coordinate from stop, if not given in request
    if (location.getCoordinate() == null) {
      var stopVertex = graph.getStopVertex(location.stopId);
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
    return location.getCoordinate() != null
      ? Optional.of(
        createVertexFromCoordinate(
          location.getCoordinate(),
          location.label,
          List.of(TraverseMode.CAR),
          type
        )
      )
      : Optional.empty();
  }

  private TraverseMode getTraverseModeForLinker(StreetMode streetMode, LocationType type) {
    TraverseMode nonTransitMode = TraverseMode.WALK;
    // for park and ride we will start in car mode and walk to the end vertex
    boolean parkAndRideDepart = streetMode == StreetMode.CAR_TO_PARK && type == LocationType.FROM;
    boolean onlyCarAvailable = streetMode == StreetMode.CAR;
    if (onlyCarAvailable || parkAndRideDepart) {
      nonTransitMode = TraverseMode.CAR;
    }
    return nonTransitMode;
  }

  private Vertex createVertexFromCoordinate(
    Coordinate coordinate,
    @Nullable String label,
    List<TraverseMode> modes,
    LocationType type
  ) {
    LOG.debug("Creating {} vertex for {}", type.description(), coordinate);

    I18NString name = label == null || label.isEmpty()
      ? new LocalizedString(type.translationKey())
      : new NonLocalizedString(label);

    var temporaryStreetLocation = new TemporaryStreetLocation(coordinate, name);

    container.addEdgeCollection(
      vertexLinker.linkVertexForRequest(
        temporaryStreetLocation,
        new TraverseModeSet(modes),
        mapDirection(type),
        (vertex, streetVertex) -> createEdges((TemporaryStreetLocation) vertex, streetVertex, type)
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
    if (from.isSpecified() && isDisconnected(fromVertices, LocationType.FROM)) {
      routingErrors.add(
        new RoutingError(getRoutingErrorCodeForDisconnected(from), InputField.FROM_PLACE)
      );
    }

    // check that vertices where found if to-location was specified
    if (to.isSpecified() && isDisconnected(toVertices, LocationType.TO)) {
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
    Predicate<Vertex> isNotConnected =
      switch (type) {
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

  private LinkingDirection mapDirection(LocationType type) {
    return switch (type) {
      case FROM -> LinkingDirection.INCOMING;
      case TO -> LinkingDirection.OUTGOING;
      case VISIT_VIA_LOCATION -> LinkingDirection.BIDIRECTIONAL;
    };
  }

  private List<Edge> createEdges(
    TemporaryStreetLocation location,
    StreetVertex streetVertex,
    LocationType type
  ) {
    return switch (type) {
      case FROM -> List.of(TemporaryFreeEdge.createTemporaryFreeEdge(location, streetVertex));
      case TO -> List.of(TemporaryFreeEdge.createTemporaryFreeEdge(streetVertex, location));
      case VISIT_VIA_LOCATION -> List.of(
        TemporaryFreeEdge.createTemporaryFreeEdge(location, streetVertex),
        TemporaryFreeEdge.createTemporaryFreeEdge(streetVertex, location)
      );
    };
  }

  private Set<TransitStopVertex> findStopOrChildStopVertices(FeedScopedId stopId) {
    return resolveSiteIds
      .apply(stopId)
      .stream()
      .flatMap(id -> graph.findStopVertex(id).stream())
      .collect(Collectors.toUnmodifiableSet());
  }

  private enum LocationType {
    FROM("origin"),
    TO("destination"),
    VISIT_VIA_LOCATION("visit via location", "via_location");

    private final String description;
    private final String translationKey;

    LocationType(String description) {
      this(description, description);
    }

    LocationType(String description, String translationKey) {
      this.description = description;
      this.translationKey = translationKey;
    }

    public String description() {
      return description;
    }

    public String translationKey() {
      return translationKey;
    }
  }
}
