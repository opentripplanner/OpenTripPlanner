package org.opentripplanner.routing.linking.internal;

import java.util.Set;
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
      request.incomingModes(),
      request.outgoingModes(),
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
    Set<TraverseModeSet> modes,
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
   * @param type       The location type (origin, destination, or via)
   * @return The traverse modes to use for linking. We don't need to find suitable links for all
   * modes in the set, just for one of them.
   */
  public static TraverseModeSet getTraverseModeForLinker(StreetMode streetMode, LocationType type) {
    var bikeParkAndRideDepart = streetMode == StreetMode.BIKE_TO_PARK && type == LocationType.FROM;
    var onlyBikeAvailable = streetMode == StreetMode.BIKE;
    if (onlyBikeAvailable || bikeParkAndRideDepart) {
      // We use the closest street(s) that are either traversable with WALK or BICYCLE, but don't
      // need to ensure we get links for both modes.
      return new TraverseModeSet(TraverseMode.WALK, TraverseMode.BICYCLE);
    }

    var flexArrival = streetMode == StreetMode.FLEXIBLE && type == LocationType.TO;
    if (
      flexArrival || streetMode == StreetMode.CAR_HAILING || streetMode == StreetMode.CAR_PICKUP
    ) {
      // Link to only the closest street(s) that are traversable with WALK or CAR (but not
      // necessarily with both)
      return new TraverseModeSet(TraverseMode.WALK, TraverseMode.CAR);
    }

    // for park and ride, we will start in car mode and walk to the end vertex
    boolean parkAndRideDepart = streetMode == StreetMode.CAR_TO_PARK && type == LocationType.FROM;
    boolean onlyCarAvailable = streetMode == StreetMode.CAR;
    if (onlyCarAvailable || parkAndRideDepart) {
      return new TraverseModeSet(TraverseMode.CAR);
    }
    return new TraverseModeSet(TraverseMode.WALK);
  }

  private static Set<TraverseModeSet> getIncomingModes(
    Set<TraverseModeSet> modes,
    LocationType type
  ) {
    return type == LocationType.TO || type == LocationType.VISIT_VIA_LOCATION ? modes : Set.of();
  }

  private static Set<TraverseModeSet> getOutgoingModes(
    Set<TraverseModeSet> modes,
    LocationType type
  ) {
    return type == LocationType.FROM || type == LocationType.VISIT_VIA_LOCATION ? modes : Set.of();
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
