package org.opentripplanner.street.model.vertex;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * A vertex representing a station centroid. This can be used as a source/destination for routing.
 */
public class StationCentroidVertex extends Vertex {

  private final FeedScopedId id;
  private final I18NString name;

  public StationCentroidVertex(FeedScopedId id, I18NString name, WgsCoordinate coordinate) {
    super(coordinate.longitude(), coordinate.latitude());
    this.id = id;
    this.name = name;
  }

  public FeedScopedId getId() {
    return this.id;
  }

  @Override
  public I18NString getName() {
    return name;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.feedScopedId(id);
  }
}
