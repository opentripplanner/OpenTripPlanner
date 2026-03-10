package org.opentripplanner.routing.linking.internal;

import java.util.List;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.utils.lang.StringUtils;
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
   * @param request   Contains the required information for creating the vertex and linking it to
   *                  the graph.
   * @return The created vertex
   */
  public Vertex createVertexFromCoordinate(
    TemporaryVerticesContainer container,
    VertexCreationRequest request
  ) {
    var temporaryStreetLocation = new TemporaryStreetLocation(
      request.coordinate(),
      request.label()
    );

    var disposableEdgeCollection = vertexLinker.linkVertexForRequest(
      temporaryStreetLocation,
      new TraverseModeSet(request.incomingModes()),
      new TraverseModeSet(request.outgoingModes()),
      TemporaryFreeEdge::createTemporaryFreeEdge
    );
    container.addEdgeCollection(disposableEdgeCollection);

    if (
      temporaryStreetLocation.getIncoming().isEmpty() &&
      temporaryStreetLocation.getOutgoing().isEmpty()
    ) {
      LOG.warn("Couldn't link {}", request.coordinate());
    }

    return temporaryStreetLocation;
  }

  public static VertexCreationRequest createVertexCreationRequest(
    GenericLocation location,
    List<TraverseMode> modes,
    LocationType type
  ) {
    var incomingModes = getIncomingModes(modes, type);
    var outgoingModes = getOutgoingModes(modes, type);
    var label = StringUtils.hasValue(location.label)
      ? new NonLocalizedString(location.label)
      : new LocalizedString(type.translationKey());
    return new VertexCreationRequest(location.getCoordinate(), label, incomingModes, outgoingModes);
  }

  /**
   * Maps a street mode to the appropriate traverse modes for vertex linking.
   *
   * @param streetMode The street mode from the request
   * @param type The location type (origin, destination, or via)
   * @return The traverse modes to use for linking
   */
  public static List<TraverseMode> getTraverseModeForLinker(
    StreetMode streetMode,
    LocationType type
  ) {
    // TODO we should try to link to cycling-only ways with BICYCLE modes if they are the closest

    // arrival and departure is allowed with either car or walk (not 100% sure about departure with
    // flex).
    if (
      streetMode == StreetMode.FLEXIBLE ||
      streetMode == StreetMode.CAR_HAILING ||
      streetMode == StreetMode.CAR_PICKUP
    ) {
      return List.of(TraverseMode.WALK, TraverseMode.CAR);
    }
    // for park and ride, we will start in car mode and walk to the end vertex
    boolean parkAndRideDepart = streetMode == StreetMode.CAR_TO_PARK && type == LocationType.FROM;
    boolean onlyCarAvailable = streetMode == StreetMode.CAR;
    if (onlyCarAvailable || parkAndRideDepart) {
      return List.of(TraverseMode.CAR);
    }
    return List.of(TraverseMode.WALK);
  }

  private static List<TraverseMode> getIncomingModes(List<TraverseMode> modes, LocationType type) {
    return type == LocationType.TO || type == LocationType.VISIT_VIA_LOCATION ? modes : List.of();
  }

  private static List<TraverseMode> getOutgoingModes(List<TraverseMode> modes, LocationType type) {
    return type == LocationType.FROM || type == LocationType.VISIT_VIA_LOCATION ? modes : List.of();
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
