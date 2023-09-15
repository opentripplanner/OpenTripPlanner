package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;

public class SimpleVertex extends StreetVertex {

  private final String label;

  public SimpleVertex(String label, double lat, double lon) {
    super(lon, lat);
    this.label = label;
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return I18NString.of(label);
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(label);
  }
}
