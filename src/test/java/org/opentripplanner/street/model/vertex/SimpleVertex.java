package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.NonLocalizedString;

public class SimpleVertex extends StreetVertex {

  public SimpleVertex(String label, double lat, double lon) {
    super(lon, lat, new NonLocalizedString(label));
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string("%s_%s".formatted(getX(), getY()));
  }
}
