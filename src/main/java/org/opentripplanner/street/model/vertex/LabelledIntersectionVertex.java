package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

public class LabelledIntersectionVertex extends IntersectionVertex {

  private final VertexLabel label;

  public LabelledIntersectionVertex(
    @Nonnull String label,
    double x,
    double y,
    @Nullable I18NString name,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(x, y, name, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.label = VertexLabel.string(label);
  }

  @Override
  public VertexLabel getLabel() {
    return label;
  }
}
