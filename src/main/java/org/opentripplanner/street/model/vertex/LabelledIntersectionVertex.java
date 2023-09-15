package org.opentripplanner.street.model.vertex;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.I18NString;

/**
 * An intersection vertex that has a label that is generated outside of it rather than
 * derived from its properties.
 */
public class LabelledIntersectionVertex extends IntersectionVertex {

  private final String label;

  public LabelledIntersectionVertex(
    @Nonnull String label,
    double x,
    double y,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(x, y, hasHighwayTrafficLight, hasCrossingTrafficLight);
    this.label = label;
  }

  @Override
  public VertexLabel getLabel() {
    return VertexLabel.string(label);
  }

  @Nonnull
  @Override
  public I18NString getName() {
    return I18NString.of(label);
  }
}
