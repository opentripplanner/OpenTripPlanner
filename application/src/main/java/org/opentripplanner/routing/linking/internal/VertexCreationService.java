package org.opentripplanner.routing.linking.internal;

import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.linking.TemporaryVerticesContainer;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for creating temporary vertices from coordinates and linking them to the
 * graph for use in A-Star searches.
 */
public class VertexCreationService {

  private static final Logger LOG = LoggerFactory.getLogger(VertexCreationService.class);

  private final VertexLinker vertexLinker;

  public VertexCreationService(VertexLinker vertexLinker) {
    this.vertexLinker = vertexLinker;
  }

  /**
   * Creates a temporary vertex from a coordinate and links it to the graph.
   *
   * @param container The container to add the temporary edges to
   * @param coordinate The coordinate where the vertex should be created
   * @param label Optional label for the vertex
   * @param modes The traverse modes that should be available from this vertex
   * @param type The type of location (origin, destination, or via)
   * @return The created vertex
   */
  public Vertex createVertexFromCoordinate(
    TemporaryVerticesContainer container,
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

    var disposableEdgeCollection = vertexLinker.linkVertexForRequest(
      temporaryStreetLocation,
      new TraverseModeSet(modes),
      mapDirection(type),
      (vertex, streetVertex) -> createEdges((TemporaryStreetLocation) vertex, streetVertex, type)
    );
    container.addEdgeCollection(disposableEdgeCollection);

    if (
      temporaryStreetLocation.getIncoming().isEmpty() &&
      temporaryStreetLocation.getOutgoing().isEmpty()
    ) {
      LOG.warn("Couldn't link {}", coordinate);
    }

    temporaryStreetLocation.setWheelchairAccessible(true);

    return temporaryStreetLocation;
  }

  /**
   * Maps a street mode to the appropriate traverse mode for vertex linking.
   *
   * @param streetMode The street mode from the request
   * @param type The location type (origin, destination, or via)
   * @return The traverse mode to use for linking
   */
  public TraverseMode getTraverseModeForLinker(StreetMode streetMode, LocationType type) {
    TraverseMode nonTransitMode = TraverseMode.WALK;
    // for park and ride we will start in car mode and walk to the end vertex
    boolean parkAndRideDepart = streetMode == StreetMode.CAR_TO_PARK && type == LocationType.FROM;
    boolean onlyCarAvailable = streetMode == StreetMode.CAR;
    if (onlyCarAvailable || parkAndRideDepart) {
      nonTransitMode = TraverseMode.CAR;
    }
    return nonTransitMode;
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

  /**
   * Represents the type of location being linked: origin (FROM), destination (TO), or an
   * intermediate via point (VISIT_VIA_LOCATION).
   */
  public enum LocationType {
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
