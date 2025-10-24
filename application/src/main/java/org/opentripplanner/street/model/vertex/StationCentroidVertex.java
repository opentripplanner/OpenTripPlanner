package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A vertex representing a station centroid. This can be used as a source/destination for routing.
 */
public class StationCentroidVertex extends Vertex {

  private final FeedScopedId id;

  public StationCentroidVertex(FeedScopedId id, WgsCoordinate coordinate) {
    super(coordinate.longitude(), coordinate.latitude());
    this.id = id;
  }

  public FeedScopedId getId() {
    return this.id;
  }

  @Override
  public I18NString getName() {
    return I18NString.of(id.getId());
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.feedScopedId(id);
  }
}
