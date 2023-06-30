package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.site.StationElement;

public abstract class StationElementVertex extends Vertex {

  protected StationElementVertex(String label, double x, double y, I18NString name) {
    super(label, x, y, name);
  }

  /** Get the corresponding StationElement */
  @Nonnull
  public abstract StationElement getStationElement();
}
