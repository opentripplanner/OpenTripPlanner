package org.opentripplanner.routing.linking.internal;

import java.util.List;
import java.util.Objects;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Request for creating a temporary vertex that is connected to the graph.
 */
public class VertexCreationRequest {

  private final Coordinate coordinate;
  private final I18NString label;
  private final List<TraverseMode> incomingModes;
  private final List<TraverseMode> outgoingModes;

  /**
   * @param incomingModes For which modes should we generate incoming edges to the vertex. Leave it
   *                      as empty if there is no need for incoming edges.
   * @param outgoingModes For which modes should we generate outgoing edges to the vertex. Leave it
   *                      as empty if there is no need for outgoing edges.
   */
  public VertexCreationRequest(
    Coordinate coordinate,
    I18NString label,
    List<TraverseMode> incomingModes,
    List<TraverseMode> outgoingModes
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
   * The incoming links to the created vertex should use these modes. If the list is empty, no
   * incoming links should be created.
   */
  public List<TraverseMode> incomingModes() {
    return incomingModes;
  }

  /**
   * The outgoing links to the created vertex should use these modes. If the list is empty, no
   * outgoing links should be created.
   */
  public List<TraverseMode> outgoingModes() {
    return outgoingModes;
  }
}
