package org.opentripplanner.street.search;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for linking the RouteRequest origin, destination and visit via
 * locations that contain coordinates to the Graph used in the A-Star search. This builder also
 * validates that it was possible to link the locations to the graph. The responsibility of cleaning
 * up the temporary vertices and edges is on the {@link TemporaryVerticesContainer}.
 */
public class TemporaryVerticesContainerBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(
    TemporaryVerticesContainerBuilder.class
  );

  private final Graph graph;
  private final VertexLinker vertexLinker;
  private final Set<DisposableEdgeCollection> tempEdges = new HashSet<>();
  private GenericLocation from = GenericLocation.UNKNOWN;
  private GenericLocation to = GenericLocation.UNKNOWN;
  private List<GenericLocation> visitViaLocations = List.of();
  private Set<Vertex> fromVertices = Set.of();
  private Set<Vertex> toVertices = Set.of();
  private Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices = Map.of();
  private Set<TransitStopVertex> fromStopVertices = Set.of();
  private Set<TransitStopVertex> toStopVertices = Set.of();

  TemporaryVerticesContainerBuilder(Graph graph) {
    this.graph = graph;
    this.vertexLinker = graph.getLinker();
  }

  public TemporaryVerticesContainerBuilder withFrom(GenericLocation location, StreetMode mode) {
    return withFrom(location, EnumSet.of(mode));
  }

  public TemporaryVerticesContainerBuilder withFrom(
    GenericLocation location,
    EnumSet<StreetMode> modes
  ) {
    this.from = location;
    this.fromVertices = getStreetVerticesForLocation(location, modes, LocationType.FROM);
    if (location.stopId != null) {
      this.fromStopVertices = graph.findStopOrChildStopsVertices(location.stopId);
    }
    return this;
  }

  public Set<Vertex> fromVertices() {
    return fromVertices;
  }

  public Set<TransitStopVertex> fromStopVertices() {
    return fromStopVertices;
  }

  public TemporaryVerticesContainerBuilder withTo(GenericLocation location, StreetMode mode) {
    return withTo(location, EnumSet.of(mode));
  }

  public TemporaryVerticesContainerBuilder withTo(
    GenericLocation location,
    EnumSet<StreetMode> modes
  ) {
    this.to = location;
    this.toVertices = getStreetVerticesForLocation(to, modes, LocationType.TO);
    if (location.stopId != null) {
      this.toStopVertices = graph.findStopOrChildStopsVertices(location.stopId);
    }
    return this;
  }

  public Set<Vertex> toVertices() {
    return toVertices;
  }

  public Set<TransitStopVertex> toStopVertices() {
    return toStopVertices;
  }

  public TemporaryVerticesContainerBuilder withVia(
    List<VisitViaLocation> visitViaLocations,
    EnumSet<StreetMode> modes
  ) {
    var visitViaLocationsWithCoordinates = visitViaLocations
      .stream()
      .filter(location -> location.coordinateLocation() != null)
      .toList();
    this.visitViaLocations = visitViaLocationsWithCoordinates
      .stream()
      .map(VisitViaLocation::coordinateLocation)
      .toList();
    this.visitViaLocationVertices = visitViaLocationsWithCoordinates
      .stream()
      .collect(
        Collectors.toMap(
          location -> location,
          location ->
            getStreetVerticesForLocation(
              location.coordinateLocation(),
              modes,
              LocationType.VISIT_VIA_LOCATION
            )
        )
      );
    return this;
  }

  public Map<VisitViaLocation, Set<Vertex>> visitViaLocationVertices() {
    return visitViaLocationVertices;
  }

  public Set<DisposableEdgeCollection> tempEdges() {
    return tempEdges;
  }

  public TemporaryVerticesContainer build() {
    checkIfVerticesFound();
    addAdjustedEdges();
    return new TemporaryVerticesContainer(this);
  }

  private void addAdjustedEdges() {
    for (Vertex fromVertex : fromVertices) {
      for (Vertex toVertex : toVertices) {
        tempEdges.add(SameEdgeAdjuster.adjust(fromVertex, toVertex, graph));
      }
    }
    var viaVertices = visitViaLocationVertices.values().stream().flatMap(Set::stream).toList();
    for (Vertex fromVertex : fromVertices) {
      for (Vertex viaVertex : viaVertices) {
        tempEdges.add(SameEdgeAdjuster.adjust(fromVertex, viaVertex, graph));
      }
    }
    for (Vertex toVertex : toVertices) {
      for (Vertex viaVertex : viaVertices) {
        tempEdges.add(SameEdgeAdjuster.adjust(viaVertex, toVertex, graph));
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
    if (!modes.stream().allMatch(TraverseMode::isInCar)) {
      // Check if Stop/StopCollection is found by FeedScopeId
      if (location.stopId != null) {
        var streetVertices = graph.findStopVertices(location.stopId);
        if (!streetVertices.isEmpty()) {
          results.addAll(streetVertices);
        }
      }
    }

    if (modes.stream().anyMatch(TraverseMode::isInCar)) {
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
    }

    // Check if coordinate is provided and connect it to graph
    if (location.getCoordinate() != null) {
      results.add(
        createVertexFromCoordinate(location.getCoordinate(), location.label, modes, type, tempEdges)
      );
    }

    return results;
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
    LocationType type,
    Set<DisposableEdgeCollection> tempEdges
  ) {
    LOG.debug("Creating {} vertex for {}", type.description(), coordinate);

    I18NString name = label == null || label.isEmpty()
      ? new LocalizedString(type.translationKey())
      : new NonLocalizedString(label);

    var temporaryStreetLocation = new TemporaryStreetLocation(coordinate, name);

    tempEdges.add(
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
    if (!visitViaLocations.isEmpty()) {
      var errors = visitViaLocationVertices
        .entrySet()
        .stream()
        .filter(entry -> isDisconnected(entry.getValue(), LocationType.VISIT_VIA_LOCATION))
        .map(entry ->
          new RoutingError(
            getRoutingErrorCodeForDisconnected(entry.getKey().coordinateLocation()),
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
