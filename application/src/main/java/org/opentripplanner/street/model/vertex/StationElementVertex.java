package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StationElement;

public abstract class StationElementVertex extends Vertex {

  private final FeedScopedId id;
  private final I18NString name;

  protected StationElementVertex(FeedScopedId id, double x, double y, I18NString name) {
    super(x, y);
    this.id = id;
    this.name = name;
  }

  @Override
  public final VertexLabel getLabel() {
    return VertexLabel.feedScopedId(id);
  }

  /** Get the corresponding StationElement */
  public abstract StationElement getStationElement();

  @Override
  public I18NString getName() {
    return name;
  }

  public FeedScopedId getId() {
    return id;
  }
}
