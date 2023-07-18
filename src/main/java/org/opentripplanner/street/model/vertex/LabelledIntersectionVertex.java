package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * An intersection vertex that has a label that is generated outside of it rather than
 * derived from its properties.
 */
public class LabelledIntersectionVertex extends IntersectionVertex {

  private final VertexLabel label;

  public LabelledIntersectionVertex(
    @Nonnull String label,
    double x,
    double y,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(x, y, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.label = VertexLabel.string(label);
  }

  @Override
  public VertexLabel getLabel() {
    return label;
  }
}
