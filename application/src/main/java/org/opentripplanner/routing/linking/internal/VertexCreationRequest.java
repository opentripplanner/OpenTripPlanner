package org.opentripplanner.routing.linking.internal;

import java.util.Objects;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.search.TraverseModeSet;

/**
 * Request for creating a temporary vertex that is connected to the graph.
 */
public class VertexCreationRequest {

  private final Coordinate coordinate;
  private final I18NString label;
  private final Set<TraverseModeSet> incomingModes;
  private final Set<TraverseModeSet> outgoingModes;

  /**
   * @param incomingModes For which modes should we generate incoming edges to the vertex. We try to
   *                      find links for each {@link TraverseModeSet} where the linked street needs
   *                      to be traversable with at least one of the modes in the mode set. Leave
   *                      the set empty if there is no need for incoming edges.
   * @param outgoingModes For which modes should we generate outgoing edges to the vertex. We try to
   *                      find links for each {@link TraverseModeSet} where the linked street needs
   *                      to be traversable with at least one of the modes in the mode set. Leave
   *                      the set empty if there is no need for outgoing edges.
   */
  public VertexCreationRequest(
    Coordinate coordinate,
    I18NString label,
    Set<TraverseModeSet> incomingModes,
    Set<TraverseModeSet> outgoingModes
  ) {
    this.coordinate = Objects.requireNonNull(coordinate);
    this.label = Objects.requireNonNull(label);
    this.incomingModes = Objects.requireNonNull(incomingModes);
    this.outgoingModes = Objects.requireNonNull(outgoingModes);
  }

  public Coordinate coordinate() {
    return coordinate;
  }

  public I18NString label() {
    return label;
  }

  /**
   * The incoming links to the created vertex should use these modes (we try to find links for each
   * {@link TraverseModeSet} where the linked street needs to be traversable with at least one of
   * the modes in the mode set). If the set is empty, no incoming links should be created.
   */
  public Set<TraverseModeSet> incomingModes() {
    return incomingModes;
  }

  /**
   * The outgoing links to the created vertex should use these modes (we try to find links for each
   * {@link TraverseModeSet} where the linked street needs to be traversable with at least one of
   * the modes in the mode set). If the set is empty, no outgoing links should be created.
   */
  public Set<TraverseModeSet> outgoingModes() {
    return outgoingModes;
  }
}
