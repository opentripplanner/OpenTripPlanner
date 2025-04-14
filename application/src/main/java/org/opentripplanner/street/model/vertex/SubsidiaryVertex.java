package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;

public class SubsidiaryVertex extends IntersectionVertex {

  private IntersectionVertex parent;
  private int counter;

  private static int counterSource = 0;

  public SubsidiaryVertex(IntersectionVertex parent) {
    super(
      parent.getX(),
      parent.getY(),
      parent.hasDrivingTrafficLight(),
      parent.hasWalkingTrafficLight()
    );
    this.parent = parent;
    this.counter = counterSource++;
  }

  @Override
  public I18NString getName() {
    return NO_NAME;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.SubsidiaryVertexLabel(parent.getLabel(), counter);
  }
}
